package dev.riley.wildlands.gameplay;

import dev.riley.wildlands.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public final class SlimeTraps extends SavedData {
  record Trap(long until, String block) {}

  record Hold(Vec3 anchor, long until) {}

  private final Map<BlockPos, Trap> traps = new HashMap<>();
  private final Map<UUID, Hold> held = new HashMap<>();

  public static SlimeTraps get(ServerLevel level) {
    return level
        .getDataStorage()
        .computeIfAbsent(new Factory<>(SlimeTraps::new, SlimeTraps::load), "wildlands_slime_traps");
  }

  public boolean place(ServerLevel level, BlockPos pos) {
    if (traps.size() >= 4096 || traps.containsKey(pos)) return false;
    traps.put(
        pos.immutable(),
        new Trap(
            level.getGameTime() + Settings.TRAP_LIFE.get(),
            BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString()));
    setDirty();
    return true;
  }

  public boolean active(BlockPos p, long time) {
    var t = traps.get(p);
    return t != null && t.until > time;
  }

  public void tick(ServerLevel level) {
    if (traps
        .entrySet()
        .removeIf(
            e ->
                e.getValue().until <= level.getGameTime()
                    || (level.hasChunkAt(e.getKey())
                        && !BuiltInRegistries.BLOCK
                            .getKey(level.getBlockState(e.getKey()).getBlock())
                            .toString()
                            .equals(e.getValue().block)))) setDirty();
    if (held.entrySet().removeIf(e -> e.getValue().until <= level.getGameTime())) setDirty();
    if (level.getGameTime() % 10 == 0)
      for (var p : traps.keySet())
        if (level.hasChunkAt(p))
          level.sendParticles(
              net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
              p.getX() + .5,
              p.getY() + 1.05,
              p.getZ() + .5,
              1,
              .25,
              .01,
              .25,
              0);
  }

  public void immobilize(ServerLevel level, LivingEntity entity) {
    if (!(entity instanceof Mob)) return;
    Hold h = held.get(entity.getUUID());
    if (h == null) {
      BlockPos under =
          BlockPos.containing(entity.getX(), entity.getBoundingBox().minY - .05, entity.getZ());
      String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
      if (!active(under, level.getGameTime())
          || (!Settings.TRAP_TYPES.get().isEmpty() && !Settings.TRAP_TYPES.get().contains(type)))
        return;
      h = new Hold(entity.position(), level.getGameTime() + Settings.TRAP_HOLD.get());
      held.put(entity.getUUID(), h);
      setDirty();
      // One activation per application. Removing the trap cannot alter the underlying block.
      traps.remove(under);
      setDirty();
    }
    if (h.until <= level.getGameTime()) {
      held.remove(entity.getUUID());
      setDirty();
      return;
    }
    if (!level.hasChunkAt(BlockPos.containing(h.anchor))) return;
    entity.setPos(h.anchor);
    entity.setDeltaMovement(Vec3.ZERO);
    entity.hasImpulse = true;
    ((Mob) entity).getNavigation().stop();
  }

  private static SlimeTraps load(CompoundTag t, HolderLookup.Provider regs) {
    var data = new SlimeTraps();
    for (Tag tag : t.getList("Traps", Tag.TAG_COMPOUND)) {
      var e = (CompoundTag) tag;
      data.traps.put(
          BlockPos.of(e.getLong("Pos")), new Trap(e.getLong("Until"), e.getString("Block")));
    }
    for (Tag tag : t.getList("Held", Tag.TAG_COMPOUND)) {
      var e = (CompoundTag) tag;
      data.held.put(
          e.getUUID("UUID"),
          new Hold(
              new Vec3(e.getDouble("X"), e.getDouble("Y"), e.getDouble("Z")), e.getLong("Until")));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag list = new ListTag();
    traps.forEach(
        (p, t) -> {
          var e = new CompoundTag();
          e.putLong("Pos", p.asLong());
          e.putLong("Until", t.until);
          e.putString("Block", t.block);
          list.add(e);
        });
    tag.put("Traps", list);
    ListTag holds = new ListTag();
    held.forEach(
        (id, h) -> {
          var e = new CompoundTag();
          e.putUUID("UUID", id);
          e.putLong("Until", h.until);
          e.putDouble("X", h.anchor.x);
          e.putDouble("Y", h.anchor.y);
          e.putDouble("Z", h.anchor.z);
          holds.add(e);
        });
    tag.put("Held", holds);
    return tag;
  }
}
