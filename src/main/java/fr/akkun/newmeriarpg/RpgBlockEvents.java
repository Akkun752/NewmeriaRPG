package fr.akkun.newmeriarpg;

import fr.akkun.newmeriacore.item.HammerExtraBlockMinedEvent;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * RPG points for mining the blocks listed in {@code rpg/blocks.json} (see {@link RpgPoints}) - but
 * only where the world put them. Every listed block placed by hand is remembered in its chunk
 * ({@link RpgAttachments#PLACED_BLOCKS}) and is worth nothing when mined, so an ore cannot be
 * mined with Silk Touch, put back down and mined again for points.
 */
@EventBusSubscriber(modid = NewmeriaRPG.MOD_ID)
public class RpgBlockEvents {
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot snapshot : multi.getReplacedBlockSnapshots()) {
                remember(level, snapshot.getPos());
            }
        } else {
            remember(level, event.getPos());
        }
    }

    private static void remember(ServerLevel level, BlockPos pos) {
        if (RpgPoints.forBlock(level.getBlockState(pos)) <= 0) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk.getData(RpgAttachments.PLACED_BLOCKS).add(pos.asLong())) {
            chunk.markUnsaved();
        }
    }

    // Lowest priority, and not for cancelled events: only once nothing else stops the block from breaking.
    /** The blocks a Hammer of NewmeriaCore mines around the one broken, which fire no break event of their own. */
    @SubscribeEvent
    public static void onHammerExtraBlockMined(HammerExtraBlockMinedEvent event) {
        onBlockMined(event.getLevel(), event.getPlayer(), event.getPos(), event.getState());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakBlock(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getPlayer() instanceof ServerPlayer player) {
            onBlockMined(level, player, event.getPos(), event.getState());
        }
    }

    /**
     * Called for every block a player mines, the extra ones a Hammer takes out included: gives the
     * block's points if it was not placed by hand and the player's tool can actually harvest it.
     */
    public static void onBlockMined(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        double points = RpgPoints.forBlock(state);
        if (points <= 0) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk.hasData(RpgAttachments.PLACED_BLOCKS)) {
            LongOpenHashSet placed = chunk.getData(RpgAttachments.PLACED_BLOCKS);
            if (placed.remove(pos.asLong())) {
                chunk.markUnsaved();
                return;
            }
        }
        if (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos)) {
            return;
        }
        RpgEvents.grantPoints(player, RpgPoints.roll(points, level.getRandom()));
    }
}
