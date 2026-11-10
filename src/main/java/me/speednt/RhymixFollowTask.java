
package me.speednt;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RhymixFollowTask extends BukkitRunnable {

    private static final double FOLLOW_DISTANCE = 3.0;

    private static final double STEP_SPEED = 0.40;

    private static final double MAX_STEP_UP = 1.0;
    private static final double MAX_STEP_DOWN = 1.0;

    @Override
    public void run() {

        if (!RhymixNPC.exists()) {
            return;
        }

        if (RhymixNPC.isFighting()) {
            return;
        }


        Player ally = RhymixAllyManager.getAlly();

        if (ally == null || !ally.isOnline() || ally.isDead()) {
            return;
        }

        Mannequin npc = RhymixNPC.get();

        if (!npc.getWorld().equals(ally.getWorld())) {
            return;
        }

        Location current = npc.getLocation();
        Location target = ally.getLocation();

        if (current.distanceSquared(target)
                <= FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
            return;
        }

        Vector direction = target.toVector()
                .subtract(current.toVector());

        direction.setY(0);

        if (direction.lengthSquared() < 0.001) {
            return;
        }

        direction.normalize().multiply(STEP_SPEED);

        Location next = current.clone().add(direction);

        // First try moving on level ground.
        if (isSafe(next)) {
            move(npc, next, direction);
            return;
        }

        // Try stepping up one block.
        Location up = next.clone().add(0, MAX_STEP_UP, 0);

        if (isSafe(up) && isPathClear(current, up)) {
            move(npc, up, direction);
            return;
        }

        // Try stepping down one block.
        Location down = next.clone().subtract(0, MAX_STEP_DOWN, 0);

        if (isSafe(down) && isPathClear(current, down)) {
            move(npc, down, direction);
        }
    }

    private void move(Mannequin npc, Location next, Vector direction) {
        next.setDirection(direction);
        npc.teleport(next);
    }

    private boolean isSafe(Location location) {

        World world = location.getWorld();

        if (world == null) {
            return false;
        }

        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block below = location.clone().subtract(0, 1, 0).getBlock();

        return isOpen(feet)
                && isOpen(head)
                && below.getType().isSolid()
                && !below.isLiquid();
    }

    private boolean isPathClear(Location from, Location to) {

        // Check the destination and a point halfway along
        // the movement to reduce clipping into obstacles.
        Location middle = from.clone().add(
                to.toVector().subtract(from.toVector()).multiply(0.5)
        );

        return isBodyClear(middle) && isBodyClear(to);
    }

    private boolean isBodyClear(Location location) {

        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();

        return isOpen(feet) && isOpen(head);
    }

    private boolean isOpen(Block block) {

        Material type = block.getType();

        return block.isPassable()
                && !block.isLiquid()
                && type != Material.LAVA
                && type != Material.FIRE
                && type != Material.SOUL_FIRE;
    }
}
