package dev.riley.wildlands.entity;

import dev.riley.wildlands.Settings;
import net.minecraft.core.*;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RootWorm extends PathfinderMob {
  private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> GRAZING =
      net.minecraft.network.syncher.SynchedEntityData.defineId(
          RootWorm.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

  @Override
  protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(GRAZING, 0);
  }

  public boolean grazing() {
    return entityData.get(GRAZING) > 0;
  }

  public RootWorm(EntityType<? extends RootWorm> type, Level level) {
    super(type, level);
  }

  public static AttributeSupplier.Builder attributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 12)
        .add(Attributes.MOVEMENT_SPEED, .22)
        .add(Attributes.ATTACK_DAMAGE, 3)
        .add(Attributes.FOLLOW_RANGE, 20);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(1, new FleeFire(this));
    goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
    goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, .7));
    goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    targetSelector.addGoal(1, new HurtByTargetGoal(this)); // No proactive player targeting.
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    return super.hurt(
        source,
        source.is(DamageTypeTags.IS_FIRE)
            ? amount * Settings.WORM_FIRE.get().floatValue()
            : amount);
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (!level().isClientSide && grazing()) entityData.set(GRAZING, entityData.get(GRAZING) - 1);
    if (!level().isClientSide
        && tickCount % 100 == 0
        && getTarget() == null
        && level()
            .getBlockState(blockPosition().below())
            .is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT)) {
      getNavigation().stop();
      entityData.set(GRAZING, 20);
      level()
          .playSound(
              null,
              blockPosition(),
              net.minecraft.sounds.SoundEvents.GENERIC_EAT,
              net.minecraft.sounds.SoundSource.NEUTRAL,
              .25f,
              .7f); // No root blocks are changed.
    }
  }

  static final class FleeFire extends Goal {
    private final RootWorm worm;
    private BlockPos danger;
    private int nextScan;

    FleeFire(RootWorm worm) {
      this.worm = worm;
      setFlags(java.util.EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
      if (!Settings.WORM_FLEE.get() || worm.tickCount < nextScan) return false;
      nextScan = worm.tickCount + 10;
      int r = Settings.WORM_RADIUS.get();
      danger = null;
      double nearest = Double.MAX_VALUE;
      for (var p :
          BlockPos.betweenClosed(
              worm.blockPosition().offset(-r, -r, -r), worm.blockPosition().offset(r, r, r))) {
        if (!worm.level().hasChunkAt(p) || p.distSqr(worm.blockPosition()) > r * r) continue;
        var state = worm.level().getBlockState(p);
        if (state.is(BlockTags.FIRE) || state.getFluidState().is(FluidTags.LAVA)) {
          double d = p.distSqr(worm.blockPosition());
          if (d < nearest) {
            nearest = d;
            danger = p.immutable();
          }
        }
      }
      return danger != null;
    }

    @Override
    public void start() {
      worm.entityData.set(GRAZING, 0);
      Vec3 away = worm.position().subtract(Vec3.atCenterOf(danger));
      if (away.horizontalDistanceSqr() < .01) away = new Vec3(1, 0, 0);
      var supported =
          net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(
              worm, 12, 5, Vec3.atCenterOf(danger));
      away = supported == null ? worm.position().add(away.normalize().scale(8)) : supported;
      worm.getNavigation().moveTo(away.x, away.y, away.z, Settings.WORM_FLEE_SPEED.get());
    }

    @Override
    public boolean canContinueToUse() {
      return !worm.getNavigation().isDone() && Settings.WORM_FLEE.get();
    }
  }
}
