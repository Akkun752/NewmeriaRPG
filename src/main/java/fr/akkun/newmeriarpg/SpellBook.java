package fr.akkun.newmeriarpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The spells a player has learned in the Spellbook, and the one their wands cast.
 *
 * <p>Each Magic level gives one spell slot, up to {@link #MAX_SLOTS} - the number of slots of the spell
 * wheel. The wheel shows the learned spells in the order they were learned; forgetting one moves the
 * next ones up.
 *
 * @param learned  in the order they were learned, at most as many as the player has slots
 * @param selected the spell wands cast; always one of {@code learned}
 */
public record SpellBook(List<RpgSpell> learned, Optional<RpgSpell> selected) {
    public static final int MAX_SLOTS = 5;
    public static final SpellBook EMPTY = new SpellBook(List.of(), Optional.empty());

    // Saved by id, so that reordering RpgSpell changes nothing; a spell that no longer exists is dropped.
    private static final Codec<Optional<RpgSpell>> SPELL_ID = Codec.STRING.xmap(RpgSpell::byId, spell -> spell.map(RpgSpell::id).orElse(""));

    public static final Codec<SpellBook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SPELL_ID.listOf().<List<RpgSpell>>xmap(
                    spells -> spells.stream().flatMap(Optional::stream).distinct().toList(),
                    spells -> spells.stream().map(Optional::of).toList()
            ).optionalFieldOf("learned", List.of()).forGetter(SpellBook::learned),
            SPELL_ID.optionalFieldOf("selected", Optional.empty()).forGetter(SpellBook::selected)
    ).apply(instance, SpellBook::new));

    private static final StreamCodec<ByteBuf, RpgSpell> SPELL_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ordinal -> RpgSpell.values()[ordinal], RpgSpell::ordinal);

    public static final StreamCodec<ByteBuf, SpellBook> STREAM_CODEC = StreamCodec.composite(
            SPELL_STREAM_CODEC.apply(ByteBufCodecs.list()), SpellBook::learned,
            ByteBufCodecs.optional(SPELL_STREAM_CODEC), SpellBook::selected,
            SpellBook::new
    );

    public SpellBook {
        learned = List.copyOf(learned);
        if (selected.isPresent() && !learned.contains(selected.get())) {
            selected = Optional.empty();
        }
    }

    public static int slots(int magicLevel) {
        return Math.clamp(magicLevel, 0, MAX_SLOTS);
    }

    public boolean hasLearned(RpgSpell spell) {
        return learned.contains(spell);
    }

    public boolean hasFreeSlot(int magicLevel) {
        return learned.size() < slots(magicLevel);
    }

    /** Learned last; selected right away when no spell was. */
    public SpellBook learn(RpgSpell spell) {
        List<RpgSpell> spells = new ArrayList<>(learned);
        spells.add(spell);
        return new SpellBook(spells, selected.isPresent() ? selected : Optional.of(spell));
    }

    public SpellBook forget(RpgSpell spell) {
        List<RpgSpell> spells = new ArrayList<>(learned);
        spells.remove(spell);
        return new SpellBook(spells, selected);
    }

    public SpellBook select(RpgSpell spell) {
        return new SpellBook(learned, Optional.of(spell));
    }

    /** Forgets the spells learned last that no longer fit, after the Magic level went down. */
    public SpellBook fitTo(int magicLevel) {
        int slots = slots(magicLevel);
        return learned.size() <= slots ? this : new SpellBook(learned.subList(0, slots), selected);
    }

    public static MapCodec<SpellBook> fieldOf(String name) {
        return CODEC.optionalFieldOf(name, EMPTY);
    }
}
