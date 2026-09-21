package dev.laf.cloudchat;

import dev.laf.cloudchat.hooks.LuckPermsHook;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(id = "cloudchat", name = "CloudChat", version = "0.1.0")
public class CloudChat {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();

    private LuckPermsHook luckPermsHook;
    private CloudChatConfig config;

    @Inject
    public CloudChat(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Load config
        this.config = new CloudChatConfig(dataDirectory);
        try {
            config.load();
            logger.info("Loaded config.yml");
        } catch (IOException e) {
            logger.error("Failed to load config.yml — plugin disabled.", e);
            return;
        }

        // LuckPerms
        try {
            this.luckPermsHook = new LuckPermsHook();
            logger.info("Successfully hooked into LuckPerms!");
        } catch (Throwable t) {
            this.luckPermsHook = null;
            logger.warn("LuckPerms not found — prefixes/suffixes will be empty.");
        }

        CommandManager cmdManager = server.getCommandManager();

        cmdManager.register(
            cmdManager.metaBuilder("globalchat")
                .aliases("gc")
                .plugin(this)
                .build(),
            CloudChatCommand.create(this)
        );

        cmdManager.register(
            cmdManager.metaBuilder("cloudchat")
                .aliases("cc")
                .plugin(this)
                .build(),
            CloudChatAdminCommand.create(this)
        );

        logger.info("*-* CloudChat has been enabled! *-*");
    }

    public void reloadConfig() throws IOException {
        config.load();
    }

    public ProxyServer getServer()       { return server; }
    public Logger getLogger()            { return logger; }
    public CloudChatConfig getConfig()   { return config; }
    public LuckPermsHook getLuckPermsHook() { return luckPermsHook; }

    public boolean isGlobalChatEnabled(UUID uuid) {
        return !disabledPlayers.contains(uuid);
    }

    public void setGlobalChatEnabled(UUID uuid, boolean enabled) {
        if (enabled) disabledPlayers.remove(uuid);
        else disabledPlayers.add(uuid);
    }

    public void sendGlobalMessage(Player sender, String rawMessage) {
        // Escape user input so they cannot inject MiniMessage tags
        String safeMessage = miniMessage.escapeTags(rawMessage);

        String prefix = "";
        String suffix = "";
        String group = "default";
        if (luckPermsHook != null) {
            prefix = luckPermsHook.getPrefix(sender);
            suffix = luckPermsHook.getSuffix(sender);
            group  = luckPermsHook.getPrimaryGroup(sender);
        }

        String serverId = sender.getCurrentServer()
            .map(conn -> conn.getServerInfo().getName())
            .orElse("UNKNOWN");

        // Resolve parts first (a part may itself reference builtin placeholders)
        String partGlobal = config.getPartGlobal();
        String partServer = config.getPartServer()
            .replace("%server%", serverId);
        String partPlayer = config.getPartPlayer()
            .replace("%prefix%", prefix)
            .replace("%player%", sender.getUsername())
            .replace("%suffix%", suffix);

        // Resolve the final layout
        String format = config.getMessageFormat()
            .replace("%part_global%", partGlobal)
            .replace("%part_server%", partServer)
            .replace("%part_player%", partPlayer)
            .replace("%server%", serverId)
            .replace("%player%", sender.getUsername())
            .replace("%prefix%", prefix)
            .replace("%suffix%", suffix)
            .replace("%group%", group)
            .replace("%message%", safeMessage);

        Component message = miniMessage.deserialize(format);

        for (Player player : server.getAllPlayers()) {
            if (isGlobalChatEnabled(player.getUniqueId())) {
                player.sendMessage(message);
            }
        }
        server.getConsoleCommandSource().sendMessage(message);
    }
}