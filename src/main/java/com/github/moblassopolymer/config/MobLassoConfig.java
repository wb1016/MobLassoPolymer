package com.github.moblassopolymer.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

public class MobLassoConfig {
    public static final ConfigClassHandler<MobLassoConfig> GSON = ConfigClassHandler.createBuilder(MobLassoConfig.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("mob-lasso-polymer.json"))
                    .build())
            .build();

    // --- Friendly Lasso (passive mobs) ---

    @SerialEntry(comment = "Seconds the friendly lasso can hold a passive mob. -1 = infinite.")
    public int friendlyHoldingTime = -1;

    // --- Hostile Lasso ---

    @SerialEntry(comment = "Seconds the hostile lasso can hold a hostile mob. -1 = infinite.")
    public int hostileHoldingTime = -1;

    @SerialEntry(comment = "Max health fraction a hostile mob must be below to capture (0.0-1.0).")
    public double hostileMobMaxHealth = 0.5;

    @SerialEntry(comment = "Seconds between damage ticks to carrier holding a hostile mob. -1 = disabled.")
    public int hostileDamageRate = 5;

    // --- Shared ---

    @SerialEntry(comment = "Allow capturing boss mobs.")
    public boolean allowBosses = false;

    @SerialEntry(comment = "Allow capturing tamed mobs (only by their owner).")
    public boolean allowTamedMobs = true;

    /**
     * Clamps hand-edited / out-of-range values. Called once after loading.
     * Sentinel semantics: holding times and damage rate use -1 for "disabled/infinite".
     */
    public void validate() {
        if (friendlyHoldingTime < -1) friendlyHoldingTime = -1;
        if (hostileHoldingTime < -1) hostileHoldingTime = -1;
        if (hostileDamageRate < -1) hostileDamageRate = -1;
        hostileMobMaxHealth = Math.clamp(hostileMobMaxHealth, 0.0, 1.0);
    }
}
