package fr.akkun.newmeriarpg.client;

import fr.akkun.newmeriarpg.companion.CompanionForm;
import fr.akkun.newmeriarpg.companion.network.SummonCompanionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Opened by casting Ink Friend: pick a companion form, or close without picking to cancel. */
public class CompanionScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int ICON_SIZE = 16;
    private static final int BUTTON_WIDTH = 90;
    private static final CompanionForm[] FORMS = CompanionForm.values();

    public CompanionScreen() {
        super(Component.translatable("gui.newmeriarpg.companion.title"));
    }

    @Override
    protected void init() {
        super.init();
        int top = topOfRows();
        int centerX = this.width / 2;
        for (int i = 0; i < FORMS.length; i++) {
            CompanionForm form = FORMS[i];
            int y = top + i * ROW_HEIGHT;
            this.addRenderableWidget(Button.builder(Component.translatable("gui.newmeriarpg.companion.summon"), b -> onSummon(form))
                    .bounds(centerX + 60, y, BUTTON_WIDTH, 20)
                    .build());
        }
    }

    private void onSummon(CompanionForm form) {
        ClientPacketDistributor.sendToServer(new SummonCompanionPayload(form.ordinal()));
        Minecraft.getInstance().gui.setScreen(null);
    }

    private int topOfRows() {
        return this.height / 2 - (FORMS.length * ROW_HEIGHT) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int top = topOfRows();
        int centerX = this.width / 2;

        graphics.centeredText(this.font, this.title, centerX, top - 20, -1);

        for (int i = 0; i < FORMS.length; i++) {
            CompanionForm form = FORMS[i];
            int y = top + i * ROW_HEIGHT;
            int iconY = y + (20 - ICON_SIZE) / 2;
            graphics.blit(form.icon(), centerX - 90, iconY, centerX - 90 + ICON_SIZE, iconY + ICON_SIZE, 0.0F, 1.0F, 0.0F, 1.0F);
            graphics.text(this.font, form.displayName(), centerX - 65, y + 6, -1);
        }
    }
}
