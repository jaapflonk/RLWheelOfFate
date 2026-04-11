package com.wheeloffate;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WheelOfFateTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WheelOfFatePlugin.class);
		RuneLite.main(args);
	}
}
