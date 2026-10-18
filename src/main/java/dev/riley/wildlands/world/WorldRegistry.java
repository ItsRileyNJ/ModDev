package dev.riley.wildlands.world;

import com.mojang.serialization.MapCodec;
import dev.riley.wildlands.Wildlands;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class WorldRegistry {
  static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
      DeferredRegister.create(Registries.BIOME_SOURCE, Wildlands.ID);
  static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
      DeferredRegister.create(Registries.CHUNK_GENERATOR, Wildlands.ID);
  static final DeferredRegister<
          MapCodec<? extends net.neoforged.neoforge.common.world.BiomeModifier>>
      MODIFIERS =
          DeferredRegister.create(
              net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
              Wildlands.ID);
  static final DeferredRegister<
          net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>>
      PLACEMENTS = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Wildlands.ID);
  public static final DeferredHolder<
          net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
          net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<
              ConfiguredPlacement>>
      PLACEMENT = PLACEMENTS.register("configured_spread", () -> () -> ConfiguredPlacement.CODEC);
  static final DeferredRegister<Feature<?>> FEATURES =
      DeferredRegister.create(Registries.FEATURE, Wildlands.ID);
  static final DeferredRegister<StructureType<?>> STRUCTURES =
      DeferredRegister.create(Registries.STRUCTURE_TYPE, Wildlands.ID);
  static final DeferredRegister<StructurePieceType> PIECES =
      DeferredRegister.create(Registries.STRUCTURE_PIECE, Wildlands.ID);
  public static final DeferredHolder<StructureType<?>, StructureType<CustomTemplateStructure>>
      TEMPLATE = STRUCTURES.register("custom_template", () -> () -> CustomTemplateStructure.CODEC);
  public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE =
      PIECES.register(
          "custom_template",
          () ->
              (ctx, nbt) -> new CustomTemplateStructure.Piece(ctx.structureTemplateManager(), nbt));

  static {
    MODIFIERS.register("spawns", () -> SpawnModifier.CODEC);
    BIOME_SOURCES.register("regions", () -> WildBiomeSource.CODEC);
    GENERATORS.register("terrain", () -> WildChunkGenerator.CODEC);
    FEATURES.register("biome_details", BiomeDetailsFeature::new);
  }

  public static void register(IEventBus bus) {
    PLACEMENTS.register(bus);
    MODIFIERS.register(bus);
    BIOME_SOURCES.register(bus);
    GENERATORS.register(bus);
    FEATURES.register(bus);
    STRUCTURES.register(bus);
    PIECES.register(bus);
  }
}
