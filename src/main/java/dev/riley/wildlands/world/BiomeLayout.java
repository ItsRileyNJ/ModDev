package dev.riley.wildlands.world;

import java.util.*;

/** Pure region topology. Coordinates are quart (4 block) coordinates, never height bands. */
public final class BiomeLayout {
  public static final List<String> NAMES =
      List.of(
          "rootlands",
          "white_mountains",
          "crater",
          "volcanic_badlands",
          "burnt_forest",
          "redwood_forest",
          "ignimbrite_flats",
          "verdent",
          "rocky_plains",
          "dead_reef",
          "sulphuric_spires",
          "depths",
          "ocean_trench");

  public static long mix(long x) {
    x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
    x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
    return x ^ (x >>> 31);
  }

  public static long hash(long seed, int x, int z) {
    return mix(seed ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L));
  }

  public static String select(
      long seed, int qx, int qz, List<String> choices, int[] weights, boolean flats, boolean reef) {
    int cx = Math.floorDiv(qx, 128), cz = Math.floorDiv(qz, 128);
    int total = Arrays.stream(weights).sum();
    if (total <= 0) return "plains";
    int pick = (int) Math.floorMod(hash(seed, cx, cz), total), index = 0;
    while (pick >= weights[index]) pick -= weights[index++];
    String base = choices.get(index);
    int x = Math.floorMod(qx, 128), z = Math.floorMod(qz, 128);
    // A one-quart inset border: EVERY satellite quart has a cardinal parent neighbor.
    boolean border =
        ((x == 1 || x == 126) && (z >= 1 && z <= 126))
            || ((z == 1 || z == 126) && (x >= 1 && x <= 126));
    if (border && base.equals("volcanic_badlands") && flats) return "ignimbrite_flats";
    if (border && base.equals("sulphuric_spires") && reef) return "dead_reef";
    return base;
  }

  public static boolean satellite(String s) {
    return s.equals("dead_reef") || s.equals("ignimbrite_flats");
  }
}
