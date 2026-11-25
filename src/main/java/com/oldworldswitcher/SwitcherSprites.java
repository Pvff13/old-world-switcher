package com.oldworldswitcher;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;
import net.runelite.api.gameval.SpriteID;

/**
 * The sprites the old login world switcher was drawn with. They are still in the game cache:
 * sl_back holds the 88x19 world tiles (even frames free, odd frames members), sl_flags the 17x19
 * flags (by location id, members versions 29 frames on), sl_arrows the sort buttons and sl_stars the
 * legend stars.
 */
class SwitcherSprites
{
	static final int FLAGS_PER_SET = 29;

	final List<BufferedImage> tiles;
	// tiles with the old 50% white hover wash already applied
	final List<BufferedImage> hoverTiles;
	final List<BufferedImage> flags;
	final List<BufferedImage> sortArrows;
	final List<BufferedImage> stars;
	final BufferedImage leftArrow;
	final BufferedImage rightArrow;

	SwitcherSprites(List<BufferedImage> tiles, List<BufferedImage> flags, List<BufferedImage> sortArrows,
		List<BufferedImage> stars, BufferedImage leftArrow, BufferedImage rightArrow)
	{
		this.tiles = tiles;
		this.flags = flags;
		this.sortArrows = sortArrows;
		this.stars = stars;
		this.leftArrow = leftArrow;
		this.rightArrow = rightArrow;
		List<BufferedImage> hover = new ArrayList<>();
		for (BufferedImage t : tiles)
		{
			hover.add(washed(t));
		}
		this.hoverTiles = hover;
	}

	static SwitcherSprites empty()
	{
		return new SwitcherSprites(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
			Collections.emptyList(), null, null);
	}

	// must run on the client thread
	static SwitcherSprites load(Client client)
	{
		List<BufferedImage> left = frames(client, SpriteID.LEFTARROW);
		List<BufferedImage> right = frames(client, SpriteID.RIGHTARROW);
		return new SwitcherSprites(
			frames(client, SpriteID.SL_BACK),
			frames(client, SpriteID.SL_FLAGS),
			frames(client, SpriteID.SL_ARROWS),
			frames(client, SpriteID.SL_STARS),
			left.isEmpty() ? null : left.get(0),
			right.isEmpty() ? null : right.get(0));
	}

	static BufferedImage frame(List<BufferedImage> frames, int i)
	{
		return i >= 0 && i < frames.size() ? frames.get(i) : null;
	}

	// Each frame is placed on its full canvas at its offset, the way the client draws it.
	private static List<BufferedImage> frames(Client client, int archive)
	{
		SpritePixels[] sprites = client.getSprites(client.getIndexSprites(), archive, 0);
		if (sprites == null)
		{
			return Collections.emptyList();
		}
		List<BufferedImage> out = new ArrayList<>();
		for (SpritePixels sp : sprites)
		{
			int w = Math.max(sp.getMaxWidth(), sp.getWidth() + sp.getOffsetX());
			int h = Math.max(sp.getMaxHeight(), sp.getHeight() + sp.getOffsetY());
			BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.drawImage(sp.toBufferedImage(), sp.getOffsetX(), sp.getOffsetY(), null);
			g.dispose();
			out.add(img);
		}
		return out;
	}

	private static BufferedImage washed(BufferedImage src)
	{
		BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < src.getHeight(); y++)
		{
			for (int x = 0; x < src.getWidth(); x++)
			{
				int argb = src.getRGB(x, y);
				if ((argb >>> 24) == 0)
				{
					continue;
				}
				int r = (((argb >> 16) & 0xff) + 255) / 2;
				int g = (((argb >> 8) & 0xff) + 255) / 2;
				int b = ((argb & 0xff) + 255) / 2;
				out.setRGB(x, y, (argb & 0xff000000) | (r << 16) | (g << 8) | b);
			}
		}
		return out;
	}
}
