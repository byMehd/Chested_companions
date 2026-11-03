# Validation

Local verification completed on 3 November 2026. CI builds, tests and plays every Minecraft version on every loader for each pull request, and provides per-run test reports and installable artifacts.

- Minecraft targets: 1.21.11, 1.21.10 and 1.21.9, built from one shared source. Each build declares exactly one of them.
- Java bytecode target: 21. Gradle/Forge build tooling: Java 25.

| Minecraft | Fabric (pinned / minimum) | NeoForge (pinned / minimum) | Forge (pinned / minimum) |
| --- | --- | --- | --- |
| 1.21.11 | 0.20.1 / 0.17.3 | 21.11.48 / 21.11.0-beta | 61.2.4 / 61.0.1 |
| 1.21.10 | 0.20.1 / 0.17.3 | 21.10.64 / 21.10.0-beta | 60.1.18 / 60.0.0 |
| 1.21.9 | 0.20.1 / 0.17.3 | 21.9.16-beta / 21.9.0-beta | 59.0.5 / 59.0.0 |

- Unit cases: configuration persistence/migration/invalid input; chest transfer/drop/invalidation/access; personal Ender inventory sharing and protection; all eight vanilla copper chest items accepted by the pet chest tag; open and close sounds, lid animation and opener tracking; copper aging, waxing and scraping at the vanilla pace; copper golem pickup and its hand-over to vanilla delivery; the remove-chest button with its access and distance rules and the chest coming back in every game mode, the screen title naming the pet and its chest; pets held still while their chest is open or a golem is about to reach them; torso measurement on the real vanilla cat, kitten, wolf and pup meshes, and the clearance between the plane an open saddlebag lid stands in and the side of the torso on those four meshes.
- Hidden Fabric client: tested locally without Fabric API; window remains invisible throughout creation and startup.
- Five special saddlebag sprites (Ender plus four copper oxidation levels) are packaged at 22×11 pixels and checked by the smoke client and artifact verifier.
- Real dedicated server (`runSmokeServer`, Fabric): a copper golem emptying a sitting cat's copper chest into a placed chest from the vanilla reach while the pet stays seated and does not move, a cat and a wolf with somewhere to go held still while their chest is open, copper chest oxidation and waxing, and chest removal and death drops, failing on the first duplicated or lost item.
- Real client (`runSmokeScreens`, Fabric): every chest type on a cat and a wolf from both flanks with the lid shut and open, sitting and baby pets, a golem visit, and the pet's chest screen, where a real mouse click on the remove-chest button must close the screen and leave the chest and its contents on the ground.

Every row holds for the three Minecraft versions, locally or in CI, unless it names one.

| Check | Result |
| --- | --- |
| Fabric builds, pinned and minimum loader | Passed; 73 unit tests |
| Forge builds, pinned and minimum loader | Passed; 73 unit tests |
| NeoForge builds, pinned and minimum loader | Passed; 77 tests including real mixed-in entity persistence, legacy migration and loaded item tags |
| Hidden Fabric client without Fabric API | Passed; the mod's assets load through the bundled resource-loader modules |
| Hidden Fabric client with Cloth Config, Mod Menu and Fabric API | Passed; configuration screen constructed and initialized |
| Release metadata, mixin registry, copper/Ender saddlebag sprites, optional tags and MCV textures | Passed for all three loaders |
| More Chest Variants + Quad on NeoForge | Passed on 1.21.11 and 1.21.10; all 77 tests, including all twelve optional chest variants. Not run on 1.21.9: the More Chest Variants build for 1.21.9 and 1.21.10 requires NeoForge 21.10.63 or newer |
| More Chest Variants + Quad on hidden Fabric client | Passed; registered items and pale oak texture verified |
| Forge game test server | Starts the real server with the mod and its mixins |
| Real dedicated server scenarios | Passed |
| Real client scenarios, including the real click on the remove-chest button | Passed locally on 1.21.10 and 1.21.9 (61 screenshots each) and on 1.21.11 (61 before the older versions were added; the cat, golem and chest-screen scenes again after). In CI it warns instead of failing when the runner cannot load the world: see below |
| More Chest Variants 1.5.14 chests (cherry and pale oak) on a cat and a wolf, real client | Rendered correctly |
| Fresh Animations 1.11.2 on EMF 3.3.12 and ETF 7.2.6, real client | Saddlebags on the torso standing, sitting and as a kitten; golem visit renders |
| The same, with Sodium 0.8.14, Iris 1.10.8, Entity Culling 1.11.3 and ImmediatelyFast 1.14.3, real client | Same result, no error from this mod |
| Fat Cat 1.0 with its `pack.mcmeta` patched, real client | Saddlebags on the flanks of the wide torso, standing and sitting adults; kittens not verified |

The original Forge 61.0.0 minimum failed inside Forge's own reconstructed `EntitySelectorParser` sources (ambiguous `Doubles` imports), before compiling this mod. Forge 61.0.1 explicitly fixes its user-development patches and is the declared minimum for 1.21.11.

## How the older versions were validated

1.21.10 was ported first and played in full: unit tests on the three loaders, the real dedicated server, the hidden and the real client on Fabric, NeoForge with More Chest Variants, the Forge game test server. That run found two real problems that only the game could show: a mixin method descriptor still naming 1.21.11's `Cat` package (the client crashed at startup), and, on Fabric, the resource-loader module of that version no longer carrying the mod resource packs (the game started without any of the mod's textures or translations); both are fixed.

1.21.9 was then validated from what differs, rather than by repeating those runs. `scripts/api_diff.py` dumps every Minecraft class the sources name, with its inner classes and the mixin targets among them, from the mapped jars of two versions and compares their members:

| Compared | What differs among the classes the mod names |
| --- | --- |
| Minecraft 1.21.9 and 1.21.10 (256 classes) | Three: the protocol and world version constants, the interface Fabric API injects into `MinecraftServer`, and one private method of `Entity` that takes a new flag. Nothing the mod calls. The community migration primer for 1.21.9 to 1.21.10 lists two small changes, a method added to `HangingEntity` and the extra flag of `BlockBehaviour#entityInside` (the `Entity` method above), neither used here |
| Minecraft 1.21.10 and 1.21.11 (control) | 49 classes: the names and APIs that 1.21.11 changed, which the Stonecutter replacements and conditions cover. It shows the comparison does catch a change |
| NeoForge 21.9.16-beta and 21.10.64, with their FML and bus jars (the 12 classes the loader code names) | Two inner classes of `DeferredRegister` gained helper overloads that the mod does not use. 21.9.16-beta and 21.10.0-beta even share FML 10.0.14 |
| Forge 59 and 60 | Forge's changelog calls 60.0.0 "1.21.10" and lists no API change; 59.0.1 to 59.0.5 only touch events, which the mod does not use |

Where the comparison found nothing the mod uses, a compile and the unit tests stand in for the real runs: NeoForge 21.9.0-beta and 21.9.16-beta built with all 69 tests, which start the real FML and apply the mixins; Fabric 1.21.9 had already been played in full when this approach was chosen; Forge 1.21.9 builds and starts its game test server in CI.

The real-client CI job is best effort when the runner is too slow. On 2 November 2026 GitHub's shared runners stopped loading its software-rendered world in time ("Timeout loading world", the server stuck at "Preparing spawn area: 16%"), for every commit, including one that had passed that morning and was rerun as a control. The job therefore retries once, runs the scenes at a render distance of 3, and raises a warning instead of failing when that timeout is the only problem; any other failure, such as a scenario assertion, still fails it. The unit tests, the dedicated-server scenarios and the hidden-client jobs stay mandatory. Capping the client's frame rate makes it worse, because the gametest ties the server to the client's frames.

Fat Cat 1.0 is refused by Minecraft 1.21.11 as published: its `pack.mcmeta` lacks the format fields the game now requires. The check above used a copy with only that file changed. The four third-party rendering rows above ran on 1.21.11 only.

Client startup checks do not substitute for a complete in-world multiplayer test. The persistence integration tests use a registry-only ephemeral server with detached entities, so they do not change world chunks or test chunk generation.

Not verified: real clients on NeoForge and Forge (the button and model-placement code is shared source, but only a Fabric client was played), third-party model packs and shader mods on 1.21.10 and 1.21.9 (only checked on 1.21.11), OptiFine (Forge only), kittens under Fat Cat, wolves under rebuilt-model packs other than Fresh Animations, the unavailable Cat Loaf and MiniMePets binaries, and Eternal Pets snapshot restoration.
