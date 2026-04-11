package com.wheeloffate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttRelay
{
	private static final String BROKER_URI = "ws://broker.hivemq.com:8000/mqtt";
	private static final String TOPIC_PREFIX = "wheeloffate/";
	private static final int KEEP_ALIVE_SECONDS = 30;

	private final Consumer<String> onMessage;
	private final Runnable onConnected;
	private final Runnable onDisconnected;

	private WebSocket webSocket;
	private String topic;
	private String clientId;
	private volatile boolean connected;
	private ScheduledExecutorService pingScheduler;
	private ScheduledFuture<?> pingTask;
	private ByteBuffer accumulatedData = ByteBuffer.allocate(0);

	public MqttRelay(Consumer<String> onMessage, Runnable onConnected, Runnable onDisconnected)
	{
		this.onMessage = onMessage;
		this.onConnected = onConnected;
		this.onDisconnected = onDisconnected;
		this.clientId = "wof-" + UUID.randomUUID().toString().substring(0, 8);
	}

	public CompletableFuture<Void> connect(String roomCode)
	{
		this.topic = TOPIC_PREFIX + roomCode;

		return HttpClient.newHttpClient()
			.newWebSocketBuilder()
			.subprotocols("mqtt")
			.buildAsync(URI.create(BROKER_URI), new WebSocket.Listener()
			{
				@Override
				public void onOpen(WebSocket ws)
				{
					log.info("WebSocket connected to broker");
					webSocket = ws;
					sendMqttConnect();
					ws.request(1);
				}

				@Override
				public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last)
				{
					// Accumulate data if message is fragmented
					ByteBuffer combined;
					if (accumulatedData.remaining() > 0)
					{
						combined = ByteBuffer.allocate(accumulatedData.remaining() + data.remaining());
						combined.put(accumulatedData);
						combined.put(data);
						combined.flip();
					}
					else
					{
						combined = data;
					}

					if (!last)
					{
						accumulatedData = ByteBuffer.allocate(combined.remaining());
						accumulatedData.put(combined);
						accumulatedData.flip();
						ws.request(1);
						return null;
					}

					accumulatedData = ByteBuffer.allocate(0);
					handleMqttPacket(combined);
					ws.request(1);
					return null;
				}

				@Override
				public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason)
				{
					log.info("WebSocket closed: {} {}", statusCode, reason);
					connected = false;
					stopPing();
					if (onDisconnected != null)
					{
						onDisconnected.run();
					}
					return null;
				}

				@Override
				public void onError(WebSocket ws, Throwable error)
				{
					log.error("WebSocket error", error);
					connected = false;
					stopPing();
					if (onDisconnected != null)
					{
						onDisconnected.run();
					}
				}
			})
			.thenAccept(ws -> webSocket = ws);
	}

	public void publish(String message)
	{
		if (!connected || webSocket == null)
		{
			return;
		}
		sendMqttPublish(topic, message);
	}

	public void disconnect()
	{
		connected = false;
		stopPing();
		if (webSocket != null)
		{
			try
			{
				// MQTT DISCONNECT packet
				webSocket.sendBinary(ByteBuffer.wrap(new byte[]{(byte) 0xE0, 0x00}), true);
				webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
			}
			catch (Exception ignored)
			{
			}
			webSocket = null;
		}
	}

	public boolean isConnected()
	{
		return connected;
	}

	// === MQTT Packet Construction ===

	private void sendMqttConnect()
	{
		byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);

		// Variable header: protocol name "MQTT", level 4, flags, keep alive
		byte[] variableHeader = {
			0x00, 0x04, 'M', 'Q', 'T', 'T', // Protocol name
			0x04,                              // Protocol level (MQTT 3.1.1)
			0x02,                              // Connect flags (clean session)
			(byte) (KEEP_ALIVE_SECONDS >> 8),  // Keep alive MSB
			(byte) (KEEP_ALIVE_SECONDS & 0xFF) // Keep alive LSB
		};

		// Payload: client ID
		byte[] payload = new byte[2 + clientIdBytes.length];
		payload[0] = (byte) (clientIdBytes.length >> 8);
		payload[1] = (byte) (clientIdBytes.length & 0xFF);
		System.arraycopy(clientIdBytes, 0, payload, 2, clientIdBytes.length);

		int remainingLength = variableHeader.length + payload.length;
		byte[] packet = buildPacket(0x10, variableHeader, payload, remainingLength);
		webSocket.sendBinary(ByteBuffer.wrap(packet), true);
	}

	private void sendMqttSubscribe(String topicFilter)
	{
		byte[] topicBytes = topicFilter.getBytes(StandardCharsets.UTF_8);

		// Variable header: packet identifier
		byte[] variableHeader = {0x00, 0x01};

		// Payload: topic filter + QoS 0
		byte[] payload = new byte[2 + topicBytes.length + 1];
		payload[0] = (byte) (topicBytes.length >> 8);
		payload[1] = (byte) (topicBytes.length & 0xFF);
		System.arraycopy(topicBytes, 0, payload, 2, topicBytes.length);
		payload[payload.length - 1] = 0x00; // QoS 0

		int remainingLength = variableHeader.length + payload.length;
		byte[] packet = buildPacket(0x82, variableHeader, payload, remainingLength);
		webSocket.sendBinary(ByteBuffer.wrap(packet), true);
	}

	private void sendMqttPublish(String topicName, String message)
	{
		byte[] topicBytes = topicName.getBytes(StandardCharsets.UTF_8);
		byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

		// Variable header: topic name
		byte[] variableHeader = new byte[2 + topicBytes.length];
		variableHeader[0] = (byte) (topicBytes.length >> 8);
		variableHeader[1] = (byte) (topicBytes.length & 0xFF);
		System.arraycopy(topicBytes, 0, variableHeader, 2, topicBytes.length);

		int remainingLength = variableHeader.length + messageBytes.length;
		byte[] packet = buildPacket(0x30, variableHeader, messageBytes, remainingLength);
		webSocket.sendBinary(ByteBuffer.wrap(packet), true);
	}

	private void sendPing()
	{
		if (webSocket != null && connected)
		{
			webSocket.sendBinary(ByteBuffer.wrap(new byte[]{(byte) 0xC0, 0x00}), true);
		}
	}

	private byte[] buildPacket(int type, byte[] variableHeader, byte[] payload, int remainingLength)
	{
		byte[] lengthBytes = encodeRemainingLength(remainingLength);
		byte[] packet = new byte[1 + lengthBytes.length + variableHeader.length + payload.length];
		packet[0] = (byte) type;
		System.arraycopy(lengthBytes, 0, packet, 1, lengthBytes.length);
		System.arraycopy(variableHeader, 0, packet, 1 + lengthBytes.length, variableHeader.length);
		System.arraycopy(payload, 0, packet, 1 + lengthBytes.length + variableHeader.length, payload.length);
		return packet;
	}

	private byte[] encodeRemainingLength(int length)
	{
		byte[] encoded = new byte[4];
		int idx = 0;
		do
		{
			byte b = (byte) (length % 128);
			length /= 128;
			if (length > 0)
			{
				b |= 0x80;
			}
			encoded[idx++] = b;
		}
		while (length > 0);

		byte[] result = new byte[idx];
		System.arraycopy(encoded, 0, result, 0, idx);
		return result;
	}

	// === MQTT Packet Parsing ===

	private void handleMqttPacket(ByteBuffer buffer)
	{
		while (buffer.hasRemaining())
		{
			int startPos = buffer.position();
			if (buffer.remaining() < 2)
			{
				break;
			}

			int firstByte = buffer.get() & 0xFF;
			int packetType = firstByte >> 4;

			// Decode remaining length
			int remainingLength = 0;
			int multiplier = 1;
			byte encoded;
			int lengthBytes = 0;
			do
			{
				if (!buffer.hasRemaining())
				{
					buffer.position(startPos);
					return;
				}
				encoded = buffer.get();
				lengthBytes++;
				remainingLength += (encoded & 0x7F) * multiplier;
				multiplier *= 128;
			}
			while ((encoded & 0x80) != 0 && lengthBytes < 4);

			if (buffer.remaining() < remainingLength)
			{
				buffer.position(startPos);
				return;
			}

			switch (packetType)
			{
				case 2: // CONNACK
					handleConnAck(buffer, remainingLength);
					break;
				case 3: // PUBLISH
					handlePublish(buffer, remainingLength, firstByte);
					break;
				case 9: // SUBACK
					buffer.position(buffer.position() + remainingLength);
					log.info("Subscribed to topic: {}", topic);
					break;
				case 13: // PINGRESP
					break;
				default:
					buffer.position(buffer.position() + remainingLength);
					break;
			}
		}
	}

	private void handleConnAck(ByteBuffer buffer, int remainingLength)
	{
		if (remainingLength >= 2)
		{
			buffer.get(); // session present
			int returnCode = buffer.get() & 0xFF;

			if (returnCode == 0)
			{
				connected = true;
				log.info("MQTT connected, subscribing to {}", topic);
				sendMqttSubscribe(topic);
				startPing();
				if (onConnected != null)
				{
					onConnected.run();
				}
			}
			else
			{
				log.error("MQTT connect failed with code {}", returnCode);
			}
		}
		else
		{
			buffer.position(buffer.position() + remainingLength);
		}
	}

	private void handlePublish(ByteBuffer buffer, int remainingLength, int firstByte)
	{
		int startPos = buffer.position();

		// Topic length
		int topicLen = ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
		byte[] topicBytes = new byte[topicLen];
		buffer.get(topicBytes);

		int consumed = 2 + topicLen;

		// QoS > 0 has packet identifier (we use QoS 0 so skip)
		int qos = (firstByte >> 1) & 0x03;
		if (qos > 0)
		{
			buffer.getShort(); // packet identifier
			consumed += 2;
		}

		// Payload
		int payloadLen = remainingLength - consumed;
		if (payloadLen > 0)
		{
			byte[] payloadBytes = new byte[payloadLen];
			buffer.get(payloadBytes);
			String payload = new String(payloadBytes, StandardCharsets.UTF_8);

			if (onMessage != null)
			{
				onMessage.accept(payload);
			}
		}
		else
		{
			buffer.position(startPos + remainingLength);
		}
	}

	private void startPing()
	{
		pingScheduler = new ScheduledThreadPoolExecutor(1, r ->
		{
			Thread t = new Thread(r, "WheelSync-Ping");
			t.setDaemon(true);
			return t;
		});
		pingTask = pingScheduler.scheduleAtFixedRate(
			this::sendPing, KEEP_ALIVE_SECONDS, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS);
	}

	private void stopPing()
	{
		if (pingTask != null)
		{
			pingTask.cancel(false);
			pingTask = null;
		}
		if (pingScheduler != null)
		{
			pingScheduler.shutdownNow();
			pingScheduler = null;
		}
	}
}
