package dev.laf.cloudchat;

import dev.laf.cloudchat.hooks.LuckPermsHook;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(id = "cloudchat", name = "CloudChat", version = "0.1.0")
public class CloudChat {

    private final ProxyServer server;
    private final Logger logger;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    // Soft rose gold for [GLOBAL]
    private static final String COLOR_GLOBAL = "#E8B4B8";
    // Soft aqua teal for [SERVER-ID]
    private static final String COLOR_SERVER = "#7FDBDA";

    // Players who have global chat DISABLED (default = enabled)
    private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();

    private LuckPermsHook luckPermsHook;

    @Inject
    public CloudChat(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Hook LuckPerms safely
        try {
            this.luckPermsHook = new LuckPermsHook();
            logger.info("Successfully hooked into LuckPerms!");
        } catch (Throwable t) {
            this.luckPermsHook = null;
            logger.warn("LuckPerms not found — prefixes/suffixes/groups will be empty.");
        }

        // Register /globalchat + /gc
        CommandManager cmdManager = server.getCommandManager();
        cmdManager.register(
            cmdManager.metaBuilder("globalchat")
                .aliases("gc")
                .plugin(this)
                .build(),
            CloudChatCommand.create(this)
        );

        logger.info("*-* CloudChat has been enabled! *-*");
    }

    public ProxyServer getServer() {
        return server;
    }

    public boolean isGlobalChatEnabled(UUID uuid) {
        return !disabledPlayers.contains(uuid);
    }

    public void setGlobalChatEnabled(UUID uuid, boolean enabled) {
        if (enabled) {
            disabledPlayers.remove(uuid);
        } else {
            disabledPlayers.add(uuid);
        }
    }

    /**
     * Builds and broadcasts a MiniMessage-formatted global chat message
     * to every player who has global chat enabled, plus the console.
     */
    public void sendGlobalMessage(Player sender, String rawMessage) {
        // Prevent players from injecting MiniMessage tags into their message
        String safeMessage = miniMessage.escapeTags(rawMessage);

        String prefix = "";
        String suffix = "";

        if (luckPermsHook != null) {
            prefix = luckPermsHook.getPrefix(sender);
            suffix = luckPermsHook.getSuffix(sender);
        }

        // Resolve the server-id (e.g. SKYBLOCK, LOBBY)
        String serverId = sender.getCurrentServer()
            // .map(conn -> conn.getServerInfo().getName())
            .map(conn -> conn.getServerInfo().getName().toUpperCase())
            .orElse("UNKNOWN");

        String format = "<dark_gray>[<" + COLOR_GLOBAL + ">GLOBAL<dark_gray>] "
                    + "<dark_gray>[<" + COLOR_SERVER + ">" + serverId + "<dark_gray>] "
                    + prefix
                    + "<white>" + sender.getUsername()
                    + " <dark_gray>» <white>"
                    + safeMessage
                    + suffix;

        Component message = miniMessage.deserialize(format);

        for (Player player : server.getAllPlayers()) {
            if (isGlobalChatEnabled(player.getUniqueId())) {
                player.sendMessage(message);
            }
        }

        server.getConsoleCommandSource().sendMessage(message);
    }
}