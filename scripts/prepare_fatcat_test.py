"""Prepare a temporary Fat Cat rendering fixture without redistributing third-party assets.

Fat Cat 1.0 by PixalThePixeled: https://modrinth.com/resourcepack/fat-cat (CC BY-SA 4.0).
Only its pack metadata is widened for testing; the model, animations and textures stay intact.
EMF and ETF use pinned Modrinth releases for the selected Minecraft version.
"""
from io import BytesIO
from pathlib import Path
import hashlib
import json
import sys
import urllib.request
import zipfile

VERSIONS = {
    "1.21.9": ("9Gv6SVe7", "CjQftb2F"),
    "1.21.10": ("9Gv6SVe7", "CjQftb2F"),
    "1.21.11": ("x4GbWVyx", "itjOkghQ"),
    "26.1.2": ("zh2AVI88", "JxWjtiQZ"),
    "26.2": ("hyDf8pNd", "nJCXY7Tp"),
    "26.3": ("vF6qLXsA", "svwZ2o4d"),
}


def download(version):
    request = urllib.request.Request(
        f"https://api.modrinth.com/v2/version/{version}",
        headers={"User-Agent": "byMehd/Chested_companions visual regression tests"},
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        metadata = json.load(response)
    file = next(file for file in metadata["files"] if file["primary"])
    with urllib.request.urlopen(file["url"], timeout=60) as response:
        data = response.read()
    if hashlib.sha512(data).hexdigest() != file["hashes"]["sha512"]:
        raise ValueError(f"Checksum mismatch for {file['filename']}")
    return file["filename"], data


def main():
    minecraft = sys.argv[1]
    directory = Path("run") / f"fatcat-{minecraft}"
    dependencies = directory / "mods"
    dependencies.mkdir(parents=True, exist_ok=True)
    for version in VERSIONS[minecraft]:
        filename, data = download(version)
        (dependencies / filename).write_bytes(data)

    _, pack = download("APZ38UeW")
    (directory / "resourcepacks").mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(BytesIO(pack)) as original, zipfile.ZipFile(
        directory / "resourcepacks/fat-cat-test.zip", "w", zipfile.ZIP_DEFLATED
    ) as fixture:
        for entry in original.infolist():
            data = original.read(entry)
            if entry.filename == "pack.mcmeta":
                metadata = json.loads(data)
                metadata["pack"].pop("pack_format", None)
                metadata["pack"].pop("supported_formats", None)
                metadata["pack"].update(min_format=65, max_format=999)
                data = json.dumps(metadata).encode()
            fixture.writestr(entry, data)
    (directory / "options.txt").write_text(
        'resourcePacks:["vanilla","file/fat-cat-test.zip"]\nincompatibleResourcePacks:[]\n'
    )
    print(f"Prepared Fat Cat 1.0 with EMF/ETF for Minecraft {minecraft}; test-only metadata adjustment")


if __name__ == "__main__":
    main()
