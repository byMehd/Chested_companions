"""Validate release packaging, including absence of development smoke-test code."""
from pathlib import Path
import json
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
    metadata = {'fabric': 'fabric.mod.json', 'neoforge': 'META-INF/neoforge.mods.toml', 'forge': 'META-INF/mods.toml'}[loader]
    assert metadata in names
    if loader == 'fabric':
        mod = json.loads(jar.read(metadata))
        assert mod['id'] == 'chestedcompanions_revived'
        assert mod['name'] == 'Chested Companions: Revived'
        assert 'fabric-api' not in mod['depends'], 'Core mod must work without Fabric API'
    tag = json.loads(jar.read('data/chestedcompanions/tags/item/pet_chest_item.json'))
    assert 'minecraft:ender_chest' in tag['values']
    variants = [value['id'] for value in tag['values'] if isinstance(value, dict)]
    assert len(variants) == 12 and all(value.startswith('lolmcv:') for value in variants)
    for wood in ('oak', 'birch', 'spruce', 'jungle', 'dark_oak', 'acacia', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped'):
        assert f'assets/chestedcompanions/textures/chest/lolmcv/{wood}_chest.png' in names
print(f'Packaging verified: {jars[0]}')
