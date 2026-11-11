package fr.akkun.newmeriarpg.client;

import fr.akkun.newmeriarpg.RpgAttachments;
import fr.akkun.newmeriarpg.RpgData;
import fr.akkun.newmeriarpg.RpgSpell;
import fr.akkun.newmeriarpg.SpellBook;
import fr.akkun.newmeriarpg.network.LearnSpellPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Arrays;
import java.util.List;

/**
 * The Spellbook (an item of NewmeriaCore), opened by right-clicking with it: the list of the spells the
 * player may learn - icon, name and a Learn / Forget / Locked button. Spells restricted to other players
 * (see {@link RpgSpell}) are not listed at all.
 *
 * <p>Learning takes a free spell slot (one per Magic level); "Locked" means there is none left. When the
 * list is longer than the screen, the mouse wheel scrolls it.
 */
public class SpellbookScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int ROW_WIDTH = 220;
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ICON_SIZE = 16;
    /** Title and slot count, above the list. */
    private static final int HEADER_HEIGHT = 30;

    private enum SpellState { LEARNED, LEARNABLE, LOCKED }

    private final List<RpgSpell> spells;
    private int scroll;
    private Button[] buttons = new Button[0];
    private SpellState[] shownStates = new SpellState[0];

    public SpellbookScreen() {
        super(Component.translatable("gui.newmeriarpg.spellbook.title"));
        LocalPlayer player = Minecraft.getInstance().player;
        this.spells = Arrays.stream(RpgSpell.values()).filter(spell -> player != null && spell.isAvailableTo(player)).toList();
    }

    private int visibleRows() {
        return Math.max(1, Math.min(this.spells.size(), (this.height - HEADER_HEIGHT - 40) / ROW_HEIGHT));
    }

    private int top() {
        return (this.height - HEADER_HEIGHT - visibleRows() * ROW_HEIGHT) / 2;
    }

    private int left() {
        return (this.width - ROW_WIDTH) / 2;
    }

    private int rowY(int row) {
        return top() + HEADER_HEIGHT + row * ROW_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.scroll = Math.clamp(this.scroll, 0, Math.max(0, this.spells.size() - visibleRows()));
        int rows = Math.min(visibleRows(), this.spells.size());
        this.buttons = new Button[rows];
        this.shownStates = new SpellState[rows];
        for (int row = 0; row < rows; row++) {
            RpgSpell spell = this.spells.get(this.scroll + row);
            Button button = Button.builder(Component.empty(), b -> onClick(spell))
                    .bounds(left() + ROW_WIDTH - BUTTON_WIDTH, rowY(row) + (ROW_HEIGHT - BUTTON_HEIGHT) / 2, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            this.buttons[row] = button;
            this.addRenderableWidget(button);
        }
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    private static SpellState state(RpgData data, RpgSpell spell) {
        if (data.spells().hasLearned(spell)) {
            return SpellState.LEARNED;
        }
        return data.spells().hasFreeSlot(data.magicLevel()) ? SpellState.LEARNABLE : SpellState.LOCKED;
    }

    private void refreshButtons() {
        RpgData data = currentData();
        for (int row = 0; row < this.buttons.length; row++) {
            SpellState state = state(data, this.spells.get(this.scroll + row));
            if (state == this.shownStates[row]) {
                continue;
            }
            this.shownStates[row] = state;
            Button button = this.buttons[row];
            button.active = state != SpellState.LOCKED;
            button.setMessage(switch (state) {
                case LEARNED -> Component.translatable("gui.newmeriarpg.spellbook.forget");
                case LEARNABLE -> Component.translatable("gui.newmeriarpg.spellbook.learn");
                case LOCKED -> Component.translatable("gui.newmeriarpg.spellbook.locked");
            });
            button.setTooltip(state == SpellState.LOCKED ? Tooltip.create(Component.translatable("gui.newmeriarpg.spellbook.locked.tooltip")) : null);
        }
    }

    private void onClick(RpgSpell spell) {
        boolean learn = !currentData().spells().hasLearned(spell);
        ClientPacketDistributor.sendToServer(new LearnSpellPayload(spell, learn));
    }

    private static RpgData currentData() {
        var player = Minecraft.getInstance().player;
        return player == null ? RpgData.DEFAULT : player.getData(RpgAttachments.RPG_DATA);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, this.spells.size() - visibleRows());
        int newScroll = Math.clamp(this.scroll - (int) Math.signum(scrollY), 0, maxScroll);
        if (newScroll != this.scroll) {
            this.scroll = newScroll;
            this.rebuildWidgets();
        }
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        RpgData data = currentData();
        SpellBook spellBook = data.spells();
        int centerX = this.width / 2;
        int top = top();

        graphics.centeredText(this.font, this.title, centerX, top, -1);
        Component slots = Component.translatable("gui.newmeriarpg.spellbook.slots",
                spellBook.learned().size(), SpellBook.slots(data.magicLevel()), SpellBook.MAX_SLOTS);
        graphics.centeredText(this.font, slots, centerX, top + 12, 0xFFA0A0A0);

        if (this.spells.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("gui.newmeriarpg.spellbook.empty"), centerX, rowY(0) + 6, 0xFFA0A0A0);
            return;
        }
        for (int row = 0; row < this.buttons.length; row++) {
            RpgSpell spell = this.spells.get(this.scroll + row);
            int y = rowY(row);
            graphics.blit(RenderPipelines.GUI_TEXTURED, spell.icon(), left(), y + (ROW_HEIGHT - ICON_SIZE) / 2,
                    0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, -1);
            Component name = spellBook.hasLearned(spell)
                    ? spell.displayName().copy().withStyle(ChatFormatting.GREEN)
                    : spell.displayName();
            graphics.text(this.font, name, left() + ICON_SIZE + 6, y + (ROW_HEIGHT - 8) / 2, -1);
        }
        if (this.spells.size() > this.buttons.length) {
            Component more = Component.translatable("gui.newmeriarpg.spellbook.scroll", this.scroll + 1,
                    this.scroll + this.buttons.length, this.spells.size());
            graphics.centeredText(this.font, more, centerX, rowY(this.buttons.length) + 4, 0xFFA0A0A0);
        }
    }
}
