package me.speednt;

public class CommandManager {
    public CommandManager(RhymixPlugin plugin) {
        RhymixCommand command = new RhymixCommand(plugin);

        plugin.getCommand("rhymix").setExecutor(command);
        plugin.getCommand("rhymix").setTabCompleter(command);
    }
}