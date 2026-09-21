package dev.laf.cloudchat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class CloudChatCommand {

    private CloudChatCommand() {}

    public static BrigadierCommand create(CloudChat plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("globalchat")
            .requires(source -> source instanceof Player)
            .executes(context -> {
                context.getSource().sendMessage(
                    Component.text("Usage: /gc <message> | /gc $on | /gc $off", NamedTextColor.YELLOW)
                );
                return 1;
            })
            .then(RequiredArgumentBuilder
                .<CommandSource, String>argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    Player player = (Player) context.getSource();

                    // Permission gate
                    String permUse = plugin.getConfig().getPermUse();
                    if (!player.hasPermission(permUse)) {
                        player.sendMessage(Component.text(
                            "You don't have permission to use global chat.",
                            NamedTextColor.RED));
                        return 0;
                    }

                    String input = StringArgumentType.getString(context, "message");

                    if (input.equals("$on")) {
                        plugin.setGlobalChatEnabled(player.getUniqueId(), true);
                        player.sendMessage(Component.text(
                            "Global chat has been enabled.", NamedTextColor.GREEN));
                        return 1;
                    }

                    if (input.equals("$off")) {
                        plugin.setGlobalChatEnabled(player.getUniqueId(), false);
                        player.sendMessage(Component.text(
                            "Global chat has been disabled.", NamedTextColor.RED));
                        return 1;
                    }

                    if (!plugin.isGlobalChatEnabled(player.getUniqueId())) {
                        player.sendMessage(Component.text(
                            "You have global chat disabled. Use /gc $on to enable it.",
                            NamedTextColor.RED));
                        return 0;
                    }

                    plugin.sendGlobalMessage(player, input);
                    return 1;
                })
            )
            .build();

        return new BrigadierCommand(node);
    }
}