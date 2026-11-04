# Chested Companions: Revived

Independent continuation of **Chested Companions** by [Khajiitos](https://github.com/Khajiitos/ChestedCompanions), targeting **Minecraft 1.21.9, 1.21.10, 1.21.11, 26.1.2, 26.2 and 26.3** on **Fabric, NeoForge and Forge**. Built with **Stonecutter 0.9.8** from one shared Java source for every version and loader.

Original code, artwork and translations are distributed under the original [MIT license](LICENSE). See [NOTICE.md](NOTICE.md) for attribution. This project is not an official update by Khajiitos.

## Features

- Equip tamed cats and wolves with chests; preserve the equipped item and its components.
- Equip any of the eight vanilla copper chest variants, from fresh copper through oxidized and waxed states; the pet uses a compact matching saddlebag texture.
- **Copper chests age like placed ones.** They oxidize at the same pace as a placed copper chest (the `randomTickSpeed` game rule applies), honeycomb waxes them so they stop, and an axe scrapes one oxidation stage or removes the wax, exactly like on a block. Contents are never touched.
- **Chests open like vanilla chests.** The screen is titled like a vanilla chest, with the pet and its chest ("Cat - Chest", "Wolf - Ender Chest"). Opening a pet's chest plays the real open and close sound of its type (wooden, Ender or each copper stage), and the saddlebag lids swing open and shut. The wooden-bag geometry and textures are unchanged while the lid is shut, so existing texture packs keep working. A pet whose chest is open, for you or for a golem, stays where it is, sitting or standing.
- **Copper golems use pet chests.** A copper golem near a pet wearing a copper chest walks up, opens the chest with the same animation and sound it uses at a placed copper chest, and carries one stack of up to 16 items away to a chest, as it does from a copper chest block. It walks up to the pet the way it does to a placed chest, and the pet keeps its pose and does not move for the visit. Disable with `copperGolemsUsePetChests`.
- Configure ordinary inventories to 9, 18 or 27 slots independently for cats and wolves.
- Owner-only access by default; optional public access, inverted crouch interaction, feeding priority, baby restrictions and per-animal toggles.
- **Remove a chest** with the button beside the pet's chest screen (a vanilla button showing a crossed-out chest), or with shears (right-click the pet). The chest and everything in it drop to the ground, exactly once, in every game mode. Contents also drop when the pet dies.
- **Ender chests:** access the opening player's actual vanilla Ender inventory (27 slots). Public access still shows each viewer their own inventory. Ender contents never belong to the pet and do not drop when it dies or loses its chest. The pet displays a compact Ender saddlebag texture.
- **Model packs.** The saddlebags are anchored to the torso that is actually drawn, so packs that rebuild the cat with Entity Model Features (Fresh Animations, Better Cats...) keep them on its back, standing, sitting and as a kitten. Checked with Fresh Animations 1.11.2 on EMF 3.3.12 and ETF 7.2.6, alone and together with Sodium, Iris, Entity Culling and ImmediatelyFast. Fat Cat 1.0 is refused by Minecraft 1.21.11 as published (its `pack.mcmeta` lacks the new format fields); with that file patched, adult cats carry their bags correctly.
- Preserve all eleven original **More Chest Variants** textures and add pale oak support through the installed mod's texture.
- Optional Cloth Config screens and Fabric Mod Menu integration; optional chest icons and hiding chest models.
- Preserve original entity save keys and datapack/resource namespaces. Import a legacy `chestedcompanions.json` without modifying it.

The mod is required on both client and server. Install exactly one JAR matching your loader and your Minecraft version (each build declares the one version it was made for). Replace the original mod; do not install both together. Existing worlds should be backed up before moving between Minecraft versions.

## Dependencies

| Minecraft | Game Java | Fabric Loader | NeoForge | Forge |
| --- | --- | --- | --- | --- |
| 1.21.11 | 21 | 0.17.3+ | 21.11.0-beta through 21.11.x | 61.0.1 through 61.x |
| 1.21.10 | 21 | 0.17.3+ | 21.10.0-beta through 21.10.x | 60.0.0 through 60.x |
| 1.21.9 | 21 | 0.17.3+ | 21.9.0-beta through 21.9.x | 59.0.0 through 59.x |
| 26.1.2 | 25 | 0.18.4+ | 26.1.2.0-beta through 26.1.2.x | 64.0.0 through 64.x |
| 26.2 | 25 | 0.18.4+ | 26.2.0.0-beta through 26.2.x | 65.0.0 through 65.x |
| 26.3 | 25 | 0.19.3+ | 26.3.0.0-beta through 26.3.x | 66.0.0 through 66.x |

Fabric bundles its required resource-loading API modules. NeoForge and Forge need no additional API. **Mod version 1.0.1** includes open-lid z-fighting and Fat Cat kitten placement fixes on all supported Minecraft versions. The previously validated 1.0.0 ports remain available separately.

Builds use the pinned versions of `gradle.properties` (Minecraft 1.21.11) and `versions/<minecraft>-<loader>/gradle.properties` (the others). The CI also checks the oldest declared versions. A range is not a guarantee about future loader releases; actual verification results are tracked in [docs/VALIDATION.md](docs/VALIDATION.md).

Cloth Config and Mod Menu are optional. A native settings screen is available if Cloth Config is absent; Fabric exposes it through Mod Menu. Their own dependencies apply if you install them. More Chest Variants provides builds for Fabric and NeoForge (its NeoForge build for 1.21.9 and 1.21.10 needs NeoForge 21.10.63 or newer, so it does not load on NeoForge 1.21.9); its original integration stays optional on Forge too. More Chest Variants and Quad have no published 26.3 build at the time of this port: their tags and textures remain supported, but the optional test runtime does not load an older incompatible binary. Cat Loaf has no published build for these game versions at the time of this port.

## Build and test

Use Java **25 for Gradle/build tools**. Minecraft 1.21 targets and tests use **Java 21**; Minecraft 26 targets and tests use **Java 25**. Both JDKs must be available to Gradle. The wrapper downloads Gradle 9.8.2.

```powershell
.\build.ps1
.\build.ps1 :1.21.11-fabric:test
.\build.ps1 :1.21.10-fabric:runSmokeClient
.\build.ps1 :1.21.9-fabric:runSmokeClient -PwithOptionalMods
```

Every task name starts with the variant it runs on, `<minecraft>-<loader>`; with no argument `build.ps1` builds the three loaders of Minecraft 1.21.11. `-Pminimum` builds against the oldest loader a version declares.

On Linux/macOS, use `./gradlew` with the same task names. `JAVA_HOME` should point to JDK 25; let Gradle detect JDK 21 or pass `-Porg.gradle.java.installations.fromEnv=JAVA_HOME_21_X64,JAVA_HOME_25_X64`.

For a targeted remote check, dispatch **Build and tests** on GitHub with the Minecraft version, validation stage (`build`, `game` or `all`) and loader. The `fatcat` and `freshanimations` visual profiles use pinned EMF/ETF releases and the matching model pack, including Fresh Animations’ dedicated Minecraft 26 baby models. The runs require screenshots and verify the geometry submitted by the real renderer; third-party assets are downloaded for testing and are never bundled in the mod. Pushes and pull requests keep the complete matrix. The workflow only builds JARs and uploads artifacts; it does not publish to CurseForge.

JARs are written to `versions/<minecraft>-<loader>/build/libs/`. Files ending in `-sources.jar` are source archives, not installable mods. For each pull request, GitHub Actions builds and unit tests every loader, plays Fabric client and server scenarios, starts the Forge GameTest server, and uploads the JARs and reports.

The smoke-test mod lives in a separate `smoke` source set and is excluded from release JARs. Its window is hidden before creation, through GLFW up to 26.2 and SDL in 26.3. Linux CI runs it under Xvfb. `runSmokeServer` starts a real dedicated server on a throw-away flat world and plays whole scenarios on it: a copper golem emptying a cat's copper chest into a placed chest, copper chest oxidation and waxing, and chest removal and death drops, failing on the first duplicated or lost item.

`runSmokeScreens` does the same on a real client: it saves screenshots of every chest type on a cat and on a wolf (both flanks, lid shut and open), of sitting and baby pets, of a copper golem emptying a pet's chest, and of the pet's chest screen, where it hovers and really clicks the remove-chest button and checks that the chest and its contents land on the ground, into `run/smoke-screens/screenshots`. It needs Fabric API on the runtime classpath:

```powershell
.\build.ps1 :1.21.11-fabric:runSmokeScreens -PwithGameTest
```

To look at a third-party combination, put the mods in `run/<name>/mods`, the resource packs in `run/<name>/resourcepacks`, enable the packs in `run/<name>/options.txt`, and add `-PscreensDir=<name>`. `-PscreensOnly=cats,wolves,sitting,baby,mcv,golem,gui` limits the scene to some of its parts.

## Stonecutter

`settings.gradle.kts` declares the Minecraft/loader variants. `stonecutter.gradle.kts` selects the active project and supplies loader constants. Shared logic and assets are in `src/main`; entrypoints and metadata are in `loaders/<loader>`. Each loader has its own build script.

The source is written for the newest version (1.21.11); the other versions are generated from it at build time. `stonecutter.gradle.kts` renames what 1.21.11 renamed or moved (`Identifier`, the cat, the copper golem, the game rules, a few models), and the few places where behaviour differs are `//? if >=1.21.11` conditions. What `src/` does not hold is not processed: the loader entrypoints and the NeoForge test must stay version-neutral.

For example, select the NeoForge development variant with the command below. It rewrites `src/` for that variant; select `1.21.11-fabric` again before committing.

```powershell
.\build.ps1 'Set active project to 1.21.10-neoforge'
```

To add another Minecraft version, register its variants in `settings.gradle.kts`, add `versions/<minecraft>-<loader>/gradle.properties` with the matching dependency versions and metadata ranges, compile, and handle the differences with a replacement or a condition. Older original branches are preserved in the local upstream checkout; they are not advertised as new working ports.

Before playing a new version, compare what the sources touch with a version that is already validated: `python scripts/api_diff.py minecraft <mapped jar A> <mapped jar B>` dumps every Minecraft class the sources name (and the mixin targets among them) from both jars and prints the members that differ, and `python scripts/api_diff.py loader ...` does the same for the classes a loader's entrypoints use (the mapped jars are in `.gradle/loom-cache/minecraftMaven` once a version's Fabric variant has been built). Together with the loaders' changelogs, it tells what needs a replacement, a condition or a real test. When nothing the mod uses changed, as between 1.21.10 and 1.21.9, a compile and the unit tests are enough and CI plays the rest.

## Review and remaining compatibility work

See [docs/AUDIT.md](docs/AUDIT.md) for upstream issues, confirmed defects and what remains unverified. The project deliberately distinguishes compilation, unit tests, hidden client startup and actual gameplay checks.
