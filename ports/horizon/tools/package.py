"""Build the source bundle with embedded, owned assets as a native .hnmod."""
import hashlib
import json
from pathlib import Path
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def package():
    compatibility = json.loads((ROOT / "compatibility.json").read_text())
    bundle = ROOT / "mixins/chested_companions"
    manifest = tomllib.loads((bundle / "horizon-mixin.toml").read_text())["mod"]
    if compatibility["game"] != "26.3":
        raise ValueError("this port targets Minecraft 26.3")
    native = dict(schema=1, id=manifest["id"], version=manifest["version"], game="26.3",
                  horizon=compatibility["horizon_revision"], mixins=compatibility["horizon_mixins_revision"])
    files = {"horizon-native.json": (json.dumps(native, indent=2, sort_keys=True) + "\n").encode()}
    for path in sorted(bundle.rglob("*")):
        if path.is_file() and (path.suffix in (".rs", ".toml") or path.name in ("LICENSE", "NOTICE.md") or path.relative_to(bundle).parts[0] == "data"):
            if path.is_symlink():
                raise ValueError(f"bundle symlink: {path}")
            files[path.relative_to(ROOT).as_posix()] = path.read_bytes()
    for name, data in files.items():
        if Path(name).suffix in (".rs", ".toml", ".json", ".md") or name.endswith("/LICENSE"):
            files[name] = data.replace(b"\r\n", b"\n")
    output = ROOT / "target" / f"chested_companions-{manifest['version']}+26.3.hnmod"
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w") as archive:
        for name, data in sorted(files.items()):
            info = zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, data)
    report = dict(file=str(output), sha256=hashlib.sha256(output.read_bytes()).hexdigest(), entries=len(files))
    print(json.dumps(report, indent=2))
    return output


if __name__ == "__main__":
    package()
