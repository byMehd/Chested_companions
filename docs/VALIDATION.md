# Validation

Local verification completed on 2 November 2026. CI provides per-run test reports and installable artifacts.

- Minecraft target: 1.21.11.
- Current pinned loaders: Fabric 0.20.1, NeoForge 21.11.48, Forge 61.2.4.
- Minimum loader matrix: Fabric 0.17.3, NeoForge 21.11.0-beta, Forge 61.0.1.
- Java bytecode target: 21. Gradle/Forge build tooling: Java 25.
- Unit cases: configuration persistence/migration/invalid input, chest transfer/drop/invalidation/access, personal Ender inventory sharing and protection.
- Hidden Fabric client: tested locally without Fabric API; window remains invisible throughout creation and startup.

| Check | Local result |
| --- | --- |
| Fabric 0.18.4 build | Passed; 15 unit tests |
| Forge 61.2.4 build | Passed; 15 unit tests |
| NeoForge 21.11.48 build | Passed; 19 tests including real mixed-in entity persistence, legacy migration and loaded item tags |
| Hidden Fabric client without Fabric API | Passed |
| Hidden Fabric client with Cloth Config, Mod Menu and Fabric API | Passed; configuration screen constructed and initialized |
| Release archive metadata, optional tags and original textures | Passed for all three loaders |
| More Chest Variants + Quad on NeoForge | Passed; all 19 tests, including all twelve optional chest variants |
| More Chest Variants + Quad on hidden Fabric client | Passed; registered items and pale oak texture verified |

The original Forge 61.0.0 minimum failed inside Forge's own reconstructed `EntitySelectorParser` sources (ambiguous `Doubles` imports), before compiling this mod. Forge 61.0.1 explicitly fixes its user-development patches and is the declared minimum. The first CI run passed the other seven jobs. Final CI reruns with the corrected minimum and packaged Fabric resource modules.

Client startup checks do not substitute for a complete in-world multiplayer test. The persistence integration tests use a registry-only ephemeral server with detached entities, so they do not change world chunks or test chunk generation.

Passing a unit test or client startup does not establish every resource-pack pose, third-party combination, or gameplay scenario. Fat Cat, unavailable Cat Loaf/MiniMePets 1.21.11 binaries and Eternal Pets snapshot restoration remain explicit verification limits.
