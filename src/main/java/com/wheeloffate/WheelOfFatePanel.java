package com.wheeloffate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class WheelOfFatePanel extends PluginPanel
{
	private static final Gson GSON = new Gson();
	private static final Type ENTRY_LIST_TYPE = new TypeToken<List<WheelEntry>>()
	{
	}.getType();
	private static final int PANEL_WIDTH = 225;

	private final ConfigManager configManager;
	private final WheelComponent wheelComponent;
	private final JPanel entryListPanel;
	private final JLabel resultLabel;
	private final JButton spinButton;
	private final JComboBox<String> bossDropdown;
	private final JTextField customEntryField;

	// Connection UI
	private final JLabel connectionStatusLabel;
	private final JTextField roomCodeField;
	private final JButton createRoomButton;
	private final JButton joinRoomButton;
	private final JButton copyCodeButton;
	private final JButton disconnectButton;

	// Confetti
	private final ConfettiOverlay confettiOverlay;

	// Networking
	private MqttRelay relay;
	private boolean isHost;
	private String localId;

	private List<WheelEntry> entries = new ArrayList<>();

	public WheelOfFatePanel(ConfigManager configManager)
	{
		super(false);
		this.configManager = configManager;
		this.localId = java.util.UUID.randomUUID().toString().substring(0, 8);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel contentPanel = new JPanel()
		{
			@Override
			public Dimension getPreferredSize()
			{
				Dimension d = super.getPreferredSize();
				d.width = Math.min(d.width, PANEL_WIDTH);
				return d;
			}
		};
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

		// === Title ===
		JLabel title = new JLabel("Wheel of Fate");
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		title.setForeground(new Color(255, 215, 0));
		title.setAlignmentX(LEFT_ALIGNMENT);
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setMaximumSize(new Dimension(PANEL_WIDTH, 24));
		contentPanel.add(title);
		contentPanel.add(Box.createVerticalStrut(8));

		// === Room Section ===
		JPanel roomPanel = createSectionPanel("Room");

		connectionStatusLabel = new JLabel("Not connected");
		connectionStatusLabel.setFont(FontManager.getRunescapeSmallFont());
		connectionStatusLabel.setForeground(Color.GRAY);
		connectionStatusLabel.setMaximumSize(new Dimension(PANEL_WIDTH, 20));
		roomPanel.add(connectionStatusLabel);
		roomPanel.add(Box.createVerticalStrut(4));

		// Room code field
		roomCodeField = new JTextField();
		roomCodeField.setFont(FontManager.getRunescapeFont().deriveFont(14f));
		roomCodeField.setToolTipText("Room code");
		roomCodeField.setMaximumSize(new Dimension(PANEL_WIDTH, 26));
		roomPanel.add(roomCodeField);
		roomPanel.add(Box.createVerticalStrut(4));

		// Create / Join buttons row
		JPanel buttonRow = new JPanel(new BorderLayout(4, 0));
		buttonRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonRow.setMaximumSize(new Dimension(PANEL_WIDTH, 28));

		createRoomButton = createSmallButton("Create");
		createRoomButton.setToolTipText("Create a new room");
		createRoomButton.addActionListener(e -> createRoom());
		buttonRow.add(createRoomButton, BorderLayout.WEST);

		joinRoomButton = createSmallButton("Join");
		joinRoomButton.setToolTipText("Join an existing room");
		joinRoomButton.addActionListener(e -> joinRoom());
		buttonRow.add(joinRoomButton, BorderLayout.CENTER);

		copyCodeButton = createSmallButton("Copy");
		copyCodeButton.setToolTipText("Copy room code");
		copyCodeButton.addActionListener(e ->
		{
			String code = roomCodeField.getText().trim();
			if (!code.isEmpty())
			{
				Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new StringSelection(code), null);
			}
		});
		buttonRow.add(copyCodeButton, BorderLayout.EAST);

		roomPanel.add(buttonRow);
		roomPanel.add(Box.createVerticalStrut(4));

		disconnectButton = new JButton("Disconnect");
		disconnectButton.setFont(FontManager.getRunescapeSmallFont());
		disconnectButton.setBackground(new Color(192, 57, 43));
		disconnectButton.setForeground(Color.WHITE);
		disconnectButton.setFocusPainted(false);
		disconnectButton.setEnabled(false);
		disconnectButton.setMaximumSize(new Dimension(PANEL_WIDTH, 26));
		disconnectButton.addActionListener(e -> disconnect());
		roomPanel.add(disconnectButton);

		contentPanel.add(roomPanel);
		contentPanel.add(Box.createVerticalStrut(8));

		// === Wheel ===
		wheelComponent = new WheelComponent();
		wheelComponent.setAlignmentX(LEFT_ALIGNMENT);
		contentPanel.add(wheelComponent);
		contentPanel.add(Box.createVerticalStrut(6));

		// === Result Label ===
		resultLabel = new JLabel("Spin the wheel!");
		resultLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(14f));
		resultLabel.setForeground(Color.LIGHT_GRAY);
		resultLabel.setAlignmentX(LEFT_ALIGNMENT);
		resultLabel.setMaximumSize(new Dimension(PANEL_WIDTH, 20));
		resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
		contentPanel.add(resultLabel);
		contentPanel.add(Box.createVerticalStrut(6));

		// === Spin Button ===
		spinButton = new JButton("SPIN!");
		spinButton.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		spinButton.setBackground(new Color(46, 204, 113));
		spinButton.setForeground(Color.WHITE);
		spinButton.setFocusPainted(false);
		spinButton.setAlignmentX(LEFT_ALIGNMENT);
		spinButton.setMaximumSize(new Dimension(PANEL_WIDTH, 36));
		spinButton.addActionListener(e -> doSpin());

		contentPanel.add(spinButton);
		contentPanel.add(Box.createVerticalStrut(10));

		// === Add Entry Section ===
		JPanel addPanel = createSectionPanel("Add Entry");

		JPanel bossRow = new JPanel(new BorderLayout(4, 0));
		bossRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bossRow.setMaximumSize(new Dimension(PANEL_WIDTH, 26));
		bossDropdown = new JComboBox<>();
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement("-- Select Boss --");
		for (String boss : BossPresets.BOSSES)
		{
			model.addElement(boss);
		}
		bossDropdown.setModel(model);
		bossDropdown.setFont(FontManager.getRunescapeSmallFont());
		bossRow.add(bossDropdown, BorderLayout.CENTER);

		JButton addBossBtn = createSmallButton("Add");
		addBossBtn.addActionListener(e ->
		{
			String selected = (String) bossDropdown.getSelectedItem();
			if (selected != null && !selected.startsWith("--"))
			{
				addEntry(new WheelEntry(selected), true);
				bossDropdown.setSelectedIndex(0);
			}
		});
		bossRow.add(addBossBtn, BorderLayout.EAST);
		addPanel.add(bossRow);
		addPanel.add(Box.createVerticalStrut(4));

		JPanel customRow = new JPanel(new BorderLayout(4, 0));
		customRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		customRow.setMaximumSize(new Dimension(PANEL_WIDTH, 26));
		customEntryField = new JTextField();
		customEntryField.setFont(FontManager.getRunescapeSmallFont());
		customEntryField.setToolTipText("Type a custom entry");
		customEntryField.addActionListener(e -> addCustomEntry());
		customRow.add(customEntryField, BorderLayout.CENTER);

		JButton addCustomBtn = createSmallButton("Add");
		addCustomBtn.addActionListener(e -> addCustomEntry());
		customRow.add(addCustomBtn, BorderLayout.EAST);
		addPanel.add(customRow);

		contentPanel.add(addPanel);
		contentPanel.add(Box.createVerticalStrut(8));

		// === Entry List ===
		JPanel listSection = createSectionPanel("Entries");
		entryListPanel = new JPanel();
		entryListPanel.setLayout(new BoxLayout(entryListPanel, BoxLayout.Y_AXIS));
		entryListPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(entryListPanel);
		scrollPane.setPreferredSize(new Dimension(0, 150));
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		listSection.add(scrollPane);

		JButton clearAllBtn = new JButton("Clear All");
		clearAllBtn.setFont(FontManager.getRunescapeSmallFont());
		clearAllBtn.setBackground(new Color(192, 57, 43));
		clearAllBtn.setForeground(Color.WHITE);
		clearAllBtn.setFocusPainted(false);
		clearAllBtn.setMaximumSize(new Dimension(PANEL_WIDTH, 26));
		clearAllBtn.addActionListener(e ->
		{
			if (!entries.isEmpty())
			{
				entries.clear();
				saveEntries();
				refreshUI();
				sendSync(WheelSyncMessage.clear());
			}
		});
		listSection.add(Box.createVerticalStrut(4));
		listSection.add(clearAllBtn);

		contentPanel.add(listSection);

		JScrollPane mainScroll = new JScrollPane(contentPanel);
		mainScroll.setBorder(BorderFactory.createEmptyBorder());
		mainScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		mainScroll.getVerticalScrollBar().setUnitIncrement(16);

		// Layered pane: content underneath, confetti on top
		JLayeredPane layeredPane = new JLayeredPane();
		layeredPane.setLayout(null); // manual layout

		confettiOverlay = new ConfettiOverlay();

		layeredPane.add(mainScroll, JLayeredPane.DEFAULT_LAYER);
		layeredPane.add(confettiOverlay, JLayeredPane.PALETTE_LAYER);

		// Keep both layers sized to the panel
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				Dimension size = layeredPane.getSize();
				mainScroll.setBounds(0, 0, size.width, size.height);
				confettiOverlay.setBounds(0, 0, size.width, size.height);
			}
		});

		layeredPane.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				Dimension size = layeredPane.getSize();
				mainScroll.setBounds(0, 0, size.width, size.height);
				confettiOverlay.setBounds(0, 0, size.width, size.height);
			}
		});

		add(layeredPane, BorderLayout.CENTER);

		// Set wheel callback now that confettiOverlay exists
		wheelComponent.setOnSpinComplete(winner ->
		{
			resultLabel.setText("Result: " + winner.getName());
			resultLabel.setForeground(new Color(255, 215, 0));
			spinButton.setEnabled(true);
			spinButton.setText("SPIN!");
			confettiOverlay.fire();
		});

		loadState();
		refreshUI();
	}

	// === Room Methods ===

	private void createRoom()
	{
		String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
		Random rand = new Random();
		StringBuilder code = new StringBuilder();
		for (int i = 0; i < 6; i++)
		{
			code.append(chars.charAt(rand.nextInt(chars.length())));
		}
		roomCodeField.setText(code.toString());
		isHost = true;
		connectToRoom(code.toString());
	}

	private void joinRoom()
	{
		String code = roomCodeField.getText().trim().toUpperCase();
		if (code.isEmpty())
		{
			return;
		}
		roomCodeField.setText(code);
		isHost = false;
		connectToRoom(code);
	}

	private void connectToRoom(String roomCode)
	{
		setConnectingState();

		relay = new MqttRelay(
			message -> SwingUtilities.invokeLater(() -> handleRelayMessage(message)),
			() -> SwingUtilities.invokeLater(() ->
			{
				connectionStatusLabel.setText("Room: " + roomCode);
				connectionStatusLabel.setForeground(new Color(46, 204, 113));
				disconnectButton.setEnabled(true);

				if (isHost)
				{
					resultLabel.setText("Room created! Share the code.");
					Toolkit.getDefaultToolkit().getSystemClipboard()
						.setContents(new StringSelection(roomCode), null);
				}
				else
				{
					resultLabel.setText("Joined! Syncing...");
					// Request sync from host
					sendSync(WheelSyncMessage.join());
				}
				resultLabel.setForeground(Color.LIGHT_GRAY);
			}),
			() -> SwingUtilities.invokeLater(() ->
			{
				connectionStatusLabel.setText("Disconnected");
				connectionStatusLabel.setForeground(Color.GRAY);
				setDisconnectedState();
				relay = null;
			})
		);

		relay.connect(roomCode).exceptionally(ex ->
		{
			SwingUtilities.invokeLater(() ->
			{
				connectionStatusLabel.setText("Failed to connect");
				connectionStatusLabel.setForeground(new Color(231, 76, 60));
				setDisconnectedState();
				relay = null;
			});
			log.error("Failed to connect to relay", ex);
			return null;
		});
	}

	private void disconnect()
	{
		if (relay != null)
		{
			relay.disconnect();
			relay = null;
		}
		connectionStatusLabel.setText("Not connected");
		connectionStatusLabel.setForeground(Color.GRAY);
		setDisconnectedState();
	}

	private void setConnectingState()
	{
		connectionStatusLabel.setText("Connecting...");
		connectionStatusLabel.setForeground(Color.YELLOW);
		createRoomButton.setEnabled(false);
		joinRoomButton.setEnabled(false);
		roomCodeField.setEnabled(false);
	}

	private void setDisconnectedState()
	{
		createRoomButton.setEnabled(true);
		joinRoomButton.setEnabled(true);
		roomCodeField.setEnabled(true);
		disconnectButton.setEnabled(false);
	}

	// === Sync Message Handling ===

	private void handleRelayMessage(String json)
	{
		try
		{
			WheelSyncMessage msg = GSON.fromJson(json, WheelSyncMessage.class);
			if (msg == null || msg.getType() == null)
			{
				return;
			}

			// Ignore our own messages
			if (localId.equals(msg.getSenderId()))
			{
				return;
			}

			switch (msg.getType())
			{
				case JOIN:
					// Someone joined, send them our current state
					if (isHost)
					{
						sendSync(WheelSyncMessage.fullSync(entries));
					}
					break;

				case FULL_SYNC:
					entries = new ArrayList<>(msg.getEntries());
					saveEntries();
					refreshUI();
					resultLabel.setText("Synced! Ready to spin.");
					resultLabel.setForeground(Color.LIGHT_GRAY);
					break;

				case ADD_ENTRY:
					addEntry(msg.getEntry(), false);
					break;

				case REMOVE_ENTRY:
					if (msg.getIndex() >= 0 && msg.getIndex() < entries.size())
					{
						entries.remove(msg.getIndex());
						saveEntries();
						refreshUI();
					}
					break;

				case CLEAR:
					entries.clear();
					saveEntries();
					refreshUI();
					break;

				case SPIN:
					if (!entries.isEmpty() && !wheelComponent.isSpinning() && msg.getWinnerName() != null)
					{
						// Find the winner by name in our local entries
						int idx = -1;
						for (int i = 0; i < entries.size(); i++)
						{
							if (entries.get(i).getName().equals(msg.getWinnerName()))
							{
								idx = i;
								break;
							}
						}
						if (idx >= 0)
						{
							spinButton.setEnabled(false);
							spinButton.setText("Spinning...");
							resultLabel.setText("...");
							resultLabel.setForeground(Color.LIGHT_GRAY);
							wheelComponent.spin(idx);
						}
						else
						{
							// Boss not on our wheel, just show the result
							resultLabel.setText("Result: " + msg.getWinnerName());
							resultLabel.setForeground(new Color(255, 215, 0));
						}
					}
					break;
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to parse sync message", e);
		}
	}

	private void sendSync(WheelSyncMessage msg)
	{
		if (relay != null && relay.isConnected())
		{
			msg.setSenderId(localId);
			relay.publish(GSON.toJson(msg));
		}
	}

	// === UI Helpers ===

	private JPanel createSectionPanel(String title)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
			new EmptyBorder(6, 6, 6, 6)
		));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(PANEL_WIDTH, Integer.MAX_VALUE));

		JLabel label = new JLabel(title);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(label);
		panel.add(Box.createVerticalStrut(4));

		return panel;
	}

	private JButton createSmallButton(String text)
	{
		JButton btn = new JButton(text);
		btn.setFont(FontManager.getRunescapeSmallFont());
		btn.setFocusPainted(false);
		btn.setMargin(new Insets(2, 6, 2, 6));
		return btn;
	}

	// === Actions ===

	private void doSpin()
	{
		if (entries.isEmpty() || wheelComponent.isSpinning())
		{
			return;
		}

		Random random = new Random();
		int winnerIndex = random.nextInt(entries.size());

		spinButton.setEnabled(false);
		spinButton.setText("Spinning...");
		resultLabel.setText("...");
		resultLabel.setForeground(Color.LIGHT_GRAY);

		wheelComponent.spin(winnerIndex);
		sendSync(WheelSyncMessage.spin(entries.get(winnerIndex).getName()));
	}

	private void addEntry(WheelEntry entry, boolean broadcast)
	{
		entries.add(entry);
		saveEntries();
		refreshUI();

		if (broadcast)
		{
			sendSync(WheelSyncMessage.addEntry(entry));
		}
	}

	private void addCustomEntry()
	{
		String text = customEntryField.getText().trim();
		if (!text.isEmpty())
		{
			addEntry(new WheelEntry(text), true);
			customEntryField.setText("");
		}
	}

	private void removeEntry(int index)
	{
		if (index >= 0 && index < entries.size())
		{
			entries.remove(index);
			saveEntries();
			refreshUI();
			sendSync(WheelSyncMessage.removeEntry(index));
		}
	}

	private void refreshUI()
	{
		wheelComponent.setEntries(entries);

		entryListPanel.removeAll();
		for (int i = 0; i < entries.size(); i++)
		{
			final int idx = i;
			WheelEntry entry = entries.get(i);

			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setBackground(i % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR);
			row.setBorder(new EmptyBorder(3, 6, 3, 4));
			row.setMaximumSize(new Dimension(PANEL_WIDTH, 28));

			JLabel numLabel = new JLabel((i + 1) + ".");
			numLabel.setFont(FontManager.getRunescapeSmallFont());
			numLabel.setForeground(Color.GRAY);
			numLabel.setPreferredSize(new Dimension(22, 20));
			row.add(numLabel, BorderLayout.WEST);

			JLabel nameLabel = new JLabel(entry.getName());
			nameLabel.setFont(FontManager.getRunescapeSmallFont());
			nameLabel.setForeground(Color.WHITE);
			row.add(nameLabel, BorderLayout.CENTER);

			JLabel removeLabel = new JLabel("X");
			removeLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
			removeLabel.setForeground(new Color(192, 57, 43));
			removeLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
			removeLabel.setToolTipText("Remove");
			removeLabel.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					removeEntry(idx);
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					removeLabel.setForeground(new Color(231, 76, 60));
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					removeLabel.setForeground(new Color(192, 57, 43));
				}
			});
			row.add(removeLabel, BorderLayout.EAST);

			entryListPanel.add(row);
		}

		entryListPanel.revalidate();
		entryListPanel.repaint();
	}

	// === Persistence ===

	private void loadState()
	{
		String entriesJson = configManager.getConfiguration(WheelOfFateConfig.GROUP, WheelOfFateConfig.ENTRIES_KEY);
		if (entriesJson != null && !entriesJson.isEmpty())
		{
			try
			{
				List<WheelEntry> loaded = GSON.fromJson(entriesJson, ENTRY_LIST_TYPE);
				if (loaded != null)
				{
					entries = loaded;
				}
			}
			catch (Exception ignored)
			{
			}
		}
	}

	private void saveEntries()
	{
		configManager.setConfiguration(WheelOfFateConfig.GROUP, WheelOfFateConfig.ENTRIES_KEY, GSON.toJson(entries));
	}

	public void shutdown()
	{
		disconnect();
	}
}
