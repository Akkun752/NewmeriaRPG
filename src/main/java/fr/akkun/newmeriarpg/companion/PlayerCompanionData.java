package fr.akkun.newmeriarpg.companion;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/** Tracks a player's current Ink Friend companion (if any), so summoning a new one can find and
 *  replace the old one. Attached to the player; server-side bookkeeping only, no sync needed. */
public record PlayerCompanionData(Optional<UUID> companionId) {
    public static final PlayerCompanionData NONE = new PlayerCompanionData(Optional.empty());

    public static final MapCodec<PlayerCompanionData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("companion_id").forGetter(PlayerCompanionData::companionId)
    ).apply(instance, PlayerCompanionData::new));

    public static PlayerCompanionData of(UUID companionId) {
        return new PlayerCompanionData(Optional.of(companionId));
    }
}
