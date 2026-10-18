# Working in this cloud project

Use the existing `/workspace/ModDev` checkout. Each cloud task is isolated;
do not create a Git worktree unless explicitly requested.

```bash
source /workspace/tooling/activate-neoforge.sh
cd /workspace/ModDev
./gradlew --no-daemon --max-workers=4 build runGameTestServer
python tools/check_resources.py /workspace/tooling/gradle-home/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar
```

The activation helper selects the verified Java 21 JDK, a writable Gradle cache,
and the current cloud HTTPS proxy and its public CA. TLS verification remains on.
Tool refresh is available through `/workspace/tooling/setup-neoforge.sh`.
Outside cloud, use your installed Java 21 and the included Gradle wrapper normally.

Java source is under `src/main/java/dev/riley/wildlands/`, divided into registries,
world generation, entities/navigation, server gameplay, client rendering, items,
blocks, and native tests. Assets/data use the 1.21.1 singular data-folder layout.

`build` compiles/packages; the Gradle `test` task currently has NO-SOURCE.
`runGameTestServer` executes actual registered Minecraft GameTests and checks
runtime initialization. A zero exit alone is insufficient: confirm the final
`All N required tests passed` message. Earlier loader crashes can misleadingly
end with a zero process exit. CI runs the build and native test server.

Edit the shipped resource files directly. Builds never regenerate or overwrite
your art or structure templates.

The prepared workflow has no required background server. `runClient` needs a
working graphical desktop. `runServer` is optional and can require your EULA
acceptance. Use the [admin guide](docs/ADMIN.md) and [asset guide](docs/ASSETS.md)
for gameplay and content editing. `runData` is optional; the complete shipped
resource/data set is already present.
