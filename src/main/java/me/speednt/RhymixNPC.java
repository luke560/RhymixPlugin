package me.speednt;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;

import java.util.List;
import java.util.Random;

public final class RhymixNPC {

    private static Mannequin mannequin;
    private static final List<String> QUOTES = List.of(
            "Performance matters.",
            "Another beautiful tick.",
            "Have you optimized your plugins today?",
            "Paper is running smoothly.",
            "20 TPS is the dream.",
            "Clean code makes happy servers.",
            "Remember to profile before optimizing.",
            "Garbage collection is not always the enemy.",
            "I wonder how many plugins are loaded...",
            "Keep your server healthy.",
            "The main thread appreciates you.",
            "No lag today.",
            "Chunk loading is interesting.",
            "I like optimized servers.",
            "I didn't say these things..."
    );

    private static final Random RANDOM = new Random();
    private static WanderTask wanderTask;
    private RhymixCombatListener combatListener;

    private RhymixNPC() {}

    private static boolean fighting = false;

    public static boolean isFighting() {
        return fighting;
    }

    public static void setFighting(boolean value) {
        fighting = value;
    }

    public static boolean exists() {
        return mannequin != null && mannequin.isValid();
    }

    public static Mannequin get() {
        return mannequin;
    }

    public static void spawn(Location location) {

        if (exists()) {
            return;
        }

        mannequin = (Mannequin) location.getWorld().spawnEntity(
                location,
                EntityType.MANNEQUIN
        );

        mannequin.customName(Component.text("RhymixGPT"));
        mannequin.setCustomNameVisible(true);

        mannequin.setDescription(Component.text("Performance matters."));

        mannequin.setProfile(
                ResolvableProfile.resolvableProfile()
                        .name("Rhymix")
                        .build()
        );

        Bukkit.broadcast(
                Component.text("RhymixGPT has joined the game", NamedTextColor.YELLOW)
        );

        RhymixDiscordBridge.sendMessage("*RhymixGPT has joined the game*");

        mannequin.setPersistent(true);
        mannequin.setRemoveWhenFarAway(false);

        AttributeInstance maxHealth =
                mannequin.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealth != null) {
            maxHealth.setBaseValue(500.0);
            mannequin.setHealth(500.0);
        }

        wanderTask = new WanderTask();
        new RhymixMobTargetTask().runTaskTimer(
                RhymixPlugin.get(),
                20L,
                20L
        );
        wanderTask.start();
    }

    public static void chat() {

        if (!exists()) {
            return;
        }

        String message = QUOTES.get(RANDOM.nextInt(QUOTES.size()));

        mannequin.getWorld().getPlayers().forEach(player ->
                player.sendMessage(
                        Component.text("<Rhymix> ", NamedTextColor.YELLOW)
                                .append(Component.text(message, NamedTextColor.WHITE))
                )
        );
        RhymixDiscordBridge.sendMessage("<Rhymix> " + message);
    }

    public static void teleport(Location location) {

        if (!exists()) {
            return;
        }

        mannequin.teleport(location);
        Bukkit.broadcast(
                Component.text("<Rhymix> ", NamedTextColor.YELLOW)
                        .append(Component.text("Teleporting to " + mannequin.getLocation().getX() + ", " + mannequin.getLocation().getY() + ", " + mannequin.getLocation().getZ(), NamedTextColor.WHITE))
        );
        RhymixDiscordBridge.sendMessage("<Rhymix> Teleporting to " + mannequin.getLocation().getX() + ", " + mannequin.getLocation().getY() + ", " + mannequin.getLocation().getZ());
    }

    public static void remove() {

        if (!exists()) {
            return;
        }

        if (wanderTask != null) {
            wanderTask.cancel();
            wanderTask = null;
        }

        RhymixCombatListener.stopCombat();
        RhymixAllyManager.clearAlly();

        mannequin.remove();
        mannequin = null;
    }

}