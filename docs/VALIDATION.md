# Validation

Local verification completed on 2 November 2026. CI provides per-run test reports and installable artifacts.

- Minecraft target: 1.21.11.
- Current pinned loaders: Fabric 0.18.4, NeoForge 21.11.48, Forge 61.2.4.
- Minimum loader matrix: Fabric 0.18.1, NeoForge 21.11.0-beta, Forge 61.0.0.
- Java bytecode target: 21. Gradle/Forge build tooling: Java 25.
- Unit cases: configuration persistence/migration/invalid input, chest transfer/drop/invalidation/access, personal Ender inventory sharing and protection.
- Hidden Fabric client: tested locally without Fabric API; window remains invisible throughout creation and startup.

| Check | Local result |
| --- | --- |
| Fabric 0.18.4 build | Passed; 15 unit tests |
| Forge 61.2.4 build | Passed; 15 unit tests |
| NeoForge 21.11.48 build | Passed; 18 tests including real mixed-in entity persistence and legacy migration |
| Hidden Fabric client without Fabric API | Passed |
| Hidden Fabric client with Cloth Config, Mod Menu and Fabric API | Passed; configuration screen constructed and initialized |
| Release archive metadata, optional tags and original textures | Passed for all three loaders |

Client startup checks do not substitute for a complete in-world multiplayer test. The persistence integration tests use a registry-only ephemeral server with detached entities, so they do not change world chunks or test chunk generation.

Passing a unit test or client startup does not establish every resource-pack pose, third-party combination, or gameplay scenario. Fat Cat, unavailable Cat Loaf/MiniMePets 1.21.11 binaries and Eternal Pets snapshot restoration remain explicit verification limits.
