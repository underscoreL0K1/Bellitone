/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Baritone. If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.behavior;

import baritone.Baritone;
import baritone.api.Settings;
import baritone.api.behavior.ILookBehavior;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.RotationMoveEvent;
import baritone.api.utils.Rotation;

public final class LookBehavior extends Behavior implements ILookBehavior {

	/**
	 * Target's values are as follows:
	 */
	private Rotation target;

	/**
	 * Whether or not rotations are currently being forced
	 */
	private boolean force;

	/**
	 * The last player yaw angle. Used when free looking
	 *
	 * @see Settings#freeLook
	 */
	private float lastYaw;

	public LookBehavior(Baritone baritone) {
		super(baritone);
	}

	/**
	 * Nudges the player's pitch to a regular level. (Between {@code -20} and {@code 10}, increments are by {@code 1})
	 */
	private void nudgeToLevel() {
		if (ctx.player().pitch < -20) {
			ctx.player().pitch++;
		} else if (ctx.player().pitch > 10) {
			ctx.player().pitch--;
		}
	}

	@Override
	public void onPlayerRotationMove(RotationMoveEvent event) {
		if (target != null) {

			event.setYaw(target.getYaw());

			// If we have antiCheatCompatibility on, we're going to use the target value later in onPlayerUpdate()
			// Also the type has to be MOTION_UPDATE because that is called after JUMP
			if (!Baritone.settings().antiCheatCompatibility.value && event.getType() == RotationMoveEvent.Type.MOTION_UPDATE && !force) {
				target = null;
			}
		}
	}

	@Override
	public void onPlayerUpdate(PlayerUpdateEvent event) {
		if (target == null)
			return;

		// Whether or not we're going to silently set our angles
		boolean silent = Baritone.settings().antiCheatCompatibility.value && !force;

		switch (event.getState()) {
			case PRE: {
				if (force) {
					ctx.player().yaw = target.getYaw();
					float oldPitch = ctx.player().pitch;
					float desiredPitch = target.getPitch();
					ctx.player().pitch = desiredPitch;
					ctx.player().yaw += (Math.random() - 0.5) * Baritone.settings().randomLooking.value;
					ctx.player().pitch += (Math.random() - 0.5) * Baritone.settings().randomLooking.value;
					if (desiredPitch == oldPitch && !Baritone.settings().freeLook.value) {
						nudgeToLevel();
					}
					target = null;
				}
				if (silent) {
					lastYaw = ctx.player().yaw;
					ctx.player().yaw = target.getYaw();
				}
				break;
			}
			case POST: {
				if (silent) {
					ctx.player().yaw = lastYaw;
					target = null;
				}
				break;
			}
			default:
				break;
		}
	}

	public void pig() {
		if (target != null) {
			ctx.player().yaw = target.getYaw();
		}
	}

	@Override
	public void updateTarget(Rotation target, boolean force) {
		this.target = target;
		if (!force) {
			double rand = Math.random() - 0.5;
			if (Math.abs(rand) < 0.1) {
				rand *= 4;
			}
			this.target = new Rotation(this.target.getYaw() + (float) (rand * Baritone.settings().randomLooking.value), this.target.getPitch());
		}
		this.force = force || !Baritone.settings().freeLook.value;
	}
}
