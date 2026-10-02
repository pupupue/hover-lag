package com.hoverlag;

import com.google.inject.Provides;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.BeforeRender;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * The client copies the mouse position into the game only once per 20ms cycle and hover-tests the
 * scene at that copy, so what a click lands on can be a cycle behind the cursor. This draws where
 * the game currently thinks the mouse is, next to the real cursor.
 *
 * The client re-adds "Walk here" every cycle with that cycle's mouse copy, so its params, read just
 * before a frame is drawn, are the (viewport-relative) position that frame hit-tests at.
 */
@PluginDescriptor(
	name = "Hover Lag",
	description = "Visually indicates scuffed clicks",
	tags = {"click", "hover", "mouse", "lag", "misclick"}
)
public class HoverLagPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HoverLagOverlay overlay;

	@Inject
	private HoverLagConfig config;

	// Client thread only (written before each frame, read by the overlay while drawing it).
	// Canvas position the current frame hit-tests at, null when the mouse isn't over the scene.
	Point hitPos;
	// Game mouse positions, oldest first, newest equal to hitPos. Stepped once per game cycle:
	// a cycle that moved adds a dot, one that didn't drops the oldest, so a still mouse drains it.
	final Deque<Dot> trail = new ArrayDeque<>();
	private int lastCycle = -1;

	@Provides
	HoverLagConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HoverLagConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		hitPos = null;
		trail.clear();
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		Point walk = walkPos(client.getMenu().getMenuEntries());
		// Off the scene, or the mouse has left the window (the client then parks its copy off-screen).
		if (walk == null || !inViewport(walk) || !mouseOnCanvas())
		{
			hitPos = null;
			trail.clear();
			return;
		}

		hitPos = new Point(walk.x + client.getViewportXOffset(), walk.y + client.getViewportYOffset());

		// Frames drawn between cycles reuse the same mouse copy; only a new cycle can move it.
		int cycle = client.getGameCycle();
		if (cycle == lastCycle)
		{
			return;
		}
		lastCycle = cycle;
		if (trail.isEmpty() || !hitPos.equals(trail.peekLast().pos))
		{
			trail.addLast(new Dot(hitPos));
			while (trail.size() > config.trailLength())
			{
				trail.removeFirst();
			}
		}
		else if (trail.size() > 1)
		{
			trail.removeFirst();
		}
	}

	static final class Dot
	{
		final Point pos;
		// Set by the overlay if a click would have been scuffed while this was the game's position.
		boolean scuffed;

		Dot(Point pos)
		{
			this.pos = pos;
		}
	}

	private boolean inViewport(Point viewport)
	{
		return viewport.x >= 0 && viewport.y >= 0
			&& viewport.x < client.getViewportWidth() && viewport.y < client.getViewportHeight();
	}

	private boolean mouseOnCanvas()
	{
		net.runelite.api.Point m = client.getMouseCanvasPosition();
		return m.getX() >= 0 && m.getY() >= 0 && m.getX() < client.getCanvasWidth() && m.getY() < client.getCanvasHeight();
	}

	private static Point walkPos(MenuEntry[] entries)
	{
		for (MenuEntry e : entries)
		{
			if (e.getType() == MenuAction.WALK)
			{
				return new Point(e.getParam0(), e.getParam1());
			}
		}
		return null;
	}
}
