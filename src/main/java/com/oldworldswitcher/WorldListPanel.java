package com.oldworldswitcher;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import net.runelite.client.ui.FontManager;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

/**
 * A replica of the login world switcher from before the November 2026 update, drawn with the
 * client's own sl_* sprites: a 23px header (Select a world, star legend, sort buttons, Cancel) over
 * columns of 88x19 world tiles, scrolled a column at a time with the side arrows. Hovering a tile
 * lightens it and shows its activity. Laid out in game pixels and scaled to the canvas, so it lines
 * up with stretched mode too.
 */
class WorldListPanel extends JComponent
{
	private static final int HEADER_H = 23;
	private static final int HEADER_W = 765;
	private static final int TILE_W = 88;
	private static final int TILE_H = 19;
	private static final int[] SORT_X = {280, 390, 500, 610};

	private static final Color TAB_TOP = new Color(0xBD9839);
	private static final Color TAB_BOTTOM = new Color(0x8B6608);
	private static final Color BAR_TOP = new Color(0x4F4F4F);
	private static final Color BAR_BOTTOM = new Color(0x292929);
	private static final Color TOOLTIP = new Color(0xFFFFA0);

	private final List<World> allWorlds;
	private final SwitcherSprites sprites;
	private final Consumer<World> onSelect;
	private final Runnable onClose;
	private final String message;

	private final int logicalWidth;
	private final int logicalHeight;
	private final Font plain11 = FontManager.getRunescapeSmallFont();
	private final Font bold12 = FontManager.getRunescapeBoldFont();

	private List<World> sorted = Collections.emptyList();
	private WorldSort sort = WorldSort.WORLD;
	private boolean ascending = true;
	// the first column shown; the arrows move it one column at a time
	private int firstColumn;
	private int hover = -1;
	private Point mouse;

	// grid, worked out the way the old client did it
	private int columns;
	private int rows;
	private int gapX;
	private int gapY;
	private int gridLeft;
	private int gridTop;

	WorldListPanel(List<World> worlds, SwitcherSprites sprites, int logicalWidth, int logicalHeight,
		String message, Consumer<World> onSelect, Runnable onClose)
	{
		this.allWorlds = worlds;
		this.sprites = sprites;
		this.logicalWidth = logicalWidth;
		this.logicalHeight = logicalHeight;
		this.message = message;
		this.onSelect = onSelect;
		this.onClose = onClose;
		setOpaque(true);
		setFocusable(true);
		resort();
		layoutGrid();

		MouseAdapter listener = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				mouse = new Point(lx(e), ly(e));
				hover = tileAt(mouse.x, mouse.y);
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				mouse = null;
				hover = -1;
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				click(lx(e), ly(e));
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				scroll(e.getWheelRotation() > 0 ? 1 : -1);
			}
		};
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addMouseWheelListener(listener);
	}

	void scroll(int delta)
	{
		int c = Math.max(0, Math.min(maxFirstColumn(), firstColumn + delta));
		if (c != firstColumn)
		{
			firstColumn = c;
			hover = mouse == null ? -1 : tileAt(mouse.x, mouse.y);
			repaint();
		}
	}

	// --- layout ---

	private void layoutGrid()
	{
		int listH = logicalHeight - HEADER_H;
		int count = Math.max(1, sorted.size());
		int cols = Math.max(1, logicalWidth / (TILE_W + 1) - 1);
		int rws = Math.max(1, listH / (TILE_H + 1));

		// with few worlds, shrink the grid so it stays centred
		int prevCols;
		int prevRows;
		do
		{
			prevCols = cols;
			prevRows = rws;
			if (cols > 1 && rws * (cols - 1) >= count)
			{
				cols--;
			}
			if (rws > 1 && cols * (rws - 1) >= count)
			{
				rws--;
			}
		}
		while (cols != prevCols || rws != prevRows);

		columns = cols;
		rows = rws;
		gapX = Math.max(0, Math.min(5, (logicalWidth - cols * TILE_W) / (cols + 1)));
		gapY = Math.max(0, Math.min(5, (listH - rws * TILE_H) / (rws + 1)));
		gridLeft = (logicalWidth - cols * TILE_W - gapX * (cols - 1)) / 2;
		gridTop = HEADER_H + (listH - rws * TILE_H - gapY * (rws - 1)) / 2;
	}

	private int totalColumns()
	{
		return (sorted.size() + rows - 1) / rows;
	}

	private int maxFirstColumn()
	{
		return Math.max(0, totalColumns() - columns);
	}

	private Rectangle tileBounds(int slot)
	{
		int col = slot / rows;
		int row = slot % rows;
		return new Rectangle(gridLeft + col * (TILE_W + gapX), gridTop + row * (TILE_H + gapY), TILE_W, TILE_H);
	}

	private int headerLeft()
	{
		return Math.max(0, (logicalWidth - HEADER_W) / 2);
	}

	private Rectangle cancelBounds()
	{
		return new Rectangle(headerLeft() + 708, 4, 50, 16);
	}

	private Rectangle leftArrowBounds()
	{
		return arrowBounds(sprites.leftArrow, 8);
	}

	private Rectangle rightArrowBounds()
	{
		int w = sprites.rightArrow != null ? sprites.rightArrow.getWidth() : 30;
		return arrowBounds(sprites.rightArrow, logicalWidth - w - 8);
	}

	private Rectangle arrowBounds(BufferedImage img, int x)
	{
		int w = img != null ? img.getWidth() : 30;
		int h = img != null ? img.getHeight() : 30;
		return new Rectangle(x, logicalHeight / 2 - h / 2, w, h);
	}

	private int tileAt(int x, int y)
	{
		int start = firstColumn * rows;
		for (int slot = 0; slot < columns * rows && start + slot < sorted.size(); slot++)
		{
			if (tileBounds(slot).contains(x, y))
			{
				return start + slot;
			}
		}
		return -1;
	}

	// --- input ---

	private int lx(MouseEvent e)
	{
		return (int) (e.getX() * (double) logicalWidth / Math.max(1, getWidth()));
	}

	private int ly(MouseEvent e)
	{
		return (int) (e.getY() * (double) logicalHeight / Math.max(1, getHeight()));
	}

	private void click(int x, int y)
	{
		if (cancelBounds().contains(x, y))
		{
			onClose.run();
			return;
		}
		// up arrow sorts ascending, down arrow and the label descending, as in the old switcher
		int left = headerLeft();
		for (WorldSort s : WorldSort.values())
		{
			int sx = left + SORT_X[s.ordinal()];
			if (y >= 4 && y <= 18 && x >= sx && x < sx + 15)
			{
				setSort(s, true);
				return;
			}
			if (y >= 4 && y <= 18 && x >= sx + 15 && x < sx + 80)
			{
				setSort(s, false);
				return;
			}
		}
		if (firstColumn > 0 && leftArrowBounds().contains(x, y))
		{
			scroll(-1);
			return;
		}
		if (firstColumn < maxFirstColumn() && rightArrowBounds().contains(x, y))
		{
			scroll(1);
			return;
		}
		int i = tileAt(x, y);
		if (i >= 0)
		{
			onSelect.accept(sorted.get(i));
		}
	}

	private void setSort(WorldSort s, boolean asc)
	{
		sort = s;
		ascending = asc;
		resort();
		firstColumn = 0;
		hover = -1;
		repaint();
	}

	private void resort()
	{
		List<World> list = new ArrayList<>(allWorlds);
		list.sort(sort.comparator(ascending));
		sorted = list;
	}

	// --- painting ---

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(logicalWidth, logicalHeight);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.scale(getWidth() / (double) logicalWidth, getHeight() / (double) logicalHeight);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

		paintHeader(g);

		if (message != null)
		{
			g.setFont(plain11);
			centred(g, message, logicalWidth / 2, logicalHeight / 2, Color.WHITE);
			g.dispose();
			return;
		}

		if (firstColumn > 0)
		{
			paintArrow(g, sprites.leftArrow, leftArrowBounds(), "<");
		}
		if (firstColumn < maxFirstColumn())
		{
			paintArrow(g, sprites.rightArrow, rightArrowBounds(), ">");
		}

		int start = firstColumn * rows;
		for (int slot = 0; slot < columns * rows && start + slot < sorted.size(); slot++)
		{
			paintTile(g, sorted.get(start + slot), tileBounds(slot), start + slot == hover);
		}

		if (hover >= 0 && mouse != null)
		{
			paintTooltip(g, sorted.get(hover));
		}
		g.dispose();
	}

	private void paintHeader(Graphics2D g)
	{
		int left = headerLeft();
		g.setPaint(new GradientPaint(0, 0, TAB_TOP, 0, HEADER_H, TAB_BOTTOM));
		g.fillRect(left, 0, 125, HEADER_H);
		g.setPaint(new GradientPaint(0, 0, BAR_TOP, 0, HEADER_H, BAR_BOTTOM));
		g.fillRect(left + 125, 0, HEADER_W - 125, HEADER_H);

		g.setFont(bold12);
		centred(g, "Select a world", left + 62, 15, Color.BLACK);

		BufferedImage free = SwitcherSprites.frame(sprites.stars, 0);
		BufferedImage members = SwitcherSprites.frame(sprites.stars, 1);
		if (members != null)
		{
			g.drawImage(members, left + 140, 1, null);
		}
		if (free != null)
		{
			g.drawImage(free, left + 140, 12, null);
		}
		g.setFont(plain11);
		g.setColor(Color.WHITE);
		g.drawString("Members only world", left + 152, 10);
		g.drawString("Free world", left + 152, 21);

		g.setFont(bold12);
		for (WorldSort s : WorldSort.values())
		{
			int x = left + SORT_X[s.ordinal()];
			boolean active = s == sort;
			drawSortArrow(g, active && ascending ? 2 : 0, x);
			drawSortArrow(g, active && !ascending ? 3 : 1, x + 15);
			g.setColor(Color.WHITE);
			g.drawString(s.label, x + 32, 17);
		}

		Rectangle c = cancelBounds();
		g.setColor(Color.BLACK);
		g.fillRect(c.x, c.y, c.width, c.height);
		g.setFont(plain11);
		centred(g, "Cancel", c.x + c.width / 2, 16, Color.WHITE);
	}

	// frames 0/1 are the red up/down buttons, 2/3 the green ones for the active sort
	private void drawSortArrow(Graphics2D g, int frame, int x)
	{
		BufferedImage img = SwitcherSprites.frame(sprites.sortArrows, frame);
		if (img != null)
		{
			g.drawImage(img, x, 4, null);
			return;
		}
		g.setColor(frame >= 2 ? new Color(40, 170, 40) : new Color(190, 30, 30));
		g.fillOval(x, 4, 13, 13);
	}

	private void paintTile(Graphics2D g, World w, Rectangle r, boolean hovered)
	{
		int frame = tileFrame(w);
		BufferedImage tile = SwitcherSprites.frame(hovered ? sprites.hoverTiles : sprites.tiles, frame);
		if (tile == null)
		{
			tile = SwitcherSprites.frame(hovered ? sprites.hoverTiles : sprites.tiles, frame & 1);
		}
		if (tile != null)
		{
			g.drawImage(tile, r.x, r.y, null);
		}
		else
		{
			g.setColor(hovered ? Color.GRAY : Color.DARK_GRAY);
			g.fillRect(r.x, r.y, r.width, r.height);
		}

		boolean members = isMembers(w);
		BufferedImage flag = null;
		if (w.getRegion() != null && w.getRegion().getId() < SwitcherSprites.FLAGS_PER_SET)
		{
			flag = SwitcherSprites.frame(sprites.flags, w.getRegion().getId() + (members ? SwitcherSprites.FLAGS_PER_SET : 0));
		}
		if (flag != null)
		{
			g.drawImage(flag, r.x + 29, r.y, null);
		}

		// deadman tiles are black, so the number goes red there
		g.setFont(bold12);
		Color numberColour = frame == 4 || frame == 5 ? Color.RED : Color.BLACK;
		centred(g, String.valueOf(w.getId()), r.x + 15, r.y + TILE_H / 2 + 5, numberColour);

		String players = w.getPlayers() < 0 ? "OFF" : w.getPlayers() > 1980 ? "FULL" : String.valueOf(w.getPlayers());
		g.setFont(plain11);
		centred(g, players, r.x + 60, r.y + TILE_H / 2 + 5, Color.WHITE);
	}

	// Which sl_back frame a world uses; even frames are free worlds, odd frames members.
	static int tileFrame(World w)
	{
		EnumSet<WorldType> t = w.getTypes() == null ? EnumSet.noneOf(WorldType.class) : w.getTypes();
		int base;
		if (t.contains(WorldType.BETA_WORLD))
		{
			base = 14;
		}
		else if (t.contains(WorldType.DEADMAN))
		{
			base = 4;
		}
		else if (t.contains(WorldType.FRESH_START_WORLD))
		{
			base = 12;
		}
		else if (t.contains(WorldType.TOURNAMENT))
		{
			base = 8;
		}
		else if (t.contains(WorldType.QUEST_SPEEDRUNNING))
		{
			base = 10;
		}
		else if (t.contains(WorldType.NOSAVE_MODE))
		{
			base = 6;
		}
		else if (t.contains(WorldType.HARDCORE_MAIN))
		{
			base = 16;
		}
		else if (t.contains(WorldType.PVP) || t.contains(WorldType.HIGH_RISK) || t.contains(WorldType.BOUNTY))
		{
			base = 2;
		}
		else
		{
			base = 0;
		}
		return base + (isMembers(w) ? 1 : 0);
	}

	private static boolean isMembers(World w)
	{
		return w.getTypes() != null && w.getTypes().contains(WorldType.MEMBERS);
	}

	private void paintArrow(Graphics2D g, BufferedImage img, Rectangle r, String fallback)
	{
		if (img != null)
		{
			g.drawImage(img, r.x, r.y, null);
			return;
		}
		g.setColor(Color.DARK_GRAY);
		g.fillRect(r.x, r.y, r.width, r.height);
		g.setFont(bold12);
		centred(g, fallback, r.x + r.width / 2, r.y + r.height / 2 + 5, Color.WHITE);
	}

	// the activity, in a pale yellow box under the cursor
	private void paintTooltip(Graphics2D g, World w)
	{
		String activity = w.getActivity() == null || w.getActivity().isEmpty() ? "-" : w.getActivity();
		g.setFont(plain11);
		FontMetrics fm = g.getFontMetrics();
		int width = fm.stringWidth(activity) + 8;
		int height = fm.getAscent() + 8;
		int x = Math.max(0, Math.min(logicalWidth - width, mouse.x + 10));
		int y = Math.max(0, Math.min(logicalHeight - height, mouse.y + 20));
		g.setColor(TOOLTIP);
		g.fillRect(x, y, width, height);
		g.setColor(Color.BLACK);
		g.drawRect(x, y, width - 1, height - 1);
		g.drawString(activity, x + 4, y + 4 + fm.getAscent() - 1);
	}

	private void centred(Graphics2D g, String s, int cx, int y, Color c)
	{
		g.setColor(c);
		g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
	}
}
