package org.icetank.communityblock.modules;


import org.icetank.communityblock.CommunityBlock;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
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

    private List<UUID> blockedPlayers = new ArrayList<>();

    public ChatBlock() {
        super(CommunityBlock.CATEGORY, "world-origin", "An example module that highlights the center of the world.");
    }

    private final Setting<Boolean> spamBots = sgGeneral.add(new BoolSetting.Builder()
        .name("Spam Bots")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> updateBlockedPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("Update Blocked Players")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> sourceRepository = sgGeneral.add(new StringSetting.Builder()
        .name("Source Repository")
        .description("The URL of the source repository for this module.")
        .defaultValue("https://github.com/IceTank/meteor-community-block")
        .build()
    );

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (updateBlockedPlayers.get()) {
            updateBlockedPlayers.set(false);
            blockedPlayers.clear();
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
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

    private boolean isPlayerBlocked(String playerName) {
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(playerName);
        if (entry != null) {
            return blockedPlayers.contains(entry.getProfile().getId());
        }
        return false;
    }

    /** fetch updated player list from the github repositories spamBots.txt file */
    private List<UUID> fetchBlockedPlayers() {
        List<UUID> blocked = new ArrayList<>();
        try {
            URI uri = URI.create("https://raw.githubusercontent.com/IceTank/meteor-community-block/refs/heads/master/spamBots.txt");
            URL url = uri.toURL();
            try (Scanner scanner = new Scanner(url.openStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.isEmpty()) {
                        try {
                            blocked.add(UUID.fromString(line));
                        } catch (IllegalArgumentException e) {
                            // Ignore invalid UUIDs
                        }
                    }
                }
            } catch (Exception e) {
                error("Failed to fetch blocked players", e);
            }
        } catch (Exception e) {
            error("Invalid URL: " + sourceRepository.get());
        }
        return blocked;
    }
}
