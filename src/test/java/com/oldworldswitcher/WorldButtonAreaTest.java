package com.oldworldswitcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class WorldButtonAreaTest
{
	@Test
	public void fixedModeButton()
	{
		assertTrue(WorldButtonArea.contains(765, 60, 475));
		assertFalse(WorldButtonArea.contains(765, 200, 475));
		assertFalse(WorldButtonArea.contains(765, 60, 400));
	}

	@Test
	public void wideCanvasCentresTheLoginScreen()
	{
		// 1765 wide: the login screen starts at x=500
		assertTrue(WorldButtonArea.contains(1765, 560, 475));
		assertFalse(WorldButtonArea.contains(1765, 60, 475));
	}
}
