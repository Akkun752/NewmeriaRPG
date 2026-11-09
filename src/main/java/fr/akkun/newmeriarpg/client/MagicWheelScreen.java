package fr.akkun.newmeriarpg.client;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import fr.akkun.newmeriarpg.RpgAttachments;
import fr.akkun.newmeriarpg.RpgData;
import fr.akkun.newmeriarpg.RpgSpell;
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

/**
 * A radial spell-select menu: hold the Magic key to show it, hover a slice with the mouse to
 * preview it, then either click the slice or release the Magic key while still hovering it to
 * confirm the selection. Releasing/closing without hovering a valid slice leaves the current
 * selection untouched. Doesn't pause the game ({@link #isPauseScreen()}) and doesn't dim the
 * background, so it reads as a quick overlay rather than a full menu.
 *
 * <p>The wheel always splits into as many equal slices as {@link RpgSpell} has entries - adding a
 * spell there (plus its 16x16 icon at {@code textures/gui/spell/<name>.png}) is the only wiring
 * needed to add a slice.
 */
public class MagicWheelScreen extends Screen {
    // Optional decorative backdrop, blitted stretched to fill the wheel's circle (any square image
    // works, it's stretched to fit) - not required, falls back to the missing-texture checkerboard
    // if absent. Deliberately NOT baking wedge dividers into it: the slice count follows RpgSpell's
    // size dynamically, so a fixed division drawn into the art would drift out of sync as spells
    // are added.
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "textures/gui/spell_wheel_background.png");

    private static final RpgSpell[] SPELLS = RpgSpell.values();
    private static final float OUTER_RADIUS = 90.0F;
    private static final float INNER_RADIUS = 28.125F;
    private static final float ICON_RADIUS = (OUTER_RADIUS + INNER_RADIUS) / 2.0F;
    private static final int ICON_SIZE = 16;

    // Unlocked+unhovered slices are fully transparent so the background art shows through; hovering
    // washes a slice with translucent white, locked slices with a darkening wash instead (regardless
    // of hover, since they can't be selected).
    private static final int BASE_UNLOCKED_COLOR = 0x00000000;
    private static final int BASE_LOCKED_COLOR = 0x80000000;
    private static final int HOVER_UNLOCKED_COLOR = 0x50FFFFFF;
    private static final int HOVER_LOCKED_COLOR = 0x80000000;
    private static final int LOCKED_ICON_TINT = 0xFF606060;
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

    /** @return whether a valid (unlocked) hovered slice was found and sent as the new selection. */
    private boolean confirmHovered(int mouseX, int mouseY) {
        RpgSpell hovered = hoveredSpell(mouseX, mouseY);
        if (hovered == null || !hovered.isUnlocked(currentData().magicLevel())) {
            return false;
        }
        ClientPacketDistributor.sendToServer(new SelectSpellPayload(hovered.ordinal()));
        return true;
    }

    private RpgSpell hoveredSpell(int mouseX, int mouseY) {
        float centerX = this.width / 2.0F;
        float centerY = this.height / 2.0F;
        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        float distance = Mth.sqrt(dx * dx + dy * dy);
        if (distance < INNER_RADIUS || distance > OUTER_RADIUS) {
            return null;
        }
        int index = sliceIndex(dx, dy);
        return SPELLS[index];
    }

    /** Angles are clockwise from straight up, matching {@link SpellWheelWedgeRenderState}. */
    private static int sliceIndex(float dx, float dy) {
        float angle = (float) Math.atan2(dx, -dy);
        if (angle < 0) {
            angle += (float) (Math.PI * 2);
        }
        float sliceAngle = (float) (Math.PI * 2) / SPELLS.length;
        return Mth.clamp((int) (angle / sliceAngle), 0, SPELLS.length - 1);
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
        RpgSpell hovered = hoveredSpell(mouseX, mouseY);
        float centerX = this.width / 2.0F;
        float centerY = this.height / 2.0F;
        float sliceAngle = (float) (Math.PI * 2) / SPELLS.length;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());

        graphics.blit(BACKGROUND, (int) (centerX - OUTER_RADIUS), (int) (centerY - OUTER_RADIUS),
                (int) (centerX + OUTER_RADIUS), (int) (centerY + OUTER_RADIUS), 0.0F, 1.0F, 0.0F, 1.0F);

        for (int i = 0; i < SPELLS.length; i++) {
            RpgSpell spell = SPELLS[i];
            boolean unlocked = spell.isUnlocked(data.magicLevel());
            boolean isHovered = spell == hovered;
            int color = isHovered
                    ? (unlocked ? HOVER_UNLOCKED_COLOR : HOVER_LOCKED_COLOR)
                    : (unlocked ? BASE_UNLOCKED_COLOR : BASE_LOCKED_COLOR);
            float start = i * sliceAngle;
            float end = start + sliceAngle;
            graphics.submitGuiElementRenderState(new SpellWheelWedgeRenderState(
                    RenderPipelines.GUI, TextureSetup.noTexture(), pose,
                    centerX, centerY, OUTER_RADIUS, start, end, color, null));
        }

        float dividerHalfWidthAngle = DIVIDER_HALF_WIDTH_PX / OUTER_RADIUS;
        for (int i = 0; i < SPELLS.length; i++) {
            float boundary = i * sliceAngle;
            graphics.submitGuiElementRenderState(new SpellWheelWedgeRenderState(
                    RenderPipelines.GUI, TextureSetup.noTexture(), pose,
                    centerX, centerY, OUTER_RADIUS, boundary - dividerHalfWidthAngle, boundary + dividerHalfWidthAngle,
                    DIVIDER_COLOR, null));
        }

        for (int i = 0; i < SPELLS.length; i++) {
            RpgSpell spell = SPELLS[i];
            boolean unlocked = spell.isUnlocked(data.magicLevel());
            float midAngle = i * sliceAngle + sliceAngle / 2.0F;
            int iconCenterX = (int) (centerX + ICON_RADIUS * Mth.sin(midAngle));
            int iconY = (int) (centerY - ICON_RADIUS * Mth.cos(midAngle) - ICON_SIZE / 2.0F);
            int iconX = iconCenterX - ICON_SIZE / 2;
            int tint = unlocked ? -1 : LOCKED_ICON_TINT;
            graphics.blit(RenderPipelines.GUI_TEXTURED, spell.icon(), iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, tint);

            int textColor;
            if (data.selectedSpell() == spell.ordinal()) {
                textColor = SELECTED_TEXT_COLOR;
            } else if (unlocked) {
                textColor = -1;
            } else {
                textColor = LOCKED_TEXT_COLOR;
            }
            graphics.centeredText(this.font, spell.displayName(), iconCenterX, iconY + ICON_SIZE + 2, textColor);
        }
    }
}
