# Wheel of Fate

A RuneLite plugin that lets you spin a wheel to randomly choose your next boss or activity. Connect with friends so everyone sees the same wheel and result.

![RuneLite](https://img.shields.io/badge/RuneLite-Plugin-orange)
![Java](https://img.shields.io/badge/Java-11%2B-blue)

## Features

- **Animated Spinning Wheel** - Colorful wheel with smooth spin animation and confetti celebration
- **Predefined Boss List** - 44 OSRS bosses and activities sorted alphabetically, ready to add
- **Custom Entries** - Add any boss, activity, or task you want
- **Multiplayer Sync** - Create or join a room with a simple code to sync your wheel with friends
- **No Port Forwarding** - Uses a public MQTT relay so it works over the internet without any setup
- **Persistent Entries** - Your wheel entries are saved between sessions

## How to Use

1. Open the **Wheel of Fate** panel from the RuneLite sidebar
2. Add bosses from the dropdown or type custom entries
3. Click **SPIN!** to randomly pick your next activity

### Playing with Friends

1. Click **Create** to start a new room - a 6-character code is generated and copied to your clipboard
2. Share the code with your friend
3. Your friend enters the code and clicks **Join**
4. Now you're synced! Adding/removing entries and spinning the wheel is shared between all players in the room

## Building

Requires JDK 11.

```bash
./gradlew build
```

## Testing

Run RuneLite with the plugin loaded:

```bash
./gradlew run
```

Or in IntelliJ, run `src/test/java/com/wheeloffate/WheelOfFateTest.java` with JDK 11.

## Project Structure

```
src/main/java/com/wheeloffate/
  WheelOfFatePlugin.java    - Main plugin, registers the sidebar panel
  WheelOfFatePanel.java     - Side panel UI with room controls and entry management
  WheelComponent.java       - Custom animated spinning wheel (Graphics2D)
  ConfettiOverlay.java      - Full-panel confetti celebration effect
  MqttRelay.java            - MQTT-over-WebSocket client for multiplayer sync
  WheelSyncMessage.java     - Sync message types (add, remove, spin, etc.)
  WheelEntry.java           - Data class for wheel items
  BossPresets.java           - Predefined list of OSRS bosses
  WheelOfFateConfig.java    - Plugin configuration
```

## Multiplayer Architecture

The multiplayer sync uses a minimal MQTT client built on Java 11's built-in WebSocket API - zero external dependencies. It connects to HiveMQ's free public broker (`broker.hivemq.com:8000`). Room codes map to MQTT topics, so players in the same room automatically receive each other's updates.

## License

This project is open source. Feel free to use, modify, and distribute.
