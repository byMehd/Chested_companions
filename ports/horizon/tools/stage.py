"""Export the committed Horizon pin and apply the mod with horizon-mixins.

No game resources or player settings are read. All generated files go to target/.
"""
import argparse
import io
import json
from pathlib import Path
import subprocess
import tarfile

ROOT = Path(__file__).resolve().parents[1]


def run(*args, **kwargs):
    return subprocess.check_output([str(arg) for arg in args], **kwargs)


def stage(horizon, mixins):
    compatibility = json.loads((ROOT / "compatibility.json").read_text())
    pin = compatibility["horizon_revision"]
    revision = run("git", "-C", mixins, "rev-parse", "HEAD", text=True).strip()
    if revision != compatibility["horizon_mixins_revision"]:
        raise ValueError("horizon-mixins revision does not match compatibility.json")
    subprocess.run(["git", "-C", str(mixins), "diff", "--quiet", "HEAD", "--", "crates", "Cargo.toml", "Cargo.lock"], check=True)
    target = ROOT / "target"
    original = target / f"horizon-{pin}"
    marker = original / ".horizon-revision"
    if not original.exists():
        archive_bytes = run("git", "-C", horizon, "archive", "--format=tar", pin)
        original.mkdir(parents=True)
        with tarfile.open(fileobj=io.BytesIO(archive_bytes)) as archive:
            members = {member.name: member for member in archive}
            for member in members.values():
                if member.isdir():
                    continue
                destination = (original / member.name).resolve()
                if not destination.is_relative_to(original.resolve()):
                    raise ValueError("archive path leaves the generated export")
                source = member
                if member.issym():
                    linked = (Path(member.name).parent / member.linkname).as_posix()
                    source = members[linked]
                if not source.isfile():
                    raise ValueError(f"unsupported archive member: {member.name}")
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(archive.extractfile(source).read())
        marker.write_text(pin + "\n")
    if not marker.exists() or marker.read_text().strip() != pin:
        raise ValueError("generated export has no valid revision marker")
    executable = mixins / "target/debug" / ("horizon-mixins.exe" if __import__("os").name == "nt" else "horizon-mixins")
    common = ["--workspace", original, "--mods", ROOT / "mixins"]
    plan = run(executable, "plan", *common, "--out", target / "plan.json", text=True)
    (target / "plan-report.json").write_text(plan)
    result = run(executable, "apply", *common, "--out", target / "game", text=True)
    (target / "stage-report.json").write_text(result)
    print(json.dumps({"game": "26.3", "revision": pin, "stage": str(target / "game")}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--horizon", type=Path, required=True)
    parser.add_argument("--mixins", type=Path, required=True)
    args = parser.parse_args()
    stage(args.horizon.resolve(), args.mixins.resolve())
