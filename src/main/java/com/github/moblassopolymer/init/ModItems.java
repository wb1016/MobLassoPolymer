package com.github.moblassopolymer.init;

import com.github.moblassopolymer.MobLassoPolymer;
import com.github.moblassopolymer.item.MobLassoItem;
import com.github.moblassopolymer.item.MobLassoItem.LassoType;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {
    private static final ResourceKey<Item> FRIENDLY_KEY =
            ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MobLassoPolymer.MOD_ID, "friendly_lasso"));
    private static final ResourceKey<Item> HOSTILE_KEY =
            ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MobLassoPolymer.MOD_ID, "hostile_lasso"));

    public static final MobLassoItem FRIENDLY_LASSO = new MobLassoItem(
            new Item.Properties()
                    .setId(FRIENDLY_KEY)
                    .stacksTo(1)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false),
            LassoType.FRIENDLY
    );

    public static final MobLassoItem HOSTILE_LASSO = new MobLassoItem(
            new Item.Properties()
                    .setId(HOSTILE_KEY)
                    .stacksTo(1)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false),
            LassoType.HOSTILE
    );

    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;
        Registry.register(BuiltInRegistries.ITEM, FRIENDLY_KEY, FRIENDLY_LASSO);
        Registry.register(BuiltInRegistries.ITEM, HOSTILE_KEY, HOSTILE_LASSO);
    }
}
