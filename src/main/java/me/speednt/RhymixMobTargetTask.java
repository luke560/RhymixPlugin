package me.speednt;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitRunnable;

public class RhymixMobTargetTask extends BukkitRunnable {

    @Override
    public void run() {
        if (!RhymixNPC.exists()) {
            return;
        }

        Mannequin rhymix = RhymixNPC.get();

        for (Entity entity : rhymix.getNearbyEntities(40, 40, 40)) {

            if (!(entity instanceof Mob mob)) {
                continue;
            }

            if (!(mob instanceof Enemy)) {
                continue;
            }

            if (!mob.isValid() || mob.isDead()) {
                continue;
            }

            // Don't override mobs already attacking something.
            if (mob.getTarget() != null) {
                continue;
            }

            mob.setTarget(rhymix);
            mob.lookAt(rhymix);
        }
    }
}