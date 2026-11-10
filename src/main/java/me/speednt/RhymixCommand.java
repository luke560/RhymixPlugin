package me.speednt;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RhymixCommand implements CommandExecutor, TabCompleter {

    private final RhymixPlugin plugin;

    public RhymixCommand(RhymixPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            help(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "spawn":

                if (RhymixNPC.exists()) {
                    player.sendMessage(ChatColor.RED + "Rhymix is already spawned.");
                    return true;
                }

                RhymixNPC.spawn(player.getLocation());

                player.sendMessage(ChatColor.GREEN + "Spawned Rhymix.");
                break;

            case "remove":

                if (!RhymixNPC.exists()) {
                    player.sendMessage(ChatColor.RED + "Rhymix is not spawned.");
                    return true;
                }

                RhymixNPC.remove();

                player.sendMessage(ChatColor.GREEN + "Removed Rhymix.");
                break;

            case "tp":

                if (!RhymixNPC.exists()) {
                    player.sendMessage(ChatColor.RED + "Rhymix is not spawned.");
                    return true;
                }

                RhymixNPC.teleport(player.getLocation());

                player.sendMessage(ChatColor.GREEN + "Teleported Rhymix.");
                break;

            case "chat":

                if (!RhymixNPC.exists()) {
                    player.sendMessage(ChatColor.RED + "Rhymix isn't spawned.");
                    return true;
                }

                RhymixNPC.chat();

                break;
            case "ally":
                if (RhymixAllyManager.getAlly() != null) {
                    player.sendMessage(ChatColor.GREEN + "Current Rhymix Ally: " + ChatColor.AQUA + RhymixAllyManager.getAlly().getName());
                } else {
                    player.sendMessage(ChatColor.RED + "Rhymix has no allies...");
                }
                break;

            default:
                help(player);
                break;
        }

        return true;
    }

    private void help(Player player) {

        player.sendMessage(ChatColor.GOLD + "Rhymix Commands");
        player.sendMessage(ChatColor.YELLOW + "/rhymix spawn");
        player.sendMessage(ChatColor.YELLOW + "/rhymix remove");
        player.sendMessage(ChatColor.YELLOW + "/rhymix tp");
        player.sendMessage(ChatColor.YELLOW + "/rhymix chat");
        player.sendMessage(ChatColor.YELLOW + "/rhymix ally");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        List<String> list = new ArrayList<>();

        if (args.length == 1) {

            list.add("spawn");
            list.add("remove");
            list.add("tp");
            list.add("chat");
            list.add("ally");
        }

        return list;
    }

}