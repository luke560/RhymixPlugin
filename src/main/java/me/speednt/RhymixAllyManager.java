
package me.speednt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

public final class RhymixAllyManager implements Listener {

    private static UUID allyUUID;

    public static UUID getAllyUUID() {
        return allyUUID;
    }

    public static Player getAlly() {
        return allyUUID == null
                ? null
                : Bukkit.getPlayer(allyUUID);
    }

    public static boolean isAlly(Player player) {
        return allyUUID != null
                && allyUUID.equals(player.getUniqueId());
    }

    public static void clearAlly() {
        allyUUID = null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(PlayerInteractEntityEvent event) {

        if (!RhymixNPC.exists()) {
            return;
        }

        Mannequin rhymix = RhymixNPC.get();

        if (!event.getRightClicked().getUniqueId()
                .equals(rhymix.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();

        // Already allied with this player.
        if (isAlly(player)) {
            player.sendMessage(Component.text(
                    "Rhymix is already your ally!",
                    NamedTextColor.GREEN
            ));
            return;
        }

        Player previousAlly = getAlly();

        allyUUID = player.getUniqueId();

        // End any existing fight when ownership changes.
        RhymixCombatListener.stopCombat();

        if (previousAlly != null
                && !previousAlly.equals(player)) {

            previousAlly.sendMessage(Component.text(
                    "Rhymix has been claimed by "
                            + player.getName() + "!",
                    NamedTextColor.RED
            ));
        }

        player.sendMessage(Component.text(
                "Rhymix is now your ally! He will follow you.",
                NamedTextColor.GREEN
        ));

        String message = previousAlly == null
                ? player.getName() + " has become Rhymix's ally!"
                : player.getName() + " has stolen Rhymix as an ally!";

        Bukkit.broadcast(Component.text(
                message,
                NamedTextColor.YELLOW
        ));

        RhymixDiscordBridge.sendMessage(message);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onAllyDamage(EntityDamageByEntityEvent event) {

        if (!RhymixNPC.exists()) {
            return;
        }

        if (!event.getEntity().getUniqueId()
                .equals(RhymixNPC.get().getUniqueId())) {
            return;
        }

        if (event.getDamager() instanceof Player player
                && isAlly(player)) {

            // Ally attacks cannot hurt Rhymix.
            event.setCancelled(true);
        }
    }
}
