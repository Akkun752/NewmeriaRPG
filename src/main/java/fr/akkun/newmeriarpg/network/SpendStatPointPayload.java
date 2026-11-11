package fr.akkun.newmeriarpg.network;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import fr.akkun.newmeriarpg.RpgStat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: buys {@code level} of {@code stat} with one stat point. Only applied if it is the
 * next level of that stat, so that clicking twice before the server answers never buys two levels.
 */
public record SpendStatPointPayload(RpgStat stat, int level) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SpendStatPointPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "spend_stat_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpendStatPointPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(id -> RpgStat.values()[id], RpgStat::ordinal),
            SpendStatPointPayload::stat,
            ByteBufCodecs.VAR_INT,
            SpendStatPointPayload::level,
            SpendStatPointPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
