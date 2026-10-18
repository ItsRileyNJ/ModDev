package dev.riley.wildlands.world;

import dev.riley.wildlands.*;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class BiomeDetailsFeature extends Feature<NoneFeatureConfiguration> {
  public BiomeDetailsFeature() {
    super(NoneFeatureConfiguration.CODEC);
  }

  public static boolean is(WorldGenLevel l, BlockPos p, String name) {
    return l.getBiome(p).is(Wildlands.id(name));
  }

  @Override
  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
    var l = ctx.level();
    var r = ctx.random();
    var origin = ctx.origin();
    for (String biome : Settings.BIOMES.keySet()) {
      if (!Settings.enabled(biome)
          || (!biome.equals("ocean_trench") && Settings.biome(biome).attempts() == null)) continue;
      if (biome.equals("depths") && r.nextInt(Settings.BACTERIA_CHANCE.get()) != 0) continue;
      int attempts =
          biome.equals("ocean_trench")
              ? Settings.TRENCH_ORES.get()
              : Settings.biome(biome).attempts().get();
      for (int i = 0; i < attempts; i++) {
        int x = origin.getX() + r.nextInt(16), z = origin.getZ() + r.nextInt(16);
        BlockPos p = l.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, new BlockPos(x, 0, z));
        if (!is(l, p, biome)) continue;
        switch (biome) {
          case "rootlands" -> {
            patch(
                l,
                r,
                p,
                Settings.biome(biome).patchSize().get(),
                r.nextBoolean() ? Blocks.ROOTED_DIRT : Blocks.COARSE_DIRT);
            BlockPos flower = l.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, p);
            if (r.nextDouble() < Settings.FLOWER_CHANCE.get()
                && l.isEmptyBlock(flower)
                && Content.ROOT_FLOWER.get().defaultBlockState().canSurvive(l, flower))
              l.setBlock(flower, Content.ROOT_FLOWER.get().defaultBlockState(), 2);
          }
          case "white_mountains" -> {
            if (l.isEmptyBlock(p)) {
              l.setBlock(p.below(), Blocks.ROOTED_DIRT.defaultBlockState(), 2);
              l.setBlock(
                  p,
                  (r.nextInt(4) == 0 ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA).defaultBlockState(),
                  2);
            }
          }
          case "burnt_forest" -> {
            if (l.isEmptyBlock(p)) l.setBlock(p, Blocks.DEAD_BUSH.defaultBlockState(), 2);
          }
          case "volcanic_badlands" -> {
            if (r.nextDouble() < Settings.POOL_CHANCE.get()) pool(l, p, Settings.POOL_RADIUS.get());
          }
          case "rocky_plains" -> {
            int radius = Settings.biome(biome).patchSize().get();
            for (int dx = -radius; dx <= radius; dx++)
              for (int dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz < radius * radius) {
                  BlockPos q =
                      l.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, p.offset(dx, 0, dz))
                          .below();
                  if (is(l, q, biome))
                    l.setBlock(q, Content.LIMESTONE.get().defaultBlockState(), 2);
                }
            if (r.nextDouble() < Settings.FOSSIL_CHANCE.get()) {
              Block fossil =
                  r.nextBoolean() ? Content.PLANT_FOSSIL.get() : Content.ANIMAL_FOSSIL.get();
              for (int k = 0, length = 2 + r.nextInt(Settings.FOSSIL_LENGTH.get() - 1);
                  k < length;
                  k++) l.setBlock(p.offset(k, 0, 0), fossil.defaultBlockState(), 2);
            }
          }
          case "dead_reef" -> deadReef(l, r, p);
          case "depths" -> {
            if (l.getBlockState(p).is(Blocks.WATER))
              l.setBlock(
                  p,
                  Content.BACTERIA
                      .get()
                      .defaultBlockState()
                      .setValue(BlockStateProperties.DOWN, true)
                      .setValue(BlockStateProperties.WATERLOGGED, true),
                  2);
          }
          case "ocean_trench" -> {
            int y = p.getY() - 1 - r.nextInt(12);
            BlockPos q = new BlockPos(x, y, z);
            l.setBlock(
                q,
                (r.nextInt(8) == 0 ? Blocks.DEEPSLATE_DIAMOND_ORE : Blocks.DEEPSLATE_IRON_ORE)
                    .defaultBlockState(),
                2);
          }
        }
      }
    }
    return true;
  }

  private static void patch(
      WorldGenLevel l, RandomSource r, BlockPos p, int radius, Block material) {
    for (int x = -radius; x <= radius; x++)
      for (int z = -radius; z <= radius; z++)
        if (x * x + z * z <= radius * radius && r.nextFloat() < 0.75) {
          var q = l.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, p.offset(x, 0, z)).below();
          if (is(l, q, "rootlands")) l.setBlock(q, material.defaultBlockState(), 2);
        }
  }

  private static void pool(WorldGenLevel l, BlockPos p, int radius) {
    // Basalt bowl with a source-only lava fluid: it cannot spill when a wall is removed.
    for (int x = -radius - 1; x <= radius + 1; x++)
      for (int z = -radius - 1; z <= radius + 1; z++) {
        var q = p.offset(x, -1, z);
        if (!is(l, q, "volcanic_badlands")) continue;
        if (x * x + z * z < radius * radius) {
          l.setBlock(q.below(), Blocks.BASALT.defaultBlockState(), 2);
          l.setBlock(q, Content.STILL_LAVA.get().defaultBlockState(), 2);
          l.setBlock(q.above(), Blocks.AIR.defaultBlockState(), 2);
        } else if (x * x + z * z <= (radius + 1) * (radius + 1))
          l.setBlock(q, Blocks.BASALT.defaultBlockState(), 2);
      }
  }

  private static boolean coral(LevelAccessor l, RandomSource r, BlockPos p) {
    if (!l.getBiome(p).is(Wildlands.id("dead_reef")) || !l.getBlockState(p).is(Blocks.WATER))
      return false;
    Block[] blocks = {
      Blocks.DEAD_TUBE_CORAL_BLOCK,
      Blocks.DEAD_BRAIN_CORAL_BLOCK,
      Blocks.DEAD_BUBBLE_CORAL_BLOCK,
      Blocks.DEAD_FIRE_CORAL_BLOCK,
      Blocks.DEAD_HORN_CORAL_BLOCK
    };
    l.setBlock(p, blocks[r.nextInt(blocks.length)].defaultBlockState(), 2);
    if (r.nextFloat() < 0.25 && l.getBlockState(p.above()).is(Blocks.WATER))
      l.setBlock(
          p.above(),
          Blocks.DEAD_TUBE_CORAL_FAN
              .defaultBlockState()
              .setValue(BlockStateProperties.WATERLOGGED, true),
          2);
    return true;
  }

  private static void deadReef(WorldGenLevel l, RandomSource r, BlockPos p) {
    // Reuse Mojang's actual three coral arrangement algorithms; every placement emits dead coral
    // only.
    Feature<NoneFeatureConfiguration> f =
        switch (r.nextInt(3)) {
          case 0 ->
              new CoralClawFeature(NoneFeatureConfiguration.CODEC) {
                @Override
                protected boolean placeCoralBlock(
                    LevelAccessor a, RandomSource b, BlockPos c, BlockState d) {
                  return coral(a, b, c);
                }
              };
          case 1 ->
              new CoralTreeFeature(NoneFeatureConfiguration.CODEC) {
                @Override
                protected boolean placeCoralBlock(
                    LevelAccessor a, RandomSource b, BlockPos c, BlockState d) {
                  return coral(a, b, c);
                }
              };
          default ->
              new CoralMushroomFeature(NoneFeatureConfiguration.CODEC) {
                @Override
                protected boolean placeCoralBlock(
                    LevelAccessor a, RandomSource b, BlockPos c, BlockState d) {
                  return coral(a, b, c);
                }
              };
        };
    f.place(
        NoneFeatureConfiguration.INSTANCE, l, l.getLevel().getChunkSource().getGenerator(), r, p);
  }
}
