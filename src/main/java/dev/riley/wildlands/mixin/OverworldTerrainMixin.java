package dev.riley.wildlands.mixin;

import dev.riley.wildlands.world.OverworldRegions;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class OverworldTerrainMixin {
  @Unique
  private OverworldRegions wildlands$regions(int x, int z) {
    var self = (NoiseBasedChunkGenerator) (Object) this;
    if (!self.generatorSettings().is(NoiseGeneratorSettings.OVERWORLD)
        && !self.generatorSettings().is(NoiseGeneratorSettings.LARGE_BIOMES)) return null;
    if (self.getBiomeSource() instanceof OverworldRegions.Access access) {
      var regions = access.wildlands$regions();
      if (regions != null && regions.custom(Math.floorDiv(x, 4), Math.floorDiv(z, 4)))
        return regions;
    }
    return null;
  }

  @Inject(method = "getBaseHeight", at = @At("RETURN"), cancellable = true)
  private void wildlands$height(
      int x,
      int z,
      Heightmap.Types type,
      LevelHeightAccessor level,
      RandomState random,
      CallbackInfoReturnable<Integer> ci) {
    var regions = wildlands$regions(x, z);
    if (regions == null) return;
    // getBaseColumn below already returns the overlaid column, so scan it with the requested
    // predicate.
    var column = ((NoiseBasedChunkGenerator) (Object) this).getBaseColumn(x, z, level, random);
    for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--)
      if (type.isOpaque().test(column.getBlock(y))) {
        ci.setReturnValue(y + 1);
        return;
      }
    ci.setReturnValue(level.getMinBuildHeight());
  }

  @Inject(method = "getBaseColumn", at = @At("RETURN"), cancellable = true)
  private void wildlands$column(
      int x,
      int z,
      LevelHeightAccessor level,
      RandomState random,
      CallbackInfoReturnable<NoiseColumn> ci) {
    var regions = wildlands$regions(x, z);
    if (regions == null) return;
    var settings = ((NoiseBasedChunkGenerator) (Object) this).generatorSettings();
    var original = ci.getReturnValue();
    int vanilla = level.getMinBuildHeight();
    for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--)
      if (original.getBlock(y).blocksMotion()) {
        vanilla = y;
        break;
      }
    int height = regions.height(x, z, vanilla, settings);
    var states = new net.minecraft.world.level.block.state.BlockState[level.getHeight()];
    String biome = regions.source().nameAt(x, z);
    for (int i = 0; i < states.length; i++)
      states[i] =
          regions
              .terrain(settings)
              .terrainBlock(x, level.getMinBuildHeight() + i, z, height, biome);
    ci.setReturnValue(new NoiseColumn(level.getMinBuildHeight(), states));
  }

  @Inject(method = "applyCarvers", at = @At("HEAD"), cancellable = true)
  private void wildlands$terrain(
      WorldGenRegion level,
      long seed,
      RandomState random,
      BiomeManager biomes,
      StructureManager structures,
      ChunkAccess chunk,
      GenerationStep.Carving step,
      CallbackInfo ci) {
    var regions = wildlands$regions(chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ());
    if (regions != null) {
      if (step == GenerationStep.Carving.AIR)
        regions.applyTerrain(chunk, ((NoiseBasedChunkGenerator) (Object) this).generatorSettings());
      ci.cancel();
    }
  }
}
