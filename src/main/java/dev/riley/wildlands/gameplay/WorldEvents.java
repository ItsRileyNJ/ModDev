package dev.riley.wildlands.gameplay;

import dev.riley.wildlands.*;
import dev.riley.wildlands.entity.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.*;

@EventBusSubscriber(modid = Wildlands.ID)
public final class WorldEvents {
  public static boolean abyssal(DamageSource source) {
    return source
        .typeHolder()
        .is(
            net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                Wildlands.id("abyssal_strike")));
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void fixedDamage(LivingDamageEvent.Pre e) {
    if (abyssal(e.getSource())) {
      e.setNewDamage(DepthStalker.FIXED_DAMAGE);
      e.getContainer()
          .addModifier(DamageContainer.Reduction.ABSORPTION, (container, reduction) -> 0);
    }
  }

  @SubscribeEvent
  public static void abyssalWound(LivingHealEvent e) {
    if (e.getEntity() instanceof Player player && !player.level().isClientSide) {
      var data = player.getPersistentData();
      if (data.getLong("WildlandsWoundUntil") > player.level().getGameTime()) e.setCanceled(true);
      else {
        data.remove("WildlandsWoundUntil");
        data.remove("WildlandsHunter");
      }
    }
  }

  @SubscribeEvent
  public static void endWound(LivingDeathEvent e) {
    if (e.getEntity() instanceof Player player) {
      player.getPersistentData().remove("WildlandsWoundUntil");
      player.getPersistentData().remove("WildlandsHunter");
    }
  }

  @SubscribeEvent
  public static void level(LevelTickEvent.Post e) {
    if (e.getLevel() instanceof ServerLevel l) SlimeTraps.get(l).tick(l);
  }

  @SubscribeEvent
  public static void entity(EntityTickEvent.Post e) {
    if (!(e.getEntity() instanceof LivingEntity living)
        || !(living.level() instanceof ServerLevel l)) return;
    SlimeTraps.get(l).immobilize(l, living);
    if (living instanceof Player player
        && player.tickCount % 20 == 0
        && player.getY() <= -36
        && l.getBiome(player.blockPosition()).is(Wildlands.id("depths"))) {
      pressure(player, MobEffects.BLINDNESS, 0);
      pressure(player, MobEffects.DIG_SLOWDOWN, 2);
    }
  }

  private static void pressure(
      Player player, net.minecraft.core.Holder<MobEffect> effect, int amplifier) {
    var active = player.getEffect(effect);
    if (active == null || active.getDuration() < 50)
      player.addEffect(new MobEffectInstance(effect, 100, amplifier, true, false, true));
  }

  @SubscribeEvent
  public static void naturalFinalized(
      net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent e) {
    if (e.getSpawnType() == MobSpawnType.NATURAL
        && e.getEntity().getType() == Creatures.STALKER.get()) {
      var clock = EncounterClock.get(e.getLevel().getLevel());
      if (!clock.ready(e.getLevel().getLevel().getGameTime())) e.setSpawnCancelled(true);
      else clock.reserve(e.getLevel().getLevel().getGameTime());
    }

    if ((e.getSpawnType() == MobSpawnType.NATURAL
            || e.getSpawnType() == MobSpawnType.CHUNK_GENERATION
            || e.getSpawnType() == MobSpawnType.PATROL)
        && e.getLevel().getBiome(e.getEntity().blockPosition()).is(Wildlands.id("depths"))
        && e.getEntity().getType() != Creatures.STALKER.get()) e.setSpawnCancelled(true);
  }

  @SubscribeEvent
  public static void placement(
      net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck e) {
    if ((e.getSpawnType() == MobSpawnType.NATURAL
            || e.getSpawnType() == MobSpawnType.CHUNK_GENERATION)
        && e.getLevel().getBiome(e.getPos()).is(Wildlands.id("depths"))
        && e.getEntityType() != Creatures.STALKER.get())
      e.setResult(
          net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
  }
}
