# Validation and remaining limits

Verified in the cloud workspace on October 19, 2026, using Java 21 and
NeoForge 21.1.262 for Minecraft 1.21.1.

## Results

- `./gradlew --no-daemon --max-workers=4 build runGameTestServer`: **passed**.
- **25 native Minecraft GameTests executed, 25 passed, zero failed or skipped.**
- The Gradle unit-test task reports NO-SOURCE; the native GameTests, not that
  empty unit-test task, supply the runtime verification.
- `tools/check_resources.py` passed: 78 model/blockstate/PNG resources, every
  JSON document, thirteen biome definitions, and the craft-only flower pattern.
  Model/texture links were checked against this exact Minecraft 1.21.1 client JAR.
- Packaged JAR verified: ZIP integrity, expanded mod metadata, Java class major
  version 65 (Java 21), thirteen biome definitions, and required Mixin config.
- No other gameplay API mod is required.

Artifact: `build/libs/wildlands-1.0.1.jar`.

SHA-256: `437b1829e90fe6f846966ffc073c17224ee2b0fe87464f001bddc127aa2389e5`

## Runtime coverage

The tests exercise:

1. Both adjacency relationships across positive/negative regions and multiple seeds.
2. Full-height Depths biome selection and generated water over a -50 to -45 floor.
3. Slug random size bounds, generic scale-attribute resistance, owner restrictions,
   and pickup-state persistence.
4. Rejection of remote waxing and success while physically touching copper.
5. Collision-checked routes up walls and onto ceilings.
6. Actual entity movement onto the ceiling, upside-down support and collision safety.
7. Flytrap phantom prerequisite, taming opportunity, one digestion state, refusal
   to consume during digestion, and saved tame/digestion state.
8. Pet protection, ownership checks, first slime collection and repeated-collection refusal.
9. Stationary custom lava and conversion of player-placed vanilla lava in the
   volcanic biome without flowing streams.
10. Pressure boundary at exactly Y=-36, with no pressure above it.
11. Persistent traps, actual immobilization, clean expiry, release, and original block preservation.
12. Charcoal-only burnt wood, gunpowder-only spire material, unchanged vanilla wood drops.
13. Real root-flower placement and damage when bush physics clears velocity.
14. Real dead-coral placement using vanilla reef arrangements, with no living coral.
15. Increased Root Worm fire damage, neutral targeting and real movement away from fire.
16. Dryness damage despite the slug being tamed.
17. Fixed 18-point damage through netherite, Protection IV, increased armor,
    Resistance V and absorption; healing suppression between the two successful hits.
18. A persistent natural-encounter cooldown that cannot be reset by entity disappearance.
19. Structure export to a real NBT file and reload through the native structure manager.
20. All thirteen custom biomes sampled through the actual `minecraft:normal`
    preset's vanilla multi-noise source, with the majority of region cells
    preserving vanilla climate lookup. Satellite parent adjacency and native
    biome-location lookup are checked on that source.
21. Normal noise-generator biome filling and terrain filling into a real ProtoChunk,
    followed by the actual injected carving hook: deep water, floor materials,
    removal of former terrain, stored chunk biomes, and matching base heights.
    Vanilla-region base columns match the unmodified source at every height.
22. Vanilla generator codec encode/decode retains automatic Overworld integration;
    the Nether and End sources are unchanged, and no realm dimension is declared.

Some tests cover multiple related behaviors; the suite contains 25 test methods.

## What is still needed or unverified

- **Your six custom structure designs are not supplied yet.** The shipped NBTs
  are explicit empty markers and are skipped. Rootlands, Burnt Forest, Redwood
  Forest and Sulphuric Spires do not receive invented replacement roots/trees/spikes.
  Vanilla biomes elsewhere in the normal Overworld still provide ordinary trees
  and wood. Use [ASSETS.md](ASSETS.md) to export/install your custom designs.
- Creature/block/item art is intentionally placeholder art. Replace the listed
  resources for your intended appearance. The rendering code supports native
  cuboid JSON models and flytrap open/digesting poses. Arbitrary skeletal or
  Blockbench keyframe animation import is **not implemented**.
- No graphical Minecraft client or human multiplayer session was run. Final
  appearance, animation quality, fog appearance with shaders, encounter pacing,
  and large user-authored structure placement still require in-game playtesting.
- Worldgen tests decode the normal and legacy presets and exercise native noise,
  biome, terrain and feature implementations. Isolated terrain tests supply an
  empty structure-start list; placement of large user-authored structures still
  requires playtesting. The GameTest server itself uses a flat test world.
- Default Overworld worlds now receive custom biomes automatically in new chunks.
  Existing chunks are unchanged. Other mods that replace the Overworld biome
  source/generator may bypass this integration and have not been tested.
- Satellite biome guarantees operate at Minecraft's four-block quart sampling
  resolution; narrow margins and fuzzy block-level borders are documented in
  [IMPLEMENTATION.md](IMPLEMENTATION.md).
- Vanilla Minecraft 1.21.1 has no Copper Golem. Healing integration is inactive
  until an administrator lists a real compatible installed entity ID. No invented
  vanilla golem or mandatory external API is included.
- The fixed two-hit scenario is for normal 20-health survival players. A mod that
  radically changes maximum health, cancels damage/death, or rewrites health can
  override that outcome; fixed damage cannot guarantee two hits for arbitrary
  health totals. See [ADMIN.md](ADMIN.md) for the exact rules.
- The test launcher logged a non-fatal online authentication metadata lookup
  failure for `api.minecraftservices.com`; all offline native tests completed.
  Online account authentication was not validated.
- Environment draft saving is separate from publication. A published snapshot
  and restoration in a fresh task have not been verified. Review/save the saved
  setup instructions and publish when you want to retain this prepared workspace.

No known failed required checks remain. Remaining art and human playtesting are
listed explicitly above rather than being described as completed gameplay.
