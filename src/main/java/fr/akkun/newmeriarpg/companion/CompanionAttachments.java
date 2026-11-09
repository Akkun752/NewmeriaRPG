package fr.akkun.newmeriarpg.companion;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class CompanionAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NewmeriaRPG.LEGACY_NAMESPACE);

    /** Only present on entities that actually are an Ink Friend companion - always check with
     *  {@code getExistingData(...)}, there is no meaningful default value. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompanionData>> COMPANION_DATA =
            ATTACHMENT_TYPES.register("companion_data", () -> AttachmentType.<CompanionData>builder(() -> null)
                    .serialize(CompanionData.MAP_CODEC)
                    .sync(CompanionData.STREAM_CODEC)
                    .build());

    /** Present on players; tracks their current companion's UUID, if any. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCompanionData>> PLAYER_COMPANION =
            ATTACHMENT_TYPES.register("player_companion", () -> AttachmentType.builder(() -> PlayerCompanionData.NONE)
                    .serialize(PlayerCompanionData.MAP_CODEC)
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
