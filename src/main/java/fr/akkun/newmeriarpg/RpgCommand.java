package fr.akkun.newmeriarpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Server-side "/rpg add|remove|reset ..." admin subcommands, all optionally targeting another
 * player (defaults to the command sender when omitted). The plain "/rpg" (opening the stats
 * screen) is handled client-side in {@code RpgClientCommands} and never reaches the server.
 */
@EventBusSubscriber(modid = NewmeriaRPG.MOD_ID)
public class RpgCommand {
    private static final PermissionCheck ADMIN_PERMISSION = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rpg")
                .then(Commands.literal("add")
                        .requires(Commands.hasPermission(ADMIN_PERMISSION))
                        .then(Commands.literal("points").then(amountWithOptionalTarget(RpgCommand::addPoints)))
                        .then(Commands.literal("level").then(amountWithOptionalTarget(RpgCommand::addLevels))))
                .then(Commands.literal("remove")
                        .requires(Commands.hasPermission(ADMIN_PERMISSION))
                        .then(Commands.literal("points").then(amountWithOptionalTarget(RpgCommand::removePoints)))
                        .then(Commands.literal("level").then(amountWithOptionalTarget(RpgCommand::removeLevels))))
                .then(Commands.literal("reset")
                        .requires(Commands.hasPermission(ADMIN_PERMISSION))
                        .executes(ctx -> reset(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));
    }

    private interface AmountAction {
        int run(CommandSourceStack source, int amount, ServerPlayer target) throws CommandSyntaxException;
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer> amountWithOptionalTarget(AmountAction action) {
        return Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(ctx -> action.run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> action.run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"), EntityArgument.getPlayer(ctx, "player"))));
    }

    private static int addPoints(CommandSourceStack source, int amount, ServerPlayer target) {
        RpgData updated = update(target, data -> data.addPoints(amount));
        source.sendSuccess(() -> Component.translatable("commands.newmeriarpg.rpg.add_points",
                amount, target.getName(), updated.level(), updated.unspentStatPoints()), true);
        return 1;
    }

    private static int addLevels(CommandSourceStack source, int amount, ServerPlayer target) {
        RpgData updated = update(target, data -> data.addLevels(amount));
        source.sendSuccess(() -> Component.translatable("commands.newmeriarpg.rpg.add_level",
                amount, target.getName(), updated.level(), updated.unspentStatPoints()), true);
        return 1;
    }

    private static int removePoints(CommandSourceStack source, int amount, ServerPlayer target) {
        RpgData updated = update(target, data -> data.removePoints(amount));
        source.sendSuccess(() -> Component.translatable("commands.newmeriarpg.rpg.remove_points",
                amount, target.getName(), updated.level(), updated.unspentStatPoints()), true);
        return 1;
    }

    private static int removeLevels(CommandSourceStack source, int amount, ServerPlayer target) {
        RpgData updated = update(target, data -> data.removeLevels(amount));
        source.sendSuccess(() -> Component.translatable("commands.newmeriarpg.rpg.remove_level",
                amount, target.getName(), updated.level(), updated.unspentStatPoints()), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer target) {
        target.setData(RpgAttachments.RPG_DATA, RpgData.DEFAULT);
        RpgAttributeModifiers.apply(target, RpgData.DEFAULT);
        source.sendSuccess(() -> Component.translatable("commands.newmeriarpg.rpg.reset", target.getName()), true);
        return 1;
    }

    private static RpgData update(ServerPlayer target, java.util.function.UnaryOperator<RpgData> mutator) {
        RpgData updated = mutator.apply(target.getData(RpgAttachments.RPG_DATA));
        target.setData(RpgAttachments.RPG_DATA, updated);
        RpgAttributeModifiers.apply(target, updated);
        return updated;
    }
}
