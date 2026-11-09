package fr.akkun.newmeriarpg;

import fr.akkun.newmeriacore.entity.PoweredAttacks;
import fr.akkun.newmeriacore.item.WandCastEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import fr.akkun.newmeriarpg.companion.network.OpenCompanionMenuPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-side spell execution and (in-memory, non-persisted) per-player cooldown tracking. */
@EventBusSubscriber(modid = NewmeriaRPG.MOD_ID)
public class SpellCasting {
    /** Right-click with a wand of NewmeriaCore: casts the selected spell. */
    @SubscribeEvent
    public static void onWandCast(WandCastEvent event) {
        if (cast(event.getPlayer())) {
            event.setCast();
        }
    }

    // General cooldown (not per-spell): casting any spell locks out every spell until it runs out.
    private static final Map<UUID, Long> COOLDOWN_END_TICK = new HashMap<>();
    // Teleport spell: how far from the centre of the world a spot may be picked, and how many random
    // spots are tried (most failures are oceans) before giving up. Each try can generate a chunk.
    private static final double TELEPORT_RADIUS = 10_000.0;
    private static final int TELEPORT_ATTEMPTS = 10;

    /** @return whether a spell was actually cast (false if none selected, locked, or still on cooldown). */
    public static boolean cast(ServerPlayer player) {
        RpgData data = player.getData(RpgAttachments.RPG_DATA);
        if (data.selectedSpell() == RpgData.NO_SPELL) {
            return false;
        }
        RpgSpell spell = RpgSpell.values()[data.selectedSpell()];
        if (!spell.isUnlocked(data.magicLevel())) {
            return false;
        }

        if (!player.isCreative()) {
            long now = player.level().getGameTime();
            Long endTick = COOLDOWN_END_TICK.get(player.getUUID());
            if (endTick != null && now < endTick) {
                long remainingSeconds = (endTick - now) / 20 + 1;
                player.sendSystemMessage(Component.translatable("rpg.newmeriarpg.spell.on_cooldown", spell.displayName(), remainingSeconds), true);
                return false;
            }
            COOLDOWN_END_TICK.put(player.getUUID(), now + cooldownTicks(data.magicLevel()));
        }

        switch (spell) {
            case LIGHTNING -> castLightning(player);
            case TELEPORT -> {
                if (!castTeleport(player)) {
                    // Nothing happened: no cooldown (and, for the caller, no wand durability) spent.
                    COOLDOWN_END_TICK.remove(player.getUUID());
                    player.sendSystemMessage(Component.translatable("rpg.newmeriarpg.spell.teleport.failed"), true);
                    return false;
                }
            }
            case FRIENDSHIP -> castFriendship(player);
            case FIREBALL -> castFireball(player);
            case INK_FRIEND -> castInkFriend(player);
        }
        return true;
    }

    /** General cooldown length, driven purely by Magic level (0: 70s, 1: 60s, 2: 45s, 3: 30s, 4: 20s, 5+: 10s). */
    private static int cooldownTicks(int magicLevel) {
        int seconds = switch (magicLevel) {
            case 0 -> 70;
            case 1 -> 60;
            case 2 -> 45;
            case 3 -> 30;
            case 4 -> 20;
            default -> 10;
        };
        return seconds * 20;
    }

    private static void castLightning(ServerPlayer player) {
        ServerLevel level = player.level();
        // Player.pick() only ray-traces blocks; use the entity-aware variant so an entity in the
        // crosshair gets struck directly instead of the ground behind/beyond it.
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, EntitySelector.CAN_BE_PICKED, 100.0);
        PoweredAttacks.strikeLightning(level, hit.getLocation(), player);
    }

    /**
     * Random teleport: sends the player to a random safe spot on the surface of the dimension they
     * are in (of the Overworld from a dimension with a ceiling such as the Nether, which has no
     * surface to speak of).
     *
     * @return false if no safe spot was found within {@link #TELEPORT_ATTEMPTS} tries
     */
    private static boolean castTeleport(ServerPlayer player) {
        ServerLevel level = player.level().dimensionType().hasCeiling() ? player.level().getServer().overworld() : player.level();
        WorldBorder border = level.getWorldBorder();
        // Never closer than a chunk to the world border.
        double radius = Math.min(TELEPORT_RADIUS, border.getSize() / 2.0 - 16.0);
        if (radius <= 0.0) {
            return false;
        }
        RandomSource random = player.getRandom();
        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
            int x = Mth.floor(border.getCenterX() + (random.nextDouble() * 2.0 - 1.0) * radius);
            int z = Mth.floor(border.getCenterZ() + (random.nextDouble() * 2.0 - 1.0) * radius);
            // Loads (generating it if needed) the chunk the spot is in.
            level.getChunk(x >> 4, z >> 4);
            BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!isSafeTeleportSpot(level, feet)) {
                continue;
            }
            if (!player.teleportTo(level, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true)) {
                return false;
            }
            Vec3 pos = player.position();
            level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1.0, pos.z, 32, 0.5, 1.0, 0.5, 0.3);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    /**
     * @param feet the block the player would stand in, the first one above the surface
     * @return whether that is on top of a solid, dry block, with free space all around: nothing to
     *         collide with and no liquid in the 3x3 blocks around the player, at feet and head height
     */
    private static boolean isSafeTeleportSpot(ServerLevel level, BlockPos feet) {
        BlockPos ground = feet.below();
        if (!level.isInsideBuildHeight(ground) || !level.getWorldBorder().isWithinBounds(feet)) {
            return false;
        }
        BlockState groundState = level.getBlockState(ground);
        if (!groundState.isFaceSturdy(level, ground, Direction.UP) || !groundState.getFluidState().isEmpty()) {
            return false;
        }
        for (BlockPos around : BlockPos.betweenClosed(feet.offset(-1, 0, -1), feet.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(around);
            if (!state.getCollisionShape(level, around).isEmpty() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Instantly tames whatever tameable mob the player is looking at, no food/riding grind needed. */
    private static void castFriendship(ServerPlayer player) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, EntitySelector.CAN_BE_PICKED, player.entityInteractionRange());
        if (!(hit instanceof EntityHitResult entityHit)) {
            return;
        }
        Entity target = entityHit.getEntity();
        if (target instanceof TamableAnimal tamable && !tamable.isTame()) {
            tamable.tame(player);
            celebrateTaming(player.level(), target);
        } else if (target instanceof AbstractHorse horse && !horse.isTamed()) {
            horse.setTamed(true);
            horse.setOwner(player);
            celebrateTaming(player.level(), target);
        }
    }

    private static void celebrateTaming(ServerLevel level, Entity tamed) {
        level.sendParticles(ParticleTypes.HEART, tamed.getX(), tamed.getY() + tamed.getBbHeight() * 0.75, tamed.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, tamed.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.7F, 1.4F);
    }

    private static void castFireball(ServerPlayer player) {
        ServerLevel level = player.level();
        PoweredAttacks.launchFireball(level, player, new Vec3(player.getX(), player.getEyeY(), player.getZ()), player.getLookAngle());
    }

    /** Opens the companion-selection menu; the actual summon happens once the player picks a form
     *  (see SummonCompanionPayload) - the cooldown/durability cost is already spent at this point,
     *  matching every other spell, regardless of what the player does with the menu afterward. */
    private static void castInkFriend(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenCompanionMenuPayload());
    }
}
