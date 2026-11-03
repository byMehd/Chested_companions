# Upstream audit — 2 November 2026

Baseline: [Khajiitos/ChestedCompanions](https://github.com/Khajiitos/ChestedCompanions), commit `401bb57`, mod 1.3.3 targeting Minecraft 1.21.8. Full original history and branches are kept in the local `upstream/` checkout. The original MIT license and attribution are retained in source and release JARs.

## Published versions

The original repository retains branches for 1.18.2, 1.19.2, 1.20.1, 1.20.4, 1.20.6, 1.21, 1.21.3 and 1.21.4; master targets 1.21.8. [Modrinth](https://modrinth.com/mod/chested-companions/versions) lists 1.20.1, 1.21–1.21.1 and 1.21.3–1.21.8. CurseForge also has older historical files. Loader availability differs by version; three loader labels do not imply every version has every loader.

## Open upstream issues

| Issue | Evidence and treatment |
| --- | --- |
| [#10: missing NeoForge config](https://github.com/Khajiitos/ChestedCompanions/issues/10) | Confirmed: NeoForge's entrypoint omitted `CCConfig.init`. All new entrypoints initialize configuration in the loader's config directory. |
| [#6: config screen crash](https://github.com/Khajiitos/ChestedCompanions/issues/6) | Attached crash shows `AbstractMethodError` for `IConfigScreenFactory.createScreen`. Recompile the native implementation against the 1.21.11 API; also guard optional Cloth Config detection and use the enum's declaring class. A historical 1.21.1 crash is not claimed fixed on that old release. |
| [#8: Fat Cat resource pack](https://github.com/Khajiitos/ChestedCompanions/issues/8) | The pack rebuilds the cat under a moved and rotated `body`, so the old fixed Fresh Animations offset put the chests at the mouth. The layer no longer assumes an offset: it measures the torso that is drawn (see below). Fat Cat 1.0 itself cannot be enabled on 1.21.11 as published, because its `pack.mcmeta` lacks the format fields the game now demands and the game refuses the pack. With that file patched locally, the saddlebags sit on the flanks of the wide torso for standing and sitting adult cats; kittens were not verified to line up. Hide-model and chest-icon options remain available. |
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
- Cats whose model a resource pack rebuilt with Entity Model Features had their saddlebags floating at the head. The chest layer posed the model a second time right before reading it, which undid the animation Entity Model Features runs just before the layers. Rebuilt cats are now read as that mod left them and the bags are placed from the measured torso, scaled for kittens and spread for wider torsos; vanilla cats keep their previous placement. Verified in a real client with Fresh Animations 1.11.2 on EMF 3.3.12 and ETF 7.2.6, alone and together with Sodium, Iris, Entity Culling and ImmediatelyFast.
- Texture lookups cached resource-pack misses permanently. They now use the current resource manager, including after reload.
- Old rendering called immediate-buffer APIs removed in 1.21.11. Chest geometry and icons now use deferred submissions; each pet gets its own submitted model-part transform.
- Release metadata is exact for Minecraft 1.21.11 rather than claiming broad, untested Minecraft compatibility. Original unrelated manifest values are removed.

## Duplication history and limits

Upstream [#7](https://github.com/Khajiitos/ChestedCompanions/issues/7) is already closed and 1.3.3 introduced validity checks for unloading/shearing. These checks are preserved and strengthened, with regression tests for detached containers and stack transfer. Upstream [#5](https://github.com/Khajiitos/ChestedCompanions/issues/5) involves Eternal Pets restoring periodic snapshots taken before death. This external snapshot behavior is not something local container clearing can universally fix; no compatibility claim is made without an actual compatible release and reproduction.

## Optional integrations

[More Chest Variants](https://modrinth.com/mod/more-chest-variants-lieonlion) publishes 1.21.11 Fabric and NeoForge builds. Original optional item-tag entries and all eleven custom textures are retained; pale oak is added using its existing standard chest texture. Missing mods do not prevent startup. [Cat Loaf](https://modrinth.com/mod/cat-loaf) currently stops at 1.21.3; its original pose handling remains, without declaring an unsupported binary dependency.

Minecraft 1.21.11 also includes eight vanilla copper chest items. They are accepted as pet chests. The four oxidation-state visuals use compact 22×11 saddlebag textures; waxed items retain the matching oxidation-state appearance, as in vanilla.

## Copper pets, golems and chest feedback

These behaviours do not exist upstream, so each lists what it touches and why it cannot duplicate or lose an item.

- **Aging.** The equipped copper chest ages on the server with the chance of a placed copper block: `randomTickSpeed / 4096` per tick, times the vanilla `0.05688889` factor, times `0.75` for the first stage (about 32,000 and 24,000 ticks on average at the default speed). The chest item is replaced with `transmuteCopy`, which keeps its components, and the container is never touched, so an oxidation step cannot lose or double contents. Waxed chests do not age. Honeycomb waxes (one item, none in creative), an axe scrapes one stage or, with nothing to scrape, removes the wax; each plays the vanilla level event and sound.
- **Golems.** `PetChestPickupBehavior` is an extra copper golem behaviour that runs before the vanilla transport behaviour. It walks to a pet wearing a copper chest, opens the pet's container as an opener (so the lid event, sound and game events are those of a real chest), plays the vanilla "getting item" animation and sound for the same 60 ticks, then takes the first non-empty slot with `Container#removeItem` (one stack of at most 16, the same pick as vanilla) into the golem's hand. It reaches the pet from the distance vanilla uses at a placed chest (one block once its path is finished); the natural range was kept on purpose. Delivery to a chest stays with vanilla code, which yields while the golem works on a pet and the other way round. A golem that dies keeps its guaranteed hand drop. Ender chests are never reachable: the golem only targets copper chests, and their contents belong to the viewing player, not the pet.
- **Sounds and lid.** The first opener plays the open sound of the chest type and the last one leaving plays the close sound (spectators stay silent, as in vanilla). Clients animate the lid from two entity events; removing the chest closes it silently and forgets every opener. The closed geometry is identical to the single box older versions drew, so existing resource packs keep working.
- **Holding pets still.** While anybody has a pet's chest open, a player or a golem, and while a golem is about to reach it, `LivingEntity#isImmobile` is true for that pet (never while it is hurt, burning or swimming). An immobile entity does not run its AI, so no goal is interrupted: a sitting pet stays seated instead of standing up and sitting down again, and a standing one does not wander off from an open screen.
- **Removal and drops.** Shears remove the chest, as before, and so does the button beside the pet's chest screen (an ordinary vanilla button showing a crossed-out chest). The screen is a vanilla chest screen whose title is built by a translation from the pet's name and its chest's name ("Cat - Chest"); the client recognises that title, adds the button, and sends the vanilla menu button packet, which the pet's menu answers after the same access, distance and liveness checks as opening the chest. Ordinary contents drop on removal and on death, exactly once, and the chest drops too, in every game mode; Ender contents never drop because they were never the pet's.
- **Hooks added.** `Cat`/`Wolf`/`TamableAnimal` (state, tick, events, a freeze goal), `LivingEntity#isImmobile` (one injection that only answers for pets), `CopperGolem`, `CopperGolemAi` and `TransportItemsBetweenContainers` (three small mixins that add a flag and the extra behaviour, and make the vanilla transport behaviour yield), `AbstractContainerScreen#init` on the client (adds the button to pet screens only), and the existing renderer and render-state mixins now also carry the lid opening. All are covered by unit tests on every loader and by the real dedicated-server and real-client scenarios described in [VALIDATION.md](VALIDATION.md).

## Forge and NeoForge packaging

Separate artifacts are maintained. They use different entrypoints and different pet-sync implementations: native attachments on NeoForge and tracked entity data on Forge/Fabric. Simply merging these JARs would collide on the differently compiled mixin classes. Both artifacts compile the same shared source using Stonecutter conditions. A universal bootstrap and selective mixin loading could be investigated separately; no unverified combined artifact is advertised.

NeoForge's actual JUnit startup found its new rejection of foreign `SynchedEntityData.defineId` calls. This port uses a synced, non-persisted attachment there, with the optional ItemStack stream codec supporting empty stacks. Persistence remains in the original entity keys. All nineteen NeoForge tests pass after this change.
