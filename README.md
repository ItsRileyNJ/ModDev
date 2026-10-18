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

Create a **new Wildlands world** to use the custom terrain/biome generator.
Existing vanilla worlds keep their existing Overworld generator. The separate
Wildlands realm is available through `/wildlands realm` to operators when loaded.

```bash
# In this prepared cloud environment:
source /workspace/tooling/activate-neoforge.sh
cd /workspace/ModDev
./gradlew --no-daemon --max-workers=4 build runGameTestServer
```

On your desktop, install JDK 21, open this folder in IntelliJ IDEA or Eclipse as
a Gradle project, and use `gradlew.bat build` (Windows) or `./gradlew build`.
The result is `build/libs/wildlands-1.0.0.jar`. Install it in both client and server
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
