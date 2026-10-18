package dev.riley.wildlands.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riley.wildlands.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/**
 * Native structure pieces are clipped per chunk: large user designs are not truncated by feature
 * write limits.
 */
public final class CustomTemplateStructure extends Structure {
  public static final MapCodec<CustomTemplateStructure> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      settingsCodec(i),
                      ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
                      com.mojang.serialization.Codec.STRING
                          .fieldOf("biome_config")
                          .forGetter(s -> s.biome),
                      com.mojang.serialization.Codec.intRange(-64, 64)
                          .optionalFieldOf("y_offset", 0)
                          .forGetter(s -> s.yOffset))
                  .apply(i, CustomTemplateStructure::new));
  private final ResourceLocation template;
  private final String biome;
  private final int yOffset;
  private static final Set<ResourceLocation> WARNED =
      java.util.concurrent.ConcurrentHashMap.newKeySet();

  public CustomTemplateStructure(
      StructureSettings settings, ResourceLocation template, String biome, int yOffset) {
    super(settings);
    this.template = template;
    this.biome = biome;
    this.yOffset = yOffset;
  }

  @Override
  protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
    if (!Settings.enabled(biome)) return Optional.empty();
    var cfg = Settings.biome(biome);
    int spacing = cfg.spacing().get();
    if (ctx.random().nextDouble() > cfg.density().get()) return Optional.empty();
    var loaded = ctx.structureTemplateManager().get(template);
    if (loaded.isEmpty()
        || loaded.get().getSize().getX() <= 1
            && loaded.get().getSize().getY() <= 1
            && loaded.get().getSize().getZ() <= 1) {
      if (WARNED.add(template))
        Wildlands.LOG.warn(
            "Custom structure {} has no supplied design yet; skipping it (no substitute"
                + " trees/roots/spikes).",
            template);
      return Optional.empty();
    }
    Rotation rotation = Rotation.getRandom(ctx.random());
    var size = loaded.get().getSize(rotation);
    if (size.getX() > 512 || size.getZ() > 512 || size.getY() > 256) {
      if (WARNED.add(template))
        Wildlands.LOG.error("Structure {} exceeds 512 x 256 x 512 safety limit", template);
      return Optional.empty();
    }
    // A complete template stays inside its biome region; grid spacing must fit its footprint.
    if (Math.max(size.getX(), size.getZ()) > (spacing - spacing / 4) * 16 - 4)
      return Optional.empty();
    int x = ctx.chunkPos().getMinBlockX(), z = ctx.chunkPos().getMinBlockZ();
    if (ctx.chunkGenerator().getBiomeSource() instanceof WildBiomeSource src) {
      for (int dx = 0; dx < size.getX(); dx += 4)
        for (int dz = 0; dz < size.getZ(); dz += 4)
          if (!src.nameAt(x + dx, z + dz).equals(biome)) return Optional.empty();
      if (!src.nameAt(x + size.getX() - 1, z + size.getZ() - 1).equals(biome))
        return Optional.empty();
    }
    int y =
        ctx.chunkGenerator()
            .getFirstFreeHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
    if (y < 63) return Optional.empty();
    y += yOffset;
    if (y < ctx.heightAccessor().getMinBuildHeight()
        || y + size.getY() >= ctx.heightAccessor().getMaxBuildHeight()) return Optional.empty();
    BlockPos pos = new BlockPos(x, y, z);
    return Optional.of(
        new GenerationStub(
            pos,
            b -> {
              Piece piece = new Piece(ctx.structureTemplateManager(), template, pos, rotation);
              var box = piece.getBoundingBox();
              piece.move(x - box.minX(), 0, z - box.minZ());
              b.addPiece(piece);
            }));
  }

  @Override
  public StructureType<?> type() {
    return WorldRegistry.TEMPLATE.get();
  }

  public static final class Piece extends TemplateStructurePiece {
    private static StructurePlaceSettings settings(Rotation rotation) {
      return new StructurePlaceSettings()
          .setRotation(rotation)
          .setIgnoreEntities(true)
          .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
    }

    public Piece(
        StructureTemplateManager manager,
        ResourceLocation template,
        BlockPos pos,
        Rotation rotation) {
      super(
          WorldRegistry.PIECE.get(),
          0,
          manager,
          template,
          template.toString(),
          settings(rotation),
          pos);
    }

    public Piece(StructureTemplateManager manager, CompoundTag tag) {
      super(
          WorldRegistry.PIECE.get(),
          tag,
          manager,
          p ->
              settings(
                  tag.contains("Rot") ? Rotation.valueOf(tag.getString("Rot")) : Rotation.NONE));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
      super.addAdditionalSaveData(ctx, tag);
      tag.putString("Rot", placeSettings.getRotation().name());
    }

    @Override
    protected void handleDataMarker(
        String name,
        BlockPos pos,
        ServerLevelAccessor level,
        RandomSource random,
        BoundingBox box) {}
  }
}
