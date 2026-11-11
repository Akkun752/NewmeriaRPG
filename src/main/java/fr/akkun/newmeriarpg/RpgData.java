package fr.akkun.newmeriarpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RpgData(int level, int pointsIntoLevel, int unspentStatPoints,
                       int forceLevel, int resistanceLevel, int speedLevel, int magicLevel,
                       SpellBook spells) {
    public static final int MIN_RPG_LEVEL = 0;
    public static final int MAX_RPG_LEVEL = 20;
    public static final int MIN_STAT_LEVEL = 0;
    public static final int MAX_STAT_LEVEL = 5;

    public static final RpgData DEFAULT = new RpgData(MIN_RPG_LEVEL, 0, 0,
            MIN_STAT_LEVEL, MIN_STAT_LEVEL, MIN_STAT_LEVEL, MIN_STAT_LEVEL, SpellBook.EMPTY);

    public static final MapCodec<RpgData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(RpgData::level),
            Codec.INT.fieldOf("points_into_level").forGetter(RpgData::pointsIntoLevel),
            Codec.INT.fieldOf("unspent_stat_points").forGetter(RpgData::unspentStatPoints),
            Codec.INT.fieldOf("force_level").forGetter(RpgData::forceLevel),
            Codec.INT.fieldOf("resistance_level").forGetter(RpgData::resistanceLevel),
            Codec.INT.fieldOf("speed_level").forGetter(RpgData::speedLevel),
            Codec.INT.fieldOf("magic_level").forGetter(RpgData::magicLevel),
            SpellBook.fieldOf("spells").forGetter(RpgData::spells)
    ).apply(instance, RpgData::new));

    public static final StreamCodec<ByteBuf, RpgData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RpgData::level,
            ByteBufCodecs.VAR_INT, RpgData::pointsIntoLevel,
            ByteBufCodecs.VAR_INT, RpgData::unspentStatPoints,
            ByteBufCodecs.VAR_INT, RpgData::forceLevel,
            ByteBufCodecs.VAR_INT, RpgData::resistanceLevel,
            ByteBufCodecs.VAR_INT, RpgData::speedLevel,
            ByteBufCodecs.VAR_INT, RpgData::magicLevel,
            SpellBook.STREAM_CODEC, RpgData::spells,
            RpgData::new
    );

    /** Level 0 -> 1 only costs a single RPG point (an onboarding freebie); every level after that
     *  costs 100 points per
     *  level already reached (100 from level 1 to 2, 200 from 2 to 3...). */
    public static int pointsToReachNextLevel(int level) {
        return level == 0 ? 1 : level * 100;
    }

    public RpgData withForceLevel(int newLevel) {
        return new RpgData(level, pointsIntoLevel, unspentStatPoints, newLevel, resistanceLevel, speedLevel, magicLevel, spells);
    }

    public RpgData withResistanceLevel(int newLevel) {
        return new RpgData(level, pointsIntoLevel, unspentStatPoints, forceLevel, newLevel, speedLevel, magicLevel, spells);
    }

    public RpgData withSpeedLevel(int newLevel) {
        return new RpgData(level, pointsIntoLevel, unspentStatPoints, forceLevel, resistanceLevel, newLevel, magicLevel, spells);
    }

    public RpgData withMagicLevel(int newLevel) {
        // Each Magic level is a spell slot: losing levels forgets the spells learned last that no longer fit.
        return new RpgData(level, pointsIntoLevel, unspentStatPoints, forceLevel, resistanceLevel, speedLevel, newLevel, spells.fitTo(newLevel));
    }

    public RpgData withUnspentStatPoints(int newUnspent) {
        return new RpgData(level, pointsIntoLevel, newUnspent, forceLevel, resistanceLevel, speedLevel, magicLevel, spells);
    }

    public RpgData withSpells(SpellBook newSpells) {
        return new RpgData(level, pointsIntoLevel, unspentStatPoints, forceLevel, resistanceLevel, speedLevel, magicLevel, newSpells);
    }

    /**
     * Adds RPG points, cascading level-ups (each granting one stat point) using the
     * {@link #pointsToReachNextLevel} thresholds. Points never reduce the level and never reset on death.
     */
    public RpgData addPoints(int gained) {
        if (gained <= 0 || level >= MAX_RPG_LEVEL) {
            return this;
        }
        int newLevel = level;
        int newPoints = pointsIntoLevel + gained;
        int newUnspent = unspentStatPoints;
        while (newLevel < MAX_RPG_LEVEL) {
            int threshold = pointsToReachNextLevel(newLevel);
            if (newPoints < threshold) {
                break;
            }
            newPoints -= threshold;
            newLevel++;
            newUnspent++;
        }
        if (newLevel >= MAX_RPG_LEVEL) {
            newPoints = 0;
        }
        return new RpgData(newLevel, newPoints, newUnspent, forceLevel, resistanceLevel, speedLevel, magicLevel, spells);
    }

    /**
     * Directly raises the RPG level by {@code amount} (capped at {@link #MAX_RPG_LEVEL}),
     * granting one stat point per level gained.
     */
    public RpgData addLevels(int amount) {
        if (amount <= 0 || level >= MAX_RPG_LEVEL) {
            return this;
        }
        int newLevel = Math.min(MAX_RPG_LEVEL, level + amount);
        int gained = newLevel - level;
        int newPoints = newLevel >= MAX_RPG_LEVEL ? 0 : pointsIntoLevel;
        return new RpgData(newLevel, newPoints, unspentStatPoints + gained, forceLevel, resistanceLevel, speedLevel, magicLevel, spells);
    }

    /**
     * Removes RPG points, cascading level-downs (never below {@link #MIN_RPG_LEVEL}) using the
     * same thresholds as {@link #addPoints}, refilling with the lower level's
     * threshold minus whatever was still left to remove. One unspent stat point is reclaimed per
     * level lost (never dropping already-spent stat levels, and never going negative).
     */
    public RpgData removePoints(int lost) {
        if (lost <= 0) {
            return this;
        }
        int newLevel = level;
        int newPoints = pointsIntoLevel - lost;
        int newUnspent = unspentStatPoints;
        while (newPoints < 0 && newLevel > MIN_RPG_LEVEL) {
            newLevel--;
            newPoints += pointsToReachNextLevel(newLevel);
            newUnspent = Math.max(0, newUnspent - 1);
        }
        if (newPoints < 0) {
            newPoints = 0;
        }
        return new RpgData(newLevel, newPoints, newUnspent, forceLevel, resistanceLevel, speedLevel, magicLevel, spells);
    }

    /**
     * Directly lowers the RPG level by {@code amount} (floored at {@link #MIN_RPG_LEVEL}),
     * resetting progress into the new level to 0 and reclaiming one unspent stat point per level
     * lost (never dropping already-spent stat levels, and never going negative).
     */
    public RpgData removeLevels(int amount) {
        if (amount <= 0) {
            return this;
        }
        int newLevel = Math.max(MIN_RPG_LEVEL, level - amount);
        int actuallyLost = level - newLevel;
        int newUnspent = Math.max(0, unspentStatPoints - actuallyLost);
        return new RpgData(newLevel, 0, newUnspent, forceLevel, resistanceLevel, speedLevel, magicLevel, spells);
    }
}
