# Validation

Local verification completed on 3 November 2026. CI runs the same suites on every push and provides per-run test reports and installable artifacts.

- Minecraft target: 1.21.11.
- Current pinned loaders: Fabric 0.20.1, NeoForge 21.11.48, Forge 61.2.4.
- Minimum loader matrix: Fabric 0.17.3, NeoForge 21.11.0-beta, Forge 61.0.1.
- Java bytecode target: 21. Gradle/Forge build tooling: Java 25.
- Unit cases: configuration persistence/migration/invalid input; chest transfer/drop/invalidation/access; personal Ender inventory sharing and protection; all eight vanilla copper chest items accepted by the pet chest tag; open and close sounds, lid animation and opener tracking; copper aging, waxing and scraping at the vanilla pace; copper golem pickup and its hand-over to vanilla delivery; the remove-chest button with its access, distance and creative rules; pets held still while their chest is open or a golem is about to reach them; torso measurement on the real vanilla cat and kitten meshes.
- Hidden Fabric client: tested locally without Fabric API; window remains invisible throughout creation and startup.
- Five special saddlebag sprites (Ender plus four copper oxidation levels) are packaged at 22×11 pixels and checked by the smoke client and artifact verifier.
- Real dedicated server (`runSmokeServer`, Fabric): a copper golem emptying a sitting cat's copper chest into a placed chest from the vanilla reach while the pet stays seated and does not move, a cat and a wolf with somewhere to go held still while their chest is open, copper chest oxidation and waxing, and chest removal and death drops, failing on the first duplicated or lost item.
- Real client (`runSmokeScreens`, Fabric): every chest type on a cat and a wolf from both flanks with the lid shut and open, sitting and baby pets, a golem visit, and the pet's chest screen, where a real mouse click on the remove-chest button must close the screen and leave the chest and its contents on the ground.

| Check | Result |
| --- | --- |
| Fabric 0.20.1 and 0.17.3 builds | Passed locally and in CI; 65 unit tests |
| Forge 61.2.4 and 61.0.1 builds | Passed in CI; 65 unit tests |
| NeoForge 21.11.48 and 21.11.0-beta builds | Passed in CI; 69 tests including real mixed-in entity persistence, legacy migration and loaded item tags |
| Hidden Fabric client without Fabric API | Passed |
| Hidden Fabric client with Cloth Config, Mod Menu and Fabric API | Passed; configuration screen constructed and initialized |
| Release metadata, mixin registry, copper/Ender saddlebag sprites, optional tags and MCV textures | Passed for all three loaders |
| More Chest Variants + Quad on NeoForge | Passed; all 69 tests, including all twelve optional chest variants |
| More Chest Variants + Quad on hidden Fabric client | Passed; registered items and pale oak texture verified |
| Real dedicated server scenarios | Passed locally and in CI |
| Real client scenarios, including the real click on the remove-chest button | Passed locally (61 screenshots). In CI it warns instead of failing when the runner cannot load the world: see below |
| More Chest Variants 1.5.14 chests (cherry and pale oak) on a cat and a wolf, real client | Rendered correctly |
| Fresh Animations 1.11.2 on EMF 3.3.12 and ETF 7.2.6, real client | Saddlebags on the torso standing, sitting and as a kitten; golem visit renders |
| The same, with Sodium 0.8.14, Iris 1.10.8, Entity Culling 1.11.3 and ImmediatelyFast 1.14.3, real client | Same result, no error from this mod |
| Fat Cat 1.0 with its `pack.mcmeta` patched, real client | Saddlebags on the flanks of the wide torso, standing and sitting adults; kittens not verified |

The original Forge 61.0.0 minimum failed inside Forge's own reconstructed `EntitySelectorParser` sources (ambiguous `Doubles` imports), before compiling this mod. Forge 61.0.1 explicitly fixes its user-development patches and is the declared minimum.

The real-client CI job is best effort when the runner is too slow. On 2 November 2026 GitHub's shared runners stopped loading its software-rendered world in time ("Timeout loading world", the server stuck at "Preparing spawn area: 16%"), for every commit, including one that had passed that morning and was rerun as a control. The job therefore retries once, runs the scenes at a render distance of 3, and raises a warning instead of failing when that timeout is the only problem; any other failure, such as a scenario assertion, still fails it. The unit tests, the dedicated-server scenarios and the hidden-client jobs stay mandatory. Capping the client's frame rate makes it worse, because the gametest ties the server to the client's frames.

Fat Cat 1.0 is refused by Minecraft 1.21.11 as published: its `pack.mcmeta` lacks the format fields the game now requires. The check above used a copy with only that file changed.

Client startup checks do not substitute for a complete in-world multiplayer test. The persistence integration tests use a registry-only ephemeral server with detached entities, so they do not change world chunks or test chunk generation.

Not verified: real clients on NeoForge and Forge (the button and model-placement code is shared source, but only a Fabric client was played), OptiFine (Forge only), kittens under Fat Cat, wolves under rebuilt-model packs other than Fresh Animations, the unavailable Cat Loaf and MiniMePets 1.21.11 binaries, and Eternal Pets snapshot restoration.
