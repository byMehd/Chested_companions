# Upstream audit — 2 November 2026

Baseline: [Khajiitos/ChestedCompanions](https://github.com/Khajiitos/ChestedCompanions), commit `401bb57`, mod 1.3.3 targeting Minecraft 1.21.8. Full original history and branches are kept in the local `upstream/` checkout. The original MIT license and attribution are retained in source and release JARs.

## Published versions

The original repository retains branches for 1.18.2, 1.19.2, 1.20.1, 1.20.4, 1.20.6, 1.21, 1.21.3 and 1.21.4; master targets 1.21.8. [Modrinth](https://modrinth.com/mod/chested-companions/versions) lists 1.20.1, 1.21–1.21.1 and 1.21.3–1.21.8. CurseForge also has older historical files. Loader availability differs by version; three loader labels do not imply every version has every loader.

## Open upstream issues

| Issue | Evidence and treatment |
| --- | --- |
| [#10: missing NeoForge config](https://github.com/Khajiitos/ChestedCompanions/issues/10) | Confirmed: NeoForge's entrypoint omitted `CCConfig.init`. All new entrypoints initialize configuration in the loader's config directory. |
| [#6: config screen crash](https://github.com/Khajiitos/ChestedCompanions/issues/6) | Attached crash shows `AbstractMethodError` for `IConfigScreenFactory.createScreen`. Recompile the native implementation against the 1.21.11 API; also guard optional Cloth Config detection and use the enum's declaring class. A historical 1.21.1 crash is not claimed fixed on that old release. |
| [#8: Fat Cat resource pack](https://github.com/Khajiitos/ChestedCompanions/issues/8) | Pack changes the model's body hierarchy and pivots. Existing Fresh Animations adjustment is preserved. Fat Cat positioning still requires visual verification; it is not treated as a proven fix. Hide-model and chest-icon options remain available. |
| [#4: MiniMePets](https://github.com/Khajiitos/ChestedCompanions/issues/4) | Historical 1.20.1 renderer interaction. MiniMePets replaces pet rendering in a cancellable parent renderer injection. No compatible 1.21.11 release was found. Its source is read for diagnosis only and is not redistributed. |
| [#9: Discord question](https://github.com/Khajiitos/ChestedCompanions/issues/9) | Support question, not a bug. |

The [CurseForge comments page](https://www.curseforge.com/minecraft/mc-mods/chested-companions/comments) was checked, but its comment bodies were not exposed by the retrieved page. This audit does not claim to have reviewed unavailable comments.

## Confirmed code problems addressed

- Chest removal spawned live inventory stack references without clearing the detached container. Removal now invalidates the pet's menu source before drops and removes each stack from the old container.
- Resizing reused stack references while leaving the old container populated. Moving items now empties the old container, including overflow dropped when shrinking.
- Open containers did not recheck ownership. Every validity check now includes current access, a living and loaded pet, chest presence, matching world and interaction distance.
- Public mode could permit equipping wild animals. Taming is now always required.
- A missing container could reach `ChestMenu`; menu construction now validates access and creates a missing ordinary inventory.
- Malformed/empty JSON, null fields and non-string enum values could abort config loading. Invalid values now fall back safely, malformed files remain intact, and writes use UTF-8 plus atomic replacement where supported.
- Client detection referenced a Cloth Config demo class. It now checks the public API without eagerly initializing optional classes.
- Texture lookups cached resource-pack misses permanently. They now use the current resource manager, including after reload.
- Old rendering called immediate-buffer APIs removed in 1.21.11. Chest geometry and icons now use deferred submissions; each pet gets its own submitted model-part transform.
- Release metadata is exact for Minecraft 1.21.11 rather than claiming broad, untested Minecraft compatibility. Original unrelated manifest values are removed.

## Duplication history and limits

Upstream [#7](https://github.com/Khajiitos/ChestedCompanions/issues/7) is already closed and 1.3.3 introduced validity checks for unloading/shearing. These checks are preserved and strengthened, with regression tests for detached containers and stack transfer. Upstream [#5](https://github.com/Khajiitos/ChestedCompanions/issues/5) involves Eternal Pets restoring periodic snapshots taken before death. This external snapshot behavior is not something local container clearing can universally fix; no compatibility claim is made without an actual compatible release and reproduction.

## Optional integrations

[More Chest Variants](https://modrinth.com/mod/more-chest-variants-lieonlion) publishes 1.21.11 Fabric and NeoForge builds. Original optional item-tag entries and all eleven custom textures are retained; pale oak is added using its existing standard chest texture. Missing mods do not prevent startup. [Cat Loaf](https://modrinth.com/mod/cat-loaf) currently stops at 1.21.3; its original pose handling remains, without declaring an unsupported binary dependency.

Minecraft 1.21.11 also includes eight vanilla copper chest items. They are accepted as pet chests. The four oxidation-state visuals use compact 22×11 saddlebag textures; waxed items retain the matching oxidation-state appearance, as in vanilla.

## Forge and NeoForge packaging

Separate artifacts are maintained. They use different entrypoints and different pet-sync implementations: native attachments on NeoForge and tracked entity data on Forge/Fabric. Simply merging these JARs would collide on the differently compiled mixin classes. Both artifacts compile the same shared source using Stonecutter conditions. A universal bootstrap and selective mixin loading could be investigated separately; no unverified combined artifact is advertised.

NeoForge's actual JUnit startup found its new rejection of foreign `SynchedEntityData.defineId` calls. This port uses a synced, non-persisted attachment there, with the optional ItemStack stream codec supporting empty stacks. Persistence remains in the original entity keys. All nineteen NeoForge tests pass after this change.
