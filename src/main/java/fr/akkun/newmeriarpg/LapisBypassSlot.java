package fr.akkun.newmeriarpg;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Replaces the enchanting table's lapis slot for Magic 2+ players (see the {@code
 * EnchantmentMenuMixin} that installs this in place of vanilla's own lapis slot): stops accepting
 * new items entirely (the "always-64-simulated" side of things is handled separately, in that same
 * mixin), and stops showing the "place lapis here" placeholder icon, since the slot is meant to be
 * paired with a custom GUI texture that no longer draws it there. Existing content, if somehow
 * already present, stays retrievable - only placing something new is blocked.
 */
public class LapisBypassSlot extends Slot {
    private static final Identifier EMPTY_LAPIS_ICON = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");

    private final Player player;

    public LapisBypassSlot(Container container, int index, int x, int y, Player player) {
        super(container, index, x, y);
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return !isBypassed() && itemStack.is(Items.LAPIS_LAZULI);
    }

    @Override
    public @Nullable Identifier getNoItemIcon() {
        return isBypassed() ? null : EMPTY_LAPIS_ICON;
    }

    private boolean isBypassed() {
        return player.getData(RpgAttachments.RPG_DATA).magicLevel() >= 2;
    }
}
