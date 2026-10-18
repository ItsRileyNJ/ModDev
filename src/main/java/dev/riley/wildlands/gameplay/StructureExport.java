package dev.riley.wildlands.gameplay;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.riley.wildlands.Wildlands;
import java.nio.file.Files;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Asset creation tool: large structures can be exported without programming or an external API. */
@EventBusSubscriber(modid = Wildlands.ID)
public final class StructureExport {
  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("wildlands")
                .requires(s -> s.hasPermission(2))
                .then(
                    Commands.literal("export")
                        .then(
                            Commands.argument("template", ResourceLocationArgument.id())
                                .then(
                                    Commands.argument("from", BlockPosArgument.blockPos())
                                        .then(
                                            Commands.argument("to", BlockPosArgument.blockPos())
                                                .executes(c -> save(c, false))
                                                .then(
                                                    Commands.literal("replace")
                                                        .executes(c -> save(c, true))))))));
  }

  private static int save(CommandContext<CommandSourceStack> ctx, boolean replace)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var level = source.getLevel();
    var id = ResourceLocationArgument.getId(ctx, "template");
    if (!id.getNamespace().equals(Wildlands.ID) || id.getPath().contains("..")) {
      source.sendFailure(
          Component.literal(
              "Use a safe wildlands: namespace name, such as wildlands:redwood_forest/tree_01."));
      return 0;
    }
    var a = BlockPosArgument.getLoadedBlockPos(ctx, "from");
    var b = BlockPosArgument.getLoadedBlockPos(ctx, "to");
    var min =
        new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ()));
    var max =
        new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ()));
    var size =
        new Vec3i(
            max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1);
    if (size.getX() > 512
        || size.getZ() > 512
        || size.getY() > 256
        || (long) size.getX() * size.getY() * size.getZ() > 4194304) {
      source.sendFailure(
          Component.literal(
              "Export is limited to 512 x 256 x 512 and 4,194,304 selected blocks. Use a smaller"
                  + " box."));
      return 0;
    }
    for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++)
      for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++)
        if (!level.hasChunk(x, z)) {
          source.sendFailure(
              Component.literal(
                  "Every selected chunk must be loaded. Move closer or use vanilla /forceload for"
                      + " the build area, then export again."));
          return 0;
        }
    var manager = level.getStructureManager();
    var path = manager.createAndValidatePathToGeneratedStructure(id, ".nbt");
    if (Files.exists(path) && !replace) {
      source.sendFailure(
          Component.literal(
              "An export already exists. Back it up or append 'replace' to deliberately overwrite"
                  + " it."));
      return 0;
    }
    var template = manager.getOrCreate(id);
    template.fillFromWorld(level, min, size, false, Blocks.AIR);
    if (!manager.save(id)) {
      source.sendFailure(
          Component.literal("Export failed; inspect the server log for the file error."));
      return 0;
    }
    source.sendSuccess(
        () ->
            Component.literal(
                "Saved "
                    + id
                    + " ("
                    + size.getX()
                    + " x "
                    + size.getY()
                    + " x "
                    + size.getZ()
                    + ") to "
                    + path
                    + ". Copy the NBT into your mod's data/wildlands/structure/ folder."),
        false);
    return 1;
  }
}
