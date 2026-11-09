package fr.akkun.newmeriarpg.network;

import fr.akkun.newmeriarpg.client.ClientCompanionHandler;
import fr.akkun.newmeriarpg.RpgAttachments;
import fr.akkun.newmeriarpg.RpgAttributeModifiers;
import fr.akkun.newmeriarpg.RpgData;
import fr.akkun.newmeriarpg.RpgSpell;
import fr.akkun.newmeriarpg.RpgStat;
import fr.akkun.newmeriarpg.companion.CompanionForm;
import fr.akkun.newmeriarpg.companion.CompanionManager;
import fr.akkun.newmeriarpg.companion.network.OpenCompanionMenuPayload;
import fr.akkun.newmeriarpg.companion.network.SummonCompanionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RpgNetworking {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RpgNetworking::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SpendStatPointPayload.TYPE, SpendStatPointPayload.STREAM_CODEC, RpgNetworking::handleSpendStatPoint);
        registrar.playToServer(SelectSpellPayload.TYPE, SelectSpellPayload.STREAM_CODEC, RpgNetworking::handleSelectSpell);
        registrar.playToServer(SummonCompanionPayload.TYPE, SummonCompanionPayload.STREAM_CODEC, RpgNetworking::handleSummonCompanion);
        registrar.playToClient(OpenCompanionMenuPayload.TYPE, OpenCompanionMenuPayload.STREAM_CODEC, RpgNetworking::handleOpenCompanionMenu);
    }

    private static void handleSpendStatPoint(SpendStatPointPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        RpgStat stat = payload.stat();
        RpgData data = player.getData(RpgAttachments.RPG_DATA);
        if (data.unspentStatPoints() <= 0 || stat.level(data) >= RpgData.MAX_STAT_LEVEL) {
            return;
        }
        RpgData updated = stat.withLevel(data, stat.level(data) + 1)
                .withUnspentStatPoints(data.unspentStatPoints() - 1);
        player.setData(RpgAttachments.RPG_DATA, updated);
        RpgAttributeModifiers.apply(player, updated);
    }

    private static void handleSelectSpell(SelectSpellPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        int spellOrdinal = payload.spell();
        RpgData data = player.getData(RpgAttachments.RPG_DATA);
        if (spellOrdinal != RpgData.NO_SPELL) {
            if (spellOrdinal < 0 || spellOrdinal >= RpgSpell.values().length) {
                return;
            }
            if (!RpgSpell.values()[spellOrdinal].isUnlocked(data.magicLevel())) {
                return;
            }
        }
        player.setData(RpgAttachments.RPG_DATA, data.withSelectedSpell(spellOrdinal));
    }

    private static void handleSummonCompanion(SummonCompanionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        int formOrdinal = payload.form();
        if (formOrdinal < 0 || formOrdinal >= CompanionForm.values().length) {
            return;
        }
        CompanionManager.summon(player, CompanionForm.values()[formOrdinal]);
    }

    private static void handleOpenCompanionMenu(OpenCompanionMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(ClientCompanionHandler::openMenu);
    }
}
