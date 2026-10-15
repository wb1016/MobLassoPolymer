package com.github.moblassopolymer.init;

import net.minecraft.sounds.SoundEvents;

/**
 * Reuses vanilla beehive sounds for the lasso.
 * To use custom sounds, register new SoundEvents here and update MobLassoItem.
 */
public final class ModSounds {
    public static final net.minecraft.sounds.SoundEvent LASSO_PICK_UP = SoundEvents.BEEHIVE_ENTER;
    public static final net.minecraft.sounds.SoundEvent LASSO_RELEASE = SoundEvents.BEEHIVE_EXIT;

    private ModSounds() {}
}
