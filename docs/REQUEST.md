Minecraft Java Edition 1.21.1 | NeoForge

You are an expert Minecraft Java mod developer specializing in NeoForge 1.21.1, custom world generation, entity AI, structure placement, rendering, networking, configuration systems, and multiplayer-compatible mod architecture.

Your task is to implement my Minecraft mod from start to finish. This is an implementation project, not merely a design exercise. You must create the actual source code, assets infrastructure, registries, data files, configuration options, recipes, loot tables, commands, tests, and documentation required to make the mod work.

I am not a programmer. Explain important decisions in plain English, make the asset workflow easy to follow, and provide precise instructions for anything I need to create or replace myself.

1. Core Requirements

Minecraft version and mod loader

Target Minecraft Java Edition 1.21.1.

Use NeoForge for Minecraft 1.21.1.

Use Java and the correct Minecraft and NeoForge APIs for that version.

Do not use Fabric APIs or code intended for another Minecraft version.

Implement all required functionality directly within this mod unless it is already provided by vanilla Minecraft, NeoForge or your own API.

Do not require another developer's API JAR. You may build your own if functionality depends on it.

Avoid unnecessary third-party dependencies.

Maintain multiplayer compatibility, with gameplay mechanics handled authoritatively on the server.

Beginner-friendly custom assets

I will create my own models, textures, and custom structures.

For every custom entity, block, plant, tree, structure, item, and other visual element:

Specify the exact directory and filename.

Identify the required model format and texture format.

Explain how to export the asset from its modeling software.

Explain how the asset connects to the mod's registration and rendering code.

Supply placeholder assets or temporary implementations where practical.

Make it easy to replace placeholders without editing Java code.

Provide a complete asset checklist grouped by biome and object type.

Explain any limitations of Minecraft's resource and structure formats.

For custom trees, roots, thorns, burnt trees, and sulphuric spires, I will provide structure files. Build the necessary placement infrastructure around those files rather than requiring me to program individual placement algorithms.

Prefer existing vanilla Minecraft blocks, textures, models, sounds, items, particles, and status effects wherever they genuinely fit. Do not create duplicate custom assets unnecessarily.

Verify that any vanilla asset or feature you rely on actually exists in Minecraft 1.21.1.

2. Architecture and Administration

Build the mod using a clean, modular architecture.

Server administrators must have meaningful control over gameplay through configuration files and permission-protected commands.

Implement the permission system directly using native NeoForge and Minecraft systems.

Provide:

Separate configuration settings for individual biomes and mechanics.

Enable/disable switches for appropriate features.

Configurable spawn rates, generation density, cooldowns, ranges, and other balancing values where specified.

Permission checks for administrative commands.

Server-side validation of administrative actions.

Persistent entity ownership and state where specified.

Multiplayer-safe handling of entity interactions, item use, status effects, and world changes.

Clear documentation explaining how administrators use the system.

Use the appropriate native NeoForge configuration mechanisms and Minecraft command permission checks. Do not assume that vanilla Minecraft provides a complete player-by-player permission framework. If individual player permissions are needed beyond native command access, implement a small, maintainable server-side permission system within this mod.

Important configuration rule: If I explicitly specify below that a behavior must remain fixed and non-configurable, do not expose it through configuration files, commands, or permission-controlled settings. General administrative control must not override those fixed design decisions.

Do not make every possible behavior configurable merely because it is convenient.

3. Rooted Forest / Rootlands

Environment and terrain

Create a biome featuring:

Darker grass with a very slight orange tint.

Extensive patches of rooted dirt and coarse dirt.

Large roots and thorn growth distributed throughout the biome.

A dark, overgrown atmosphere.

Use vanilla rooted dirt and coarse dirt where appropriate, with configurable patch sizes and frequencies.

Custom root and thorn structures

I will create the roots and thorns as custom structure files.

Implement a system that places my supplied structures throughout Rootlands.

Do not substitute vanilla trees or generate generic roots in place of my custom structures.

Document exact structure-file locations and naming conventions.

Allow structure density, spacing, and placement frequency to be configured.

Ensure structures generate naturally without excessive overlap.

Root Worm entity

Add a mob called the Root Worm.

Behavior:

Neutral by default; it must not attack a player unless attacked first.

Lives among the roots, with behavior implying that it eats roots.

Must not actually destroy root structures by default.

Takes increased fire damage.

Actively flees from nearby fire and lava.

Provide configuration for spawn frequency, fire damage multiplier, fire-avoidance detection radius, and fleeing behavior.

Root destruction, if implemented as an optional mechanic, must default to disabled.

I will provide its model and textures.

Root Flower plant

Create a small, gaunt, scraggly bush with tiny white and red flowers.

Generates naturally in Rootlands.

Damages entities that walk through it, similar to vanilla sweet berry bushes.

May slow affected entities in a similar manner.

Has a distinct appearance.

Allow patch frequency, damage, and applicable entity behavior to be configured.

Root Flower banner pattern

Add a banner pattern associated with the Root Flower.

The pattern must be craftable using a Root Flower in a crafting table.

Do not make it a random drop.

Implement the required registration, recipe, and asset instructions.

4. White Mountains

Create mountainous terrain dominated by:

Calcite.

Diorite.

Azalea vegetation.

The result should feel like a pale, rocky mountain biome.

Provide configuration for rock distribution, terrain generation, and azalea frequency. Reuse vanilla blocks and vegetation wherever possible.

5. Crater

Create a large crater biome featuring a substantial crater formation and an iron deposit in the middle.

The deposit should feel like a distinctive geological feature.

Provide configuration for crater dimensions, generation frequency, and iron deposit size or richness.

6. Volcanic Badlands

Create a dead, volcanic-looking biome featuring dark, barren terrain, lava pools, and an ashy atmospheric particle effect similar to the vanilla Basalt Deltas biome.

There must be no flowing lava in this biome.

Generate stationary lava pools only. Do not create flowing lava streams, lava rivers, or flowing-lava features.

Reuse vanilla particles when suitable.

Provide configuration for lava pool frequency and size, terrain generation, and ash particle intensity where appropriate.

7. Burnt Forest

Create a dead forest containing burnt vegetation, bushes, and custom burnt trees.

Custom structures

I will provide custom burnt tree and other burnt forest structure files.

Implement structure placement for my supplied files.

Do not substitute ordinary vanilla trees.

Document the exact file locations and replacement process.

Charcoal

Designated burnt trees and burnt wood blocks must yield charcoal instead of ordinary wood when harvested, as appropriate to the implementation.

Use proper loot tables. Do not globally change unrelated vanilla wood drops.

Provide configuration for burnt structure density and charcoal drop amounts.

8. Redwood Forest

Create a forest dominated by enormous redwood trees.

Custom redwood structures

I will supply custom redwood tree structures.

Place these structures as complete custom objects or structures.

Do not generate vanilla-style 3×3 or 4×4 trees as substitutes.

Support my own tree designs, dimensions, and layouts.

Make placement natural and configurable through spacing and generation frequency.

Provide exact structure directories, naming conventions, and replacement instructions.

Banana Slug entity

Create a banana slug that naturally inhabits the Redwood Forest.

Appearance and size

Each slug receives a randomized size when it spawns.

Its length must never exceed one Minecraft block.

It may vary anywhere within the allowed size range, including very small individuals.

The chosen size persists through saving, unloading, reloading, and pickup/replacement.

I will create its model and textures. Explain how size variation connects to the entity model and rendering.

Movement and pathfinding

The banana slug must:

Wander randomly around its environment.

Climb vertical walls.

Cling to and travel along ceilings.

Hang upside down.

Navigate vertically instead of being restricted to ground movement.

Explore its surroundings naturally, including climbing onto structures.

Do not implement wall climbing as merely a visual animation. The movement and collision behavior must actually support the intended capabilities.

Copper interaction

The slug automatically waxes copper blocks.

Detect a nearby eligible copper block.

Navigate to that block.

Physically reach and position itself on the copper block.

Perform the waxing interaction while in contact with the block.

Apply the appropriate vanilla waxing transformation.

The slug must not wax copper from a distance. It must physically reach and occupy the copper block before waxing it.

Use the appropriate vanilla copper-waxing mechanics and block mappings for Minecraft 1.21.1.

The slug can also heal Copper Golems. Verify that the Copper Golem entity is available in the actual installed mod environment before implementing this behavior. Do not invent a vanilla entity that does not exist in the target version.

Provide configuration for copper interaction cooldowns, eligible copper blocks, and healing behavior.

Taming and pickup

Banana Slugs can be tamed by feeding them frogspawn.

Verify the correct Minecraft 1.21.1 item or block interaction.

Once tamed, the slug has an owner.

Its owner can pick it up.

Pickup preserves ownership and entity data.

When placed back into the world, the slug remains tamed by the same owner.

Prevent unauthorized players from picking up another player's slug.

Persist the slug's size, owner, and essential entity state.

Dryness and temperature

Banana Slugs cannot survive in hot, dry biomes, including Deserts, Badlands, and Volcanic Badlands.

When exposed to a sufficiently hot and dry environment, the slug suffers damage and eventually dies if it remains there.

Provide configuration for applicable biomes, exposure grace period, damage rate, and environmental exceptions. Taming must not grant immunity to the environmental weakness.

9. Ignimbrite Flats

Create a relatively flat volcanic-rock biome associated with Volcanic Badlands.

Generation restriction

Ignimbrite Flats may generate only next to Volcanic Badlands. It must not generate independently in unrelated regions.

Implement the strongest practical biome placement or adjacency solution supported by Minecraft 1.21.1 and explain any limitations.

Block set

Create the registration and generation infrastructure for an ignimbrite block set.

Use placeholders for ignimbrite blocks, models, and textures. I will create these myself.

Provide placeholder registrations and data, clear block names and asset paths, generation rules, and instructions for replacing placeholders.

Do not invent a finished custom rock appearance that replaces the assets I intend to provide.

10. The Verdent

Create a carnivorous-plant biome containing large flytrap plants.

Flytrap behavior

Implement a large carnivorous flytrap that:

Can eat Phantoms and Bees.

Can be tamed with a Phantom Membrane only after it has already eaten a Phantom.

Creates a valid taming opportunity after eating a Phantom.

Cannot be tamed with a Phantom Membrane alone before eating a Phantom.

Can eat smaller mobs that wander close to it after taming.

Can target pets, so pet safety must be explicitly accounted for in targeting configuration.

I will create its model, textures, and any custom animations.

Feeding and digestion

Whenever the flytrap eats a mob:

Its mouth closes.

Its body shrivels or closes into a digestion posture.

It enters a clear digestion state.

It cannot attack or consume another target while digesting.

It returns to its active state when digestion ends.

All consumed mob types use the same configurable digestion duration. Do not create separate digestion durations for individual mob types.

Provide configuration that lets administrators blacklist specific mobs or categories of mobs from being targeted.

Slime collection

The flytrap produces slime that can be harvested while it is tamed.

The player uses an appropriate empty jar on the tamed flytrap.

The interaction produces a filled jar containing flytrap slime.

The flytrap enters a collection cooldown.

Repeated collection is prevented until the cooldown ends.

Verify whether a suitable vanilla jar item exists in Minecraft 1.21.1. Do not invent a vanilla jar item. If none exists, implement the smallest necessary custom empty jar and filled slime jar.

Document the items, recipes if needed, textures, registration, and collection behavior.

Flytrap slime traps

Collected slime can be applied to any suitable block, not just a carnivorous plant block.

The player uses the slime on a block.

The block becomes an active slime trap for a limited duration.

A mob that steps on the trapped block is immobilized in place.

Immobilization lasts for a defined duration.

The trap expires and cleans up correctly.

Do not permanently damage the underlying block or leave invalid world state.

Provide configuration for trap duration and affected entity types. Ensure placement, triggering, and expiration work correctly in multiplayer.

11. Rocky Plains

Create a warmer, relatively open plains biome featuring limestone outcrops, fossilized plants, fossilized animals, banner patterns, and trophies associated with fossil discoveries.

Use appropriate vanilla blocks where possible.

Implement fossil generation as world features or structures, with configurable frequency and distribution.

Create necessary banner pattern and trophy registrations, loot tables or recipes as appropriate, and placeholders for custom assets.

Document the textures, models, icons, and other assets I need to create.

12. Dead Reef

Create a dead coral reef biome using vanilla coral reef shapes and arrangements.

Coral appearance

Use vanilla coral reef-style structures and arrangements.

Coral must appear dead and gray, as though it has died from being out of water.

Do not generate living, brightly colored coral.

Use appropriate vanilla dead coral blocks and variants wherever possible.

Generation restriction

Dead Reef must generate only immediately adjacent to Sulphuric Spires.

It must not generate independently in unrelated ocean areas.

Implement the required biome placement and adjacency constraints. Do not silently relax this restriction.

Provide configuration for reef density and generation frequency, but no setting that allows Dead Reef to generate independently of Sulphuric Spires.

13. Sulphuric Spires

Create a hot, acidic-looking biome featuring large sulphuric spires.

Custom structures

I will provide custom sulphuric spire structure files.

Place spires using those structures.

Do not generate arbitrary random spikes as substitutes.

Harvestable spire block

Create a dedicated custom block for the harvestable spire material.

Register the block and its properties.

Integrate it into my custom structures.

Provide the required loot table and block behavior.

Ensure appropriate spire blocks can be harvested.

The intended biome-specific drop is gunpowder only.

Do not add charcoal as a Sulphuric Spires drop.

Provide configuration for spire generation density and gunpowder drop amounts or chances.

Document exact block, structure, model, and texture paths.

14. The Depths

Create a deep, dark ocean biome with a distinctive, hostile atmosphere and extremely limited visibility.

The intended experience is oppressive darkness, isolation, and an encounter with a creature that feels almost impossible to defeat.

Water column and seafloor

The biome consists of a deep water column extending down to a seafloor generally around Y = -45 to Y = -50.

This is the seafloor elevation, not the vertical height range of the biome.

The biome must not be restricted to a five-block vertical band.

Generate an uneven, jagged seafloor consisting predominantly of clay and gravel. The terrain must be irregular rather than smooth and flat, with almost no visible life.

Rare bioluminescent organism

Add an exceedingly rare, lichen-like bacterium growing on the seafloor.

It should look like subtle biological growth rather than a large plant.

It must be extremely rare.

It emits light level 6.

It should be one of the very few signs of life in the biome.

I will provide custom textures and models if needed. Use appropriate vanilla light-emission mechanics.

Underwater fog and visibility

The underwater fog must be very close to the player, preventing them from seeing far.

This is a fixed design requirement, not an administrative option.

Do not expose fog distance as a configuration setting.

Do not allow ordinary configuration or admin commands to disable this visibility restriction.

Apply the fog specifically to The Depths.

Ensure the visual effect works correctly in multiplayer.

Implement the closest practical underwater fog behavior supported by Minecraft 1.21.1 and NeoForge.

If client-side rendering code is required, implement it while keeping gameplay state authoritative on the server.

Pressure effects

Simulate extreme underwater pressure using vanilla status effects.

At Y = -36 and below, while the player is in The Depths, apply:

Blindness.

Mining Fatigue.

The trigger depth is fixed at Y = -36. Do not expose this depth as a configuration setting.

Apply effects only when appropriate and avoid repeatedly resetting effect durations in a way that produces unintended behavior.

Giant sea creature

Add a giant, extremely rare creature that inhabits The Depths.

Its behavior must emphasize stalking, sudden attacks, retreat, and uncertainty rather than a conventional boss fight.

Spawn and rarity

It spawns very rarely.

It is associated specifically with The Depths.

It must not become an ordinary recurring enemy.

Fixed damage behavior

The creature must feel overwhelmingly dangerous, even to players wearing full netherite armor or armor from other mods.

The first successful attack should leave a fully armored player barely alive in the intended two-hit scenario.

The second successful attack should kill that player.

The damage must be a fixed amount independent of armor value.

Armor from other mods must not reduce or scale the intended damage.

Do not expose boss damage, attack count, or the intended two-hit profile as configuration options.

Implement the damage mechanism using appropriate Minecraft damage APIs. Account for armor, enchantments, damage modifiers, and other mitigation mechanics as necessary to preserve the intended behavior.

Do not expose these fixed damage rules through configuration or administrative commands.

Hunting sequence

The creature must not attack twice immediately.

Its encounter should follow this pattern:

The creature appears or reveals itself.

It makes one attack.

It retreats away from the player into darkness or fog.

It repositions, circles, or remains elusive.

It returns for another attack after an interval.

It attempts to disappear when the player is no longer looking at it.

The creature should feel as though it is hunting the player rather than simply charging and repeatedly attacking.

Implement an appropriate AI state machine for appearing, attacking, retreating, hiding, repositioning, and reengaging.

The creature should attempt to despawn whenever the player is not looking at it. Its normal encounter consists of one or two attacks before it disappears.

Do not implement this as a purely visual trick. Ensure its AI and despawning logic work correctly as the player moves and the entity unloads or reloads.

I will create the creature's model and textures. Provide exact asset paths and explain any animation requirements.

The Depths spawn restrictions

Ordinary hostile and passive mobs must not spawn naturally in The Depths.

The giant sea creature and rare seafloor organism are the intended exceptions.

Ensure the biome's spawn configuration does not inadvertently populate it with ordinary mobs.

15. Ocean Trench

Create an ocean trench biome featuring:

A higher-than-normal natural Drowned spawn rate.

Increased opportunities to find resources.

Provide configuration for Drowned spawn frequency and appropriate resource-generation rates.

Do not accidentally apply the increased Drowned spawn rate to unrelated ocean biomes.

16. Technical Implementation Standards

World generation

Use appropriate NeoForge 1.21.1 biome registration, configured features, placed features, structure placement, and data-driven world-generation systems.

Enforce these generation relationships:

Ignimbrite Flats only next to Volcanic Badlands.

Dead Reef only immediately adjacent to Sulphuric Spires.

Redwood Forest uses supplied custom redwood structures.

Rootlands uses supplied custom root and thorn structures.

Burnt Forest uses supplied custom burnt structures.

Sulphuric Spires uses supplied custom spire structures.

The Depths has the specified deep water column and seafloor.

Do not claim adjacency restrictions are implemented unless the generation code enforces them to a reasonable degree. Explain technical limitations and implement the strongest practical solution supported by the target version.

Entity AI

Ensure the Root Worm, Banana Slug, Flytrap, and giant sea creature have actual functioning behavior, appropriate AI goals or equivalent state machines, persistence, and multiplayer-safe interactions.

Do not substitute descriptions, placeholder AI, or visual-only behavior for required mechanics.

Data persistence

Persist all necessary entity state, including:

Banana Slug size and ownership.

Flytrap taming and feeding state.

Flytrap digestion state where needed.

Boss encounter state where needed.

Other gameplay state necessary for correct behavior after saving, unloading, and reloading.

Assets and data

Create all required registrations, recipes, loot tables, tags, data files, models, textures, and language entries that can be completed without my custom assets.

Where custom assets are required, provide functional placeholders and precise replacement instructions.

Use the correct resource directory layouts and conventions for Minecraft 1.21.1. Do not copy patterns from another Minecraft version without verifying compatibility.

17. Configuration Rules

Provide configuration options for appropriate mechanics, including:

Biome generation frequency and density.

Structure placement frequency and spacing.

Root Worm spawning and fire behavior.

Root Flower generation and contact damage.

Banana Slug spawning, copper interaction cooldowns, healing, and environmental dryness behavior.

Flytrap spawning, taming, collection cooldown, digestion duration, and target exclusions.

Flytrap slime trap duration and affected entity types.

Fossil generation and resource-generation settings.

Sulphuric Spires generation and gunpowder drops.

Ocean Trench Drowned spawning and resource generation.

Do not expose these fixed requirements as configuration options:

The Depths underwater fog distance.

The Depths pressure trigger at Y = -36.

The giant sea creature's fixed damage behavior.

The intended two-hit lethality profile.

The creature's armor-independent damage.

The rule that Volcanic Badlands contains lava pools only and no flowing lava.

The rule that Dead Reef generates only immediately adjacent to Sulphuric Spires.

The rule that Ignimbrite Flats generates only next to Volcanic Badlands.

Do not add settings that contradict these restrictions.

18. Development Workflow

Work directly in the supplied mod project or repository.

Follow this sequence:

Inspect the existing project, build configuration, and source code.

Verify that the project targets NeoForge 1.21.1.

Identify essential missing information that genuinely prevents implementation. Ask concise questions only when necessary.

Produce a concise implementation plan mapping the requirements to concrete code, registries, data files, and assets.

Implement the project incrementally, completing related features together.

Build and test after each major implementation stage.

Fix compilation errors, missing registrations, invalid data files, and runtime issues before proceeding.

Verify the final project targets NeoForge 1.21.1.

Provide a final implementation summary, build and test results, and any remaining limitations.

Do not stop after writing the plan. Continue implementing the mod.

If the project cannot currently be built because a dependency or development environment is missing, identify the exact blocker, complete work that can be completed independently, and explain precisely what is needed to continue.

Do not claim that something compiles, works in-game, or has been tested unless you actually verified it.

19. Final Deliverables

Provide:

Complete mod source code.

Required registrations, configuration files, recipes, loot tables, tags, and data files.

A complete asset checklist organized by biome and entity.

Exact directories and filenames for custom models, textures, and structure files.

Clear instructions for replacing placeholders with my assets.

A list of configuration settings and their defaults.

A clear distinction between configurable mechanics and fixed design requirements.

Instructions for building and testing the mod.

A list of incomplete mechanics and the specific reason each remains incomplete.

The ultimate goal is a functional, maintainable Minecraft 1.21.1 NeoForge mod that I can continue developing by creating assets and replacing placeholders without needing to become a Java programmer.

Follow the specifications precisely. Do not silently remove, simplify, or replace requested mechanic
