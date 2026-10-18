package dev.riley.wildlands.entity;

import dev.riley.wildlands.Wildlands;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DepthStalker extends WaterAnimal {
  public enum Phase {
    REVEAL,
    STRIKE,
    RETREAT,
    HIDE,
    REPOSITION
  }

  public static final float FIXED_DAMAGE = 18f;
  private Phase phase = Phase.REVEAL;
  private long until;
  private int strikes, age;
  private UUID hunted;

  public DepthStalker(EntityType<? extends DepthStalker> t, Level l) {
    super(t, l);
    moveControl = new SmoothSwimmingMoveControl(this, 85, 10, .2f, .02f, false);
  }

  public static AttributeSupplier.Builder attributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 300)
        .add(Attributes.MOVEMENT_SPEED, 1)
        .add(Attributes.FOLLOW_RANGE, 64)
        .add(Attributes.KNOCKBACK_RESISTANCE, 1);
  }

  @Override
  protected PathNavigation createNavigation(Level l) {
    return new WaterBoundPathNavigation(this, l);
  }

  @Override
  protected void registerGoals() {}

  @Override
  public boolean removeWhenFarAway(double d) {
    return true;
  }

  private void phase(Phase p, int ticks) {
    phase = p;
    until = level().getGameTime() + ticks;
  }

  public Phase phase() {
    return phase;
  }

  public boolean looking(Player p) {
    Vec3 to = getBoundingBox().getCenter().subtract(p.getEyePosition());
    return to.lengthSqr() < 1
        || p.getLookAngle().dot(to.normalize()) > .35 && p.hasLineOfSight(this);
  }

  public boolean strike(Player p) {
    if (level().isClientSide
        || p.isCreative()
        || p.isSpectator()
        || !p.isAlive()
        || !getBoundingBox().inflate(.65).intersects(p.getBoundingBox())
        || !hasLineOfSight(p)) return false;
    var key =
        net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.DAMAGE_TYPE, Wildlands.id("abyssal_strike"));
    var source =
        new net.minecraft.world.damagesource.DamageSource(
            level()
                .registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key),
            this);
    boolean hit = p.hurt(source, FIXED_DAMAGE);
    if (hit && p.isAlive()) {
      hunted = p.getUUID();
      var data = p.getPersistentData();
      data.putUUID("WildlandsHunter", getUUID());
      data.putLong("WildlandsWoundUntil", level().getGameTime() + 1800);
      p.displayClientMessage(
          net.minecraft.network.chat.Component.literal(
              "An abyssal wound prevents healing while the creature hunts you."),
          true);
    }
    return hit;
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (level().isClientSide) return;
    age++;
    Player player = hunted == null ? null : level().getPlayerByUUID(hunted);
    if (player == null) {
      player = level().getNearestPlayer(this, 56);
      if (player != null && !player.isCreative() && !player.isSpectator())
        hunted = player.getUUID();
      else {
        if (age > 100) discard();
        return;
      }
    }
    if (!player.isAlive()
        || player.isCreative()
        || player.isSpectator()
        || !player.isInWater()
        || !level().getBiome(player.blockPosition()).is(Wildlands.id("depths"))) {
      discard();
      return;
    }
    if (age > 20 && !looking(player)) {
      discard();
      return;
    }
    if (until == 0) until = level().getGameTime() + 60;
    if (age > 20 * 90) {
      discard();
      return;
    }
    setTarget(player);
    double angle = (getId() * 1.71 + level().getGameTime() * .012);
    switch (phase) {
      case REVEAL -> {
        if (tickCount % 10 == 0)
          getNavigation()
              .moveTo(
                  player.getX() + Math.cos(angle) * 10,
                  player.getY() - 1,
                  player.getZ() + Math.sin(angle) * 10,
                  .7);
        if (level().getGameTime() >= until) phase(Phase.STRIKE, 160);
      }
      case STRIKE -> {
        if (tickCount % 5 == 0) getNavigation().moveTo(player, 1.7);
        if (strike(player)) {
          strikes++;
          phase(Phase.RETREAT, 160);
        } else if (level().getGameTime() >= until) phase(Phase.RETREAT, 160);
      }
      case RETREAT -> {
        Vec3 away = position().subtract(player.position()).normalize().scale(26);
        if (tickCount % 10 == 0)
          getNavigation()
              .moveTo(
                  player.getX() + away.x,
                  Math.max(-35, player.getY() - 8),
                  player.getZ() + away.z,
                  1.6);
        if (level().getGameTime() >= until) phase(Phase.HIDE, 100);
      }
      case HIDE -> {
        getNavigation().stop();
        if (level().getGameTime() >= until) {
          if (strikes >= 2) discard();
          else phase(Phase.REPOSITION, 100);
        }
      }
      case REPOSITION -> {
        if (tickCount % 10 == 0)
          getNavigation()
              .moveTo(
                  player.getX() + Math.cos(angle) * 16,
                  player.getY() - 2,
                  player.getZ() + Math.sin(angle) * 16,
                  1);
        if (level().getGameTime() >= until) phase(Phase.REVEAL, 40);
      }
    }
  }

  @Override
  public void remove(RemovalReason reason) {
    if (reason.shouldDestroy() && !level().isClientSide && hunted != null) {
      Player victim = level().getPlayerByUUID(hunted);
      if (victim != null
          && victim.getPersistentData().hasUUID("WildlandsHunter")
          && getUUID().equals(victim.getPersistentData().getUUID("WildlandsHunter"))) {
        victim.getPersistentData().remove("WildlandsWoundUntil");
        victim.getPersistentData().remove("WildlandsHunter");
      }
    }
    super.remove(reason);
  }

  @Override
  public void travel(Vec3 v) {
    if (isEffectiveAi() && isInWater()) {
      moveRelative(.06f, v);
      move(MoverType.SELF, getDeltaMovement());
      setDeltaMovement(getDeltaMovement().scale(.85));
    } else super.travel(v);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putString("HuntPhase", phase.name());
    t.putLong("PhaseUntil", until);
    t.putInt("Strikes", strikes);
    t.putInt("EncounterAge", age);
    if (hunted != null) t.putUUID("Hunted", hunted);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    try {
      phase = Phase.valueOf(t.getString("HuntPhase"));
    } catch (IllegalArgumentException ex) {
      phase = Phase.REVEAL;
    }
    until = t.getLong("PhaseUntil");
    strikes = t.getInt("Strikes");
    age = t.getInt("EncounterAge");
    hunted = t.hasUUID("Hunted") ? t.getUUID("Hunted") : null;
  }
}
