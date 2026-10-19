# Implementation map

Working identity: Wildlands (`wildlands`). Java 21, Minecraft 1.21.1,
NeoForge 21.1.262. No external gameplay API or runtime dependencies.

- World: thirteen data-driven biome registrations, integrated into the vanilla
  Overworld multi-noise preset during datapack decoding. A world-seeded gate adds
  custom terrain/biome regions while most cells retain vanilla climate lookup
  and terrain. The source's possible-biome set includes every custom biome for
  native locate, feature, and structure generation. No separate realm is declared.
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

The Overworld integration uses native Mixins and no external worldgen API.
`OverworldParametersMixin` resolves mod biome holders during datapack decoding,
leaving vanilla bootstrap registries untouched. `OverworldBiomeSourceMixin` adds
these holders to the vanilla source and selects custom biomes in approximately
one quarter of world-seeded 512-block region cells. Other cells preserve vanilla
climate selection, including cave biomes. Nether presets, direct biome lists,
Superflat, and single-biome sources are unaffected. `OverworldSeedMixin` supplies
the seed before structure-state initialization.

Within custom cells, the existing weighted region sampler chooses enabled biomes.
`OverworldTerrainMixin` applies the matching terrain after the native surface step,
replacing carving in custom cells before features/structure pieces. It supplies
matching base columns/heights for structure queries. A 32-block margin blends toward native terrain at cell
edges. Default and Large Biomes noise settings are supported. These are geometric
regions, not vanilla climate blending; transitions can still be visible. Custom
cells use purpose-built solid terrain rather than vanilla cave/aquifer carving.
The Depths occupies the full water column, with a jagged floor around -50 to -45
away from the transition margins. Existing generated chunks are never rewritten.

The old custom generator, source codec and hidden world preset remain for loading
1.0.0 custom-world saves and focused terrain tests; they are not required for new
worlds. The mod no longer declares a separate realm or offers realm travel.

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
