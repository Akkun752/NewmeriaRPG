package fr.akkun.newmeriarpg.client;

import fr.akkun.newmeriarpg.RpgAttachments;
import fr.akkun.newmeriarpg.RpgData;
import fr.akkun.newmeriarpg.RpgStat;
import fr.akkun.newmeriarpg.network.SpendStatPointPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.EnumMap;
import java.util.Map;

public class StatScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_WIDTH = 20;
    private static final RpgStat[] STATS = RpgStat.values();

    private final Map<RpgStat, Button> buttons = new EnumMap<>(RpgStat.class);

    public StatScreen() {
        super(Component.translatable("gui.newmeriarpg.stats.title"));
    }

    @Override
    protected void init() {
        super.init();
        int top = topOfRows();
        int centerX = this.width / 2;
        for (int i = 0; i < STATS.length; i++) {
            RpgStat stat = STATS[i];
            int y = top + i * ROW_HEIGHT;
            Button button = Button.builder(Component.literal("+"), b -> onSpend(stat))
                    .bounds(centerX + 80, y, BUTTON_WIDTH, 20)
                    .build();
            this.buttons.put(stat, button);
            this.addRenderableWidget(button);
        }
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    private int topOfRows() {
        return this.height / 2 - (STATS.length * ROW_HEIGHT) / 2;
    }

    private void refreshButtons() {
        RpgData data = currentData();
        for (Map.Entry<RpgStat, Button> entry : buttons.entrySet()) {
            int level = entry.getKey().level(data);
            entry.getValue().active = data.unspentStatPoints() > 0 && level < RpgData.MAX_STAT_LEVEL;
        }
    }

    private RpgData currentData() {
        var player = Minecraft.getInstance().player;
        return player == null ? RpgData.DEFAULT : player.getData(RpgAttachments.RPG_DATA);
    }

    private void onSpend(RpgStat stat) {
        ClientPacketDistributor.sendToServer(new SpendStatPointPayload(stat));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        RpgData data = currentData();
        int top = topOfRows();
        int centerX = this.width / 2;

        graphics.centeredText(this.font, this.title, centerX, top - 36, -1);

        Component levelLine = Component.translatable("gui.newmeriarpg.stats.level", data.level(), RpgData.MAX_RPG_LEVEL);
        graphics.centeredText(this.font, levelLine, centerX, top - 24, -1);

        if (data.level() < RpgData.MAX_RPG_LEVEL) {
            int missing = RpgData.pointsToReachNextLevel(data.level()) - data.pointsIntoLevel();
            Component nextLevelLine = Component.translatable("gui.newmeriarpg.stats.next_level", missing);
            graphics.centeredText(this.font, nextLevelLine, centerX, top - 13, -1);
        }

        for (int i = 0; i < STATS.length; i++) {
            RpgStat stat = STATS[i];
            int y = top + i * ROW_HEIGHT + 6;
            Component label = Component.translatable("gui.newmeriarpg.stats.row",
                    stat.displayName(), stat.level(data), RpgData.MAX_STAT_LEVEL);
            graphics.text(this.font, label, centerX - 90, y, -1);
        }

        Component pointsLeft = Component.translatable("gui.newmeriarpg.stats.points", data.unspentStatPoints());
        graphics.centeredText(this.font, pointsLeft, centerX, top + STATS.length * ROW_HEIGHT + 10, -1);
    }
    /** The key that opens this menu (K by default) also closes it, on top of Escape. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (RpgKeybinds.OPEN_STATS.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
