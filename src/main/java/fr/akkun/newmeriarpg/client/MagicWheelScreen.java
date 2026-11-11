package fr.akkun.newmeriarpg.client;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import fr.akkun.newmeriarpg.RpgAttachments;
import fr.akkun.newmeriarpg.RpgData;
import fr.akkun.newmeriarpg.RpgSpell;
import fr.akkun.newmeriarpg.SpellBook;
import fr.akkun.newmeriarpg.network.SelectSpellPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * A radial spell-select menu: hold the Magic key to show it, hover a slice with the mouse to
 * preview it, then either click the slice or release the Magic key while still hovering it to
 * confirm the selection. Releasing/closing without hovering a spell leaves the current selection
 * untouched. Doesn't pause the game ({@link #isPauseScreen()}) and doesn't dim the background, so it
 * reads as a quick overlay rather than a full menu.
 *
 * <p>The wheel always has {@link SpellBook#MAX_SLOTS} slices, one per spell slot. They hold the spells
 * learned in the Spellbook, in the order they were learned; then come the free slots (Magic levels
 * with no spell learned yet), then the locked ones (Magic levels not reached).
 */
public class MagicWheelScreen extends Screen {
    // Optional decorative backdrop, blitted stretched to fill the wheel's circle (any square image
    // works, it's stretched to fit) - falls back to the missing-texture checkerboard if absent.
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "textures/gui/spell_wheel_background.png");

    private static final int SLOTS = SpellBook.MAX_SLOTS;
    private static final float OUTER_RADIUS = 90.0F;
    private static final float INNER_RADIUS = 28.125F;
    private static final float ICON_RADIUS = (OUTER_RADIUS + INNER_RADIUS) / 2.0F;
    private static final int ICON_SIZE = 16;

    // Slices holding a spell are fully transparent so the background art shows through; hovering
    // washes them with translucent white. Slices without a spell get a darkening wash instead
    // (regardless of hover, since they can't be selected), darker when locked.
    private static final int BASE_SPELL_COLOR = 0x00000000;
    private static final int HOVER_SPELL_COLOR = 0x50FFFFFF;
    private static final int FREE_SLOT_COLOR = 0x50000000;
    private static final int LOCKED_SLOT_COLOR = 0x80000000;
    private static final int EMPTY_TEXT_COLOR = 0xFF909090;
    private static final int LOCKED_TEXT_COLOR = 0xFF606060;
    private static final int SELECTED_TEXT_COLOR = 0xFF55FF55;

    // Divider "spokes" between slices, drawn as very thin wedges (tapering from a point at the
    // center to DIVIDER_HALF_WIDTH_PX*2 wide at the rim) reusing the same wedge primitive as the
    // slice fills - no separate line-drawing code needed.
    private static final int DIVIDER_COLOR = 0xB0FFFFFF;
    private static final float DIVIDER_HALF_WIDTH_PX = 0.75F;

    private int lastMouseX;
    private int lastMouseY;

    public MagicWheelScreen() {
        super(Component.translatable("gui.newmeriarpg.magic_wheel.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Called by RpgKeybinds the instant the Magic key is released. */
    public void closeAndConfirm() {
        confirmHovered(lastMouseX, lastMouseY);
        Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && confirmHovered((int) event.x(), (int) event.y())) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** @return whether a slice holding a spell was hovered, and sent as the new selection. */
    private boolean confirmHovered(int mouseX, int mouseY) {
        int slot = hoveredSlot(mouseX, mouseY);
        RpgSpell spell = slot < 0 ? null : spellIn(currentData().spells(), slot);
        if (spell == null) {
            return false;
        }
        ClientPacketDistributor.sendToServer(new SelectSpellPayload(spell));
        return true;
    }

    private static @Nullable RpgSpell spellIn(SpellBook spells, int slot) {
        return slot < spells.learned().size() ? spells.learned().get(slot) : null;
    }

    /** @return the index of the hovered slice, or -1 */
    private int hoveredSlot(int mouseX, int mouseY) {
        float dx = mouseX - this.width / 2.0F;
        float dy = mouseY - this.height / 2.0F;
        float distance = Mth.sqrt(dx * dx + dy * dy);
        if (distance < INNER_RADIUS || distance > OUTER_RADIUS) {
            return -1;
        }
        return sliceIndex(dx, dy);
    }

    /** Angles are clockwise from straight up, matching {@link SpellWheelWedgeRenderState}. */
    private static int sliceIndex(float dx, float dy) {
        float angle = (float) Math.atan2(dx, -dy);
        if (angle < 0) {
            angle += (float) (Math.PI * 2);
        }
        float sliceAngle = (float) (Math.PI * 2) / SLOTS;
        return Mth.clamp((int) (angle / sliceAngle), 0, SLOTS - 1);
    }

    private static RpgData currentData() {
        var player = Minecraft.getInstance().player;
        return player == null ? RpgData.DEFAULT : player.getData(RpgAttachments.RPG_DATA);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        RpgData data = currentData();
        SpellBook spells = data.spells();
        int unlockedSlots = SpellBook.slots(data.magicLevel());
        int hovered = hoveredSlot(mouseX, mouseY);
        float centerX = this.width / 2.0F;
        float centerY = this.height / 2.0F;
        float sliceAngle = (float) (Math.PI * 2) / SLOTS;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());

        graphics.blit(BACKGROUND, (int) (centerX - OUTER_RADIUS), (int) (centerY - OUTER_RADIUS),
                (int) (centerX + OUTER_RADIUS), (int) (centerY + OUTER_RADIUS), 0.0F, 1.0F, 0.0F, 1.0F);

        for (int i = 0; i < SLOTS; i++) {
            int color;
            if (spellIn(spells, i) != null) {
                color = i == hovered ? HOVER_SPELL_COLOR : BASE_SPELL_COLOR;
            } else {
                color = i < unlockedSlots ? FREE_SLOT_COLOR : LOCKED_SLOT_COLOR;
            }
            float start = i * sliceAngle;
            graphics.submitGuiElementRenderState(new SpellWheelWedgeRenderState(
                    RenderPipelines.GUI, TextureSetup.noTexture(), pose,
                    centerX, centerY, OUTER_RADIUS, start, start + sliceAngle, color, null));
        }

        float dividerHalfWidthAngle = DIVIDER_HALF_WIDTH_PX / OUTER_RADIUS;
        for (int i = 0; i < SLOTS; i++) {
            float boundary = i * sliceAngle;
            graphics.submitGuiElementRenderState(new SpellWheelWedgeRenderState(
                    RenderPipelines.GUI, TextureSetup.noTexture(), pose,
                    centerX, centerY, OUTER_RADIUS, boundary - dividerHalfWidthAngle, boundary + dividerHalfWidthAngle,
                    DIVIDER_COLOR, null));
        }

        for (int i = 0; i < SLOTS; i++) {
            float midAngle = i * sliceAngle + sliceAngle / 2.0F;
            int iconCenterX = (int) (centerX + ICON_RADIUS * Mth.sin(midAngle));
            int iconY = (int) (centerY - ICON_RADIUS * Mth.cos(midAngle) - ICON_SIZE / 2.0F);
            RpgSpell spell = spellIn(spells, i);
            if (spell == null) {
                boolean free = i < unlockedSlots;
                Component label = Component.translatable(free ? "gui.newmeriarpg.magic_wheel.free_slot" : "gui.newmeriarpg.magic_wheel.locked_slot");
                graphics.centeredText(this.font, label, iconCenterX, iconY + ICON_SIZE / 2 - 4, free ? EMPTY_TEXT_COLOR : LOCKED_TEXT_COLOR);
                continue;
            }
            int iconX = iconCenterX - ICON_SIZE / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, spell.icon(), iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, -1);
            int textColor = spells.selected().orElse(null) == spell ? SELECTED_TEXT_COLOR : -1;
            graphics.centeredText(this.font, spell.displayName(), iconCenterX, iconY + ICON_SIZE + 2, textColor);
        }
    }
}
