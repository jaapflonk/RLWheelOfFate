package com.wheeloffate;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WheelSyncMessage
{
	public enum MessageType
	{
		JOIN,
		FULL_SYNC,
		ADD_ENTRY,
		REMOVE_ENTRY,
		CLEAR,
		SPIN
	}

	private MessageType type;
	private String senderId;
	private List<WheelEntry> entries;
	private WheelEntry entry;
	private int index;
	private int winnerIndex;
	private String winnerName;

	public static WheelSyncMessage join()
	{
		WheelSyncMessage msg = new WheelSyncMessage();
		msg.type = MessageType.JOIN;
		return msg;
	}

	public static WheelSyncMessage fullSync(List<WheelEntry> entries)
	{
		WheelSyncMessage msg = new WheelSyncMessage();
		msg.type = MessageType.FULL_SYNC;
		msg.entries = entries;
		return msg;
	}

	public static WheelSyncMessage addEntry(WheelEntry entry)
	{
		WheelSyncMessage msg = new WheelSyncMessage();
		msg.type = MessageType.ADD_ENTRY;
		msg.entry = entry;
		return msg;
	}

	public static WheelSyncMessage removeEntry(int index)
	{
		WheelSyncMessage msg = new WheelSyncMessage();
		msg.type = MessageType.REMOVE_ENTRY;
		msg.index = index;
		return msg;
	}

	public static WheelSyncMessage clear()
	{
		WheelSyncMessage msg = new WheelSyncMessage();
		msg.type = MessageType.CLEAR;
		return msg;
	}

	public static WheelSyncMessage spin(String winnerName)
	{
		WheelSyncMessage msg = new WheelSyncMessage();
		msg.type = MessageType.SPIN;
		msg.winnerName = winnerName;
		return msg;
	}
}
