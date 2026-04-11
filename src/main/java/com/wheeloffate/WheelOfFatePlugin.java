package com.wheeloffate;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Wheel of Fate",
	description = "Spin a wheel to randomly choose your next boss or activity. Share results with friends using a friend code.",
	tags = {"wheel", "spin", "random", "boss", "activity", "fate", "friend"}
)
public class WheelOfFatePlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	private WheelOfFatePanel panel;
	private NavigationButton navButton;

	@Provides
	WheelOfFateConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WheelOfFateConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new WheelOfFatePanel(configManager);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Wheel of Fate")
			.icon(icon)
			.priority(10)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		log.info("Wheel of Fate started");
	}

	@Override
	protected void shutDown()
	{
		if (panel != null)
		{
			panel.shutdown();
		}
		clientToolbar.removeNavigation(navButton);
		log.info("Wheel of Fate stopped");
	}
}
