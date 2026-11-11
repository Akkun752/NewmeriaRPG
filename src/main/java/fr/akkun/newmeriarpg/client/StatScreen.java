package fr.akkun.newmeriarpg.client;

import fr.akkun.newmeriarpg.RpgAttachments;
import fr.akkun.newmeriarpg.RpgData;
import fr.akkun.newmeriarpg.RpgStat;
import fr.akkun.newmeriarpg.network.SpendStatPointPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Locale;

/**
 * The RPG menu (K): one column per stat, one button per level of that stat, level 1 at the top.
 * A level can be bought - one stat point - once the level above it is owned. Hovering a level shows
 * what it gives ({@code gui.newmeriarpg.stats.bonus.<stat>.<level>} in the language files).
 *
 * <ul>
 *   <li>Owned levels: green, with a check mark.</li>
 *   <li>The next level of a stat, while the player has a stat point: an active button.</li>
 *   <li>Every other level: greyed out.</li>
 * </ul>
 */
public class StatScreen extends Screen {
    private static final RpgStat[] STATS = RpgStat.values();
    private static final int COLUMN_WIDTH = 74;
    private static final int BUTTON_WIDTH = 64;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;
    /** From the top of the screen's content to the first row of buttons: title, RPG level, next level, stat names. */
    private static final int HEADER_HEIGHT = 52;
    private static final int FOOTER_HEIGHT = 18;

    // [stat][level - 1]
    private final Button[][] buttons = new Button[STATS.length][RpgData.MAX_STAT_LEVEL];
    // What each button currently shows, so its text and tooltip are only rebuilt when it changes.
    private final LevelState[][] shownStates = new LevelState[STATS.length][RpgData.MAX_STAT_LEVEL];

    private enum LevelState { OWNED, AVAILABLE, NO_POINTS, LOCKED }

    public StatScreen() {
        super(Component.translatable("gui.newmeriarpg.stats.title"));
    }

    @Override
    protected void init() {
        super.init();
        for (int column = 0; column < STATS.length; column++) {
            RpgStat stat = STATS[column];
            for (int level = 1; level <= RpgData.MAX_STAT_LEVEL; level++) {
                int targetLevel = level;
                Button button = Button.builder(Component.empty(), b -> onBuy(stat, targetLevel))
                        .bounds(columnX(column) + (COLUMN_WIDTH - BUTTON_WIDTH) / 2, rowY(level), BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build();
                this.buttons[column][level - 1] = button;
                this.shownStates[column][level - 1] = null;
                this.addRenderableWidget(button);
            }
        }
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    private int contentTop() {
        int contentHeight = HEADER_HEIGHT + RpgData.MAX_STAT_LEVEL * ROW_HEIGHT + FOOTER_HEIGHT;
        return (this.height - contentHeight) / 2;
    }

    private int columnX(int column) {
        return this.width / 2 - (STATS.length * COLUMN_WIDTH) / 2 + column * COLUMN_WIDTH;
    }

    private int rowY(int level) {
        return contentTop() + HEADER_HEIGHT + (level - 1) * ROW_HEIGHT;
    }

    private static LevelState state(RpgData data, RpgStat stat, int level) {
        int owned = stat.level(data);
        if (level <= owned) {
            return LevelState.OWNED;
        }
        if (level > owned + 1) {
            return LevelState.LOCKED;
        }
        return data.unspentStatPoints() > 0 ? LevelState.AVAILABLE : LevelState.NO_POINTS;
    }

    private void refreshButtons() {
        RpgData data = currentData();
        for (int column = 0; column < STATS.length; column++) {
            RpgStat stat = STATS[column];
            for (int level = 1; level <= RpgData.MAX_STAT_LEVEL; level++) {
                LevelState state = state(data, stat, level);
                if (state == this.shownStates[column][level - 1]) {
                    continue;
                }
                this.shownStates[column][level - 1] = state;
                Button button = this.buttons[column][level - 1];
                button.active = state == LevelState.AVAILABLE;
                button.setMessage(label(level, state));
                button.setTooltip(Tooltip.create(tooltip(stat, level, state)));
            }
        }
    }

    private static Component label(int level, LevelState state) {
        MutableComponent text = Component.translatable("gui.newmeriarpg.stats.level_button", level);
        return state == LevelState.OWNED
                ? Component.literal("✔ ").append(text).withStyle(ChatFormatting.GREEN)
                : text;
    }

    private static Component tooltip(RpgStat stat, int level, LevelState state) {
        MutableComponent tooltip = Component.translatable("gui.newmeriarpg.stats.tooltip.title", stat.displayName(), level)
                .withStyle(ChatFormatting.GOLD)
                .append("\n")
                .append(Component.translatable("gui.newmeriarpg.stats.bonus." + stat.name().toLowerCase(Locale.ROOT) + "." + level)
                        .withStyle(ChatFormatting.WHITE))
                .append("\n\n");
        Component status = switch (state) {
            case OWNED -> Component.translatable("gui.newmeriarpg.stats.tooltip.owned").withStyle(ChatFormatting.GREEN);
            case AVAILABLE -> Component.translatable("gui.newmeriarpg.stats.tooltip.available").withStyle(ChatFormatting.YELLOW);
            case NO_POINTS -> Component.translatable("gui.newmeriarpg.stats.tooltip.no_points").withStyle(ChatFormatting.RED);
            case LOCKED -> Component.translatable("gui.newmeriarpg.stats.tooltip.needs_previous", level - 1).withStyle(ChatFormatting.RED);
        };
        return tooltip.append(status);
    }

    private RpgData currentData() {
        var player = Minecraft.getInstance().player;
        return player == null ? RpgData.DEFAULT : player.getData(RpgAttachments.RPG_DATA);
    }

    private void onBuy(RpgStat stat, int level) {
        ClientPacketDistributor.sendToServer(new SpendStatPointPayload(stat, level));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        RpgData data = currentData();
        int top = contentTop();
        int centerX = this.width / 2;

        graphics.centeredText(this.font, this.title, centerX, top, -1);

        Component levelLine = Component.translatable("gui.newmeriarpg.stats.level", data.level(), RpgData.MAX_RPG_LEVEL);
        graphics.centeredText(this.font, levelLine, centerX, top + 12, -1);

        if (data.level() < RpgData.MAX_RPG_LEVEL) {
            int missing = RpgData.pointsToReachNextLevel(data.level()) - data.pointsIntoLevel();
            Component nextLevelLine = Component.translatable("gui.newmeriarpg.stats.next_level", missing);
            graphics.centeredText(this.font, nextLevelLine, centerX, top + 23, -1);
        }

        for (int column = 0; column < STATS.length; column++) {
            Component name = STATS[column].displayName().copy().withStyle(ChatFormatting.BOLD);
            graphics.centeredText(this.font, name, columnX(column) + COLUMN_WIDTH / 2, top + HEADER_HEIGHT - 13, -1);
        }

        Component pointsLeft = Component.translatable("gui.newmeriarpg.stats.points", data.unspentStatPoints());
        graphics.centeredText(this.font, pointsLeft, centerX, rowY(RpgData.MAX_STAT_LEVEL) + ROW_HEIGHT + 6, -1);
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
