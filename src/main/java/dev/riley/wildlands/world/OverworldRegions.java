package dev.riley.wildlands.world;

import dev.riley.wildlands.Wildlands;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/** Per-registry Overworld overlay. Vanilla regions retain the vanilla generator. */
public final class OverworldRegions {
  public interface Access {
    OverworldRegions wildlands$regions();
  }

  public interface RegistryAccess extends Access {
    void wildlands$configure(net.minecraft.core.HolderGetter<Biome> biomes);
  }

  private final WildBiomeSource source;
  private WildChunkGenerator terrain;

  public OverworldRegions(List<Holder<Biome>> biomes) {
    source = new WildBiomeSource(biomes);
  }

  public void seed(long seed) {
    source.seed(seed);
  }

  public WildBiomeSource source() {
    return source;
  }

  public boolean custom(int quartX, int quartZ) {
    // A shared region gate preserves the satellite/parent adjacency inside each cell.
    return Math.floorMod(
                BiomeLayout.hash(
                    source.seed() ^ 0x57494c444c414e44L,
                    Math.floorDiv(quartX, 128),
                    Math.floorDiv(quartZ, 128)),
                4)
            == 0
        && !source.quartName(quartX, quartZ).equals("plains");
  }

  public List<Holder<Biome>> biomes() {
    return source.possibleBiomes().stream()
        .filter(b -> b.unwrapKey().orElseThrow().location().getNamespace().equals(Wildlands.ID))
        .toList();
  }

  public WildChunkGenerator terrain(Holder<NoiseGeneratorSettings> settings) {
    if (terrain == null) terrain = new WildChunkGenerator(source, settings);
    return terrain;
  }

  public double strength(int x, int z) {
    int lx = Math.floorMod(x, 512), lz = Math.floorMod(z, 512);
    return Math.min(1.0, Math.min(Math.min(lx, 511 - lx), Math.min(lz, 511 - lz)) / 32.0);
  }

  public int height(int x, int z, int vanilla, Holder<NoiseGeneratorSettings> settings) {
    return (int) Math.round(vanilla + (terrain(settings).height(x, z) - vanilla) * strength(x, z));
  }

  /** Replaces native carving in custom cells, before structures and biome features are placed. */
  public void applyTerrain(ChunkAccess chunk, Holder<NoiseGeneratorSettings> settings) {
    var pos = new BlockPos.MutableBlockPos();
    var generator = terrain(settings);
    for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++)
      for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
        if (!custom(Math.floorDiv(x, 4), Math.floorDiv(z, 4))) continue;
        int vanilla = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15);
        int height = height(x, z, vanilla, settings);
        String biome = source.nameAt(x, z);
        for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
          BlockState state = generator.terrainBlock(x, y, z, height, biome);
          chunk.setBlockState(pos.set(x, y, z), state, false);
        }
      }
    Heightmap.primeHeightmaps(
        chunk, java.util.Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
  }
}
