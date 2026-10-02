package com.hoverlag;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("hoverlag")
public interface HoverLagConfig extends Config
{
	@ConfigItem(
		keyName = "showRing",
		name = "Show ring",
		description = "Draw a ring at the game's mouse position: red when a click now would miss what's under the cursor",
		position = 0
	)
	default boolean showRing()
	{
		return false;
	}

	@Range(min = 5, max = 10)
	@ConfigItem(
		keyName = "trailLength",
		name = "Dots",
		description = "How many of the game's recent mouse positions to show",
		position = 1
	)
	default int trailLength()
	{
		return 5;
	}
}
