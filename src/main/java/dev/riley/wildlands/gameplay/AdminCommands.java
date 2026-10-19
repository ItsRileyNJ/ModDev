package dev.riley.wildlands.gameplay;

import dev.riley.wildlands.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Wildlands.ID)
public final class AdminCommands {
  @SubscribeEvent
  public static void register(RegisterCommandsEvent e) {
    e.getDispatcher()
        .register(
            Commands.literal("wildlands")
                .requires(s -> s.hasPermission(2))
                .then(
                    Commands.literal("status")
                        .executes(
                            c -> {
                              c.getSource()
                                  .sendSuccess(
                                      () ->
                                          Component.literal(
                                              "Wildlands: Minecraft 1.21.1 / NeoForge 21.1.262. 13"
                                                  + " biomes. Fixed: Depths fog 5 blocks, pressure"
                                                  + " at Y <= -36; stalker hit 18 health points."
                                                  + " Normal Overworld generation. Config:"
                                                  + " config/wildlands-common.toml. Worldgen"
                                                  + " changes affect new chunks; use a new world"
                                                  + " for topology changes."),
                                      false);
                              return 1;
                            }))
                .then(
                    Commands.literal("structures")
                        .executes(
                            c -> {
                              int found = 0;
                              for (String id :
                                  java.util.List.of(
                                      "rootlands/root_01",
                                      "rootlands/thorn_01",
                                      "burnt_forest/tree_01",
                                      "burnt_forest/bush_01",
                                      "redwood_forest/tree_01",
                                      "sulphuric_spires/spire_01")) {
                                var t =
                                    c.getSource()
                                        .getLevel()
                                        .getStructureManager()
                                        .get(Wildlands.id(id));
                                boolean ready =
                                    t.isPresent()
                                        && (t.get().getSize().getX() > 1
                                            || t.get().getSize().getY() > 1
                                            || t.get().getSize().getZ() > 1);
                                c.getSource()
                                    .sendSuccess(
                                        () ->
                                            Component.literal(
                                                id
                                                    + ": "
                                                    + (ready
                                                        ? "loaded " + t.get().getSize()
                                                        : "awaiting your NBT design")),
                                        false);
                                if (ready) found++;
                              }
                              return found;
                            })));
  }
}
