package dev.riley.wildlands.mixin;

import dev.riley.wildlands.world.OverworldRegions;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerator.class)
public abstract class OverworldSeedMixin {
  @Inject(method = "createState", at = @At("HEAD"))
  private void wildlands$seed(
      HolderLookup<StructureSet> lookup,
      RandomState random,
      long seed,
      CallbackInfoReturnable<ChunkGeneratorStructureState> ci) {
    var source = ((ChunkGenerator) (Object) this).getBiomeSource();
    if (source instanceof OverworldRegions.Access access && access.wildlands$regions() != null)
      access.wildlands$regions().seed(seed);
  }
}
