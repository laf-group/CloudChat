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
                    String input = StringArgumentType.getString(context, "message");

                    // --- Toggle: enable ---
                    if (input.equals("$on")) {
                        plugin.setGlobalChatEnabled(player.getUniqueId(), true);
                        player.sendMessage(Component.text(
                            "Global chat has been enabled.", NamedTextColor.GREEN));
                        return 1;
                    }

                    // --- Toggle: disable ---
                    if (input.equals("$off")) {
                        plugin.setGlobalChatEnabled(player.getUniqueId(), false);
                        player.sendMessage(Component.text(
                            "Global chat has been disabled.", NamedTextColor.RED));
                        return 1;
                    }

                    // --- Send message (but only if enabled) ---
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