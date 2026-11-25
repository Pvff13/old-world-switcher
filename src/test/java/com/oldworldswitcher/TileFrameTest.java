package com.oldworldswitcher;

import static org.junit.Assert.assertEquals;
import java.util.EnumSet;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;
import org.junit.Test;

public class TileFrameTest
{
	private static int frame(WorldType... types)
	{
		EnumSet<WorldType> t = EnumSet.noneOf(WorldType.class);
		for (WorldType type : types)
		{
			t.add(type);
		}
		return WorldListPanel.tileFrame(World.builder().id(301).types(t).build());
	}

	@Test
	public void plainWorlds()
	{
		assertEquals(0, frame());
		assertEquals(1, frame(WorldType.MEMBERS));
	}

	@Test
	public void specialWorlds()
	{
		assertEquals(3, frame(WorldType.MEMBERS, WorldType.PVP));
		assertEquals(3, frame(WorldType.MEMBERS, WorldType.PVP, WorldType.HIGH_RISK));
		assertEquals(5, frame(WorldType.MEMBERS, WorldType.DEADMAN, WorldType.PVP));
		assertEquals(15, frame(WorldType.MEMBERS, WorldType.BETA_WORLD));
		assertEquals(17, frame(WorldType.MEMBERS, WorldType.HARDCORE_MAIN));
		assertEquals(12, frame(WorldType.FRESH_START_WORLD));
	}
}
