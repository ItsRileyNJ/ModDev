package dev.riley.wildlands.testing;

import dev.riley.wildlands.*;
import dev.riley.wildlands.entity.*;
import dev.riley.wildlands.gameplay.SlimeTraps;
import dev.riley.wildlands.world.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Wildlands.ID)
@PrefixGameTestTemplate(false)
public final class WildlandsTests {
  @GameTest(template = "empty")
  public static void topologyEnforcesAdjacency(GameTestHelper h) {
    var choices = BiomeLayout.NAMES.stream().filter(s -> !BiomeLayout.satellite(s)).toList();
    int[] weights = choices.stream().mapToInt(s -> 1).toArray();
    int flats = 0, reef = 0;
    for (long seed : new long[] {0, 1234, -127})
      for (int cell = -8; cell <= 8; cell++)
        for (int z = -8; z <= 8; z++)
          for (int edge : new int[] {1, 126}) {
            int x = cell * 128 + edge, qz = z * 128 + 64;
            String name = BiomeLayout.select(seed, x, qz, choices, weights, true, true);
            if (BiomeLayout.satellite(name)) {
              String parent = name.equals("dead_reef") ? "sulphuric_spires" : "volcanic_badlands";
              boolean neighbor = false;
              for (var d : Direction.Plane.HORIZONTAL)
                neighbor |=
                    BiomeLayout.select(
                            seed, x + d.getStepX(), qz + d.getStepZ(), choices, weights, true, true)
                        .equals(parent);
              h.assertTrue(neighbor, "Satellite without cardinal parent: " + name);
              if (name.equals("dead_reef")) reef++;
              else flats++;
            }
          }
    h.assertTrue(flats > 0 && reef > 0, "Both relationships must actually be exercised");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void biomeColumnAndDepthFloor(GameTestHelper h) {
    var preset =
        h.getLevel()
            .registryAccess()
            .registryOrThrow(Registries.WORLD_PRESET)
            .get(
                net.minecraft.resources.ResourceKey.create(
                    Registries.WORLD_PRESET, Wildlands.id("wildlands")));
    h.assertTrue(preset != null, "Custom world preset decoded and registered");
    var gen = (WildChunkGenerator) preset.overworld().orElseThrow().generator();
    var source = (WildBiomeSource) gen.getBiomeSource();
    boolean found = false;
    for (int x = -10; x <= 10 && !found; x++)
      for (int z = -10; z <= 10 && !found; z++)
        if (source.nameAt(x * 512 + 256, z * 512 + 256).equals("depths")) {
          int bx = x * 512 + 256, bz = z * 512 + 256;
          for (int y = -50; y <= 60; y += 10)
            h.assertTrue(
                source.getNoiseBiome(bx / 4, y / 4, bz / 4, null).is(Wildlands.id("depths")),
                "Depths must occupy full water column");
          h.assertTrue(
              gen.height(bx, bz) >= -50 && gen.height(bx, bz) <= -45, "Seafloor around -45 to -50");
          var chunk =
              new net.minecraft.world.level.chunk.ProtoChunk(
                  new net.minecraft.world.level.ChunkPos(
                      Math.floorDiv(bx, 16), Math.floorDiv(bz, 16)),
                  net.minecraft.world.level.chunk.UpgradeData.EMPTY,
                  h.getLevel(),
                  h.getLevel().registryAccess().registryOrThrow(Registries.BIOME),
                  null);
          gen.fillFromNoise(null, null, null, chunk).join();
          h.assertTrue(
              chunk.getBlockState(new BlockPos(bx, 0, bz)).is(Blocks.WATER),
              "Actual generated deep water column");
          found = true;
        }
    h.assertTrue(found, "Depths located in sample");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void slugOwnershipAndPersistence(GameTestHelper h) {
    BananaSlug slug = h.spawn(Creatures.SLUG.get(), 3, 1, 3);
    Player owner = h.makeMockPlayer(GameType.SURVIVAL);
    slug.tame(owner);
    float size = slug.size();
    slug.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(16);
    slug.refreshDimensions();
    h.assertTrue(size >= .05 && size <= 1 && slug.getBbWidth() <= 1, "Spawned size bound");
    h.assertTrue(
        slug.pickupFor(h.makeMockPlayer(GameType.SURVIVAL)).isEmpty(),
        "Another player cannot pick up slug");
    var item = slug.pickupFor(owner);
    h.assertTrue(!item.isEmpty(), "Owner can pick up slug");
    BananaSlug restored = Creatures.SLUG.get().create(h.getLevel());
    restored.readAdditionalSaveData(item.get(DataComponents.CUSTOM_DATA).copyTag());
    h.assertTrue(
        restored.size() == size
            && restored.isTame()
            && owner.getUUID().equals(restored.getOwnerUUID()),
        "Size/taming/UUID survive pickup data round trip");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void copperRequiresPhysicalContact(GameTestHelper h) {
    h.setBlock(3, 1, 3, Blocks.COPPER_BLOCK);
    BananaSlug slug = h.spawn(Creatures.SLUG.get(), 8, 1, 8);
    var copper = h.absolutePos(new BlockPos(3, 1, 3));
    h.assertTrue(!slug.waxAtContact(copper), "No remote waxing");
    slug.moveTo(copper.getX() + .5, copper.getY() + 1, copper.getZ() + .5, 0, 0);
    h.assertTrue(slug.waxAtContact(copper), "Wax on physical copper contact");
    h.assertBlockPresent(Blocks.WAXED_COPPER_BLOCK, new BlockPos(3, 1, 3));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void flytrapPrerequisiteAndDigestion(GameTestHelper h) {
    Flytrap f = h.spawn(Creatures.FLYTRAP.get(), 4, 1, 4);
    Player p = h.makeMockPlayer(GameType.SURVIVAL);
    p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PHANTOM_MEMBRANE));
    h.assertTrue(
        f.mobInteract(p, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && !f.isTame(),
        "Membrane alone must not tame");
    var phantom = h.spawn(EntityType.PHANTOM, 4, 2, 4);
    phantom.setNoAi(true);
    phantom.setHealth(1);
    h.assertTrue(
        f.consume(phantom) && f.tamingOpportunity() && f.digesting(),
        "Phantom consumption opens taming and digestion");
    var bee = h.spawn(EntityType.BEE, 4, 2, 4);
    h.assertTrue(!f.consume(bee), "No eating during digestion");
    f.mobInteract(p, InteractionHand.MAIN_HAND);
    h.assertTrue(f.isTame(), "Membrane tames after eating phantom");
    var saved = new CompoundTag();
    f.addAdditionalSaveData(saved);
    var restored = Creatures.FLYTRAP.get().create(h.getLevel());
    restored.readAdditionalSaveData(saved);
    h.assertTrue(restored.isTame() && restored.digesting(), "Taming/digestion survive reload");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void stationaryLavaDoesNotFlow(GameTestHelper h) {
    h.setBlock(4, 3, 4, Content.STILL_LAVA.get());
    h.runAfterDelay(
        100,
        () -> {
          h.assertBlockPresent(Content.STILL_LAVA.get(), 4, 3, 4);
          h.assertBlockNotPresent(Blocks.LAVA, 4, 2, 4);
          h.assertBlockNotPresent(Blocks.LAVA, 5, 3, 4);
          h.succeed();
        });
  }

  @GameTest(template = "empty")
  public static void trapPersistsWithoutChangingBlock(GameTestHelper h) {
    h.setBlock(4, 0, 4, Blocks.STONE);
    var l = h.getLevel();
    var p = h.absolutePos(new BlockPos(4, 0, 4));
    var data = SlimeTraps.get(l);
    h.assertTrue(data.place(l, p), "Trap placement");
    var saved = new CompoundTag();
    data.save(saved, l.registryAccess());
    h.assertTrue(saved.getList("Traps", 10).size() > 0, "Trap persists");
    var zombie = h.spawn(EntityType.ZOMBIE, 4, 1, 4);
    zombie.setNoAi(true);
    data.immobilize(l, zombie);
    Vec3 anchor = zombie.position();
    zombie.setPos(anchor.add(2, 0, 0));
    data.immobilize(l, zombie);
    h.assertTrue(zombie.position().distanceToSqr(anchor) < .0001, "Server immobilizes victim");
    h.assertBlockPresent(Blocks.STONE, 4, 0, 4);
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void abyssalDamageIgnoresArmorAndAbsorption(GameTestHelper h) {
    Player p = h.makeMockPlayer(GameType.SURVIVAL);
    p.moveTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(5, 1, 5))));
    p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
    p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
    p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
    p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
    p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(4000);
    p.addEffect(
        new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 600, 4));
    for (EquipmentSlot slot :
        java.util.List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET))
      p.getItemBySlot(slot)
          .enchant(
              h.getLevel()
                  .registryAccess()
                  .registryOrThrow(Registries.ENCHANTMENT)
                  .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION),
              4);
    p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION)
        .setBaseValue(20);
    p.setAbsorptionAmount(20);
    p.setHealth(20);
    var stalker = h.spawn(Creatures.STALKER.get(), 5, 1, 5);
    h.assertTrue(stalker.strike(p), "Successful first hit");
    h.assertTrue(p.getHealth() == 2, "Fixed 18 health loss despite armor and absorption");
    p.heal(18);
    h.assertTrue(
        p.getHealth() == 2, "Healing cannot reset the two-hit profile during the active hunt");
    p.invulnerableTime = 0;
    h.assertTrue(stalker.strike(p), "Successful second hit");
    h.assertTrue(p.isDeadOrDying(), "Second successful hit kills");
    h.succeed();
  }

  private static void biome(GameTestHelper h, String name) {
    var a = h.absolutePos(new BlockPos(0, 0, 0));
    var b = a.offset(15, 64, 15);
    String command =
        "fillbiome "
            + a.getX()
            + " -64 "
            + a.getZ()
            + " "
            + b.getX()
            + " 0 "
            + b.getZ()
            + " wildlands:"
            + name;
    h.getLevel()
        .getServer()
        .getCommands()
        .performPrefixedCommand(
            h.getLevel()
                .getServer()
                .createCommandSourceStack()
                .withLevel(h.getLevel())
                .withSuppressedOutput(),
            command);
  }

  @GameTest(template = "empty", timeoutTicks = 150)
  public static void playerLavaCannotFlowInVolcanicBiome(GameTestHelper h) {
    biome(h, "volcanic_badlands");
    h.setBlock(5, 3, 5, Blocks.LAVA);
    h.runAfterDelay(
        100,
        () -> {
          h.assertBlockPresent(Content.STILL_LAVA.get(), 5, 3, 5);
          h.assertBlockNotPresent(Blocks.LAVA, 5, 2, 5);
          h.succeed();
        });
  }

  @GameTest(template = "empty")
  public static void pressureStartsAtFixedDepth(GameTestHelper h) {
    biome(h, "depths");
    Player p = h.makeMockPlayer(GameType.SURVIVAL);
    var pos = h.absolutePos(new BlockPos(6, 1, 6));
    p.tickCount = 20;
    p.moveTo(pos.getX() + .5, -35, pos.getZ() + .5);
    dev.riley.wildlands.gameplay.WorldEvents.entity(
        new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(p));
    h.assertTrue(
        !p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "No pressure above -36");
    p.setPos(p.getX(), -36, p.getZ());
    dev.riley.wildlands.gameplay.WorldEvents.entity(
        new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(p));
    h.assertTrue(
        p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
            && p.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN),
        "Both pressure effects at -36");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void trapsAndHoldsExpire(GameTestHelper h) {
    h.setBlock(4, 0, 4, Blocks.STONE);
    var l = h.getLevel();
    var data = SlimeTraps.get(l);
    var p = h.absolutePos(new BlockPos(4, 0, 4));
    int previousLife = Settings.TRAP_LIFE.get(), previousHold = Settings.TRAP_HOLD.get();
    Settings.TRAP_LIFE.set(20);
    Settings.TRAP_HOLD.set(5);
    var zombie = h.spawn(EntityType.ZOMBIE, 4, 1, 4);
    zombie.setNoAi(true);
    try {
      data.place(l, p);
      data.immobilize(l, zombie);
      data.place(l, p.offset(1, 0, 0));
    } finally {
      Settings.TRAP_LIFE.set(previousLife);
      Settings.TRAP_HOLD.set(previousHold);
    }
    h.runAfterDelay(
        30,
        () -> {
          h.assertTrue(!data.active(p.offset(1, 0, 0), l.getGameTime()), "Unused trap expires");
          Vec3 free = zombie.position().add(2, 0, 0);
          zombie.setPos(free);
          data.immobilize(l, zombie);
          h.assertTrue(
              zombie.position().distanceToSqr(free) < .0001,
              "Expired immobilization does not pull victim back");
          h.succeed();
        });
  }

  @GameTest(template = "empty")
  public static void dedicatedMaterialLootOnly(GameTestHelper h) {
    var pos = h.absolutePos(new BlockPos(4, 1, 4));
    var burnt =
        Block.getDrops(Content.BURNT_WOOD.get().defaultBlockState(), h.getLevel(), pos, null);
    h.assertTrue(
        !burnt.isEmpty() && burnt.stream().allMatch(i -> i.is(Items.CHARCOAL)),
        "Burnt wood gives charcoal, no wood");
    var spire =
        Block.getDrops(Content.SPIRE_MATERIAL.get().defaultBlockState(), h.getLevel(), pos, null);
    h.assertTrue(
        spire.stream().allMatch(i -> i.is(Items.GUNPOWDER)), "Spire material drops only gunpowder");
    var oak = Block.getDrops(Blocks.OAK_LOG.defaultBlockState(), h.getLevel(), pos, null);
    h.assertTrue(
        oak.stream().noneMatch(i -> i.is(Items.CHARCOAL)), "Unrelated vanilla wood unchanged");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void rootFlowerActuallyGenerates(GameTestHelper h) {
    var l = h.getLevel();
    int bx = 0, bz = 0, y = 80;
    for (int x = bx; x < bx + 16; x++)
      for (int z = bz; z < bz + 16; z++) {
        l.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 3);
        for (int dy = 1; dy < 8; dy++)
          l.setBlock(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState(), 3);
      }
    l.getServer()
        .getCommands()
        .performPrefixedCommand(
            l.getServer().createCommandSourceStack().withLevel(l).withSuppressedOutput(),
            "fillbiome 0 64 0 15 96 15 wildlands:rootlands");
    int prior = Settings.biome("rootlands").attempts().get();
    double chance = Settings.FLOWER_CHANCE.get();
    Settings.biome("rootlands").attempts().set(64);
    Settings.FLOWER_CHANCE.set(1.0);
    try {
      new BiomeDetailsFeature()
          .place(
              net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration
                  .INSTANCE,
              l,
              l.getChunkSource().getGenerator(),
              net.minecraft.util.RandomSource.create(12),
              new BlockPos(bx, y, bz));
    } finally {
      Settings.biome("rootlands").attempts().set(prior);
      Settings.FLOWER_CHANCE.set(chance);
    }
    long flowers =
        BlockPos.betweenClosedStream(new BlockPos(0, 80, 0), new BlockPos(15, 83, 15))
            .filter(p -> l.getBlockState(p).is(Content.ROOT_FLOWER.get()))
            .count();
    h.assertTrue(flowers > 0, "Real feature places root flowers over dirt patches");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void deadReefFeatureNeverPlacesLivingCoral(GameTestHelper h) {
    var l = h.getLevel();
    int bx = 64, bz = 0, y = 80;
    for (int x = bx; x < bx + 16; x++)
      for (int z = bz; z < bz + 16; z++) {
        l.setBlock(new BlockPos(x, y, z), Blocks.GRAVEL.defaultBlockState(), 3);
        for (int dy = 1; dy < 9; dy++)
          l.setBlock(new BlockPos(x, y + dy, z), Blocks.WATER.defaultBlockState(), 3);
      }
    l.getServer()
        .getCommands()
        .performPrefixedCommand(
            l.getServer().createCommandSourceStack().withLevel(l).withSuppressedOutput(),
            "fillbiome 64 64 0 79 96 15 wildlands:dead_reef");
    int prior = Settings.biome("dead_reef").attempts().get();
    Settings.biome("dead_reef").attempts().set(64);
    try {
      new BiomeDetailsFeature()
          .place(
              net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration
                  .INSTANCE,
              l,
              l.getChunkSource().getGenerator(),
              net.minecraft.util.RandomSource.create(23),
              new BlockPos(bx, y, bz));
    } finally {
      Settings.biome("dead_reef").attempts().set(prior);
    }
    int dead = 0;
    for (var p : BlockPos.betweenClosed(new BlockPos(60, 80, -4), new BlockPos(84, 92, 20))) {
      String id =
          net.minecraft.core.registries.BuiltInRegistries.BLOCK
              .getKey(l.getBlockState(p).getBlock())
              .getPath();
      if (id.contains("coral")) {
        h.assertTrue(id.startsWith("dead_"), "No living coral allowed");
        dead++;
      }
    }
    h.assertTrue(dead > 0, "Vanilla reef arrangement actually places dead coral");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void rootWormIsNeutralAndFleesFire(GameTestHelper h) {
    for (int x = 1; x <= 12; x++)
      for (int z = 1; z <= 12; z++) {
        h.setBlock(x, 0, z, Blocks.STONE);
        for (int y = 1; y < 5; y++) h.setBlock(x, y, z, Blocks.AIR);
      }
    h.setBlock(4, 1, 5, Blocks.FIRE);
    var worm = h.spawn(Creatures.WORM.get(), 5, 1, 5);
    var fire = Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 1, 5)));
    float before = worm.getHealth();
    worm.hurt(h.getLevel().damageSources().inFire(), 2);
    h.assertTrue(
        worm.getHealth() == before - 2 * Settings.WORM_FIRE.get().floatValue(),
        "Configured increased fire damage");
    double original = worm.position().distanceToSqr(fire);
    h.runAfterDelay(
        70,
        () -> {
          h.assertTrue(worm.getTarget() == null, "No proactive player aggression");
          h.assertTrue(
              worm.position().distanceToSqr(fire) > original + 4,
              "Actively moves away from nearby fire: "
                  + " position="
                  + worm.position()
                  + " ticks="
                  + worm.tickCount
                  + " fire="
                  + h.getBlockState(new BlockPos(4, 1, 5)));
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void tamedSlugStillSuffersDryness(GameTestHelper h) {
    biome(h, "volcanic_badlands");
    var slug = h.spawn(Creatures.SLUG.get(), 6, 1, 6);
    slug.tame(h.makeMockPlayer(GameType.SURVIVAL));
    var t = new CompoundTag();
    slug.addAdditionalSaveData(t);
    t.putInt("Dryness", Settings.DRY_GRACE.get());
    slug.readAdditionalSaveData(t);
    float health = slug.getHealth();
    h.runAfterDelay(
        50,
        () -> {
          h.assertTrue(slug.getHealth() < health, "Taming does not remove environmental weakness");
          h.succeed();
        });
  }

  @GameTest(template = "empty")
  public static void flytrapPetSafetyAndCollectionCooldown(GameTestHelper h) {
    var f = h.spawn(Creatures.FLYTRAP.get(), 5, 1, 5);
    Player owner = h.makeMockPlayer(GameType.SURVIVAL);
    f.tame(owner);
    var pet = h.spawn(EntityType.WOLF, 5, 1, 5);
    pet.tame(owner);
    h.assertTrue(!f.allowedPrey(pet), "Pets protected by default");
    owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.EMPTY_JAR.get(), 2));
    h.assertTrue(
        f.mobInteract(owner, InteractionHand.MAIN_HAND).consumesAction(),
        "Owner receives first slime jar");
    h.assertTrue(
        f.mobInteract(owner, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
        "Repeated collection blocked by cooldown");
    var stranger = h.makeMockPlayer(GameType.SURVIVAL);
    stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.EMPTY_JAR.get()));
    h.assertTrue(
        f.mobInteract(stranger, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
        "Another player cannot harvest flytrap");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void rareEncounterCooldownPersists(GameTestHelper h) {
    var clock = dev.riley.wildlands.gameplay.EncounterClock.get(h.getLevel());
    long now = h.getLevel().getGameTime();
    clock.reserve(now);
    h.assertTrue(
        !clock.ready(now + 100), "Despawning cannot immediately repeat the natural encounter");
    var tag = new CompoundTag();
    clock.save(tag, h.getLevel().registryAccess());
    h.assertTrue(
        tag.getLong("NextEncounter") == now + Settings.ENCOUNTER_INTERVAL.get(),
        "Exact next encounter time saved");
    h.assertTrue(
        clock.ready(now + Settings.ENCOUNTER_INTERVAL.get()),
        "Encounter cooldown eventually expires");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void nativeStructureExportRoundTrip(GameTestHelper h) throws Exception {
    h.setBlock(3, 1, 3, Content.BURNT_WOOD.get());
    var a = h.absolutePos(new BlockPos(3, 1, 3));
    var b = a.offset(1, 1, 1);
    var id = Wildlands.id("test/export_" + UUID.randomUUID());
    var path =
        h.getLevel().getStructureManager().createAndValidatePathToGeneratedStructure(id, ".nbt");
    String command =
        "wildlands export "
            + id
            + " "
            + a.getX()
            + " "
            + a.getY()
            + " "
            + a.getZ()
            + " "
            + b.getX()
            + " "
            + b.getY()
            + " "
            + b.getZ();
    try {
      int result =
          h.getLevel()
              .getServer()
              .getCommands()
              .getDispatcher()
              .execute(
                  command,
                  h.getLevel()
                      .getServer()
                      .createCommandSourceStack()
                      .withLevel(h.getLevel())
                      .withSuppressedOutput());
      h.assertTrue(
          result == 1 && java.nio.file.Files.isRegularFile(path),
          "Native export writes actual NBT file");
      h.getLevel().getStructureManager().remove(id);
      var loaded = h.getLevel().getStructureManager().get(id);
      h.assertTrue(
          loaded.isPresent() && loaded.get().getSize().equals(new Vec3i(2, 2, 2)),
          "Export reloads through native structure manager");
      h.succeed();
    } finally {
      java.nio.file.Files.deleteIfExists(path);
      h.getLevel().getStructureManager().remove(id);
    }
  }

  @GameTest(template = "empty")
  public static void rootFlowerHurtsMovingPlayerWithClearedVelocity(GameTestHelper h) {
    h.setBlock(3, 0, 3, Blocks.DIRT);
    h.setBlock(3, 1, 3, Content.ROOT_FLOWER.get());
    Player player = h.makeMockPlayer(GameType.SURVIVAL);
    var pos = h.absolutePos(new BlockPos(3, 1, 3));
    player.moveTo(Vec3.atBottomCenterOf(pos));
    player.xOld = player.getX() - .1;
    player.zOld = player.getZ();
    player.setDeltaMovement(Vec3.ZERO);
    player.tickCount = 10;
    float before = player.getHealth();
    Content.ROOT_FLOWER.get().defaultBlockState().entityInside(h.getLevel(), pos, player);
    h.assertTrue(
        player.getHealth() == before - Settings.FLOWER_DAMAGE.get().floatValue(),
        "Bush uses actual movement, even when stuck-block physics cleared velocity");
    h.succeed();
  }
}
