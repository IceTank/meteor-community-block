package org.icetank.communityblock.modules;


import it.unimi.dsi.fastutil.Pair;
import org.icetank.communityblock.CommunityBlock;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * @author IceTank
 * @since 28.05.2026
 */
public class ChatBlock extends Module {
    private static final Pattern ChatPattern = Pattern.compile("^<(.*?)> (.*)$");
    private static final Pattern WhisperPattern = Pattern.compile("^(.*?) whispers to you: (.*)$");

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private List<Pair<String, UUID>> blockedPlayers = new CopyOnWriteArrayList<>();

    public ChatBlock() {
        super(CommunityBlock.CATEGORY, "chat-block", "Blocks chat messages from players on the blocked list.");
        updateBlockedPlayers();
    }

    private final Setting<Boolean> spamBots = sgGeneral.add(new BoolSetting.Builder()
        .name("Mute Spam Bots")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> updateBlockedPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("Update Block List")
        .defaultValue(true)
        .build()
    );

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (updateBlockedPlayers.get()) {
            updateBlockedPlayers.set(false);
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (!spamBots.get()) {
            return;
        }
        String message = event.getMessage().getString();

        Matcher chatMatcher = ChatPattern.matcher(message);
        Matcher whisperMatcher = WhisperPattern.matcher(message);
        if (chatMatcher.matches()) {
            String playerName = chatMatcher.group(1);
            if (playerName != null && isPlayerBlocked(playerName)) {
                event.setCancelled(true);
            }
        } else if (WhisperPattern.matcher(message).matches()) {
            String playerName = whisperMatcher.group(1);
            if (playerName != null && isPlayerBlocked(playerName)) {
                event.setCancelled(true);
            }
        }
    }

    private void updateBlockedPlayers() {
        CompletableFuture.runAsync(() -> {
            blockedPlayers.addAll(fetchBlockedPlayers());
            info("Blocked players list updated. Total blocked players: " + blockedPlayers.size());
        });
    }

    private boolean isPlayerBlocked(String playerName) {
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(playerName);
        if (entry != null) {
            return blockedPlayers.stream().anyMatch(pair -> pair.right().equals(entry.getProfile().getId()));
        }
        return false;
    }

    /** fetch updated player list from the github repositories spamBots.txt file */
    private List<Pair<String, UUID>> fetchBlockedPlayers() {
        List<Pair<String, UUID>> blocked = new ArrayList<>();
        try {
            URI uri = URI.create("https://raw.githubusercontent.com/IceTank/meteor-community-block/refs/heads/1.21.4/data/spamBots.txt");
            URL url = uri.toURL();
            try (Scanner scanner = new Scanner(url.openStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.isEmpty()) {
                        var parts = line.split(":");
                        var uuid = parts[1];
                        if (blockedPlayers.stream().anyMatch(pair -> pair.right().toString().equals(uuid))) {
                            continue; // Skip if player is already blocked
                        }
                        try {
                            blocked.add(Pair.of(parts[0], UUID.fromString(parts[1])));
                        } catch (IllegalArgumentException e) {
                            // Ignore invalid UUIDs
                        }
                    }
                }
            } catch (Exception e) {
                error("Failed to fetch blocked players", e);
            }
        } catch (Exception e) {
            error("Invalid URL: " + e.getMessage(), e);
        }
        return blocked;
    }

    public boolean block(String playerName, UUID playerUUID) {
        if (blockedPlayers.stream().anyMatch(pair -> pair.right() == playerUUID)) {
            return false; // Player is already blocked
        }
        blockedPlayers.add(Pair.of(playerName, playerUUID));
        return true;
    }

    public void unblock(String playerName) {
        blockedPlayers.removeIf(pair -> pair.left().equalsIgnoreCase(playerName));
    }

    public List<Pair<String, UUID>> getBlockedPlayers() {
        return new ArrayList<>(blockedPlayers);
    }
}
