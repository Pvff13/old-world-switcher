package com.oldworldswitcher;

import java.util.Comparator;
import net.runelite.http.api.worlds.World;

enum WorldSort
{
	WORLD("World", Comparator.comparingInt(World::getId)),
	PLAYERS("Players", Comparator.comparingInt(World::getPlayers)),
	LOCATION("Location", Comparator.comparing((World w) -> w.getRegion() == null ? "" : w.getRegion().getAlpha2())),
	TYPE("Type", Comparator.comparing((World w) -> w.getActivity() == null ? "" : w.getActivity(), String.CASE_INSENSITIVE_ORDER));

	final String label;
	private final Comparator<World> comparator;

	WorldSort(String label, Comparator<World> comparator)
	{
		this.label = label;
		this.comparator = comparator;
	}

	// Ties always fall back to world number, like the old switcher.
	Comparator<World> comparator(boolean ascending)
	{
		Comparator<World> c = ascending ? comparator : comparator.reversed();
		return c.thenComparingInt(World::getId);
	}
}
