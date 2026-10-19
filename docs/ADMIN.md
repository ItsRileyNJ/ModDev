# Server administration and gameplay

Install the built `wildlands-1.0.1.jar` in the `mods/` folder on the server AND
on every connecting Minecraft 1.21.1 client, using NeoForge 21.1.262. No other
mod API is required. Back up existing worlds before installing worldgen mods.

Use Minecraft's normal **Default** world type. On a dedicated server, keep
`level-type=minecraft:normal`. Wildlands biomes now generate alongside vanilla
biomes in the normal Overworld, including new chunks of existing Default worlds.
No separate dimension or special world type is required. Already generated
chunks remain unchanged; travel beyond previously explored/generated terrain or
create a fresh Default world to see the change immediately. Installing on the
client alone cannot change a remote server's world generation.

Replace the old JAR with version 1.0.1 on both client and server, remove the old
copy, and restart. Superflat and single-biome sources are deliberately unaffected.
Default and Large Biomes use the overlay; the added regions have a fixed 512-block
size. The Nether and End retain their vanilla sources. The old custom generator
and hidden preset codec remain available so 1.0.0 custom-world saves can still
load; those old saves retain their original generator. New worlds no longer
declare `wildlands:realm`, and `/wildlands realm` has been removed.

## Operator commands

All `/wildlands` commands require native Minecraft permission level 2 or above.
They are validated by the server, not a client GUI. No separate permission API
or external permissions mod is needed.

- `/wildlands status`: versions, configuration location, and fixed rules.
- `/wildlands structures`: audits the six user-supplied NBT designs.
- `/wildlands export <template> <from> <to> [replace]`: saves a large structure NBT from loaded blocks, with overwrite protection; see the asset guide.
- Vanilla `/locate biome wildlands:BIOME` locates a biome in the normal Overworld.
  Vanilla `/locate structure wildlands:redwood_forest_tree_01` can locate
  supplied designs after the templates are installed and spacing permits them.
- Vanilla `/give` and `/summon wildlands:root_worm`, `banana_slug`, `flytrap`,
  `depth_stalker` support operator testing. Spawn eggs are in the Wildlands tab.

There are no individual admin grants because native operator commands cover all
administrative actions. Player ownership is separately enforced by persistent
UUID: even another ordinary player cannot pick up your slug or harvest your flytrap.
Creative commands can of course modify worlds; this is not an anti-cheat system
against the server operator.

## Configuration

The server's file is `config/wildlands-common.toml` (development: `run/config/`).
A complete generated default file, including comments and bounds, is shipped as
[DEFAULT_CONFIG.toml](DEFAULT_CONFIG.toml). Stop the server, edit the file, then
restart. Spawn-list changes are applied when biome modifiers load; worldgen
changes affect new chunks. Changing region weights/enabled biomes in an existing
world can cause abrupt borders: prefer a fresh test world. Clients do not decide
ownership, damage, taming, traps, spawning, or world changes. Cosmetic ash uses
the client's common-config intensity; it does not affect authoritative mechanics.

The significant defaults are:

| Group | Defaults |
| --- | --- |
| Biomes | Enabled. Each independent biome weight 1. Satellites have no independent region weight. If all independent biomes are disabled, the normal Overworld retains vanilla biome selection. |
| Custom structures | Placement probability 0.65; spacing 2 chunks, redwoods 4. Empty templates are skipped. Increase spacing for large designs. |
| Surface details | 5 attempts per chunk in applicable biomes; root/limestone patch radius 5. Bacteria has one attempt, gated by 1-in-400 chunks. |
| White Mountains | Mountain height variation 90; calcite share 0.65; configurable azalea attempts. |
| Crater | Radius 100 blocks, depth 38; central iron radius 10 and raw-iron share 0.6. |
| Volcanic | Terrain roughness 15; pool radius 3, pool chance 0.15 per attempt; ash intensity 0.12. |
| Burnt wood | 1–3 charcoal. Only the dedicated burnt wood loot table is affected. |
| Sulphuric material | 70% chance of 1–2 gunpowder; no charcoal drop. |
| Root Worm | Spawn weight 12; fire multiplier 2; flee enabled, radius 6, speed multiplier 1.6. No root destruction is implemented. |
| Root Flower | Chance 0.65 per dirt patch; contact damage 1 every 10 moving-contact ticks; slows; affects players and mobs. |
| Banana Slug | Spawn weight 10; copper cooldown 1200 ticks; all vanilla/data-map waxable copper eligible unless a whitelist is supplied. |
| Slug dryness | Grace 200 ticks, 1 damage every 40 ticks afterward; rain and water protect; configured hot/dry biome list plus actual hot/dry climate. Taming never grants immunity. |
| Golem healing | No entity IDs enabled: vanilla 1.21.1 has no Copper Golem. For a compatible installed mod, list its exact golem entity ID; heal 2 while touching it, sharing the copper cooldown. Unknown IDs never create entities. |
| Flytrap | Spawn weight 4; one digestion duration of 1200 ticks for every prey; post-digestion taming window 6000 ticks; collection cooldown 2400 ticks. |
| Flytrap safety | Pets excluded by default; villagers/allays blacklisted; `misc` category excluded. Tamed flytraps can eat other non-player mobs up to width 0.9. Players/other flytraps/allies are always excluded. |
| Slime traps | Lifetime 1200 ticks; immobilization 100 ticks; all mobs eligible unless an exact-type allowlist is configured. Players are excluded. |
| Depth Stalker | Spawn weight 1 and extra 1-in-100 spawn gate; at most one in a 256-block query neighborhood at spawn, plus a persistent 72,000-tick (one-hour) minimum between natural encounters per dimension. The interval can be raised or lowered to a minimum of 6,000 ticks. |
| Ocean Trench | Drowned weight 100, ore attempts 12. Changes apply only to this biome. |

Rocky Plains also has a separate fossil chance (1/3 per outcrop) and maximum fossil length (5 blocks).

One second is 20 game ticks at normal server speed. Weight is relative to other
eligible entities/biomes, not a percentage chance per tick.

### Fixed rules (no config or command override)

- The Depths underwater fog ends 5 blocks away and uses very dark fog color.
- Pressure begins at Y <= -36 in The Depths: Blindness and Mining Fatigue III.
- Abyssal strikes remove 18 health points, ignoring armor, enchantment mitigation,
  resistance, absorption and shields. A healthy, normal 20-health player is left
  at 2 health by the first successful hit and dies to the second. Damage is never
  based on armor, and the creature retreats between attacks. Creative/spectator
  players are not hunted. The damage type bypasses totem-style invulnerability.
  A server-enforced abyssal wound blocks healing during the active encounter so
  natural regeneration cannot erase the first hit while the creature retreats.
  It clears when the hunter disappears/dies or the player dies; a saved 90-second
  safety deadline clears it if an unloaded hunter never returns. An action-bar
  message explains the wound when hit. This fixed mechanic has no admin override.
- Ignimbrite Flats only occurs as a border of Volcanic Badlands; Dead Reef only
  as a border of Sulphuric Spires. Neither has an independent spawn-region option.
- Generated volcanic pools use an actual source-only lava fluid that cannot spread.
  Vanilla lava ticking or spreading into Volcanic Badlands is converted to that
  source-only fluid. There is no setting to enable streams.

A mod that rewrites health, cancels damage/death events, replaces fog rendering,
or alters the maximum health attribute can change outcomes beyond the normal
Minecraft mechanics. In particular, a fixed 18-point hit cannot also guarantee
exactly two hits for players whose maximum health is changed above 36 by other
mods. The implemented two-hit profile is explicitly tested at vanilla 20 health
with netherite armor and absorption; it does not falsely promise universal control
over every third-party mod.

## Player interactions

- Feed a wild slug `minecraft:frogspawn` to tame it. The item exists in 1.21.1,
  but vanilla does not offer ordinary survival harvesting; Wildlands adds a
  shapeless recipe: two slimeballs + seagrass -> one frogspawn.
- Sneak and right-click your slug with an empty hand to pick it up. Its item
  preserves size, ownership, health, and cooldowns. Right-click a permitted clear
  block face to place it. Another player's UUID cannot use the stored slug.
- Slugs seek accessible copper, climb to it, and wax only while actually touching
  it. They respect mobGriefing and the native entity-place event. A denied wax
  operation leaves the copper unchanged.
- A wild flytrap eats bees and phantoms in mouth range. After it eats a phantom,
  give it a phantom membrane during its opportunity window. A membrane alone
  cannot tame one. The window includes digestion plus the configured interval.
- Craft empty jars from glass; use one on your tamed, non-digesting flytrap after
  its collection cooldown to receive slime. The cooldown prevents repeated grabs.
- Use a slime jar on the solid, clear top face of any permitted block. The first
  eligible mob stepping there consumes the trap and becomes immobilized for the
  configured duration. Untriggered traps expire. The underlying block is never
  replaced; traps and immobilization deadlines persist across save/reload.
- Root Flower + paper crafts its banner pattern. Plant fossil + paper crafts the
  fossil pattern. Use them in a loom. Animal fossil + smooth stone crafts a trophy.
