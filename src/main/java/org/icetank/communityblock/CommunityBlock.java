package org.icetank.communityblock;

import meteordevelopment.meteorclient.commands.Commands;
import org.icetank.communityblock.commands.PlayerInfoCommand;
import org.icetank.communityblock.modules.ChatBlock;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class CommunityBlock extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Chat Block");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Community Block Addon");

        // Modules
        Modules.get().add(new ChatBlock());

        Commands.add(new PlayerInfoCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "org.icetank.communityblock";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("IceTank", "meteor-community-block");
    }
}
