package dev.riley.wildlands.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riley.wildlands.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** Surface-aware generator with continuous height transitions and an actual deep ocean floor. */
public final class WildChunkGenerator extends NoiseBasedChunkGenerator {
  public static final MapCodec<WildChunkGenerator> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.getBiomeSource()),
                      NoiseGeneratorSettings.CODEC
                          .fieldOf("settings")
                          .forGetter(g -> g.generatorSettings()))
                  .apply(i, WildChunkGenerator::new));

  public WildChunkGenerator(BiomeSource source, Holder<NoiseGeneratorSettings> settings) {
    super(source, settings);
    if (!(source instanceof WildBiomeSource))
      throw new IllegalArgumentException(
          "Wildlands generator needs wildlands:regions biome source");
  }

  private WildBiomeSource source() {
    return (WildBiomeSource) getBiomeSource();
  }

  @Override
  protected MapCodec<? extends NoiseBasedChunkGenerator> codec() {
    return CODEC;
  }

  @Override
  public ChunkGeneratorStructureState createState(
      HolderLookup<StructureSet> lookup, RandomState random, long seed) {
    source().seed(seed);
    return super.createState(lookup, random, seed);
  }

  private double wave(int x, int z) {
    return Math.sin((x + (source().seed() & 1023)) * 0.017) * Math.cos(z * 0.021);
  }

  private double rawHeight(String name, int x, int z) {
    double w = wave(x, z), small = Math.sin(x * 0.067) * Math.cos(z * 0.053) * 2;
    return switch (name) {
      case "white_mountains" -> 85 + Math.abs(w) * Settings.MOUNTAIN_HEIGHT.get() + small;
      case "depths" -> -48 + ((BiomeLayout.hash(source().seed(), x, z) >>> 60) % 6) - 2;
      case "ocean_trench" -> -30 - Math.abs(w) * 22 + small;
      case "dead_reef" -> -8 + small;
      case "sulphuric_spires" -> 68 + w * 9 + small;
      case "volcanic_badlands" -> 76 + w * Settings.VOLCANIC_ROUGHNESS.get() + small;
      case "ignimbrite_flats" -> 73 + small;
      case "rocky_plains" -> 70 + w * 3 + small;
      case "crater" -> {
        double dx = Math.floorMod(x, 512) - 256, dz = Math.floorMod(z, 512) - 256;
        double d = Math.sqrt(dx * dx + dz * dz), r = Settings.CRATER_RADIUS.get();
        yield 66
            + Settings.CRATER_DEPTH.get()
            + small
            - (d < r ? Settings.CRATER_DEPTH.get() * (1 - d * d / (r * r)) : 0)
            + Math.exp(-Math.pow((d - r) / 10, 2)) * 12;
      }
      default -> 76 + w * 8 + small;
    };
  }

  public int height(int x, int z) {
    String name = source().nameAt(x, z);
    double h = rawHeight(name, x, z);
    // Satellite bands deliberately meet their parent as narrow geological margins.
    if (BiomeLayout.satellite(name)) return (int) Math.round(h);
    int lx = Math.floorMod(x, 512), lz = Math.floorMod(z, 512);
    int dx = lx < 256 ? -1 : 1, dz = lz < 256 ? -1 : 1;
    int ex = Math.min(lx, 511 - lx), ez = Math.min(lz, 511 - lz);
    if (ex < 32) {
      double t = (32 - ex) / 64.0;
      h = h * (1 - t) + rawHeight(source().nameAt(x + dx * 64, z), x, z) * t;
    }
    if (ez < 32) {
      double t = (32 - ez) / 64.0;
      h = h * (1 - t) + rawHeight(source().nameAt(x, z + dz * 64), x, z) * t;
    }
    return (int) Math.round(h);
  }

  private BlockState block(int x, int y, int z, int h, String biome) {
    if (y == -64) return Blocks.BEDROCK.defaultBlockState();
    if (y > h) return (y <= 62 ? Blocks.WATER : Blocks.AIR).defaultBlockState();
    long v = BiomeLayout.hash(source().seed() + y, x, z);
    if (biome.equals("crater")
        && Math.hypot(Math.floorMod(x, 512) - 256, Math.floorMod(z, 512) - 256)
            < Settings.IRON_RADIUS.get()
        && y > h - 6)
      return ((v & 65535) / 65535.0 < Settings.IRON_RICHNESS.get()
              ? Blocks.RAW_IRON_BLOCK
              : Blocks.IRON_ORE)
          .defaultBlockState();
    if (y < h - 4) {
      if ((v & 511) == 0)
        return (y < 0 ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE).defaultBlockState();
      if ((v & 511) == 1)
        return (y < 0 ? Blocks.DEEPSLATE_COAL_ORE : Blocks.COAL_ORE).defaultBlockState();
      if (y < 0 && (v & 4095) == 2) return Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
      return (y < 0 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
    }
    return switch (biome) {
      case "white_mountains" ->
          ((v & 65535) / 65535.0 < Settings.CALCITE_SHARE.get() ? Blocks.CALCITE : Blocks.DIORITE)
              .defaultBlockState();
      case "depths" -> ((v & 3) == 0 ? Blocks.GRAVEL : Blocks.CLAY).defaultBlockState();
      case "ocean_trench", "dead_reef" -> Blocks.GRAVEL.defaultBlockState();
      case "volcanic_badlands" ->
          ((v & 3) == 0 ? Blocks.BLACKSTONE : Blocks.BASALT).defaultBlockState();
      case "sulphuric_spires" -> Blocks.YELLOW_TERRACOTTA.defaultBlockState();
      case "ignimbrite_flats" -> Content.IGNIMBRITE.get().defaultBlockState();
      case "burnt_forest" ->
          ((v & 1) == 0 ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT).defaultBlockState();
      case "rocky_plains" ->
          y == h && ((v & 31) == 0)
              ? Content.LIMESTONE.get().defaultBlockState()
              : (y == h ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
      default -> (y == h ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
    };
  }

  @Override
  public CompletableFuture<ChunkAccess> fillFromNoise(
      Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
    BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
    var origin = chunk.getPos().getWorldPosition();
    for (int x = 0; x < 16; x++)
      for (int z = 0; z < 16; z++) {
        int wx = origin.getX() + x, wz = origin.getZ() + z, h = height(wx, wz);
        String biome = source().nameAt(wx, wz);
        for (int y = -64; y <= Math.max(h, 62); y++)
          chunk.setBlockState(p.set(wx, y, wz), block(wx, y, wz, h, biome), false);
      }
    Heightmap.primeHeightmaps(
        chunk, java.util.Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
    return CompletableFuture.completedFuture(chunk);
  }

  @Override
  public void buildSurface(
      WorldGenRegion level, StructureManager structures, RandomState random, ChunkAccess chunk) {}

  @Override
  public void applyCarvers(
      WorldGenRegion level,
      long seed,
      RandomState random,
      BiomeManager manager,
      StructureManager structures,
      ChunkAccess chunk,
      GenerationStep.Carving step) {}

  @Override
  public int getBaseHeight(
      int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
    return (type == Heightmap.Types.OCEAN_FLOOR_WG || type == Heightmap.Types.OCEAN_FLOOR
            ? height(x, z)
            : Math.max(height(x, z), 62))
        + 1;
  }

  @Override
  public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
    var a = new BlockState[384];
    int h = height(x, z);
    String b = source().nameAt(x, z);
    for (int i = 0; i < a.length; i++) a[i] = block(x, i - 64, z, h, b);
    return new NoiseColumn(-64, a);
  }
}
