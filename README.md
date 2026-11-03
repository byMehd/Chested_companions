# Chested Companions: Revived

Independent continuation of **Chested Companions** by [Khajiitos](https://github.com/Khajiitos/ChestedCompanions), targeting **Minecraft 1.21.11** on **Fabric, NeoForge and Forge**. Built with **Stonecutter 0.9.8** from shared Java sources.

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

The mod is required on both client and server. Install exactly one JAR matching your loader. Replace the original mod; do not install both together. Existing worlds should be backed up before moving between Minecraft versions.

## Dependencies

| Platform | Declared range for Minecraft 1.21.11 | Required extra API |
| --- | --- | --- |
| Fabric | Loader 0.17.3 and newer | **No separate API download**; the small resource-loader module and its base are bundled |
| NeoForge | 21.11 family, including beta builds | None |
| Forge | 61.0.1 through 61.x | None |

Builds use pinned versions in `gradle.properties`. The CI also checks the oldest declared versions. A range is not a guarantee about future loader releases; actual verification results are tracked in [docs/VALIDATION.md](docs/VALIDATION.md).

Cloth Config and Mod Menu are optional. A native settings screen is available if Cloth Config is absent; Fabric exposes it through Mod Menu. Their own dependencies apply if you install them. More Chest Variants currently provides 1.21.11 builds for Fabric and NeoForge; its original integration stays optional on Forge too. Cat Loaf has no published 1.21.11 build at the time of this port.

## Build and test

Use Java **25 for Gradle/build tools** and Java **21 for the Minecraft target and tests**. Both JDKs must be available to Gradle. The wrapper downloads Gradle 9.8.2.

```powershell
.\build.ps1
.\build.ps1 :1.21.11-fabric:test
.\build.ps1 :1.21.11-fabric:runSmokeClient
.\build.ps1 :1.21.11-fabric:runSmokeClient -PwithOptionalMods
```

On Linux/macOS, use `./gradlew` with the same task names. `JAVA_HOME` should point to JDK 25; let Gradle detect JDK 21 or pass `-Porg.gradle.java.installations.fromEnv=JAVA_HOME_21_X64,JAVA_HOME_25_X64`.

JARs are written to `versions/1.21.11-<loader>/build/libs/`. Files ending in `-sources.jar` are source archives, not installable mods. GitHub Actions uploads the three loader builds and test reports on every push and pull request.

The smoke-test mod lives in a separate `smoke` source set and is excluded from release JARs. Its GLFW window is hidden before creation. Linux CI runs it under Xvfb. `runSmokeServer` starts a real dedicated server on a throw-away flat world and plays whole scenarios on it: a copper golem emptying a cat's copper chest into a placed chest, copper chest oxidation and waxing, and chest removal and death drops, failing on the first duplicated or lost item.

`runSmokeScreens` does the same on a real client: it saves screenshots of every chest type on a cat and on a wolf (both flanks, lid shut and open), of sitting and baby pets, of a copper golem emptying a pet's chest, and of the pet's chest screen, where it hovers and really clicks the remove-chest button and checks that the chest and its contents land on the ground, into `run/smoke-screens/screenshots`. It needs Fabric API on the runtime classpath:

```powershell
.\build.ps1 :1.21.11-fabric:runSmokeScreens -PwithGameTest
```

To look at a third-party combination, put the mods in `run/<name>/mods`, the resource packs in `run/<name>/resourcepacks`, enable the packs in `run/<name>/options.txt`, and add `-PscreensDir=<name>`. `-PscreensOnly=cats,wolves,sitting,baby,mcv,golem,gui` limits the scene to some of its parts.

## Stonecutter

`settings.gradle.kts` declares the Minecraft/loader variants. `stonecutter.gradle.kts` selects the active project and supplies loader constants. Shared logic and assets are in `src/main`; entrypoints and metadata are in `loaders/<loader>`. Each loader has its own build script.

For example, select the NeoForge development variant with:

```powershell
.\build.ps1 'Set active project to 1.21.11-neoforge'
```

To add another Minecraft version, register its variants in `settings.gradle.kts`, supply matching dependency properties, and add Stonecutter version conditions only where APIs differ. Older original branches are preserved in the local upstream checkout; they are not advertised as new working ports.

## Review and remaining compatibility work

See [docs/AUDIT.md](docs/AUDIT.md) for upstream issues, confirmed defects and what remains unverified. The project deliberately distinguishes compilation, unit tests, hidden client startup and actual gameplay checks.
