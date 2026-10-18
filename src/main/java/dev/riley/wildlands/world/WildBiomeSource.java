package dev.riley.wildlands.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riley.wildlands.Settings;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

public final class WildBiomeSource extends BiomeSource {
  public static final MapCodec<WildBiomeSource> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(Biome.CODEC.listOf().fieldOf("biomes").forGetter(s -> s.biomes))
                  .apply(i, WildBiomeSource::new));
  private final List<Holder<Biome>> biomes;
  private final Map<String, Holder<Biome>> named = new HashMap<>();
  private long seed;

  public WildBiomeSource(List<Holder<Biome>> biomes) {
    this.biomes = List.copyOf(biomes);
    for (var h : biomes) h.unwrapKey().ifPresent(k -> named.put(k.location().getPath(), h));
    if (!named.keySet().containsAll(BiomeLayout.NAMES))
      throw new IllegalArgumentException(
          "Wildlands biome source requires all thirteen biome holders");
  }

  public void seed(long value) {
    seed = value;
  }

  public long seed() {
    return seed;
  }

  public String nameAt(int blockX, int blockZ) {
    return quartName(Math.floorDiv(blockX, 4), Math.floorDiv(blockZ, 4));
  }

  public String quartName(int x, int z) {
    var names =
        BiomeLayout.NAMES.stream()
            .filter(s -> !BiomeLayout.satellite(s) && Settings.enabled(s))
            .toList();
    int[] weights = names.stream().mapToInt(Settings::weight).toArray();
    return BiomeLayout.select(
        seed,
        x,
        z,
        names,
        weights,
        Settings.enabled("ignimbrite_flats"),
        Settings.enabled("dead_reef"));
  }

  @Override
  protected MapCodec<? extends BiomeSource> codec() {
    return CODEC;
  }

  @Override
  protected Stream<Holder<Biome>> collectPossibleBiomes() {
    return biomes.stream();
  }

  @Override
  public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
    return named.get(quartName(x, z));
  }
}
