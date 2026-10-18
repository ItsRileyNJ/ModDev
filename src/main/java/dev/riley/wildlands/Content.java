package dev.riley.wildlands;

import dev.riley.wildlands.block.*;
import dev.riley.wildlands.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class Content {
  public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wildlands.ID);
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Wildlands.ID);
  public static final DeferredRegister<Fluid> FLUIDS =
      DeferredRegister.create(Registries.FLUID, Wildlands.ID);
  public static final DeferredHolder<Fluid, StillLava> LAVA =
      FLUIDS.register("stationary_lava", StillLava::new);
  public static final DeferredBlock<LiquidBlock> STILL_LAVA =
      BLOCKS.register(
          "stationary_lava",
          () -> new LiquidBlock(LAVA.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)));
  public static final DeferredBlock<Block> IGNIMBRITE = block("ignimbrite", Blocks.TUFF);
  public static final DeferredBlock<Block> POLISHED_IGNIMBRITE =
      block("polished_ignimbrite", Blocks.POLISHED_TUFF);
  public static final DeferredBlock<SlabBlock> IGNIMBRITE_SLAB =
      BLOCKS.register(
          "ignimbrite_slab",
          () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF_SLAB)));
  public static final DeferredBlock<StairBlock> IGNIMBRITE_STAIRS =
      BLOCKS.register(
          "ignimbrite_stairs",
          () ->
              new StairBlock(
                  IGNIMBRITE.get().defaultBlockState(),
                  BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF_STAIRS)));
  public static final DeferredBlock<WallBlock> IGNIMBRITE_WALL =
      BLOCKS.register(
          "ignimbrite_wall",
          () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF_WALL)));
  public static final DeferredBlock<Block> LIMESTONE = block("limestone", Blocks.CALCITE);
  public static final DeferredBlock<RotatedPillarBlock> BURNT_WOOD =
      BLOCKS.register(
          "burnt_wood",
          () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
  public static final DeferredBlock<Block> SPIRE_MATERIAL =
      block("sulphuric_spire", Blocks.YELLOW_TERRACOTTA);
  public static final DeferredBlock<RootFlowerBlock> ROOT_FLOWER =
      BLOCKS.register(
          "root_flower",
          () -> new RootFlowerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH)));
  public static final DeferredBlock<GlowLichenBlock> BACTERIA =
      BLOCKS.register(
          "seafloor_bacteria",
          () ->
              new GlowLichenBlock(
                  BlockBehaviour.Properties.ofFullCopy(Blocks.GLOW_LICHEN).lightLevel(s -> 6)));
  public static final DeferredBlock<Block> PLANT_FOSSIL = block("plant_fossil", Blocks.BONE_BLOCK);
  public static final DeferredBlock<Block> ANIMAL_FOSSIL =
      block("animal_fossil", Blocks.BONE_BLOCK);
  public static final DeferredBlock<Block> FOSSIL_TROPHY =
      block("fossil_trophy", Blocks.DECORATED_POT);
  public static final DeferredItem<Item> EMPTY_JAR = ITEMS.registerSimpleItem("empty_jar");
  public static final DeferredItem<SlimeJarItem> SLIME_JAR =
      ITEMS.registerItem(
          "flytrap_slime_jar", SlimeJarItem::new, new Item.Properties().stacksTo(16));
  public static final DeferredItem<SlugCarrierItem> SLUG_CARRIER =
      ITEMS.registerItem(
          "carried_banana_slug", SlugCarrierItem::new, new Item.Properties().stacksTo(1));
  public static final DeferredItem<Item> ROOT_PATTERN =
      ITEMS.registerItem(
          "root_flower_banner_pattern",
          p ->
              new BannerPatternItem(
                  net.minecraft.tags.TagKey.create(
                      Registries.BANNER_PATTERN, Wildlands.id("pattern_item/root_flower")),
                  p),
          new Item.Properties().stacksTo(1));
  public static final DeferredItem<Item> FOSSIL_PATTERN =
      ITEMS.registerItem(
          "fossil_banner_pattern",
          p ->
              new BannerPatternItem(
                  net.minecraft.tags.TagKey.create(
                      Registries.BANNER_PATTERN, Wildlands.id("pattern_item/fossil")),
                  p),
          new Item.Properties().stacksTo(1));

  private static DeferredBlock<Block> block(String name, Block copy) {
    return BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.ofFullCopy(copy));
  }

  public static void register(IEventBus bus) {
    var drops =
        DeferredRegister
            .<com.mojang.serialization.MapCodec<
                    ? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>>
                create(
                    net.neoforged.neoforge.registries.NeoForgeRegistries.Keys
                        .GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    Wildlands.ID);
    drops.register("configured_drops", () -> dev.riley.wildlands.gameplay.ConfiguredDrops.CODEC);
    drops.register(bus);
    BLOCKS.register(bus);
    FLUIDS.register(bus);
    BLOCKS
        .getEntries()
        .forEach(
            h -> {
              if (h != STILL_LAVA) ITEMS.registerSimpleBlockItem(h.getId().getPath(), h);
            });
    ITEMS.register(bus);
    var tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Wildlands.ID);
    tabs.register(
        "main",
        () ->
            CreativeModeTab.builder()
                .title(Component.literal("Wildlands"))
                .icon(() -> ROOT_FLOWER.get().asItem().getDefaultInstance())
                .displayItems((p, o) -> ITEMS.getEntries().forEach(h -> o.accept(h.get())))
                .build());
    tabs.register(bus);
  }
}
