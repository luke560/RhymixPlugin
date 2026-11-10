package me.speednt;

import me.totalfreedom.api.FreedomAPI;
import me.totalfreedom.api.PluginProvider;
import me.totalfreedom.totalfreedommod.discord.DiscordBridge;
import net.kyori.adventure.text.Component;

public final class RhymixDiscordBridge {

    public static DiscordBridge db;

    public static void sendMessage(String message) {

        FreedomAPI tfm = PluginProvider.get();

        if (tfm == null || !tfm.isEnabled()) {
            return;
        }

        DiscordBridge bridge = db;

        if (bridge == null || !bridge.isReady()) {
            return;
        }

        bridge.relayLoginMessage(
                Component.text(message)
        );

    }
}
