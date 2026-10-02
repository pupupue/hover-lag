# Hover Lag

Visually indicates scuffed clicks.

The game only picks up your mouse position 50 times a second (once per 20ms client cycle), no
matter how high your FPS is, and a left click does whatever was under the mouse at the last
position it picked up. Click shortly after moving onto an NPC and you can get "Walk here" (a yellow
click) even though your cursor is dead centre on it.

Hover Lag draws the positions the game actually used as a short fading trail of dots behind your
cursor. A dot turns red when a click at that moment would have done something other than what is
under your cursor:

- your cursor is on an NPC, but the click would walk, or
- the click would hit an NPC your cursor has already left.

It only draws; it never changes, delays or sends any clicks.

## Settings

- **Show ring** (off by default): a ring at the game's current mouse position, green or red the same way.
- **Dots** (5-10, default 5): how many of the game's recent mouse positions to show.
