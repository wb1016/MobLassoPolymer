package com.github.moblassopolymer.init;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public class ModGameRules {
    /** When true, lassos are discarded after releasing a mob. Set to false to keep lassos. */
    public static GameRule<Boolean> DISCARD_LASSO_ON_RELEASE;

    private ModGameRules() {}

    /** Registers the gamerules. Must be called once from {@code onInitialize} (not from {@code <clinit>}). */
    public static void register() {
        DISCARD_LASSO_ON_RELEASE = GameRuleBuilder.forBoolean(true)
                .category(GameRuleCategory.PLAYER)
                .buildAndRegister(Identifier.parse("moblassopolymer:discard_lasso_on_release"));
    }
}
