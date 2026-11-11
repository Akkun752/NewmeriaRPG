package fr.akkun.newmeriarpg.network;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import fr.akkun.newmeriarpg.RpgSpell;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: the Learn ({@code learn} true) or Forget button of a spell in the Spellbook. The
 * server checks everything again (Spellbook in hand, free slot, spell available to the player...).
 */
public record LearnSpellPayload(RpgSpell spell, boolean learn) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LearnSpellPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "learn_spell"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LearnSpellPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> RpgSpell.values()[ordinal], RpgSpell::ordinal),
            LearnSpellPayload::spell,
            ByteBufCodecs.BOOL,
            LearnSpellPayload::learn,
            LearnSpellPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
