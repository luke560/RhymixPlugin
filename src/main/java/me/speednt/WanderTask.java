package me.speednt;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class WanderTask extends BukkitRunnable {

    private final double speed = 0.15;

    private Location target;

    @Override
    public void run() {

        if (!RhymixNPC.exists()) {
            cancel();
            return;
        }

        if (RhymixNPC.isFighting()) {
            return;
        }

        if (RhymixAllyManager.getAllyUUID() != null) {
            return;
        }

        var npc = RhymixNPC.get();

        Location current = npc.getLocation();

        if (target == null || current.distanceSquared(target) < 1.0) {
            target = randomTarget(current);
        }

        Vector movement = target.toVector()
                .subtract(current.toVector());

        if (movement.lengthSquared() < 0.001)
            return;

        movement.normalize().multiply(speed);

        Location next = current.clone().add(movement);

        // Face the direction of travel
        next.setDirection(movement);

        npc.teleport(next);
    }

    private Location randomTarget(Location center) {

        World world = center.getWorld();

        double radius = 8;

        double x = center.getX() + ThreadLocalRandom.current().nextDouble(-radius, radius);
        double z = center.getZ() + ThreadLocalRandom.current().nextDouble(-radius, radius);

        int y = world.getHighestBlockYAt((int) x, (int) z);

        return new Location(world, x, y + 1, z);
    }

    public void start() {
        runTaskTimer(RhymixPlugin.get(), 20L, 1L);
    }

}