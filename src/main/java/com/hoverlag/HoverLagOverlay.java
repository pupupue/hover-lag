package com.hoverlag;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Ring (optional): where the game thinks the mouse is; red when a left click right now would hit something
 * other than what is under the cursor (a walk with the cursor on an NPC, or an NPC the cursor is
 * no longer on), green otherwise. Dots: the last few positions the game picked up, one per 20ms
 * update, fading with age; red where a click would have been scuffed while the game was there.
 */
class HoverLagOverlay extends Overlay
{
	private static final Color IN_SYNC = new Color(0, 255, 0, 200);
	private static final Color SCUFFED = new Color(255, 0, 0, 230);
	private static final int RING = 6;

	private final HoverLagPlugin plugin;
	private final Client client;
	private final HoverLagConfig config;

	@Inject
	HoverLagOverlay(HoverLagPlugin plugin, Client client, HoverLagConfig config)
	{
		this.plugin = plugin;
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Point hit = plugin.hitPos;
		if (hit == null)
		{
			return null;
		}
		net.runelite.api.Point m = client.getMouseCanvasPosition();
		Point mouse = new Point(m.getX(), m.getY());

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		boolean scuffed = scuffed(mouse);
		HoverLagPlugin.Dot newest = plugin.trail.peekLast();
		if (scuffed && newest != null)
		{
			newest.scuffed = true;
		}

		int i = 0;
		int n = plugin.trail.size();
		for (HoverLagPlugin.Dot d : plugin.trail)
		{
			int alpha = 40 + 140 * ++i / n;
			g.setColor(d.scuffed ? new Color(255, 0, 0, alpha) : new Color(255, 255, 255, alpha));
			g.fillOval(d.pos.x - 2, d.pos.y - 2, 4, 4);
		}

		if (config.showRing())
		{
			g.setColor(scuffed ? SCUFFED : IN_SYNC);
			g.setStroke(new BasicStroke(2f));
			g.drawOval(hit.x - RING, hit.y - RING, RING * 2, RING * 2);
		}
		return null;
	}

	// Overlays draw after the scene, so the menu now holds this frame's hover test: its top entry is
	// what a left click would do. Compare that with the NPCs under the live cursor.
	private boolean scuffed(Point mouse)
	{
		if (client.isMenuOpen())
		{
			return false;
		}
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		if (entries.length == 0)
		{
			return false;
		}
		MenuEntry top = entries[entries.length - 1];
		NPC aimed = top.getNpc();
		if (aimed != null)
		{
			Shape box = clickbox(aimed);
			return box != null && !box.contains(mouse);
		}
		if (top.getType() == MenuAction.WALK)
		{
			for (NPC npc : client.getTopLevelWorldView().npcs())
			{
				// Dying NPCs keep their model but offer no options, so walking is right there.
				NPCComposition comp = npc.getTransformedComposition();
				if (comp == null || !comp.isInteractible() || npc.isDead())
				{
					continue;
				}
				Shape box = clickbox(npc);
				if (box != null && box.contains(mouse))
				{
					return true;
				}
			}
		}
		return false;
	}

	// The area the client hit-tests the NPC's model against: its bounding box for models that use
	// one, otherwise each face's screen bounds padded by a few pixels. Placed the way the client
	// places the NPC's convex hull.
	private Shape clickbox(NPC npc)
	{
		LocalPoint lp = npc.getLocalLocation();
		WorldView wv = npc.getWorldView();
		if (lp == null || wv == null)
		{
			return null;
		}
		int z = Perspective.getFootprintTileHeight(client, lp, wv.getPlane(), npc.getFootprintSize())
			- npc.getAnimationHeightOffset();
		return Perspective.getClickbox(client, wv, npc.getModel(), npc.getCurrentOrientation(), lp.getX(), lp.getY(), z);
	}
}
