package dev.riley.wildlands.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riley.wildlands.Settings;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.*;

/** Retains native /locate support while consulting configured spacing. */
public final class ConfiguredPlacement extends RandomSpreadStructurePlacement {
  public static final MapCodec<ConfiguredPlacement> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              placementCodec(i)
                  .and(Codec.STRING.fieldOf("biome_config").forGetter(p -> p.biome))
                  .apply(i, ConfiguredPlacement::new));
  private final String biome;

  public ConfiguredPlacement(
      Vec3i offset,
      FrequencyReductionMethod reduction,
      float frequency,
      int salt,
      Optional<ExclusionZone> exclusion,
      String biome) {
    super(offset, reduction, frequency, salt, exclusion, 4, 1, RandomSpreadType.LINEAR);
    this.biome = biome;
  }

  @Override
  public int spacing() {
    return Settings.biome(biome).spacing().get();
  }

  @Override
  public int separation() {
    return Math.max(0, spacing() - 1);
  }

  @Override
  public ChunkPos getPotentialStructureChunk(long seed, int x, int z) {
    int s = spacing(), cx = Math.floorDiv(x, s), cz = Math.floorDiv(z, s);
    long random = BiomeLayout.hash(seed + salt(), cx, cz);
    int jitter = Math.max(1, s / 4 + 1);
    return new ChunkPos(
        cx * s + (int) Math.floorMod(random, jitter),
        cz * s + (int) Math.floorMod(random >>> 32, jitter));
  }

  @Override
  public StructurePlacementType<?> type() {
    return WorldRegistry.PLACEMENT.get();
  }
}
