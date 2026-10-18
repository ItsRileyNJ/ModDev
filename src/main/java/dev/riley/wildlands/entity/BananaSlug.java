package dev.riley.wildlands.entity;

import dev.riley.wildlands.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class BananaSlug extends TamableAnimal {
  private static final EntityDataAccessor<Float> SIZE =
      SynchedEntityData.defineId(BananaSlug.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Direction> FACE =
      SynchedEntityData.defineId(BananaSlug.class, EntityDataSerializers.DIRECTION);
  final SurfaceNavigator surface = new SurfaceNavigator(this);
  private long copperReady;
  private int dryness;
  private BlockPos copper;

  public BananaSlug(EntityType<? extends BananaSlug> t, Level l) {
    super(t, l);
    entityData.set(SIZE, .05f + random.nextFloat() * .95f);
    refreshDimensions();
  }

  public static AttributeSupplier.Builder attributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 6)
        .add(Attributes.MOVEMENT_SPEED, .08);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(SIZE, 1f);
    b.define(FACE, Direction.DOWN);
  }

  @Override
  public float getScale() {
    return 1f;
  } // Fixed maximum size is owned by SlugSize, not the generic scale attribute.

  public float size() {
    return entityData.get(SIZE);
  }

  public Direction face() {
    return entityData.get(FACE);
  }

  void attach(Direction d) {
    entityData.set(FACE, d);
    setNoGravity(true);
  }

  @Override
  public EntityDimensions getDefaultDimensions(Pose pose) {
    return EntityDimensions.scalable(.9f, .18f).scale(size());
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
    super.onSyncedDataUpdated(key);
    if (SIZE.equals(key)) refreshDimensions();
  }

  @Override
  protected void
      registerGoals() {} // Movement uses the supported-surface graph, not ground navigation.

  @Override
  public boolean isOwnedBy(LivingEntity entity) {
    return entity.getUUID().equals(getOwnerUUID());
  }

  @Override
  public boolean isFood(ItemStack stack) {
    return false;
  }

  @Override
  public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob other) {
    return null;
  }

  @Override
  public boolean removeWhenFarAway(double distance) {
    return !isTame();
  }

  @Override
  public void travel(Vec3 input) {
    if (isEffectiveAi()) {
      move(MoverType.SELF, getDeltaMovement());
      setDeltaMovement(
          isNoGravity() ? Vec3.ZERO : getDeltaMovement().multiply(.9, .98, .9).add(0, -.06, 0));
    }
  }

  @Override
  public void aiStep() {
    if (!level().isClientSide) {
      boolean supported = !surface.nodes(blockPosition()).isEmpty();
      setNoGravity(supported);
      if (!supported) {
        surface.route.clear();
        setDeltaMovement(getDeltaMovement().add(0, -.04, 0));
      }
      if (supported && tickCount % 40 == 0 && surface.route.isEmpty()) {
        copper = null;
        if (level().getGameTime() >= copperReady) {
          for (var p :
              BlockPos.betweenClosed(
                  blockPosition().offset(-6, -6, -6), blockPosition().offset(6, 6, 6))) {
            if (!level().hasChunkAt(p) || !eligible(p)) continue;
            if (surface.find(p, true)) {
              copper = p.immutable();
              break;
            }
          }
        }
        if (copper == null) {
          for (int tries = 0; tries < 8; tries++)
            if (surface.find(
                blockPosition()
                    .offset(random.nextInt(13) - 6, random.nextInt(9) - 4, random.nextInt(13) - 6),
                false)) break;
        }
      }
      surface.tick();
      if (copper != null && surface.route.isEmpty()) {
        waxAtContact(copper);
        copper = null;
      }
      if (tickCount % 20 == 0
          && level().getGameTime() >= copperReady
          && Settings.GOLEM_HEAL.get() > 0) {
        for (LivingEntity e :
            level()
                .getEntitiesOfClass(
                    LivingEntity.class, getBoundingBox().inflate(.15), e -> e != this)) {
          String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
          if (Settings.GOLEMS.get().contains(id) && e.getHealth() < e.getMaxHealth()) {
            e.heal(Settings.GOLEM_HEAL.get().floatValue());
            copperReady = level().getGameTime() + Settings.WAX_COOLDOWN.get();
            break;
          }
        }
      }
      boolean hotDry =
          level().getBiome(blockPosition()).value().getBaseTemperature() >= 1.5f
              && level().getBiome(blockPosition()).value().getModifiedClimateSettings().downfall()
                  < .2f;
      String biome =
          level()
              .getBiome(blockPosition())
              .unwrapKey()
              .map(k -> k.location().toString())
              .orElse("");
      boolean dry = hotDry || Settings.DRY_BIOMES.get().contains(biome);
      if ((Settings.WATER_PROTECTS.get() && isInWater())
          || (Settings.RAIN_PROTECTS.get() && level().isRainingAt(blockPosition()))) dry = false;
      dryness = dry ? dryness + 1 : 0;
      if (dryness > Settings.DRY_GRACE.get() && dryness % Settings.DRY_INTERVAL.get() == 0)
        hurt(damageSources().dryOut(), Settings.DRY_DAMAGE.get().floatValue());
    }
    super.aiStep();
  }

  private boolean eligible(BlockPos pos) {
    var state = level().getBlockState(pos);
    String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    return (Settings.WAX_BLOCKS.get().isEmpty() || Settings.WAX_BLOCKS.get().contains(id))
        && HoneycombItem.getWaxed(state).isPresent();
  }

  public boolean waxAtContact(BlockPos pos) {
    if (level().isClientSide
        || level().getGameTime() < copperReady
        || !getBoundingBox().inflate(.035).intersects(new AABB(pos))
        || !eligible(pos)) return false;
    if (!net.neoforged.neoforge.event.EventHooks.canEntityGrief(level(), this)) return false;
    var state = level().getBlockState(pos);
    var waxed = HoneycombItem.getWaxed(state).orElseThrow();
    var snapshot =
        net.neoforged.neoforge.common.util.BlockSnapshot.create(level().dimension(), level(), pos);
    if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS
        .post(
            new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(
                snapshot, state, this))
        .isCanceled()) return false;
    level().setBlockAndUpdate(pos, waxed);
    level().levelEvent(3003, pos, 0);
    copperReady = level().getGameTime() + Settings.WAX_COOLDOWN.get();
    return true;
  }

  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    var stack = player.getItemInHand(hand);
    if (!isTame() && stack.is(Items.FROGSPAWN)) {
      if (!level().isClientSide) {
        tame(player);
        stack.consume(1, player);
        setPersistenceRequired();
        level().broadcastEntityEvent(this, (byte) 7);
      }
      return InteractionResult.sidedSuccess(level().isClientSide);
    }
    if (player.isShiftKeyDown() && stack.isEmpty() && isTame()) {
      if (!isOwnedBy(player)) return InteractionResult.FAIL;
      if (!level().isClientSide) {
        var item = pickupFor(player);
        if (item.isEmpty()) return InteractionResult.FAIL;
        player.setItemInHand(hand, item);
        discard();
      }
      return InteractionResult.sidedSuccess(level().isClientSide);
    }
    return super.mobInteract(player, hand);
  }

  public ItemStack pickupFor(Player player) {
    if (!isOwnedBy(player)) return ItemStack.EMPTY;
    CompoundTag data = new CompoundTag();
    saveWithoutId(data);
    for (String key : List.of("UUID", "Pos", "Motion", "Rotation", "Leash", "Passengers"))
      data.remove(key);
    var stack = new ItemStack(Content.SLUG_CARRIER.get());
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    return stack;
  }

  @Override
  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putFloat("SlugSize", size());
    t.putLong("CopperReady", copperReady);
    t.putInt("Dryness", dryness);
    t.putString("AttachedFace", face().getName());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    entityData.set(SIZE, net.minecraft.util.Mth.clamp(t.getFloat("SlugSize"), .05f, 1f));
    copperReady = t.getLong("CopperReady");
    dryness = Math.max(0, t.getInt("Dryness"));
    var d = Direction.byName(t.getString("AttachedFace"));
    entityData.set(FACE, d == null ? Direction.DOWN : d);
    refreshDimensions();
  }
}
