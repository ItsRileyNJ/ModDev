package dev.riley.wildlands.world;

import com.mojang.serialization.MapCodec;
import dev.riley.wildlands.*;
import dev.riley.wildlands.entity.Creatures;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.biome.*;
import net.neoforged.neoforge.common.world.*;

public record SpawnModifier() implements BiomeModifier {
  public static final MapCodec<SpawnModifier> CODEC = MapCodec.unit(new SpawnModifier());

  @Override
  public MapCodec<? extends BiomeModifier> codec() {
    return CODEC;
  }

  @Override
  public void modify(
      Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
    if (phase != Phase.ADD) return;
    String path =
        biome
            .unwrapKey()
            .filter(k -> k.location().getNamespace().equals(Wildlands.ID))
            .map(k -> k.location().getPath())
            .orElse("");
    if (!Settings.BIOMES.containsKey(path) || !Settings.enabled(path)) return;
    EntityType<?> type = null;
    int weight = 0;
    switch (path) {
      case "rootlands" -> {
        type = Creatures.WORM.get();
        weight = Settings.WORM_WEIGHT.get();
      }
      case "redwood_forest" -> {
        type = Creatures.SLUG.get();
        weight = Settings.SLUG_WEIGHT.get();
      }
      case "verdent" -> {
        type = Creatures.FLYTRAP.get();
        weight = Settings.FLYTRAP_WEIGHT.get();
      }
      case "depths" -> {
        type = Creatures.STALKER.get();
        weight = Settings.DEPTHS_WEIGHT.get();
      }
      case "ocean_trench" -> {
        type = EntityType.DROWNED;
        weight = Settings.DROWNED_WEIGHT.get();
      }
    }
    if (type != null && weight > 0)
      builder
          .getMobSpawnSettings()
          .addSpawn(
              type.getCategory(),
              new MobSpawnSettings.SpawnerData(type, weight, 1, path.equals("depths") ? 1 : 3));
  }
}
