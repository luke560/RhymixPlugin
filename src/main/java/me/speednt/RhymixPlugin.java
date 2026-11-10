package me.speednt;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class RhymixPlugin extends JavaPlugin {

    private static RhymixPlugin instance;

    private RhymixCombatListener combatListener;
    private RhymixFollowTask followTask;

    @Override
    public void onEnable() {
        instance = this;
        new CommandManager(this);

        combatListener = new RhymixCombatListener(this);
        getServer().getPluginManager().registerEvents(
                combatListener,
                this
        );

        getServer().getPluginManager().registerEvents(
                new RhymixAllyManager(),
                this
        );

        followTask = new RhymixFollowTask();
        followTask.runTaskTimer(this, 20L, 2L);

        Plugin tfm = Bukkit.getPluginManager()
                .getPlugin("TotalFreedomMod");

        if (tfm == null || !tfm.isEnabled()) {
            getLogger().warning("TotalFreedomMod is unavailable!");
            return;
        }

        getLogger().info("RhymixPlugin enabled.");
    }

    @Override
    public void onDisable() {
        if (combatListener != null) {
            combatListener.shutdown();
        }

        if (followTask != null) {
            followTask.cancel();
        }

        RhymixAllyManager.clearAlly();

        getLogger().info("RhymixPlugin disabled.");
    }

    public static RhymixPlugin get() {
        return instance;
    }
}