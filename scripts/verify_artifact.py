"""Validate release packaging, including absence of development smoke-test code."""
from pathlib import Path
import json
import struct
import sys
import zipfile

loader = sys.argv[1]
directory = Path(f'versions/1.21.11-{loader}/build/libs')
jars = [p for p in directory.glob('*.jar') if not any(x in p.stem for x in ('sources', '-dev', '-plain'))]
assert len(jars) == 1, f'Expected one installable JAR, found {jars}'
with zipfile.ZipFile(jars[0]) as jar:
    names = jar.namelist()
    assert 'LICENSE' in names and 'NOTICE.md' in names
    assert not any('/smoke/' in name or 'revived-smoke' in name for name in names), 'Development tests leaked into release'
    assert 'chestedcompanions.mixins.json' in names
    mixins = json.loads(jar.read('chestedcompanions.mixins.json'))
    for mixin in ('entity.TamableAnimalMixin', 'entity.LivingEntityMixin', 'golem.CopperGolemAiMixin', 'golem.CopperGolemMixin', 'golem.TransportItemsBetweenContainersMixin'):
        assert mixin in mixins['mixins'], f'Missing mixin: {mixin}'
    assert 'gui.AbstractContainerScreenMixin' in mixins['client'], 'Missing client mixin: gui.AbstractContainerScreenMixin'
    for language in ('en_us', 'fr_fr', 'pl_pl'):
        assert f'assets/chestedcompanions/lang/{language}.json' in names, f'Missing translation: {language}'
    metadata = {'fabric': 'fabric.mod.json', 'neoforge': 'META-INF/neoforge.mods.toml', 'forge': 'META-INF/mods.toml'}[loader]
    assert metadata in names
    if loader == 'fabric':
        mod = json.loads(jar.read(metadata))
        assert mod['id'] == 'chestedcompanions_revived'
        assert mod['name'] == 'Chested Companions: Revived'
        assert 'fabric-api' not in mod['depends'], 'Core mod must work without Fabric API'
        assert any(name.startswith('META-INF/jars/fabric-resource-loader-v1-') for name in names), 'Standalone resource loader must be bundled'
        assert any(name.startswith('META-INF/jars/fabric-api-base-') for name in names), 'The resource loader base dependency must also be bundled'
    tag = json.loads(jar.read('data/chestedcompanions/tags/item/pet_chest_item.json'))
    assert 'minecraft:ender_chest' in tag['values']
    copper_variants = {
        'minecraft:copper_chest', 'minecraft:exposed_copper_chest', 'minecraft:weathered_copper_chest',
        'minecraft:oxidized_copper_chest', 'minecraft:waxed_copper_chest',
        'minecraft:waxed_exposed_copper_chest', 'minecraft:waxed_weathered_copper_chest',
        'minecraft:waxed_oxidized_copper_chest',
    }
    tag_ids = {value if isinstance(value, str) else value['id'] for value in tag['values']}
    assert copper_variants.issubset(tag_ids), 'A vanilla copper chest variant is missing from the pet chest tag'
    for texture in ('ender', 'copper', 'copper_exposed', 'copper_weathered', 'copper_oxidized'):
        texture_path = f'assets/chestedcompanions/textures/chest/special/{texture}.png'
        assert texture_path in names, f'Missing saddlebag texture: {texture}'
        png = jar.read(texture_path)
        assert png[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', png[16:24]) == (22, 11), f'Invalid sack texture dimensions: {texture}'
    variants = [value['id'] for value in tag['values'] if isinstance(value, dict)]
    assert len(variants) == 12 and all(value.startswith('lolmcv:') for value in variants)
    for wood in ('oak', 'birch', 'spruce', 'jungle', 'dark_oak', 'acacia', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped'):
        assert f'assets/chestedcompanions/textures/chest/lolmcv/{wood}_chest.png' in names
print(f'Packaging verified: {jars[0]}')
