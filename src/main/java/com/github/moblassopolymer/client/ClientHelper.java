package com.github.moblassopolymer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Isolated client-side access to avoid NoClassDefFoundError on dedicated servers.
 * This class is only loaded when explicitly called from an EnvType.CLIENT context.
 */
public class ClientHelper {
    @Nullable
    public static Level getLevel() {
        return Minecraft.getInstance().level;
    }
}
