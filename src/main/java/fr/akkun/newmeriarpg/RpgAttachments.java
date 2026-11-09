package fr.akkun.newmeriarpg;

import com.mojang.serialization.Codec;
import fr.akkun.newmeriarpg.NewmeriaRPG;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

public class RpgAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NewmeriaRPG.LEGACY_NAMESPACE);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RpgData>> RPG_DATA = ATTACHMENT_TYPES.register("rpg_data",
            () -> AttachmentType.builder(() -> RpgData.DEFAULT)
                    .serialize(RpgData.MAP_CODEC)
                    .copyOnDeath()
                    .sync(RpgData.STREAM_CODEC)
                    .build());

    /** On each chunk: the positions (as {@code BlockPos#asLong}) of the blocks worth RPG points that
     *  were placed by hand rather than generated. See {@link RpgBlockEvents}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LongOpenHashSet>> PLACED_BLOCKS = ATTACHMENT_TYPES.register("rpg_placed_blocks",
            () -> AttachmentType.builder(() -> new LongOpenHashSet())
                    .serialize(Codec.LONG.listOf().<LongOpenHashSet>xmap(LongOpenHashSet::new, List::copyOf).fieldOf("positions"))
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
