package fr.akkun.newmeriarpg.network;

import fr.akkun.newmeriacore.item.ModItems;
import fr.akkun.newmeriarpg.SpellBook;
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
        registrar.playToServer(LearnSpellPayload.TYPE, LearnSpellPayload.STREAM_CODEC, RpgNetworking::handleLearnSpell);
        registrar.playToServer(SummonCompanionPayload.TYPE, SummonCompanionPayload.STREAM_CODEC, RpgNetworking::handleSummonCompanion);
        registrar.playToClient(OpenCompanionMenuPayload.TYPE, OpenCompanionMenuPayload.STREAM_CODEC, RpgNetworking::handleOpenCompanionMenu);
    }

    private static void handleSpendStatPoint(SpendStatPointPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        RpgStat stat = payload.stat();
        RpgData data = player.getData(RpgAttachments.RPG_DATA);
        if (data.unspentStatPoints() <= 0 || stat.level(data) >= RpgData.MAX_STAT_LEVEL || payload.level() != stat.level(data) + 1) {
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
        RpgData data = player.getData(RpgAttachments.RPG_DATA);
        if (!data.spells().hasLearned(payload.spell())) {
            return;
        }
        player.setData(RpgAttachments.RPG_DATA, data.withSpells(data.spells().select(payload.spell())));
    }

    private static void handleLearnSpell(LearnSpellPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        // Spells are only learned and forgotten with the Spellbook open, so in hand.
        if (!player.getMainHandItem().is(ModItems.SPELLBOOK.get()) && !player.getOffhandItem().is(ModItems.SPELLBOOK.get())) {
            return;
        }
        RpgSpell spell = payload.spell();
        RpgData data = player.getData(RpgAttachments.RPG_DATA);
        SpellBook spells = data.spells();
        if (payload.learn()) {
            if (spells.hasLearned(spell) || !spells.hasFreeSlot(data.magicLevel()) || !spell.isAvailableTo(player)) {
                return;
            }
            player.setData(RpgAttachments.RPG_DATA, data.withSpells(spells.learn(spell)));
        } else if (spells.hasLearned(spell)) {
            player.setData(RpgAttachments.RPG_DATA, data.withSpells(spells.forget(spell)));
        }
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
