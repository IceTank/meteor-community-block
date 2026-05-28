package org.icetank.communityblock.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.Pair;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import org.icetank.communityblock.modules.ChatBlock;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.UUID;

/**
 * The Meteor Client command API uses the <a href="https://github.com/Mojang/brigadier">same command system as Minecraft does</a>.
 */
public class PlayerInfoCommand extends Command {
    /**
     * The {@code name} parameter should be in kebab-case.
     */
    public PlayerInfoCommand() {
        super("communityblock", "A command to get player information and copy it to the clipboard.", "cb");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("copy").then(argument("playerName", StringArgumentType.word())
            .suggests((context, suggestionsBuilder) -> {
                for (PlayerListEntry player : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
                    suggestionsBuilder.suggest(player.getProfile().getName());
                }
                return suggestionsBuilder.buildFuture();
            })
            .executes(context -> {
                String playerName = StringArgumentType.getString(context, "playerName");
                PlayerListEntry playerEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerList().stream()
                    .filter(entry -> entry.getProfile().getName().equalsIgnoreCase(playerName))
                    .findFirst()
                    .orElse(null);
                if (playerEntry != null) {
                    String name = playerEntry.getProfile().getName();
                    String uuid = playerEntry.getProfile().getId().toString();
                    String toCopy = name + ":" + uuid;

                    // Copy to clipboard
                    mc.keyboard.setClipboard(toCopy);

                    info("Copied to clipboard: " + toCopy);
                } else {
                    info("Player not found: " + playerName);
                }
                return SINGLE_SUCCESS;
            })));

        builder.then(literal("export").executes(c -> {
            var blockedPlayers = Modules.get().get(ChatBlock.class).getBlockedPlayers();
            if (blockedPlayers.isEmpty()) {
                info("No blocked players to export.");
                return SINGLE_SUCCESS;
            }

            StringBuilder sb = new StringBuilder();
            for (Pair<String, UUID> player : blockedPlayers) {
                sb.append(player.left()).append(":").append(player.right().toString()).append("\n");
            }
            String toCopy = sb.toString().trim();
            mc.keyboard.setClipboard(toCopy);
            info("Exported %s blocked players to clipboard".formatted(blockedPlayers.size()));

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("block").then(argument("playerName", StringArgumentType.word())
            .suggests((context, suggestionsBuilder) -> {
                for (PlayerListEntry player : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
                    suggestionsBuilder.suggest(player.getProfile().getName());
                }
                return suggestionsBuilder.buildFuture();
            })
            .executes(context -> {
                String playerName = StringArgumentType.getString(context, "playerName");
                PlayerListEntry playerEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerList().stream()
                    .filter(entry -> entry.getProfile().getName().equalsIgnoreCase(playerName))
                    .findFirst()
                    .orElse(null);

                if (playerEntry == null) {
                    info("Player not found: " + playerName);
                    return SINGLE_SUCCESS;
                }
                if (!Modules.get().get(ChatBlock.class).block(playerEntry.getProfile().getName(), playerEntry.getProfile().getId())) {
                    info("Player is already blocked: " + playerName);
                } else {
                    info("Blocked player: " + playerName);
                }
                return SINGLE_SUCCESS;
            })));

        builder.then(literal("unblock").then(argument("playerName", StringArgumentType.word())
            .suggests((context, suggestionsBuilder) -> {
                for (PlayerListEntry player : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
                    suggestionsBuilder.suggest(player.getProfile().getName());
                }
                return suggestionsBuilder.buildFuture();
            })
            .executes(context -> {
                String playerName = StringArgumentType.getString(context, "playerName");
                PlayerListEntry playerEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerList().stream()
                    .filter(entry -> entry.getProfile().getName().equalsIgnoreCase(playerName))
                    .findFirst()
                    .orElse(null);

                if (playerEntry == null) {
                    info("Player not found: " + playerName);
                    return SINGLE_SUCCESS;
                }
                Modules.get().get(ChatBlock.class).unblock(playerEntry.getProfile().getName());
                info("Unblocked player: " + playerName);
                return SINGLE_SUCCESS;
            })));
    }
}
