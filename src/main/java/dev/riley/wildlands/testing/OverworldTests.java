package dev.riley.wildlands.testing;

import com.mojang.serialization.JsonOps;
import dev.riley.wildlands.*;
import dev.riley.wildlands.world.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Wildlands.ID)
@PrefixGameTestTemplate(false)
public final class OverworldTests {
  private static NoiseBasedChunkGenerator normal(GameTestHelper h) {
    return (NoiseBasedChunkGenerator)
        h.getLevel()
            .registryAccess()
            .registryOrThrow(Registries.WORLD_PRESET)
            .getHolderOrThrow(WorldPresets.NORMAL)
            .value()
            .overworld()
            .orElseThrow()
            .generator();
  }

  private static RandomState random(GameTestHelper h, NoiseBasedChunkGenerator gen, long seed) {
    return RandomState.create(
        gen.generatorSettings().value(),
        h.getLevel().registryAccess().lookupOrThrow(Registries.NOISE),
        seed);
  }

  private static OverworldRegions seeded(
      GameTestHelper h, NoiseBasedChunkGenerator gen, long seed) {
    gen.createState(
        h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),
        random(h, gen, seed),
        seed);
    var regions = ((OverworldRegions.Access) gen.getBiomeSource()).wildlands$regions();
    h.assertTrue(regions != null, "Normal Overworld source has the overlay after datapack loading");
    h.assertTrue(regions.source().seed() == seed, "Generator startup supplies the world seed");
    return regions;
  }

  @GameTest(template = "empty", timeoutTicks = 400)
  public static void normalOverworldContainsVanillaAndAllCustomBiomes(GameTestHelper h) {
    var gen = normal(h);
    var source = (MultiNoiseBiomeSource) gen.getBiomeSource();
    var regions = seeded(h, gen, 734921);
    var sampler = random(h, gen, 734921).sampler();
    var found = new HashSet<String>();
    int vanilla = 0, custom = 0;
    for (int x = -24; x <= 24; x++)
      for (int z = -24; z <= 24; z++) {
        int qx = x * 128 + 64, qz = z * 128 + 64;
        var actual = source.getNoiseBiome(qx, 20, qz, sampler);
        if (regions.custom(qx, qz)) {
          custom++;
          found.add(actual.unwrapKey().orElseThrow().location().getPath());
          for (int edge : new int[] {1, 126}) {
            var border = source.getNoiseBiome(x * 128 + edge, 20, qz, sampler);
            found.add(border.unwrapKey().orElseThrow().location().getPath());
            if (border.is(Wildlands.id("ignimbrite_flats"))
                || border.is(Wildlands.id("dead_reef"))) {
              var parent =
                  border.is(Wildlands.id("dead_reef")) ? "sulphuric_spires" : "volcanic_badlands";
              h.assertTrue(
                  source
                      .getNoiseBiome(x * 128 + (edge == 1 ? 0 : 127), 20, qz, sampler)
                      .is(Wildlands.id(parent)),
                  "Overworld satellite has its actual parent neighbor");
            }
          }
        } else {
          vanilla++;
          h.assertTrue(
              actual.equals(source.getNoiseBiome(sampler.sample(qx, 20, qz))),
              "Vanilla regions use the original climate lookup unchanged");
        }
      }
    h.assertTrue(
        found.containsAll(BiomeLayout.NAMES), "All 13 custom biomes actually sampled: " + found);
    h.assertTrue(vanilla > custom && custom > 0, "Vanilla remains the majority of region cells");
    for (String name : BiomeLayout.NAMES)
      h.assertTrue(
          source.possibleBiomes().stream().anyMatch(b -> b.is(Wildlands.id(name))),
          "Locate/feature biome set includes " + name);
    var located =
        source.findClosestBiome3d(
            new BlockPos(0, 80, 0),
            6400,
            64,
            64,
            b -> b.is(Wildlands.id("redwood_forest")),
            sampler,
            h.getLevel());
    h.assertTrue(located != null, "Native locate finds a custom biome in the normal Overworld");
    regions.seed(h.getLevel().getSeed());
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 400)
  public static void normalOverworldDepthTerrainAndVanillaColumns(GameTestHelper h) {
    var gen = normal(h);
    var regions = seeded(h, gen, 734921);
    var random = random(h, gen, 734921);
    BlockPos depths = null, vanilla = null;
    for (int x = -24; x <= 24; x++)
      for (int z = -24; z <= 24; z++) {
        int bx = x * 512 + 256, bz = z * 512 + 256;
        if (regions.custom(bx / 4, bz / 4) && regions.source().nameAt(bx, bz).equals("depths"))
          depths = new BlockPos(bx, 0, bz);
        if (!regions.custom(bx / 4, bz / 4)) vanilla = new BlockPos(bx, 0, bz);
      }
    h.assertTrue(depths != null && vanilla != null, "Found both terrain paths");
    var column = gen.getBaseColumn(depths.getX(), depths.getZ(), h.getLevel(), random);
    int floor =
        gen.getBaseHeight(
                depths.getX(), depths.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, h.getLevel(), random)
            - 1;
    h.assertTrue(
        floor >= -50 && floor <= -45, "Normal Overworld Depths has the required deep floor");
    h.assertTrue(
        column.getBlock(0).is(Blocks.WATER)
            && column.getBlock(62).is(Blocks.WATER)
            && column.getBlock(63).isAir(),
        "Water extends to sea level in actual normal-generator columns");
    var chunk =
        new ProtoChunk(
            new ChunkPos(depths),
            UpgradeData.EMPTY,
            h.getLevel(),
            h.getLevel().registryAccess().registryOrThrow(Registries.BIOME),
            null);
    // This isolated chunk has no structure starts; avoid consulting the test server's flat world.
    var structures =
        new StructureManager(h.getLevel(), new WorldOptions(734921, false, false), null) {
          @Override
          public java.util.List<net.minecraft.world.level.levelgen.structure.StructureStart>
              startsForStructure(
                  ChunkPos pos,
                  java.util.function.Predicate<
                          net.minecraft.world.level.levelgen.structure.Structure>
                      filter) {
            return java.util.List.of();
          }
        };
    var blender = net.minecraft.world.level.levelgen.blending.Blender.empty();
    gen.createBiomes(random, blender, structures, chunk).join();
    chunk.setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);
    gen.fillFromNoise(blender, random, structures, chunk).join();
    gen.applyCarvers(null, 734921, random, null, null, chunk, GenerationStep.Carving.AIR);
    h.assertTrue(
        chunk.getBlockState(depths).is(Blocks.WATER),
        "Native noise/carving pipeline creates the deep water column");
    h.assertTrue(
        chunk.getBlockState(depths.atY(floor)).is(Blocks.CLAY)
            || chunk.getBlockState(depths.atY(floor)).is(Blocks.GRAVEL),
        "Actual seafloor material");
    h.assertTrue(
        chunk.getBlockState(depths.atY(80)).isAir(), "Old noise terrain above ocean was removed");
    h.assertTrue(
        chunk.getNoiseBiome(depths.getX() / 4, 0, depths.getZ() / 4).is(Wildlands.id("depths")),
        "Biome actually stored in the generated normal Overworld chunk");
    var plain =
        new NoiseBasedChunkGenerator(
            MultiNoiseBiomeSource.createFromList(
                ((MultiNoiseBiomeSourceParameterList)
                        h.getLevel()
                            .registryAccess()
                            .registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                            .getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
                            .value())
                    .parameters()),
            gen.generatorSettings());
    var expected = plain.getBaseColumn(vanilla.getX(), vanilla.getZ(), h.getLevel(), random);
    var actual = gen.getBaseColumn(vanilla.getX(), vanilla.getZ(), h.getLevel(), random);
    for (int y = -64; y < 320; y++)
      h.assertTrue(
          expected.getBlock(y).equals(actual.getBlock(y)), "Vanilla column unchanged at " + y);
    regions.seed(h.getLevel().getSeed());
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void normalWorldCodecAndOtherDimensions(GameTestHelper h) {
    var gen = normal(h);
    var regions = seeded(h, gen, 87235);
    var ops = RegistryOps.create(JsonOps.INSTANCE, h.getLevel().registryAccess());
    var encoded = ChunkGenerator.CODEC.encodeStart(ops, gen).getOrThrow();
    h.assertTrue(
        encoded.getAsJsonObject().get("type").getAsString().equals("minecraft:noise"),
        "Normal saves keep the vanilla generator ID");
    var decoded = ChunkGenerator.CODEC.parse(ops, encoded).getOrThrow();
    h.assertTrue(
        ((OverworldRegions.Access) decoded.getBiomeSource()).wildlands$regions() != null,
        "Reloaded normal saves retain automatic integration");
    var preset =
        h.getLevel()
            .registryAccess()
            .registryOrThrow(Registries.WORLD_PRESET)
            .getHolderOrThrow(WorldPresets.NORMAL)
            .value();
    var nether =
        preset
            .createWorldDimensions()
            .get(net.minecraft.world.level.dimension.LevelStem.NETHER)
            .orElseThrow()
            .generator()
            .getBiomeSource();
    h.assertTrue(
        ((OverworldRegions.Access) nether).wildlands$regions() == null,
        "Nether has no Overworld overlay");
    h.assertTrue(
        nether.possibleBiomes().stream()
            .noneMatch(
                b -> b.unwrapKey().orElseThrow().location().getNamespace().equals(Wildlands.ID)),
        "No custom biomes in Nether");
    h.assertTrue(
        preset
                .createWorldDimensions()
                .get(net.minecraft.world.level.dimension.LevelStem.END)
                .orElseThrow()
                .generator()
                .getBiomeSource()
            instanceof TheEndBiomeSource,
        "End source unchanged");
    h.assertTrue(
        h.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.LEVEL_STEM)
                .get(Wildlands.id("realm"))
            == null,
        "Separate realm removed from datapack dimensions");
    regions.seed(h.getLevel().getSeed());
    h.succeed();
  }
}
