# Old World Switcher

Brings back the login screen world switcher from before the November 2026 update.

Clicking the world button on the login screen opens the old list instead of the new paged one with
filters: every world as a tile with its number, flag, player count and star, in columns across the
screen. It is drawn with the old switcher's own sprites, which are still in the game cache, so PvP,
Deadman, Beta, Fresh Start and other special worlds keep their old tile colours and icons.

- Sort by world, players, location or type with the red/green arrows in the header.
- Scroll through the columns with the side arrows, the mouse wheel or the Left/Right keys.
- Hover a world to see its activity.
- Click a world to switch to it. Escape or Cancel closes the list.

## Settings

- **Shift for new switcher** - hold Shift while clicking the world button to open the game's new
  world switcher instead.
- **Open hotkey** - opens the old list on the login screen.
- **Log clicks** - writes every login-screen click position to the client log, in case the world
  button area ever needs adjusting.

## How it works

RuneLite can't draw overlays on the login screen, so the list is a borderless window placed exactly
over the game canvas (it follows the client when moved or resized, and scales with stretched mode).
Changing world uses the same steps as the core World Hopper plugin does on the login screen.
