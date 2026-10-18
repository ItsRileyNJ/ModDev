package dev.riley.wildlands;

import com.mojang.logging.LogUtils;
import dev.riley.wildlands.entity.Creatures;
import dev.riley.wildlands.world.WorldRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Wildlands.ID)
public final class Wildlands {
  public static final String ID = "wildlands";
  public static final org.slf4j.Logger LOG = LogUtils.getLogger();

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(ID, path);
  }

  public Wildlands(IEventBus bus, ModContainer container) {
    Content.register(bus);
    Creatures.register(bus);
    WorldRegistry.register(bus);
    container.registerConfig(ModConfig.Type.COMMON, Settings.SPEC);
  }
}
