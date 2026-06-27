package com.github.moblassopolymer;

import com.github.moblassopolymer.config.MobLassoConfig;
import com.github.moblassopolymer.init.ModItems;
import com.github.moblassopolymer.init.ModComponents;
import com.github.moblassopolymer.init.ModGameRules;
import com.github.moblassopolymer.init.ModSounds;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobLassoPolymer implements ModInitializer {
    public static final String MOD_ID = "moblassopolymer";
    public static final Logger LOGGER = LoggerFactory.getLogger("MobLassoPolymer");

    @Override
    public void onInitialize() {
        MobLassoConfig.GSON.load();

        ModComponents.register();
        ModSounds.register();
        ModItems.register();
        ModGameRules.register();

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> {
                    output.accept(ModItems.FRIENDLY_LASSO);
                    output.accept(ModItems.HOSTILE_LASSO);
                });

        PolymerResourcePackUtils.addModAssets(MOD_ID);
        PolymerResourcePackUtils.markAsRequired();

        LOGGER.info("Mob Lasso (Polymer) initialized");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
