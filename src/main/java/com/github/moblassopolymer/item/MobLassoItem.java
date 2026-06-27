package com.github.moblassopolymer.item;

import com.github.moblassopolymer.MobLassoPolymer;
import com.github.moblassopolymer.client.ClientHelper;
import com.github.moblassopolymer.config.MobLassoConfig;
import com.github.moblassopolymer.init.ModComponents;
import com.github.moblassopolymer.init.ModGameRules;
import com.github.moblassopolymer.init.ModSounds;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MobLassoItem extends Item implements PolymerItem {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("MobLassoItem");
    private static final int BAR_COLOR = ARGB.colorFromFloat(1.0F, 0.4F, 0.4F, 1.0F);

    public static final TagKey<EntityType<?>> CAPTURING_NOT_SUPPORTED =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("c:capturing_not_supported"));

    private static final List<String> IGNORED_ENTITY_TAGS = List.of(
            "Air", "Brain", "DeathTime", "fall_distance", "FallFlying", "Fire",
            "HurtTime", "Motion", "NoGravity", "OnGround", "PortalCooldown",
            "Pos", "Rotation", "sleeping_pos",
            "CannotEnterHiveTicks", "TicksSincePollination", "CropsGrownSincePollination",
            "hive_pos", "Passengers", "leash", "UUID"
    );

    public enum LassoType {
        FRIENDLY,
        HOSTILE
    }

    private final LassoType lassoType;
    private final Identifier model;

    public MobLassoItem(Properties properties, LassoType lassoType) {
        super(properties);
        this.lassoType = lassoType;
        this.model = MobLassoPolymer.id(switch (lassoType) {
            case FRIENDLY -> "friendly_lasso";
            case HOSTILE -> "hostile_lasso";
        });
    }

    public LassoType getLassoType() {
        return lassoType;
    }

    // ==================== PolymerItem ====================

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.LEAD;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context,
                                                      HolderLookup.Provider lookup) {
        return model;
    }

    // ==================== Boss detection ====================

    private static final Map<Class<?>, Boolean> BOSS_CACHE = new ConcurrentHashMap<>();

    /**
     * Checks if a mob has a boss bar by looking for a ServerBossEvent field.
     * Works for vanilla (Wither) and modded bosses.
     */
    private static boolean hasBossBar(Mob mob) {
        return BOSS_CACHE.computeIfAbsent(mob.getClass(), clazz -> {
            for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (net.minecraft.server.level.ServerBossEvent.class.isAssignableFrom(field.getType())) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    // ==================== Capture ====================

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                   InteractionHand hand) {
        if (!(target instanceof Mob mob) || !target.isAlive()) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (hasOccupant(stack) || stack.has(ModComponents.ENTITY_RELEASE_TIME)) {
            sendFailure(player, "item.moblassopolymer.failure.occupied");
            return InteractionResult.PASS;
        }
        if (!canCapture(player, mob)) {
            return InteractionResult.PASS;
        }

        if (player.hasInfiniteMaterials()) {
            ItemStack filled = new ItemStack(this);
            storeOccupant(filled, mob, player);
            setPickUpTime(filled, player);
            player.getInventory().placeItemBackInInventory(filled);
        } else {
            storeOccupant(stack, mob, player);
            setPickUpTime(stack, player);
        }
        return InteractionResult.SUCCESS;
    }

    private void setPickUpTime(ItemStack stack, Player player) {
        if (getMaxHoldingTimeTicks() > 0) {
            stack.set(ModComponents.ENTITY_PICK_UP_TIME, player.level().getGameTime());
        }
    }

    private boolean canCapture(Player player, Mob mob) {
        MobLassoConfig cfg = MobLassoConfig.GSON.instance();

        // Boss check — detects mobs with actual boss bars (Wither, modded bosses, etc.)
        if (!cfg.allowBosses && hasBossBar(mob)) {
            sendFailure(player, "item.moblassopolymer.failure.boss", mob.getDisplayName());
            return false;
        }

        // Also check convention tag for edge cases
        if (!cfg.allowBosses
                && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(mob.getType()).is(CAPTURING_NOT_SUPPORTED)) {
            sendFailure(player, "item.moblassopolymer.failure.boss", mob.getDisplayName());
            return false;
        }

        // Tamed mob check (shared)
        if (mob instanceof OwnableEntity ownable && ownable.getOwner() != null) {
            if (!cfg.allowTamedMobs || ownable.getOwner() != player) {
                sendFailure(player, "item.moblassopolymer.failure.tamed", mob.getDisplayName());
                return false;
            }
        }

        // Type-specific checks
        if (lassoType == LassoType.FRIENDLY) {
            if (mob instanceof Enemy) {
                sendFailure(player, "item.moblassopolymer.failure.passive_only", mob.getDisplayName());
                return false;
            }
        } else { // HOSTILE
            if (!(mob instanceof Enemy)) {
                sendFailure(player, "item.moblassopolymer.failure.hostile_only", mob.getDisplayName());
                return false;
            }
            if (cfg.hostileMobMaxHealth < 1.0
                    && mob.getHealth() / mob.getMaxHealth() >= cfg.hostileMobMaxHealth) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(
                            Component.translatable("item.moblassopolymer.failure.health",
                                            mob.getDisplayName(),
                                            String.format("%.0f", cfg.hostileMobMaxHealth * mob.getMaxHealth()),
                                            String.format("%.0f", mob.getHealth()))
                                    .withStyle(ChatFormatting.RED));
                }
                return false;
            }
        }

        return true;
    }

    private static void sendFailure(Player player, String key, Object... args) {
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED));
        }
    }

    private void storeOccupant(ItemStack stack, Mob mob, Player player) {
        mob.stopRiding();
        mob.ejectPassengers();
        mob.dropLeash();

        stack.set(DataComponents.ENTITY_DATA, createEntityData(mob));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        if (mob.hasCustomName()) {
            stack.set(DataComponents.CUSTOM_NAME, mob.getCustomName());
        }

        player.level().playSound(null, player.blockPosition(), ModSounds.LASSO_PICK_UP,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        mob.discard();
    }

    private static TypedEntityData<EntityType<?>> createEntityData(Entity entity) {
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
            entity.saveWithoutId(output);
            IGNORED_ENTITY_TAGS.forEach(output::discard);
            CompoundTag tag = output.buildResult();
            return TypedEntityData.of(entity.getType(), tag);
        }
    }

    // ==================== Release ====================

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!hasOccupant(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        ServerLevel level = (ServerLevel) context.getLevel();
        BlockPos pos = getReleasePosition(context);

        releaseContents(context.getPlayer(), level, stack, pos);
        convertPickUpTimeToCooldown(level, stack);
        discardIfEnabled(level, stack);

        return InteractionResult.CONSUME;
    }

    private BlockPos getReleasePosition(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);
        return state.getCollisionShape(context.getLevel(), pos).isEmpty()
                ? pos
                : pos.relative(context.getClickedFace());
    }

    @SuppressWarnings("unchecked")
    public void releaseContents(@Nullable Entity carrier, ServerLevel level, ItemStack stack, BlockPos pos) {
        if (!stack.has(DataComponents.ENTITY_DATA)) return;

        TypedEntityData<?> data = stack.get(DataComponents.ENTITY_DATA);
        EntityType<?> type = (EntityType<?>) data.type();
        Entity entity = ((EntityType<Entity>) type).create(level,
                EntityType.createDefaultStackConfig(level, stack, null),
                pos, EntitySpawnReason.BUCKET, true, false);

        if (entity != null) {
            level.addFreshEntity(entity);
            level.playSound(null, pos, ModSounds.LASSO_RELEASE, SoundSource.NEUTRAL, 1.0F, 1.0F);
            level.gameEvent(carrier, GameEvent.ENTITY_PLACE, pos);
        }

        stack.remove(DataComponents.ENTITY_DATA);
        stack.remove(DataComponents.CUSTOM_NAME);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
    }

    private void convertPickUpTimeToCooldown(Level level, ItemStack stack) {
        if (!stack.has(ModComponents.ENTITY_PICK_UP_TIME)) return;

        long pickUpTime = stack.get(ModComponents.ENTITY_PICK_UP_TIME);
        stack.remove(ModComponents.ENTITY_PICK_UP_TIME);

        int maxTicks = getMaxHoldingTimeTicks();
        if (maxTicks <= 0) return;

        long elapsed = level.getGameTime() - pickUpTime;
        long cooldownTicks = Math.min(0, elapsed - maxTicks) / 5;
        stack.set(ModComponents.ENTITY_RELEASE_TIME, level.getGameTime() + cooldownTicks);
    }

    // ==================== Inventory Tick ====================

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        // Only process server-side
        if (level.isClientSide()) return;
        boolean hasMob = hasOccupant(stack);

        // Hostile lasso: damage carrier
        if (lassoType == LassoType.HOSTILE && hasMob) {
            int rate = MobLassoConfig.GSON.instance().hostileDamageRate;
            if (rate > 0 && level.getGameTime() % (rate * 20L) == 0) {
                entity.hurtServer(level, level.damageSources().magic(), 1.0F);
            }
        }

        int maxTicks = getMaxHoldingTimeTicks();
        if (maxTicks <= 0) return;

        // Auto-release when time expires
        if (hasMob && stack.has(ModComponents.ENTITY_PICK_UP_TIME)) {
            long elapsed = level.getGameTime() - stack.get(ModComponents.ENTITY_PICK_UP_TIME);
            if (elapsed >= maxTicks) {
                releaseContents(entity, level, stack, entity.blockPosition());
                convertPickUpTimeToCooldown(level, stack);
                discardIfEnabled(level, stack);
            }
        }

        // Clear expired cooldown
        if (stack.has(ModComponents.ENTITY_RELEASE_TIME)) {
            if (stack.get(ModComponents.ENTITY_RELEASE_TIME) <= level.getGameTime()) {
                stack.remove(ModComponents.ENTITY_RELEASE_TIME);
            }
        }
    }

    // ==================== Durability Bar ====================

    @Override
    public boolean isBarVisible(ItemStack stack) {
        int maxTicks = getMaxHoldingTimeTicks();
        return maxTicks > 0 && (stack.has(ModComponents.ENTITY_PICK_UP_TIME)
                || stack.has(ModComponents.ENTITY_RELEASE_TIME));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int maxTicks = getMaxHoldingTimeTicks();
        if (maxTicks <= 0) return 0;

        if (stack.has(ModComponents.ENTITY_PICK_UP_TIME)) {
            Level level = getClientLevel();
            if (level == null) return 13;
            long elapsed = level.getGameTime() - stack.get(ModComponents.ENTITY_PICK_UP_TIME);
            long remaining = Math.max(0, maxTicks - Math.min(elapsed, maxTicks));
            return Math.round(13.0F * remaining / maxTicks);
        }

        if (stack.has(ModComponents.ENTITY_RELEASE_TIME)) {
            Level level = getClientLevel();
            if (level == null) return 0;
            long remaining = stack.get(ModComponents.ENTITY_RELEASE_TIME) - level.getGameTime();
            long cooldownMax = maxTicks / 5L;
            remaining = Math.max(0, Math.min(remaining, cooldownMax));
            return Math.round(13.0F * remaining / cooldownMax);
        }

        return 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Nullable
    private static Level getClientLevel() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return ClientHelper.getLevel();
        }
        return null;
    }

    // ==================== Utility ====================

    public boolean hasOccupant(ItemStack stack) {
        return stack.has(DataComponents.ENTITY_DATA);
    }

    private void discardIfEnabled(ServerLevel level, ItemStack stack) {
        if (level.getGameRules().get(ModGameRules.DISCARD_LASSO_ON_RELEASE)) {
            stack.shrink(1);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public EntityType<?> getStoredEntityType(ItemStack stack) {
        TypedEntityData<?> data = stack.get(DataComponents.ENTITY_DATA);
        return data != null ? (EntityType<?>) data.type() : null;
    }

    private int getMaxHoldingTimeTicks() {
        MobLassoConfig cfg = MobLassoConfig.GSON.instance();
        int seconds = switch (lassoType) {
            case FRIENDLY -> cfg.friendlyHoldingTime;
            case HOSTILE -> cfg.hostileHoldingTime;
        };
        return seconds < 0 ? -1 : seconds * 20;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand,
                                                    ItemStack oldStack, ItemStack newStack) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                 net.minecraft.world.item.component.TooltipDisplay display,
                                 java.util.function.Consumer<Component> builder, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        TypedEntityData<?> data = stack.get(DataComponents.ENTITY_DATA);
        if (data != null) {
            EntityType<?> type = (EntityType<?>) data.type();
            builder.accept(Component.translatable(type.getDescriptionId()).withStyle(ChatFormatting.GRAY));
        }
    }
}
