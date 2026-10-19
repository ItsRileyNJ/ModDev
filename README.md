# Wildlands

Minecraft Java **1.21.1**, NeoForge **21.1.262**, Java **21**.

A standalone mod with thirteen biomes, Root Worms, climbing Banana Slugs,
Flytraps, and the Depth Stalker. This repository contains the implementation,
replaceable placeholder art, native Minecraft GameTests, and administrator guides.
No additional gameplay API JAR is required.

Start with the [asset checklist](docs/ASSETS.md) if you are creating the art.
The [administrator guide](docs/ADMIN.md) explains world creation, configuration,
recipes, commands, ownership, and fixed design rules. The complete configuration
with defaults is [DEFAULT_CONFIG.toml](docs/DEFAULT_CONFIG.toml).

Use the normal **Default** world type. The thirteen Wildlands biomes generate
alongside vanilla biomes in the Overworld; no separate realm or special world
type is needed. In an existing Default world, explore **previously ungenerated
chunks**. Already generated terrain is unchanged. For a dedicated server, keep
`level-type=minecraft:normal`. Superflat and single-biome worlds keep their own
biome sources. With cheats enabled, try `/locate biome wildlands:redwood_forest`.

**Upgrading:** replace the old JAR with `wildlands-1.0.1.jar` on both client and
server. Keep only one Wildlands JAR in each `mods/` folder and restart Minecraft.

```bash
# In this prepared cloud environment:
source /workspace/tooling/activate-neoforge.sh
cd /workspace/ModDev
./gradlew --no-daemon --max-workers=4 build runGameTestServer
```

On your desktop, install JDK 21, open this folder in IntelliJ IDEA or Eclipse as
a Gradle project, and use `gradlew.bat build` (Windows) or `./gradlew build`.
The result is `build/libs/wildlands-1.0.1.jar`. Install it in both client and server
`mods/` folders. Optional development commands: `runClient`, `runServer`, `runData`.
The dedicated server may ask you to read/accept Minecraft's EULA yourself.

**Art still required:** the six custom structure NBT files are explicit empty
markers. They are skipped until you replace them with your roots/thorns/burnt
trees/redwoods/spires. Their biomes do not contain generic replacement trees or
spikes. Creature and block art are deliberately simple replaceable placeholders.
The native JSON renderer supports cuboids and open/digesting flytrap poses;
arbitrary skeletal/keyframe animation import is not implemented.

See [validation and remaining limits](docs/VALIDATION.md),
[architecture](docs/IMPLEMENTATION.md), and the original [request](docs/REQUEST.md).
Source repository: [ItsRileyNJ/ModDev](https://github.com/ItsRileyNJ/ModDev).
