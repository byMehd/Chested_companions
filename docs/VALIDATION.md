# Validation

## Supported variants

Version **1.0.1** uses one Stonecutter source for all six Minecraft versions and three loaders. The 1.0.0 baseline passed all 66 jobs of [run 41710086399](https://github.com/byMehd/Chested_companions/actions/runs/41710086399); its artifacts remain separate from 1.0.1.

| Minecraft | Java | Fabric pinned / minimum | NeoForge pinned / minimum | Forge pinned / minimum |
| --- | --- | --- | --- | --- |
| 1.21.9 | 21 | 0.20.1 / 0.17.3 | 21.9.16-beta / 21.9.0-beta | 59.0.5 / 59.0.0 |
| 1.21.10 | 21 | 0.20.1 / 0.17.3 | 21.10.64 / 21.10.0-beta | 60.1.18 / 60.0.0 |
| 1.21.11 | 21 | 0.20.1 / 0.17.3 | 21.11.48 / 21.11.0-beta | 61.2.4 / 61.0.1 |
| 26.1.2 | 25 | 0.20.1 / 0.18.4 | 26.1.2.126 / 26.1.2.0-beta | 64.1.6 / 64.0.0 |
| 26.2 | 25 | 0.20.1 / 0.18.4 | 26.2.0.93 / 26.2.0.0-beta | 65.1.6 / 65.0.0 |
| 26.3 | 25 | 0.20.1 / 0.19.3 | 26.3.0.101 / 26.3.0.0-beta | 66.1.5 / 66.0.0 |

Loader ranges allow subsequent releases in the same compatible family; they do not claim that future binaries have already been tested. Forge and NeoForge use separate JARs.

## Automated checks

The [Build and tests workflow](https://github.com/byMehd/Chested_companions/actions/workflows/build.yml) runs the complete matrix for pull requests and `main`. Manual inputs select a Minecraft version, loader and validation stage without repeating the entire matrix.

- Every loader builds on its minimum and pinned version. Unit cases cover inventory transfer and invalidation, access rules, personal Ender storage, copper aging/waxing/scraping, golem pickup, chest removal, configuration and renderer geometry. NeoForge also runs four real mixed-in entity persistence/tag cases using the test server framework.
- Three Fabric startup profiles check minimum/current loaders and optional integrations. The native window must remain hidden, assets must load and configuration screens must initialize.
- Fabric dedicated-server scenarios exercise actual golem delivery, copper transformations, open-pet immobility, removal and death drops, failing on lost or duplicated items. The pinned Forge build starts a real GameTest server.
- Vanilla Fabric clients photograph all chest textures, open and closed lids, both flanks, sitting adults and babies, a golem visit and an actual click on the remove button: **67 screenshots**. A rendering run passes only after its scenes execute and all expected images exist.
- Artifact checks verify loader metadata, the exact game target, licenses, mixins, translations, copper/Ender saddlebag sprites, optional MCV tags/textures, and the exclusion of development smoke code.

Minecraft 26 uses Java 25 and unobfuscated toolchains. Its tests initialize item components from world/reloadable registries. NeoForge 26.3 copper tests load real server data maps. Standalone axe fixtures supply the vanilla axe tag after the removal of `AxeItem`.

## Rendering fixes in 1.0.1

The kitten and pup models introduced in Minecraft 26 have wider torsos than scaled adult models. Tests place the actual layers on baked standing and sitting baby models and verify both flanks. The model root transform and drawn cube vertices are included in clearance measurements. A buried lid is moved outside the torso, rather than only avoiding coincident surfaces.

Torso selection excludes named heads and manes when a body candidate exists. This prevents a Fat Cat kitten’s enlarged head, or Fresh Animations’ wolf mane, from becoming the bag anchor. Head-only or unnamed models retain a fallback. The closed bag’s geometry and existing chest textures are preserved.

The manual `fatcat` profile requires **38 screenshots**. The `freshanimations` profile requires **62 screenshots**, using its dedicated 26.x pack on Minecraft 26 and its legacy pack on 1.21. Each also samples the real renderer for eight combinations of animal, age and sitting pose, with lids closed and open. `check_modelpack_geometry.py` checks visible outer lid faces against the drawn torso and rejects overlapping coplanar side faces. Paw contacts are inspected separately with a separating-axis test on posed cubes; they are not mistaken for lid z-fighting.

[Fresh Animations 26.3 run 41748246622](https://github.com/byMehd/Chested_companions/actions/runs/41748246622) produced all 62 images and passed the posed-geometry checks, with a minimum outer-face clearance of 0.063 model units. The same run passed both Fabric builds; [1.21.11 build checks](https://github.com/byMehd/Chested_companions/actions/runs/41748250427) passed on its minimum and current loader.

Fat Cat 1.0 by PixalThePixeled is [CC BY-SA 4.0](https://modrinth.com/resourcepack/fat-cat). Its published metadata is too old for current Minecraft, so only the test copy’s pack metadata is adjusted. Fresh Animations 1.11.2 by FreshLX is downloaded from its [official project](https://modrinth.com/resourcepack/fresh-animations) without changing its models or textures. Pinned EMF/ETF versions and checksum validation are in the preparation script. No third-party pack or mod binary is committed or distributed with this mod.

## Limits

Fresh Animations may move a paw into a saddlebag during some animations. One standing adult wolf pose showed a small hind-paw contact; the tested baby pose did not have a solid-body intersection. Avoiding all moving-limb contacts would require moving bags away from the animal or changing the pack’s animation. This remains a cosmetic limitation, distinct from a hidden lid or z-fighting.

Graphical checks use Fabric clients. Real Forge/NeoForge clients, multiplayer under every rendering combination, shaders, OptiFine, Cat Loaf, MiniMePets and Eternal Pets snapshot restoration are not claimed verified. The shared rendering source and loader-specific builds do not substitute for those tests.

More Chest Variants and Quad have no published Minecraft 26.3 binary at the time of this release. Their optional tags and textures remain packaged; 26.3 tests do not load an incompatible older binary. Earlier optional checks use the corresponding published versions.

Hosted software rendering can take longer to load worlds with animated packs. The smoke harness allows three minutes instead of one for world loading, still fails on timeout, and keeps this change out of release JARs. Screenshots and logs are retained as CI artifacts; a green startup check alone is never described as a completed visual test.
