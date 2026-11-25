package com.oldworldswitcher;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup(OldWorldSwitcherConfig.GROUP)
public interface OldWorldSwitcherConfig extends Config
{
	String GROUP = "oldworldswitcher";

	@ConfigItem(
		position = 0,
		keyName = "shiftForNew",
		name = "Shift for new switcher",
		description = "Hold Shift while clicking the world button to open the game's new world switcher instead."
	)
	default boolean shiftForNew()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "hotkey",
		name = "Open hotkey",
		description = "Opens the old-style world list on the login screen, in case clicking the world button doesn't."
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		position = 2,
		keyName = "logClicks",
		name = "Log clicks",
		description = "Write every login-screen click position to the client log, to help adjust the world button area if it misses."
	)
	default boolean logClicks()
	{
		return false;
	}
}
