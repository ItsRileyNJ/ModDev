# Implementation map

Working identity: Wildlands (`wildlands`). Java 21, Minecraft 1.21.1,
NeoForge 21.1.262. No external gameplay API or runtime dependencies.

- World: thirteen data-driven biome registrations; a native custom biome source
  and chunk generator in a selectable Wildlands world preset and optional realm.
  Terrain and adjacency share one deterministic, world-seeded region sampler.
  Independent-biome overlays cannot enforce strict adjacency; this generator can.
- Content: native deferred registries, blocks, items, loot, recipes, banner
  patterns, tags, placeholder resource-pack models and structure templates.
- Creatures: server AI/state machines; synchronized presentation; saved ownership,
  size and cooldown state; no third-party animation API.
- Administration: native config specs, permission level 2 commands, server
  validation, no command/config for the fixed design rules.
- Validation: staged compilation and native headless GameTests, then artifact
  inspection. In-game visual quality requires the owner's assets and playtesting.

Compatibility findings from the installed 1.21.1 sources: frogspawn has an item
(`minecraft:frogspawn`, obtainable with silk touch); copper waxing is mapped by
HoneycombItem.WAXABLES. There is no vanilla Copper Golem and no empty jar item.
The mod supplies jars. Copper Golem healing must be opt-in by an explicit entity
ID provided by a compatible installed mod, never by inventing a vanilla entity.

## Implementation details and deliberate boundaries

The thirteen biomes are sampled over world-seeded 512-block regions, with
weighted independent biome selection and height-based terrain appropriate to each
biome. This is a native custom generator/preset, not an external terrain API.
Regions are geometric and do not reproduce vanilla climate blending; terrain
heights blend at ordinary region edges. The Depths is a full-height biome with
a deep water column, not a five-block vertical biome layer. Its seafloor is
jagged at approximately -50 through -45 away from shore transitions.

Dead Reef and Ignimbrite Flats are one-quart (four-block) inset borders inside
their parent regions. Every satellite quart sample has a cardinal parent sample.
They cannot be chosen independently. Minecraft's fuzzy block-level biome lookup
can visually jitter the boundary around those samples; the guarantee is at the
native quart sampling resolution, not a promise of identical pixel-level fog or
biome boundaries on every block. The narrow borders are a deliberate cost of the
strict immediate-adjacency rule. Reef shapes are clipped to the reef biome.

Structure placement uses native persistent template pieces rather than writing
large templates as ordinary features, so cross-chunk objects are not silently
truncated. Empty/user-missing templates, oversized footprints, and incompatible
biome boundaries skip placement. Configured spacing and shared variant sets
control overlap. No absent user design is replaced by an invented tree/spike.

Server actions include native permission checks, persistent UUID ownership,
collision checks, block-use checks, and native place/mobGriefing hooks. There is
no client packet that can choose a new owner, remotely wax copper, or override
fixed damage. Entity size/orientation/digestion presentation uses synchronized
entity data. Slug item state uses native data components, not legacy item tags.

Cooldowns use absolute game time, so saving/reloading cannot reset collection or
waxing cooldowns. Digestion, taming opportunities, and encounter phases persist.
Slime traps are SavedData per dimension; immobilization deadlines and anchors are
saved too. Trap expiry never replaces the original block.

The custom source-only lava still uses native lava mechanics/textures and a
native lava fluid tag/type. Its spreading tick is disabled. A small required
Mixin intercepts vanilla lava tick/spread inside Volcanic Badlands to enforce the
same fixed rule for introduced lava. This uses the Mixin runtime already supplied
by NeoForge; no extra mod is required.


Natural Depth Stalker encounters have a per-dimension SavedData clock, defaulting
to at least one hour between spawns; despawning or restarting cannot reset it.
During a hunt, a persistent bounded wound cancels healing, so regeneration cannot
reset the fixed two-hit scenario while the hunter retreats. Wounds clear on the
hunter's removal or the victim's death and have an absolute 90-second failsafe.
The slug ignores the generic scale attribute; its saved random size owns its
fixed maximum physical size. Resource authors must still keep its model inside
the documented one-block model envelope.
