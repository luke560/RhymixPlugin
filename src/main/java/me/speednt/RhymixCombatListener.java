
package me.speednt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RhymixCombatListener implements Listener {

    private final RhymixPlugin plugin;

    private final List<String> quotes = List.of(
            "OI! WHAT WAS THAT FOR?!",
            "You really wanna fight me?",
            "Bro thinks he can beat Rhymix.",
            "Nah, you're DONE.",
            "Square up then!",
            "I'm literally just walking here.",
            "You picked the wrong Rhymix."
    );

    private final List<String> deathRoasts = List.of(
            "%s, you got folded like a lawn chair.",
            "RIP %s. Skill issue.",
            "%s really thought they could beat Rhymix.",
            "LMAOOO %s got absolutely destroyed.",
            "%s, maybe try fighting someone your own level.",
            "Bro, %s didn't even stand a chance.",
            "%s just got sent back to the lobby.",
            "Imagine losing to me, %s.",
            "%s, that was embarrassing to watch.",
            "Rest in pieces, %s.",
            "%s got humbled REAL quick.",
            "Someone clip that. %s just got cooked."
    );

    private final List<String> escapeRoasts = List.of(
            "%s, WHERE THE F@%& ARE YOU GOING?!",
            "COME BACK HERE, %s!",
            "%s, running away won't save you.",
            "LMAOOO %s is actually scared of me.",
            "%s, bro really chose to run.",
            "You started this fight, %s. FINISH IT!",
            "%s, you can't escape Rhymix...",
            "Look at %s running for their life!",
            "%s, get back here you coward!",
            "Imagine attacking me and then running, %s.",
            "%s is really doing an AJAX right now"
    );

    private BukkitRunnable combatTask;
    private LivingEntity currentTarget;

    public RhymixCombatListener(RhymixPlugin plugin) {
        this.plugin = plugin;
        activeListener = this;
    }

    private static RhymixCombatListener activeListener;

    public static void stopCombat() {
        if (activeListener != null) {
            activeListener.shutdown();
        }
    }



    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {

        if (!RhymixNPC.exists()) {
            return;
        }

        Mannequin npc = RhymixNPC.get();

        // Only reflect projectiles that hit Rhymix.
        if (event.getHitEntity() == null
                || !event.getHitEntity().getUniqueId()
                .equals(npc.getUniqueId())) {
            return;
        }

        Projectile projectile = event.getEntity();

        // Prevent already-reflected projectiles from being reflected again.
        NamespacedKey reflectedKey =
                new NamespacedKey(plugin, "rhymix_reflected");

        if (projectile.getPersistentDataContainer().has(
                reflectedKey,
                PersistentDataType.BYTE
        )) {
            return;
        }

        // Find the original shooter.
        if (!(projectile.getShooter() instanceof LivingEntity shooter)) {
            return;
        }

        // Protect Rhymix's ally.
        if (shooter instanceof Player player
                && RhymixAllyManager.isAlly(player)) {

            event.setCancelled(true);
            projectile.remove();
            return;
        }

        // Lock onto the shooter if Rhymix has no current target.
        // Do not replace an existing living target.
        if (currentTarget == null) {
            startCombat(shooter);
        }

        // Cancel the incoming projectile's collision.
        event.setCancelled(true);

        // Calculate direction toward the original shooter.
        Vector direction = shooter.getEyeLocation().toVector()
                .subtract(npc.getEyeLocation().toVector());

        if (direction.lengthSquared() < 0.001) {
            projectile.remove();
            return;
        }

        direction.normalize();

        // Spawn the reflected projectile in front of Rhymix.
        Location spawnLocation = npc.getEyeLocation()
                .add(direction.clone().multiply(1.2));

        Projectile reflected = (Projectile) npc.getWorld()
                .spawnEntity(spawnLocation, projectile.getType());

        // Rhymix is now the shooter.
        reflected.setShooter(npc);

        // Launch it toward the original shooter.
        reflected.setVelocity(direction.multiply(3.0));

        // Mark it as reflected.
        reflected.getPersistentDataContainer().set(
                reflectedKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        // Reflected arrows deal extra damage.
        if (reflected instanceof AbstractArrow arrow) {
            arrow.setDamage(10.0);
        }

        // Remove the original projectile.
        projectile.remove();

        // Reflection sound.
        npc.getWorld().playSound(
                npc.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.0F,
                0.7F
        );

        // Announce the reflection.
        broadcastRhymix("RETURN TO SENDER!");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {

        if (!RhymixNPC.exists()) {
            return;
        }

        Mannequin npc = RhymixNPC.get();

        if (!event.getEntity().getUniqueId()
                .equals(npc.getUniqueId())) {
            return;
        }

        LivingEntity attacker = getAttacker(event.getDamager());

        if (attacker instanceof Player player
                && RhymixAllyManager.isAlly(player)) {
            return;
        }

        if (attacker == null || attacker.equals(npc)) {
            return;
        }

        if (currentTarget != null) {
            if (!currentTarget.isDead()
                    && currentTarget.getHealth() > 0) {
                return;
            }
        }

        startCombat(attacker);
    }

    private LivingEntity getAttacker(Entity damager) {

        // Direct attacks: players, zombies, spiders, etc.
        if (damager instanceof LivingEntity living) {
            return living;
        }

        // Projectile attacks: skeleton arrows, tridents, etc.
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();

            if (shooter instanceof LivingEntity living) {
                return living;
            }
        }

        return null;
    }

    private void startCombat(LivingEntity attacker) {

        currentTarget = attacker;

        if (combatTask != null) {
            combatTask.cancel();
            combatTask = null;
        }

        RhymixNPC.setFighting(true);

        String quote = quotes.get(
                ThreadLocalRandom.current().nextInt(quotes.size())
        );

        broadcastRhymix(quote);

        Mannequin npc = RhymixNPC.get();

        combatTask = new BukkitRunnable() {

            private int attackCooldown = 0;
            private int escapeMockCooldown = 0;

            @Override
            public void run() {

                // Check death BEFORE checking whether the entity is valid.
                if (attacker.isDead() || attacker.getHealth() <= 0) {
                    roastDefeatedEntity(attacker);
                    stop();
                    return;
                }

                if (!RhymixNPC.exists()
                        || !npc.isValid()
                        || !attacker.isValid()) {

                    stop();
                    return;
                }

                if (attacker instanceof Player player
                        && !player.isOnline()) {

                    stop();
                    return;
                }

                if (attacker instanceof Player player) {
                    punishPlayer(player);
                }

                Location npcLocation = npc.getLocation();
                Location attackerLocation = attacker.getLocation();

                if (!npc.getWorld().equals(attacker.getWorld())) {
                    // Can't chase across worlds.
                    return;
                }

                double distanceSquared =
                        npcLocation.distanceSquared(attackerLocation);
                // Count down the mockery cooldown.
// The combat task runs every 2 ticks.
                if (escapeMockCooldown > 0) {
                    escapeMockCooldown -= 2;
                }

// More than 10 blocks away?
                if (distanceSquared > 100.0) {

                    if (escapeMockCooldown <= 0) {

                        mockEscapingTarget(attacker);

                        // 200 ticks = 10 seconds
                        escapeMockCooldown = 200;
                    }

                } else {

                    // Reset when the target comes back within 10 blocks.
                    escapeMockCooldown = 0;
                }


// Chase and face the enemy using ONE teleport.
                Vector direction = attackerLocation.toVector()
                        .subtract(npcLocation.toVector());

                direction.setY(0);

                if (direction.lengthSquared() > 0.001) {

                    direction.normalize();

                    Location next = npcLocation.clone();

                    // Chase the enemy if more than 2 blocks away.
                    if (distanceSquared > 4.0) {
                        next.add(direction.clone().multiply(0.30));
                    }

                    // Face the enemy without resetting our position.
                    next.setDirection(direction);

                    npc.teleport(next);
                }

                double attackDistanceSquared =
                        npc.getLocation().distanceSquared(attacker.getLocation());

                if (attackCooldown <= 0 && attackDistanceSquared <= 6.25) {

                    // Deal damage
                    attacker.damage(3.0, npc);

                    // Check whether Rhymix just killed the opponent.
                    if (attacker.isDead() || attacker.getHealth() <= 0) {
                        roastDefeatedEntity(attacker);
                        stop();
                        return;
                    }

                    // Direction away from Rhymix
                    Vector knockback = attacker.getLocation().toVector()
                            .subtract(npc.getLocation().toVector());

                    knockback.setY(0);

                    if (knockback.lengthSquared() < 0.001) {
                        knockback = npc.getLocation().getDirection();
                        knockback.setY(0);
                    }

                    knockback.normalize();

                    // Crazy knockback
                    knockback.multiply(2.0);
                    knockback.setY(0.4);

                    attacker.setVelocity(knockback);

                    attackCooldown = 20;
                }

                if (attackCooldown > 0) {
                    attackCooldown -= 2;
                }

            }

            private void stop() {
                cancel();
                combatTask = null;
                currentTarget = null;
                RhymixNPC.setFighting(false);
            }
        };

        combatTask.runTaskTimer(plugin, 0L, 2L);
    }

    private void roastDefeatedEntity(LivingEntity defeated) {

        String name;

        if (defeated instanceof Player player) {
            name = player.getName();
        } else if (defeated.customName() != null) {
            name = net.kyori.adventure.text.serializer.plain
                    .PlainTextComponentSerializer.plainText()
                    .serialize(defeated.customName());
        } else {
            // ZOMBIE -> Zombie, ZOMBIFIED_PIGLIN -> Zombified Piglin
            String typeName = defeated.getType().name()
                    .toLowerCase()
                    .replace('_', ' ');

            name = Character.toUpperCase(typeName.charAt(0))
                    + typeName.substring(1);
        }

        String roast = deathRoasts.get(
                ThreadLocalRandom.current().nextInt(deathRoasts.size())
        );

        broadcastRhymix(String.format(roast, name));
    }

    private void punishPlayer(Player attacker) {

        // Only force Creative players into Survival.
        if (attacker.getGameMode() == GameMode.CREATIVE) {

            attacker.setGameMode(GameMode.SURVIVAL);

            broadcastRhymix(
                    "HAH, thought you could cheese me huh? "
                            + "Thought you could do a shroomp on me?"
            );
        }

        attacker.playSound(
                attacker.getLocation(),
                Sound.AMBIENT_CAVE,
                10.0F,
                10.0F
        );

        attacker.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.DARKNESS,
                        20 * 10,
                        0,
                        false,
                        true,
                        true
                )
        );

        attacker.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.NAUSEA,
                        20 * 10,
                        0,
                        false,
                        true,
                        true
                )
        );

        attacker.sendMessage(
                Component.text(
                        "You shouldn't have attacked Rhymix...",
                        NamedTextColor.DARK_RED
                )
        );
    }

    private void mockEscapingTarget(LivingEntity target) {

        String name;

        if (target instanceof Player player) {
            name = player.getName();
        } else if (target.customName() != null) {
            name = net.kyori.adventure.text.serializer.plain
                    .PlainTextComponentSerializer.plainText()
                    .serialize(target.customName());
        } else {
            String typeName = target.getType().name()
                    .toLowerCase()
                    .replace('_', ' ');

            name = Character.toUpperCase(typeName.charAt(0))
                    + typeName.substring(1);
        }

        String roast = escapeRoasts.get(
                ThreadLocalRandom.current().nextInt(escapeRoasts.size())
        );

        broadcastRhymix(String.format(roast, name));
    }

    private void broadcastRhymix(String message) {

        Bukkit.broadcast(
                Component.text("<Rhymix> ", NamedTextColor.YELLOW)
                        .append(Component.text(
                                message,
                                NamedTextColor.WHITE
                        ))
        );

        RhymixDiscordBridge.sendMessage(
                "<Rhymix> " + message
        );
    }

    public void shutdown() {

        if (combatTask != null) {
            combatTask.cancel();
            combatTask = null;
        }

        currentTarget = null;
        RhymixNPC.setFighting(false);
    }
}
