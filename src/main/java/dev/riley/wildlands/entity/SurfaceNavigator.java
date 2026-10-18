package dev.riley.wildlands.entity;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;

/** Bounded BFS over supported air cells with real collision checks on each swept edge. */
final class SurfaceNavigator {
  record Node(BlockPos air, Direction support) {}

  private final BananaSlug slug;
  String lastFailure = "";
  final Deque<Node> route = new ArrayDeque<>();

  SurfaceNavigator(BananaSlug slug) {
    this.slug = slug;
  }

  Vec3 position(Node n) {
    double w = slug.getBbWidth() / 2.0, h = slug.getBbHeight();
    Vec3 p = Vec3.atBottomCenterOf(n.air());
    return switch (n.support()) {
      case DOWN -> p;
      case UP -> p.add(0, 1 - h, 0);
      case WEST -> p.add(-.5 + w, .5 - h / 2, 0);
      case EAST -> p.add(.5 - w, .5 - h / 2, 0);
      case NORTH -> p.add(0, .5 - h / 2, -.5 + w);
      case SOUTH -> p.add(0, .5 - h / 2, .5 - w);
    };
  }

  List<Node> nodes(BlockPos air) {
    var l = slug.level();
    if (!l.hasChunkAt(air) || !l.getBlockState(air).getCollisionShape(l, air).isEmpty())
      return List.of();
    var result = new ArrayList<Node>();
    for (Direction d : Direction.values())
      if (l.getBlockState(air.relative(d)).isFaceSturdy(l, air.relative(d), d.getOpposite())) {
        var n = new Node(air.immutable(), d);
        if (clear(position(n))) result.add(n);
      }
    return result;
  }

  private boolean clear(Vec3 p) {
    double w = slug.getBbWidth() / 2.;
    return slug.level()
        .noCollision(
            slug,
            new AABB(
                p.x - w + .001,
                p.y + .001,
                p.z - w + .001,
                p.x + w - .001,
                p.y + slug.getBbHeight() - .001,
                p.z + w - .001));
  }

  private boolean edge(Node a, Node b) {
    Vec3 start = position(a), end = position(b);
    for (int i = 1; i <= 5; i++) if (!clear(start.lerp(end, i / 5.0))) return false;
    return true;
  }

  boolean find(BlockPos target, boolean copper) {
    route.clear();
    var starts = nodes(slug.blockPosition());
    if (starts.isEmpty()) {
      lastFailure =
          "no start nodes at "
              + slug.blockPosition()
              + " below="
              + slug.level().getBlockState(slug.blockPosition().below())
              + " air="
              + slug.level().getBlockState(slug.blockPosition())
              + " dims="
              + slug.getBbWidth()
              + ","
              + slug.getBbHeight()
              + " supported="
              + slug.level()
                  .getBlockState(slug.blockPosition().below())
                  .isFaceSturdy(slug.level(), slug.blockPosition().below(), Direction.UP)
              + " clear="
              + clear(Vec3.atBottomCenterOf(slug.blockPosition()));
      return false;
    }
    var cache = new HashMap<BlockPos, List<Node>>();
    var queue = new ArrayDeque<Node>();
    var previous = new HashMap<Node, Node>();
    for (var s : starts) {
      queue.add(s);
      previous.put(s, null);
    }
    Node found = null;
    int count = 0;
    while (!queue.isEmpty() && count++ < 1600) {
      var n = queue.remove();
      if (copper ? n.air().relative(n.support()).equals(target) : n.air().equals(target)) {
        found = n;
        break;
      }
      for (int x = -1; x <= 1; x++)
        for (int y = -1; y <= 1; y++)
          for (int z = -1; z <= 1; z++) {
            BlockPos q = n.air().offset(x, y, z);
            if (q.distSqr(slug.blockPosition()) > 144) continue;
            for (var b : cache.computeIfAbsent(q.immutable(), this::nodes))
              if (!previous.containsKey(b) && edge(n, b)) {
                previous.put(b, n);
                queue.add(b);
              }
          }
    }
    if (found == null) {
      lastFailure =
          "visited="
              + previous.size()
              + " expanded="
              + count
              + " start="
              + starts
              + " target="
              + target
              + " maxY="
              + previous.keySet().stream().mapToInt(n -> n.air().getY()).max().orElse(0);
      return false;
    }
    while (found != null) {
      route.addFirst(found);
      found = previous.get(found);
    }
    return true;
  }

  void tick() {
    if (route.isEmpty()) return;
    Node n = route.peek();
    if (!nodes(n.air()).contains(n)) {
      route.clear();
      return;
    }
    Vec3 delta = position(n).subtract(slug.position());
    if (delta.length() < .035) {
      route.remove();
      slug.attach(n.support());
      return;
    }
    slug.attach(n.support());
    slug.setDeltaMovement(delta.normalize().scale(Math.min(.045, delta.length())));
  }
}
