package dev.laf.cloudchat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class CloudChatAdminCommand {

    private CloudChatAdminCommand() {}

    public static BrigadierCommand create(CloudChat plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("cloudchat")
            .requires(source -> source.hasPermission(plugin.getConfig().getPermReload()))
            .executes(context -> {
                context.getSource().sendMessage(Component.text(
                    "Usage: /cloudchat reload", NamedTextColor.YELLOW));
                return 1;
            })
            .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    String permReload = plugin.getConfig().getPermReload();
                    if (!source.hasPermission(permReload)) {
                        source.sendMessage(Component.text(
                            "You don't have permission to reload CloudChat.",
                            NamedTextColor.RED));
                        return 0;
                    }
                    try {
                        plugin.reloadConfig();
                        source.sendMessage(Component.text(
                            "CloudChat config reloaded.", NamedTextColor.GREEN));
                        plugin.getLogger().info("Config reloaded by " + source);
                    } catch (Exception e) {
                        source.sendMessage(Component.text(
                            "Failed to reload: " + e.getMessage(), NamedTextColor.RED));
                        plugin.getLogger().error("Failed to reload config", e);
                    }
                    return 1;
                })
            )
            .build();

        return new BrigadierCommand(node);
    }
}