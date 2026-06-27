package com.github.moblassopolymer.init;

import com.github.moblassopolymer.MobLassoPolymer;
import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public class ModComponents {
    public static final DataComponentType<Long> ENTITY_PICK_UP_TIME =
            DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build();

    public static final DataComponentType<Long> ENTITY_RELEASE_TIME =
            DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build();

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                MobLassoPolymer.id("entity_pick_up_time"), ENTITY_PICK_UP_TIME);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                MobLassoPolymer.id("entity_release_time"), ENTITY_RELEASE_TIME);

        // Mark as server-only so vanilla clients don't choke on unknown registry entries
        PolymerComponent.registerDataComponent(ENTITY_PICK_UP_TIME, ENTITY_RELEASE_TIME);
    }
}
