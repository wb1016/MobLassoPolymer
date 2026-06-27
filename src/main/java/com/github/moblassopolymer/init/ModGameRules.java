package com.github.moblassopolymer.init;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public class ModGameRules {
    /** When true, lassos are discarded after releasing a mob. Set to false to keep lassos. */
    public static final GameRule<Boolean> DISCARD_LASSO_ON_RELEASE =
            GameRuleBuilder.forBoolean(true)
                    .category(GameRuleCategory.PLAYER)
                    .buildAndRegister(Identifier.parse("moblassopolymer:discardLassoOnRelease"));

    public static void register() {}
}
