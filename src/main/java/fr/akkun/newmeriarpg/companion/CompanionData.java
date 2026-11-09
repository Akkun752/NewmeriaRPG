package fr.akkun.newmeriarpg.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Marks an entity as an Ink Friend companion, attached directly to the companion entity itself.
 * Which entity is currently "provoked" (see {@code CompanionEvents}) is tracked separately, in
 * memory only - it's ephemeral combat state, not something that needs to survive a save/reload or
 * be synced to clients.
 */
public record CompanionData(UUID ownerId, CompanionForm form) {
    public static final MapCodec<CompanionData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner_id").forGetter(CompanionData::ownerId),
            Codec.INT.xmap(i -> CompanionForm.values()[i], CompanionForm::ordinal).fieldOf("form").forGetter(CompanionData::form)
    ).apply(instance, CompanionData::new));

    public static final StreamCodec<ByteBuf, CompanionData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CompanionData::ownerId,
            ByteBufCodecs.VAR_INT.map(i -> CompanionForm.values()[i], CompanionForm::ordinal), CompanionData::form,
            CompanionData::new
    );
}
