package com.oldworldswitcher;

/**
 * Where the "World ### / Mode: ..." button sits on the login screen (since the November 2026 world switcher update).
 * The login screen is 765 pixels wide, centred horizontally and anchored to the top of the canvas;
 * the button fills the bottom-left corner of it. Measured from a screenshot of the fixed-mode client.
 */
final class WorldButtonArea
{
	static final int LOGIN_SCREEN_WIDTH = 765;
	static final int LEFT = 4;
	static final int TOP = 450;
	static final int RIGHT = 130;
	static final int BOTTOM = 503;

	private WorldButtonArea()
	{
	}

	static boolean contains(int canvasWidth, int x, int y)
	{
		int padding = (canvasWidth - LOGIN_SCREEN_WIDTH) / 2;
		return x >= padding + LEFT && x <= padding + RIGHT && y >= TOP && y <= BOTTOM;
	}
}
