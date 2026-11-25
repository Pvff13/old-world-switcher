package com.oldworldswitcher;

import com.google.inject.Provides;
import java.awt.Canvas;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

@PluginDescriptor(
	name = "Old World Switcher",
	description = "Brings back the old login screen world switcher, drawn with the game's own sprites.",
	tags = {"login", "world", "switcher", "select", "old", "classic", "legacy"}
)
public class OldWorldSwitcherPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(OldWorldSwitcherPlugin.class);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private WorldService worldService;

	@Inject
	private OldWorldSwitcherConfig config;

	// only touched on the Swing thread
	private JWindow window;
	private ComponentAdapter follower;
	private Window followed;

	// Registered after Stretched Mode's translating listener, so coordinates are already in game space.
	private final MouseAdapter mouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent e)
		{
			if (config.logClicks() && onLoginScreen())
			{
				log.info("[OldWorldSwitcher] click at {},{} (canvas width {})", e.getX(), e.getY(), client.getCanvasWidth());
			}
			if (isWorldButtonClick(e))
			{
				e.consume();
				open();
			}
			return e;
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent e)
		{
			if (isWorldButtonClick(e))
			{
				e.consume();
			}
			return e;
		}

		@Override
		public MouseEvent mouseClicked(MouseEvent e)
		{
			if (isWorldButtonClick(e))
			{
				e.consume();
			}
			return e;
		}
	};

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (onLoginScreen())
			{
				open();
			}
		}
	};

	@Provides
	OldWorldSwitcherConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OldWorldSwitcherConfig.class);
	}

	@Override
	protected void startUp()
	{
		mouseManager.registerMouseListener(mouseListener);
		keyManager.registerKeyListener(hotkeyListener);
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(mouseListener);
		keyManager.unregisterKeyListener(hotkeyListener);
		SwingUtilities.invokeLater(this::close);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (!onLoginScreen())
		{
			SwingUtilities.invokeLater(this::close);
		}
	}

	private boolean isWorldButtonClick(MouseEvent e)
	{
		return onLoginScreen()
			&& !(config.shiftForNew() && e.isShiftDown())
			&& WorldButtonArea.contains(client.getCanvasWidth(), e.getX(), e.getY());
	}

	private boolean onLoginScreen()
	{
		GameState state = client.getGameState();
		return state == GameState.LOGIN_SCREEN || state == GameState.LOGIN_SCREEN_AUTHENTICATOR;
	}

	// Sprites have to be read on the client thread; the window itself is built on the Swing thread.
	private void open()
	{
		clientThread.invoke(() ->
		{
			WorldResult result = worldService.getWorlds();
			List<World> worlds = result == null ? Collections.emptyList() : result.getWorlds();
			String message = null;
			if (worlds.isEmpty())
			{
				worldService.refresh();
				message = "The world list hasn't loaded yet. Close this and try again in a moment.";
			}

			SwitcherSprites sprites;
			try
			{
				sprites = SwitcherSprites.load(client);
			}
			catch (RuntimeException ex)
			{
				log.warn("[OldWorldSwitcher] couldn't load the world switcher sprites", ex);
				sprites = SwitcherSprites.empty();
			}

			WorldListPanel panel = new WorldListPanel(worlds, sprites,
				client.getCanvasWidth(), client.getCanvasHeight(), message,
				this::select, () -> SwingUtilities.invokeLater(this::close));

			Canvas canvas = client.getCanvas();
			SwingUtilities.invokeLater(() -> show(panel, canvas));
		});
	}

	private void show(WorldListPanel panel, Canvas canvas)
	{
		close();
		if (canvas == null || !canvas.isShowing())
		{
			return;
		}

		Window owner = SwingUtilities.getWindowAncestor(canvas);
		window = new JWindow(owner);
		window.setContentPane(panel);
		window.setFocusableWindowState(true);

		bindKey(panel, KeyEvent.VK_ESCAPE, "close", this::close);
		bindKey(panel, KeyEvent.VK_LEFT, "prev", () -> panel.scroll(-1));
		bindKey(panel, KeyEvent.VK_RIGHT, "next", () -> panel.scroll(1));

		// keep the list over the game area when the client window moves or resizes
		followed = owner;
		follower = new ComponentAdapter()
		{
			@Override
			public void componentMoved(ComponentEvent e)
			{
				place(canvas);
			}

			@Override
			public void componentResized(ComponentEvent e)
			{
				place(canvas);
			}
		};
		if (owner != null)
		{
			owner.addComponentListener(follower);
		}
		canvas.addComponentListener(follower);

		place(canvas);
		window.setVisible(true);
		panel.requestFocusInWindow();
	}

	private void place(Canvas canvas)
	{
		if (window == null || !canvas.isShowing())
		{
			return;
		}
		Point p = canvas.getLocationOnScreen();
		window.setBounds(p.x, p.y, canvas.getWidth(), canvas.getHeight());
	}

	private void close()
	{
		if (window == null)
		{
			return;
		}
		if (followed != null)
		{
			followed.removeComponentListener(follower);
		}
		Canvas canvas = client.getCanvas();
		if (canvas != null)
		{
			canvas.removeComponentListener(follower);
		}
		window.dispose();
		window = null;
		follower = null;
		followed = null;
	}

	private static void bindKey(JComponent c, int key, String name, Runnable action)
	{
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
		c.getActionMap().put(name, new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				action.run();
			}
		});
	}

	// Same steps the core World Hopper plugin uses to change world from the login screen.
	private void select(World world)
	{
		SwingUtilities.invokeLater(this::close);
		clientThread.invoke(() ->
		{
			if (!onLoginScreen())
			{
				return;
			}
			net.runelite.api.World rsWorld = client.createWorld();
			rsWorld.setActivity(world.getActivity());
			rsWorld.setAddress(world.getAddress());
			rsWorld.setId(world.getId());
			rsWorld.setPlayerCount(world.getPlayers());
			rsWorld.setLocation(world.getRegion() == null ? 0 : world.getRegion().getId());
			rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));
			client.changeWorld(rsWorld);
		});
	}
}
