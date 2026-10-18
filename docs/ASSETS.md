# Your art and structure checklist

Everything below is relative to the `src/main/resources/` folder in this project.
Keep filenames lowercase, using underscores. Replace files at the listed paths;
you do not need to edit Java. After replacing resources, rebuild the JAR. In a
development client, F3+T reloads textures/models, but structures and worldgen
require `/reload` and newly generated chunks (a new test world is safest).

## Exporting entity models

Open Blockbench, select **Java Block/Item**, and work in pixels: 16 pixels = one
block in the exported model. Export **Block/Item Model** JSON, not Java source,
Bedrock `.geo.json`, or a GeckoLib model. Export each texture as a PNG.

The renderer loads `assets/wildlands/models/entity/NAME.json` and bakes it into
Minecraft's block texture atlas. Set each model's texture to
`wildlands:entity/NAME`; this resolves to
`assets/wildlands/textures/entity/NAME.png`. UV coordinates in the exported JSON
control the texture mapping. Keep a `particle` texture entry too. The supplied
placeholder models are working examples of the supported format.

| Creature | Model JSON | Texture PNG | Model scale / states |
| --- | --- | --- | --- |
| Root Worm (Rootlands) | `assets/wildlands/models/entity/root_worm.json` | `assets/wildlands/textures/entity/root_worm.png` | One model unit = one world block; design a low body. |
| Banana Slug (Redwood Forest) | `assets/wildlands/models/entity/banana_slug.json` | `assets/wildlands/textures/entity/banana_slug.png` | Keep the full model inside 16 pixels along its length. Runtime size is 0.05–1.0, persisted and synchronized. The renderer rotates it to its supporting wall/ceiling. |
| Flytrap (The Verdent), open | `assets/wildlands/models/entity/flytrap.json` | `assets/wildlands/textures/entity/flytrap.png` | Rendered at 2.2×; make an open mouth in this model. |
| Flytrap, digesting | `assets/wildlands/models/entity/flytrap_digesting.json` | Same `flytrap.png` | Make a closed mouth and shriveled body. Server digestion state selects this model. |
| Depth Stalker (The Depths) | `assets/wildlands/models/entity/depth_stalker.json` | `assets/wildlands/textures/entity/depth_stalker.png` | Rendered at 6×. The placeholder is deliberately low-detail and dark. |

**Format limits:** Java block/item JSON is cuboid geometry. It supports limited
fixed element rotations (0, ±22.5°, ±45° around one axis), not animated bone
hierarchies, meshes, or arbitrary Blockbench keyframe animations. `.bbmodel`
files are editable source projects, not in-game models. Keep your `.bbmodel`
files outside the shipped resources. The mod provides real movement, orientation,
and open/digesting posture selection; arbitrary skeletal animation import is not
implemented. Use the two exported flytrap poses for the required closed/shriveled
digestion appearance. You can replace both poses without changing code.

Textures may be higher resolution than 16×16: keep the same UV proportions.
Normal entity textures in this renderer use opaque/cutout pixels; translucent
materials and advanced shader effects are not a supported art assumption.

## Exporting structures

Build your design in a Minecraft **1.21.1** creative test world using blocks from
this mod and vanilla blocks. Use a Structure Block (`/give @s structure_block`).
In Save mode, select the structure's size and offset, turn **Include Entities OFF**,
and save under the exact names below. Structure-block exports normally appear in
`<world>/generated/wildlands/structures/...`; the mod/resource-pack **input** path
in 1.21.1 is the singular `data/wildlands/structure/` directory below.

Structure files are gzip-compressed Minecraft `.nbt` structures with `size`,
`palette`, `blocks`, and `entities`; they are not JSON, `.schem`, or `.litematic`.
A schematic must first be pasted into Minecraft and saved with a Structure Block.
The vanilla UI generally limits one saved template to 48×48×48. For larger designs, Wildlands includes its own permission-protected exporter:

```text
/wildlands export wildlands:redwood_forest/tree_01 X1 Y1 Z1 X2 Y2 Z2
```

Replace the six coordinate values with opposite corners of your build. All selected
chunks must be loaded. This writes the actual `.nbt` under your world's
`generated/wildlands/structures/redwood_forest/tree_01.nbt`; copy that file to the
resource path in the table below. On a hosted server, use the host's file manager
to download it. Export includes blocks/block-entity contents, skips air, and never
includes entities. Empty decorative containers before exporting unless you intend
their contents to be copied. The command accepts up to 512×256×512 but limits one
selection to 4,194,304 blocks. It refuses to overwrite an existing export unless
you deliberately append `replace`. Back up your earlier design first. This removes
the vanilla 48-block UI limit without requiring you to program an exporter.
Do not rename a schematic to `.nbt`: paste it into your creative world first.

| Biome / object | Structure Block save name | Exact resource file |
| --- | --- | --- |
| Rootlands root | `wildlands:rootlands/root_01` | `data/wildlands/structure/rootlands/root_01.nbt` |
| Rootlands thorn | `wildlands:rootlands/thorn_01` | `data/wildlands/structure/rootlands/thorn_01.nbt` |
| Burnt Forest tree | `wildlands:burnt_forest/tree_01` | `data/wildlands/structure/burnt_forest/tree_01.nbt` |
| Burnt Forest bush | `wildlands:burnt_forest/bush_01` | `data/wildlands/structure/burnt_forest/bush_01.nbt` |
| Redwood Forest tree | `wildlands:redwood_forest/tree_01` | `data/wildlands/structure/redwood_forest/tree_01.nbt` |
| Sulphuric Spires spire | `wildlands:sulphuric_spires/spire_01` | `data/wildlands/structure/sulphuric_spires/spire_01.nbt` |

The provided NBT files are **empty markers, not substitute designs**. Generation
skips them and logs one warning per missing design. `/wildlands structures`
reports whether each supplied design is loaded. Until you replace them, those
biomes do not contain your custom roots/trees/spires. Ordinary trees, generated
roots, and random spikes are never substituted.

Normally put the ground-contact layer at template Y=0. For buried roots, edit
`y_offset` (default 0, allowed -64 through 64) in the corresponding
`data/wildlands/worldgen/structure/NAME.json`; e.g. -8 buries the bottom eight layers. Placement uses the terrain surface
at the template's minimum X/Z corner. Air and structure blocks are ignored,
so your saved bounding box does not carve a giant rectangular hole. Entities
inside templates are ignored to prevent unintended entity duplication.
Use `wildlands:burnt_wood` in charcoal-yielding trees; do not expect vanilla logs
to become charcoal drops. Use `wildlands:sulphuric_spire` in harvestable spires;
its only material drop is gunpowder. Redwood and root designs can use vanilla
logs, roots, leaves, and other appropriate blocks with their normal drops.

A single template may be up to 512×256×512 in code, but must also fit the configured
spacing: maximum X/Z footprint is `(spacingChunks - spacingChunks/4) * 16 - 4`,
using integer division. Increase that biome's `structureSpacingChunks` before
using a huge design. Structures that cannot fit are skipped, never truncated.
Native structure pieces place large designs across chunk boundaries and persist
their bounds. The grid leaves room for jitter and avoids overlapping starts;
root/thorn and burnt tree/bush variants share placement sets. Terrain may slope
under a broad design: include roots/foundations where necessary. Templates are
not terrain-conforming meshes. The placement randomly rotates complete designs by 0, 90, 180, or 270 degrees, with the rotation saved in the structure piece.

## Blocks, plants and items, grouped by biome

For ordinary cube blocks, `assets/wildlands/blockstates/NAME.json` selects
`assets/wildlands/models/block/NAME.json`. Their inventory form is
`assets/wildlands/models/item/NAME.json`. PNG textures are under
`assets/wildlands/textures/block/NAME.png` when custom textures are listed.
Blockbench's **Java Block/Item** export is the correct block/item model format.
For a simple block, replace only the PNG; keep the model and blockstate unchanged.

| Biome | Content | Files / replacement work |
| --- | --- | --- |
| Rootlands | Root Flower | `models/block/root_flower.json`, `models/item/root_flower.json`, `blockstates/root_flower.json`, `textures/block/root_flower.png`, all under `assets/wildlands/`. A transparent 16×16 white/red scraggly bush placeholder is supplied. |
| Rootlands | Root Flower banner | `assets/wildlands/textures/entity/banner/root_flower.png` and `.../shield/root_flower.png` (64×64 white-on-transparent dye masks); recipe `data/wildlands/recipe/root_flower_banner_pattern.json`. Item icon `assets/wildlands/textures/item/root_flower_banner_pattern.png`. |
| Rootlands | Dirt and roots | Vanilla rooted dirt/coarse dirt: no duplicate textures. Custom NBT roots/thorns listed above. |
| White Mountains | Calcite, diorite, azalea | All vanilla blocks, models, textures: no custom assets needed. |
| Crater | Iron deposit | Vanilla raw iron blocks and iron ore: no custom assets needed. |
| Volcanic Badlands | Dark terrain, stationary lava, ash | Vanilla basalt/blackstone, lava texture and WHITE_ASH particle: no custom art needed. |
| Burnt Forest | Burnt wood | `assets/wildlands/models/block/burnt_wood.json` currently reuses stripped dark oak texture. Change its `all` texture to your own `wildlands:block/burnt_wood`, then add `assets/wildlands/textures/block/burnt_wood.png` if desired. Its block has an `axis` property; directional variants can be added in the blockstate JSON. |
| Redwood Forest | Trees and Banana Slug | Structure and creature paths above; vanilla block textures can be used inside the trees. |
| Ignimbrite Flats | Ignimbrite and polished ignimbrite | `assets/wildlands/textures/block/ignimbrite.png`, `polished_ignimbrite.png`; neutral checker placeholders, not a proposed final rock appearance. Corresponding block/item models and blockstates are supplied. |
| Ignimbrite Flats | Slab, stairs, wall | `models/block/ignimbrite_slab.json`, `ignimbrite_slab_top.json`, `ignimbrite_stairs.json`, `ignimbrite_stairs_inner.json`, `ignimbrite_stairs_outer.json`, and `ignimbrite_wall_{post,side,side_tall,inventory}.json` under `assets/wildlands/`. All inherit the same replaceable ignimbrite texture. |
| The Verdent | Flytrap | Open and digesting creature models above. |
| The Verdent | Empty jar, slime jar | `assets/wildlands/models/item/empty_jar.json`, `flytrap_slime_jar.json`; PNGs `assets/wildlands/textures/item/empty_jar.png`, `flytrap_slime_jar.png`. Glass/green item placeholders supplied. |
| Rocky Plains | Limestone | `assets/wildlands/models/block/limestone.json` reuses calcite. You may point `all` at `wildlands:block/limestone` and add that PNG. |
| Rocky Plains | Plant fossil, animal fossil, trophy | `assets/wildlands/models/block/{plant_fossil,animal_fossil,fossil_trophy}.json` reuse bone-block textures. Replace those JSON models to create your fossil shapes/trophy; no Java changes. Matching blockstates and item models exist. |
| Rocky Plains | Fossil banner | `assets/wildlands/textures/entity/banner/fossil.png`, `.../shield/fossil.png`; icon `textures/item/fossil_banner_pattern.png`. White mask placeholder supplied; make this distinct from the flower design. |
| Dead Reef | Dead coral arrangements | Vanilla dead tube/brain/bubble/fire/horn coral blocks and dead tube fans, using vanilla reef arrangement code. No bright living coral or custom art required. |
| Sulphuric Spires | Harvestable spire material | `assets/wildlands/textures/block/sulphuric_spire.png`, `models/block/sulphuric_spire.json`, `blockstates/sulphuric_spire.json` and matching item model. Yellow placeholder supplied. Structure path above. |
| The Depths | Bacteria | `assets/wildlands/textures/block/seafloor_bacteria.png`, `models/block/seafloor_bacteria.json`, `blockstates/seafloor_bacteria.json`. Sparse transparent growth, vanilla lichen-like face geometry; block light is fixed at level 6. |
| The Depths | Creature | `depth_stalker` creature model/texture above. |
| Ocean Trench | Drowned and resource deposits | All vanilla entities/ore textures, no additional art. |
| Shared slug interaction | Carried slug icon | `assets/wildlands/textures/item/carried_banana_slug.png`, matching item model. Actual size, owner and entity data live in item components, not the icon. |

Do not include the `assets/wildlands/` prefix when writing a texture reference
inside a model: `wildlands:block/ignimbrite` is correct. PNG paths are case-sensitive.
Banner/shield pattern PNGs are white masks; Minecraft applies dye colors. Check
placement/UV alignment against the supplied mask before finalizing a detailed design.

No custom sound recording is required: the mod uses vanilla sounds/particles.
No finished textures, finished creature sculptures, or custom structure designs
are claimed. Those are your replaceable art deliverables.


## Adding more designs without Java

For a second redwood design, copy `tree_02.nbt` into
`data/wildlands/structure/redwood_forest/`. Copy
`data/wildlands/worldgen/structure/redwood_forest_tree_01.json` to
`redwood_forest_tree_02.json` in the same directory and change its `template` to
`wildlands:redwood_forest/tree_02`. Then append
`{"structure":"wildlands:redwood_forest_tree_02","weight":1}` to the
`structures` list in
`data/wildlands/worldgen/structure_set/redwood_forest_tree_01.json`.
Keep the existing entry and commas between entries. The native placement set
chooses one design per start and uses the same spacing configuration. This pattern
also works for root/thorn, burnt, and spire variants. Keep `biome_config` set to
that design's biome. JSON changes require a rebuild/reload and new chunks.
