package dev.riley.wildlands.entity;

import dev.riley.wildlands.Wildlands;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Wildlands.ID)
@PrefixGameTestTemplate(false)
public final class SurfaceTests {
  @GameTest(template = "empty")
  public static void surfaceRouteReachesWallAndCeiling(GameTestHelper h) {
    for (int x = 1; x <= 9; x++)
      for (int z = 1; z <= 9; z++) {
        h.setBlock(x, 0, z, Blocks.STONE);
        h.setBlock(x, 7, z, Blocks.STONE);
        for (int y = 1; y < 7; y++) h.setBlock(x, y, z, Blocks.AIR);
      }
    for (int y = 1; y <= 7; y++) for (int z = 1; z <= 9; z++) h.setBlock(9, y, z, Blocks.STONE);
    var slug = h.spawn(Creatures.SLUG.get(), 7, 1, 5);
    var graph = new SurfaceNavigator(slug);
    var target = h.absolutePos(new BlockPos(7, 7, 5));
    boolean found = graph.find(target, true);
    h.assertTrue(
        found, "Actual collision-checked route from floor to ceiling: " + graph.lastFailure);
    h.assertTrue(
        graph.route.stream().anyMatch(n -> n.support() == Direction.EAST),
        "Route uses wall support");
    h.assertTrue(
        graph.route.getLast().support() == Direction.UP,
        "Route finishes upside down against ceiling");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 600)
  public static void slugActuallyClimbsAndHangs(GameTestHelper h) {
    for (int x = 1; x <= 9; x++)
      for (int z = 1; z <= 9; z++) {
        h.setBlock(x, 0, z, Blocks.STONE);
        h.setBlock(x, 7, z, Blocks.STONE);
        for (int y = 1; y < 7; y++) h.setBlock(x, y, z, Blocks.AIR);
      }
    for (int y = 1; y <= 7; y++) for (int z = 1; z <= 9; z++) h.setBlock(9, y, z, Blocks.STONE);
    var slug = h.spawn(Creatures.SLUG.get(), 7, 1, 5);
    var target = h.absolutePos(new BlockPos(7, 7, 5));
    h.assertTrue(slug.surface.find(target, true), "Route exists before moving");
    h.succeedWhen(
        () -> {
          h.assertTrue(
              slug.getY() > target.getY() - .5,
              "Slug physically reaches ceiling, not visual-only climbing");
          h.assertTrue(
              slug.face() == Direction.UP && slug.isNoGravity(),
              "Slug hangs upside down with gravity disabled only on support");
          h.assertTrue(
              slug.level().noCollision(slug, slug.getBoundingBox().deflate(.001)),
              "Slug does not pass through solid blocks");
        });
  }
}
