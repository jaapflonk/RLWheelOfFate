package com.wheeloffate;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(WheelOfFateConfig.GROUP)
public interface WheelOfFateConfig extends Config
{
	String GROUP = "wheeloffate";
	String ENTRIES_KEY = "entries";

	@ConfigItem(
		keyName = "syncPort",
		name = "Sync Port",
		description = "Port used when hosting a sync session"
	)
	default int syncPort()
	{
		return 7483;
	}

	@ConfigItem(
		keyName = ENTRIES_KEY,
		name = "Wheel Entries",
		description = "JSON list of wheel entries",
		hidden = true
	)
	default String entries()
	{
		return "[]";
	}
}
