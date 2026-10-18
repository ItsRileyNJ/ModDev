package dev.riley.wildlands.gameplay;

import dev.riley.wildlands.Settings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Global per-dimension natural encounter spacing survives both entity despawn and server restart.
 */
public final class EncounterClock extends SavedData {
  private long nextEncounter;

  public static EncounterClock get(ServerLevel level) {
    return level
        .getDataStorage()
        .computeIfAbsent(
            new Factory<>(EncounterClock::new, EncounterClock::load), "wildlands_encounters");
  }

  public boolean ready(long time) {
    return time >= nextEncounter;
  }

  public void reserve(long time) {
    nextEncounter = time + Settings.ENCOUNTER_INTERVAL.get();
    setDirty();
  }

  private static EncounterClock load(CompoundTag tag, HolderLookup.Provider registries) {
    var c = new EncounterClock();
    c.nextEncounter = tag.getLong("NextEncounter");
    return c;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    tag.putLong("NextEncounter", nextEncounter);
    return tag;
  }
}
