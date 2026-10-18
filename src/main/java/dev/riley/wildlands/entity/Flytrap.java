package dev.riley.wildlands.entity;

import dev.riley.wildlands.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Flytrap extends TamableAnimal {
  private static final EntityDataAccessor<Boolean> DIGESTING =
      SynchedEntityData.defineId(Flytrap.class, EntityDataSerializers.BOOLEAN);
  private long digestUntil, tameUntil, collectAfter;
  private boolean atePhantom;

  public Flytrap(EntityType<? extends Flytrap> type, Level l) {
    super(type, l);
  }

  public static AttributeSupplier.Builder attributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 40)
        .add(Attributes.MOVEMENT_SPEED, 0)
        .add(Attributes.KNOCKBACK_RESISTANCE, 1)
        .add(Attributes.FOLLOW_RANGE, 8);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(DIGESTING, false);
  }

  @Override
  protected void registerGoals() {}

  @Override
  public boolean isOwnedBy(LivingEntity entity) {
    return entity.getUUID().equals(getOwnerUUID());
  }

  @Override
  public boolean isFood(ItemStack s) {
    return false;
  }

  @Override
  public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob other) {
    return null;
  }

  public boolean digesting() {
    return level().isClientSide ? entityData.get(DIGESTING) : level().getGameTime() < digestUntil;
  }

  public boolean tamingOpportunity() {
    return atePhantom && level().getGameTime() <= tameUntil;
  }

  @Override
  public boolean removeWhenFarAway(double distance) {
    return !isTame();
  }

  @Override
  public void travel(Vec3 input) {
    super.travel(Vec3.ZERO);
  }

  public boolean allowedPrey(LivingEntity prey) {
    if (prey == this || prey instanceof Player || prey instanceof Flytrap || !prey.isAlive())
      return false;
    String id = BuiltInRegistries.ENTITY_TYPE.getKey(prey.getType()).toString();
    if (Settings.PREY_BLACKLIST.get().contains(id)
        || Settings.CATEGORY_BLACKLIST.get().contains(prey.getType().getCategory().getName()))
      return false;
    if (!Settings.PETS.get() && prey instanceof OwnableEntity own && own.getOwnerUUID() != null)
      return false;
    if (prey.isAlliedTo(this)) return false;
    return prey.getType() == EntityType.PHANTOM
        || prey.getType() == EntityType.BEE
        || (isTame() && prey.getBbWidth() <= Settings.SMALL_MOB.get());
  }

  public boolean consume(LivingEntity prey) {
    if (level().isClientSide
        || digesting()
        || !allowedPrey(prey)
        || !getBoundingBox().inflate(.8, 1, .8).intersects(prey.getBoundingBox())
        || !hasLineOfSight(prey)) return false;
    if (!prey.hurt(damageSources().mobAttack(this), 1000) || prey.isAlive()) return false;
    digestUntil = level().getGameTime() + Settings.DIGEST.get();
    if (prey.getType() == EntityType.PHANTOM) {
      atePhantom = true;
      tameUntil = digestUntil + Settings.TAMING_WINDOW.get();
    }
    entityData.set(DIGESTING, true);
    return true;
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (!level().isClientSide) {
      entityData.set(DIGESTING, level().getGameTime() < digestUntil);
      if (!digesting() && tickCount % 10 == 0) {
        for (var prey :
            level()
                .getEntitiesOfClass(
                    LivingEntity.class, getBoundingBox().inflate(.8, 1, .8), this::allowedPrey))
          if (consume(prey)) break;
      }
    }
  }

  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (stack.is(Items.PHANTOM_MEMBRANE) && !isTame()) {
      if (!tamingOpportunity()) return InteractionResult.FAIL;
      if (!level().isClientSide) {
        tame(player);
        setPersistenceRequired();
        collectAfter = level().getGameTime() + Settings.COLLECTION.get();
        stack.consume(1, player);
        level().broadcastEntityEvent(this, (byte) 7);
      }
      return InteractionResult.sidedSuccess(level().isClientSide);
    }
    if (stack.is(Content.EMPTY_JAR.get()) && isTame()) {
      if (!isOwnedBy(player) || digesting() || level().getGameTime() < collectAfter)
        return InteractionResult.FAIL;
      if (!level().isClientSide) {
        player.setItemInHand(
            hand,
            ItemUtils.createFilledResult(
                stack, player, new ItemStack(Content.SLIME_JAR.get()), false));
        collectAfter = level().getGameTime() + Settings.COLLECTION.get();
      }
      return InteractionResult.sidedSuccess(level().isClientSide);
    }
    return super.mobInteract(player, hand);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putBoolean("AtePhantom", atePhantom);
    t.putLong("DigestUntil", digestUntil);
    t.putLong("TameUntil", tameUntil);
    t.putLong("CollectAfter", collectAfter);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    atePhantom = t.getBoolean("AtePhantom");
    digestUntil = t.getLong("DigestUntil");
    tameUntil = t.getLong("TameUntil");
    collectAfter = t.getLong("CollectAfter");
    entityData.set(DIGESTING, level().getGameTime() < digestUntil);
  }
}
