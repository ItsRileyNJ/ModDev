package dev.riley.wildlands.entity;

import dev.riley.wildlands.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.registries.*;

public final class Creatures {
  static final DeferredRegister<EntityType<?>> TYPES =
      DeferredRegister.create(Registries.ENTITY_TYPE, Wildlands.ID);
  public static final DeferredHolder<EntityType<?>, EntityType<RootWorm>> WORM =
      TYPES.register(
          "root_worm",
          () ->
              EntityType.Builder.of(RootWorm::new, MobCategory.CREATURE)
                  .sized(.8f, .35f)
                  .clientTrackingRange(8)
                  .build("root_worm"));
  public static final DeferredHolder<EntityType<?>, EntityType<BananaSlug>> SLUG =
      TYPES.register(
          "banana_slug",
          () ->
              EntityType.Builder.of(BananaSlug::new, MobCategory.CREATURE)
                  .sized(.8f, .18f)
                  .clientTrackingRange(8)
                  .build("banana_slug"));
  public static final DeferredHolder<EntityType<?>, EntityType<Flytrap>> FLYTRAP =
      TYPES.register(
          "flytrap",
          () ->
              EntityType.Builder.of(Flytrap::new, MobCategory.CREATURE)
                  .sized(1.7f, 2.3f)
                  .clientTrackingRange(10)
                  .build("flytrap"));
  public static final DeferredHolder<EntityType<?>, EntityType<DepthStalker>> STALKER =
      TYPES.register(
          "depth_stalker",
          () ->
              EntityType.Builder.of(DepthStalker::new, MobCategory.WATER_CREATURE)
                  .sized(4, 3)
                  .clientTrackingRange(16)
                  .build("depth_stalker"));

  public static void register(IEventBus bus) {
    TYPES.register(bus);
    bus.addListener(Creatures::attributes);
    bus.addListener(Creatures::placements);
    egg("root_worm", WORM, 0x50321c, 0xab7240);
    egg("banana_slug", SLUG, 0xd6b53a, 0x9c7826);
    egg("flytrap", FLYTRAP, 0x315429, 0x9d3647);
    egg("depth_stalker", STALKER, 0x101b28, 0x243544);
  }

  private static void egg(
      String id,
      java.util.function.Supplier<? extends EntityType<? extends Mob>> type,
      int a,
      int b) {
    Content.ITEMS.register(
        id + "_spawn_egg",
        () ->
            new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                type, a, b, new Item.Properties()));
  }

  static void attributes(EntityAttributeCreationEvent e) {
    e.put(WORM.get(), RootWorm.attributes().build());
    e.put(SLUG.get(), BananaSlug.attributes().build());
    e.put(FLYTRAP.get(), Flytrap.attributes().build());
    e.put(STALKER.get(), DepthStalker.attributes().build());
  }

  static void placements(RegisterSpawnPlacementsEvent e) {
    e.register(
        WORM.get(),
        SpawnPlacementTypes.ON_GROUND,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        (t, l, r, p, random) ->
            Settings.WORM_WEIGHT.get() > 0
                && l.getBiome(p).is(Wildlands.id("rootlands"))
                && Mob.checkMobSpawnRules(t, l, r, p, random),
        RegisterSpawnPlacementsEvent.Operation.REPLACE);
    e.register(
        SLUG.get(),
        SpawnPlacementTypes.ON_GROUND,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        (t, l, r, p, random) ->
            Settings.SLUG_WEIGHT.get() > 0
                && l.getBiome(p).is(Wildlands.id("redwood_forest"))
                && Mob.checkMobSpawnRules(t, l, r, p, random),
        RegisterSpawnPlacementsEvent.Operation.REPLACE);
    e.register(
        FLYTRAP.get(),
        SpawnPlacementTypes.ON_GROUND,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        (t, l, r, p, random) ->
            Settings.FLYTRAP_WEIGHT.get() > 0
                && l.getBiome(p).is(Wildlands.id("verdent"))
                && Mob.checkMobSpawnRules(t, l, r, p, random),
        RegisterSpawnPlacementsEvent.Operation.REPLACE);
    e.register(
        STALKER.get(),
        SpawnPlacementTypes.IN_WATER,
        Heightmap.Types.OCEAN_FLOOR,
        (t, l, r, p, random) ->
            Settings.DEPTHS_WEIGHT.get() > 0
                && l.getBiome(p).is(Wildlands.id("depths"))
                && l.getFluidState(p).is(FluidTags.WATER)
                && p.getY() < 45
                && dev.riley.wildlands.gameplay.EncounterClock.get(l.getLevel())
                    .ready(l.getLevel().getGameTime())
                && random.nextInt(Settings.DEPTHS_SPAWN_CHANCE.get()) == 0
                && l.getLevel()
                    .getEntitiesOfClass(
                        DepthStalker.class, new net.minecraft.world.phys.AABB(p).inflate(256))
                    .isEmpty(),
        RegisterSpawnPlacementsEvent.Operation.REPLACE);
  }
}
