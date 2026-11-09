package fr.akkun.newmeriarpg.companion.network;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: the player picked a form (by ordinal into {@code CompanionForm}) in the menu. */
public record SummonCompanionPayload(int form) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SummonCompanionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "summon_companion"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonCompanionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SummonCompanionPayload::form,
            SummonCompanionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
