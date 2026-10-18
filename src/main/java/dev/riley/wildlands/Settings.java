package dev.riley.wildlands;

import java.util.*;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server operators own this common config. Worldgen changes affect new chunks only. */
public final class Settings {
  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

  public record BiomeOptions(
      ModConfigSpec.BooleanValue enabled,
      ModConfigSpec.IntValue weight,
      ModConfigSpec.IntValue spacing,
      ModConfigSpec.DoubleValue density,
      ModConfigSpec.IntValue attempts,
      ModConfigSpec.IntValue patchSize) {}

  public static final Map<String, BiomeOptions> BIOMES = new LinkedHashMap<>();

  static {
    for (String name :
        List.of(
            "rootlands",
            "white_mountains",
            "crater",
            "volcanic_badlands",
            "burnt_forest",
            "redwood_forest",
            "ignimbrite_flats",
            "verdent",
            "rocky_plains",
            "dead_reef",
            "sulphuric_spires",
            "depths",
            "ocean_trench")) {
      B.push(name);
      boolean templates =
          List.of("rootlands", "burnt_forest", "redwood_forest", "sulphuric_spires").contains(name);
      boolean details =
          List.of(
                  "rootlands",
                  "white_mountains",
                  "volcanic_badlands",
                  "burnt_forest",
                  "rocky_plains",
                  "dead_reef",
                  "depths")
              .contains(name);
      BIOMES.put(
          name,
          new BiomeOptions(
              B.define("enabled", true),
              List.of("dead_reef", "ignimbrite_flats").contains(name)
                  ? null
                  : B.defineInRange("regionWeight", 1, 1, 100),
              templates
                  ? B.defineInRange(
                      "structureSpacingChunks", name.equals("redwood_forest") ? 4 : 2, 1, 64)
                  : null,
              templates ? B.defineInRange("structureProbability", 0.65, 0, 1) : null,
              details
                  ? B.defineInRange("featureAttempts", name.equals("depths") ? 1 : 5, 0, 64)
                  : null,
              List.of("rootlands", "rocky_plains").contains(name)
                  ? B.defineInRange("patchRadius", 5, 1, 12)
                  : null));
      B.pop();
    }
  }

  private static ModConfigSpec.IntValue integer(String key, int value, int min, int max) {
    return B.defineInRange(key, value, min, max);
  }

  private static ModConfigSpec.DoubleValue decimal(
      String key, double value, double min, double max) {
    return B.defineInRange(key, value, min, max);
  }

  public static final ModConfigSpec.IntValue CRATER_RADIUS =
      integer("terrain.craterRadius", 100, 24, 200);
  public static final ModConfigSpec.IntValue CRATER_DEPTH =
      integer("terrain.craterDepth", 38, 8, 80);
  public static final ModConfigSpec.IntValue IRON_RADIUS =
      integer("terrain.ironDepositRadius", 10, 1, 30);
  public static final ModConfigSpec.DoubleValue IRON_RICHNESS =
      decimal("terrain.ironRichness", 0.6, 0, 1);
  public static final ModConfigSpec.IntValue MOUNTAIN_HEIGHT =
      integer("terrain.mountainHeight", 90, 20, 180);
  public static final ModConfigSpec.DoubleValue CALCITE_SHARE =
      decimal("terrain.calciteShare", 0.65, 0, 1);
  public static final ModConfigSpec.IntValue VOLCANIC_ROUGHNESS =
      integer("terrain.volcanicRoughness", 15, 0, 40);
  public static final ModConfigSpec.IntValue POOL_RADIUS = integer("volcanicPools.radius", 3, 1, 8);
  public static final ModConfigSpec.DoubleValue POOL_CHANCE =
      decimal("volcanicPools.chancePerAttempt", 0.15, 0, 1);
  public static final ModConfigSpec.DoubleValue ASH =
      decimal("volcanicPools.ashIntensity", 0.12, 0, 1);
  public static final ModConfigSpec.IntValue CHARCOAL_MIN = integer("drops.charcoalMin", 1, 0, 16);
  public static final ModConfigSpec.IntValue CHARCOAL_MAX = integer("drops.charcoalMax", 3, 0, 16);
  public static final ModConfigSpec.IntValue GUNPOWDER_MAX =
      integer("drops.gunpowderMax", 2, 0, 16);
  public static final ModConfigSpec.DoubleValue GUNPOWDER_CHANCE =
      decimal("drops.gunpowderChance", 0.7, 0, 1);
  public static final ModConfigSpec.DoubleValue FLOWER_CHANCE =
      decimal("rootFlower.chancePerDirtPatch", 0.65, 0, 1);
  public static final ModConfigSpec.DoubleValue FLOWER_DAMAGE =
      decimal("rootFlower.damage", 1, 0, 20);
  public static final ModConfigSpec.BooleanValue FLOWER_SLOW = B.define("rootFlower.slows", true);
  public static final ModConfigSpec.BooleanValue FLOWER_PLAYERS =
      B.define("rootFlower.affectsPlayers", true);
  public static final ModConfigSpec.BooleanValue FLOWER_MOBS =
      B.define("rootFlower.affectsMobs", true);
  public static final ModConfigSpec.IntValue WORM_WEIGHT =
      integer("rootWorm.spawnWeight", 12, 0, 100);
  public static final ModConfigSpec.DoubleValue WORM_FIRE =
      decimal("rootWorm.fireMultiplier", 2, 1, 10);
  public static final ModConfigSpec.IntValue WORM_RADIUS =
      integer("rootWorm.fireDetectionRadius", 6, 1, 16);
  public static final ModConfigSpec.BooleanValue WORM_FLEE = B.define("rootWorm.fleeFire", true);
  public static final ModConfigSpec.DoubleValue WORM_FLEE_SPEED =
      decimal("rootWorm.fleeSpeed", 1.6, 0.1, 3);
  public static final ModConfigSpec.IntValue SLUG_WEIGHT =
      integer("bananaSlug.spawnWeight", 10, 0, 100);
  public static final ModConfigSpec.IntValue WAX_COOLDOWN =
      integer("bananaSlug.waxCooldownTicks", 1200, 20, 72000);
  public static final ModConfigSpec.ConfigValue<List<? extends String>> WAX_BLOCKS =
      B.defineListAllowEmpty(
          "bananaSlug.eligibleCopper",
          List.of(),
          () -> "minecraft:copper_block",
          x -> x instanceof String);
  public static final ModConfigSpec.ConfigValue<List<? extends String>> GOLEMS =
      B.comment("Opt-in installed entity IDs only. No vanilla copper golem exists in 1.21.1.")
          .defineListAllowEmpty(
              "bananaSlug.healableGolemTypes",
              List.of(),
              () -> "othermod:copper_golem",
              x -> x instanceof String);
  public static final ModConfigSpec.DoubleValue GOLEM_HEAL =
      decimal("bananaSlug.golemHealAmount", 2, 0, 100);
  public static final ModConfigSpec.IntValue DRY_GRACE =
      integer("bananaSlug.dryGraceTicks", 200, 0, 72000);
  public static final ModConfigSpec.IntValue DRY_INTERVAL =
      integer("bananaSlug.dryDamageInterval", 40, 1, 1200);
  public static final ModConfigSpec.DoubleValue DRY_DAMAGE =
      decimal("bananaSlug.dryDamage", 1, 0.1, 20);
  public static final ModConfigSpec.BooleanValue RAIN_PROTECTS =
      B.define("bananaSlug.rainProtects", true);
  public static final ModConfigSpec.BooleanValue WATER_PROTECTS =
      B.define("bananaSlug.waterProtects", true);
  public static final ModConfigSpec.ConfigValue<List<? extends String>> DRY_BIOMES =
      B.defineListAllowEmpty(
          "bananaSlug.dryBiomes",
          List.of(
              "minecraft:desert",
              "minecraft:badlands",
              "minecraft:wooded_badlands",
              "minecraft:eroded_badlands",
              "wildlands:volcanic_badlands"),
          () -> "minecraft:desert",
          x -> x instanceof String);
  public static final ModConfigSpec.IntValue FLYTRAP_WEIGHT =
      integer("flytrap.spawnWeight", 4, 0, 100);
  public static final ModConfigSpec.IntValue DIGEST =
      integer("flytrap.digestionTicks", 1200, 20, 72000);
  public static final ModConfigSpec.IntValue COLLECTION =
      integer("flytrap.collectionCooldownTicks", 2400, 20, 72000);
  public static final ModConfigSpec.IntValue TAMING_WINDOW =
      integer("flytrap.tamingWindowTicks", 6000, 20, 72000);
  public static final ModConfigSpec.BooleanValue PETS = B.define("flytrap.targetPets", false);
  public static final ModConfigSpec.DoubleValue SMALL_MOB =
      decimal("flytrap.maxPreyWidth", 0.9, 0.1, 2);
  public static final ModConfigSpec.ConfigValue<List<? extends String>> PREY_BLACKLIST =
      B.defineListAllowEmpty(
          "flytrap.blacklistedEntities",
          List.of("minecraft:villager", "minecraft:allay"),
          () -> "minecraft:villager",
          x -> x instanceof String);
  public static final ModConfigSpec.ConfigValue<List<? extends String>> CATEGORY_BLACKLIST =
      B.defineListAllowEmpty(
          "flytrap.blacklistedCategories",
          List.of("misc"),
          () -> "creature",
          x -> x instanceof String);
  public static final ModConfigSpec.IntValue TRAP_LIFE =
      integer("slimeTrap.lifetimeTicks", 1200, 20, 72000);
  public static final ModConfigSpec.IntValue TRAP_HOLD =
      integer("slimeTrap.immobilizeTicks", 100, 1, 1200);
  public static final ModConfigSpec.ConfigValue<List<? extends String>> TRAP_TYPES =
      B.defineListAllowEmpty(
          "slimeTrap.affectedEntityTypes",
          List.of(),
          () -> "minecraft:zombie",
          x -> x instanceof String);
  public static final ModConfigSpec.IntValue DEPTHS_WEIGHT =
      integer("depths.creatureSpawnWeight", 1, 0, 10);
  public static final ModConfigSpec.IntValue DEPTHS_SPAWN_CHANCE =
      integer("depths.creatureSpawnOneIn", 100, 1, 10000);
  public static final ModConfigSpec.IntValue ENCOUNTER_INTERVAL =
      integer("depths.minimumEncounterIntervalTicks", 72000, 6000, 1728000);
  public static final ModConfigSpec.IntValue BACTERIA_CHANCE =
      integer("depths.bacteriaOneInChunks", 400, 20, 10000);
  public static final ModConfigSpec.IntValue DROWNED_WEIGHT =
      integer("oceanTrench.drownedWeight", 100, 0, 200);
  public static final ModConfigSpec.DoubleValue FOSSIL_CHANCE =
      decimal("rockyPlains.fossilChancePerOutcrop", 1.0 / 3, 0, 1);
  public static final ModConfigSpec.IntValue FOSSIL_LENGTH =
      integer("rockyPlains.fossilMaxLength", 5, 2, 12);
  public static final ModConfigSpec.IntValue TRENCH_ORES =
      integer("oceanTrench.oreAttempts", 12, 0, 64);
  public static final ModConfigSpec SPEC = B.build();

  public static BiomeOptions biome(String id) {
    return BIOMES.get(id);
  }

  public static boolean enabled(String id) {
    return !SPEC.isLoaded() || biome(id).enabled().get();
  }

  public static int weight(String id) {
    return SPEC.isLoaded() ? biome(id).weight().get() : 1;
  }
}
