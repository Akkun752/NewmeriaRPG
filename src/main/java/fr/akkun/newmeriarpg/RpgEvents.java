package fr.akkun.newmeriarpg;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

@EventBusSubscriber(modid = NewmeriaRPG.MOD_ID)
public class RpgEvents {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RpgAttributeModifiers.apply(player, player.getData(RpgAttachments.RPG_DATA));
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RpgAttributeModifiers.apply(player, player.getData(RpgAttachments.RPG_DATA));
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        RpgData data = event.getEntity().getData(RpgAttachments.RPG_DATA);
        double multiplier = RpgAttributeModifiers.miningSpeedMultiplier(data.forceLevel());
        if (multiplier != 1.0) {
            event.setNewSpeed((float) (event.getNewSpeed() * multiplier));
        }
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) {
            return;
        }
        double multiplier = RpgAttributeModifiers.xpMultiplier(player.getData(RpgAttachments.RPG_DATA).magicLevel());
        if (multiplier != 1.0) {
            event.setAmount((int) Math.round(event.getAmount() * multiplier));
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RpgData data = player.getData(RpgAttachments.RPG_DATA);
        DamageSource source = event.getSource();
        float damage = event.getNewDamage();
        if (data.resistanceLevel() >= 1 && source.is(DamageTypeTags.IS_PROJECTILE)) {
            damage /= 2f;
        }
        if (data.resistanceLevel() >= 3 && source.is(DamageTypeTags.IS_EXPLOSION)) {
            damage /= 2f;
        }
        if (data.resistanceLevel() >= 5 && source.is(DamageTypeTags.IS_FIRE)) {
            damage /= 2f;
        }
        if (damage != event.getNewDamage()) {
            event.setNewDamage(damage);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        int gained = RpgPoints.roll(pointsForKill(victim), killer.getRandom());
        if (gained > 0) {
            grantPoints(killer, gained);
        }
    }

    /** Every catch reeled in with a fishing rod (fish, junk or treasure alike) is worth RPG points. */
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            grantPoints(player, RpgPoints.roll(RpgPoints.forFishing(), player.getRandom()));
        }
    }

    /** A player is worth 5 points per RPG level of theirs; every mob is worth what {@link RpgPoints} lists for it. */
    private static double pointsForKill(LivingEntity victim) {
        if (victim instanceof Player player) {
            return player.getData(RpgAttachments.RPG_DATA).level() * 5;
        }
        return RpgPoints.forKill(victim);
    }

    static void grantPoints(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        if (TotemBonuses.hasMatchingTotem(player)) {
            amount *= 2;
        }
        RpgData data = player.getData(RpgAttachments.RPG_DATA);
        RpgData updated = data.addPoints(amount);
        if (updated.equals(data)) {
            return;
        }
        player.setData(RpgAttachments.RPG_DATA, updated);
        if (updated.level() > data.level()) {
            player.sendSystemMessage(Component.translatable("rpg.newmeriarpg.level_up",
                    updated.level(), updated.unspentStatPoints()), true);
        }
    }
}
