"""Compare the API the mod touches between two Minecraft or loader versions, to decide what a port needs to test.

  python scripts/api_diff.py minecraft <mapped jar A> <mapped jar B>
  python scripts/api_diff.py loader <package prefix> <source dir> <jars A> <jars B>

`minecraft` dumps every net.minecraft class that src/ names (imports, fully qualified names, mixin targets) and its inner classes
with javap from both mapped jars, and prints the members that differ. B may be 1.21.11: the names it changed are mapped back
with the replacements of stonecutter.gradle.kts. The mapped jars are in .gradle/loom-cache/minecraftMaven once the Fabric
variant of a version has been built.

`loader` does the same for the classes under a package prefix (net.neoforged, net.minecraftforge) that the sources of a loader
name, over ';'-separated lists of jars, for example loaders/neoforge with the NeoForge, FML and bus jars of each version.

A class or member that shows up here and that the mod really uses is what needs a replacement, a condition or a test; when
nothing does, a compile and the unit tests are enough and CI covers the rest.
"""
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def javap_executable():
    candidates = [Path(os.environ["JAVA_HOME"]) / "bin" / "javap"] if os.environ.get("JAVA_HOME") else []
    candidates += sorted((ROOT / "tools").glob("jdk-*/bin/javap*"))
    for candidate in candidates:
        if candidate.exists() or candidate.with_suffix(".exe").exists():
            return str(candidate)
    return shutil.which("javap") or sys.exit("javap not found: set JAVA_HOME or put a JDK in tools/")


def renames():
    """(older name, 1.21.11 name) pairs of the Stonecutter replacements, in dotted and slashed form."""
    script = (ROOT / "stonecutter.gradle.kts").read_text(encoding="utf-8")
    return re.findall(r'replace\("([^"]+)",\s*"([^"]+)"\)', script)


def named_classes(prefix, directories):
    pattern = re.compile(r"\b" + re.escape(prefix) + r"[a-z0-9_.]*[A-Z][A-Za-z0-9_]*")
    names = set()
    for directory in directories:
        for path in (ROOT / directory).rglob("*.java"):
            names.update(pattern.findall(path.read_text(encoding="utf-8")))
    return {name for name in names if not name.endswith("Test")}


def classes_in(jars):
    found = {}
    for jar in jars:
        for entry in zipfile.ZipFile(jar).namelist():
            if entry.endswith(".class"):
                found.setdefault(entry[:-6].replace("/", "."), jar)
    return found


def with_inner(names, available):
    wanted = set()
    for name in names:
        if name in available:
            wanted.add(name)
            wanted.update(c for c in available if c.startswith(name + "$"))
    return wanted


def dump(jars, names):
    """The members of each class (javap -p), without the synthetic names whose numbering changes between builds."""
    members, names = {}, sorted(names)
    for start in range(0, len(names), 40):
        out = subprocess.run([javap_executable(), "-p", "-constants", "-cp", os.pathsep.join(jars), *names[start:start + 40]],
                             capture_output=True, text=True, encoding="utf-8").stdout
        current = None
        for line in out.splitlines():
            if line.startswith("Compiled from") or not line.strip():
                continue
            if not line.startswith(" ") and re.search(r"\b(class|interface|enum|record) ", line):
                current = re.search(r"(?:class|interface|enum|record) ([\w.$]+)", line).group(1)
                members[current] = {line.strip()}
            elif current and line.startswith(" ") and not re.search(r"lambda\$|access\$|\$values|static \{\}", line):
                members[current].add(line.strip())
    return members


def compare(named, jars_a, jars_b, normalise=lambda text: text):
    """Members of the named classes in both sets of jars. `normalise` maps a name to the one both sides are compared under."""
    def members_of(jars):
        available = classes_in(jars)
        # a class is looked up under the name the sources use, or under the older name when the jars predate a move
        chosen = {c for name in named for c in (name, normalise(name)) if c in available}
        return {normalise(cls): {normalise(line) for line in lines} for cls, lines in dump(jars, with_inner(chosen, available)).items()}

    members_a, members_b = members_of(jars_a), members_of(jars_b)
    changed = 0
    for cls in sorted(set(members_a) & set(members_b)):
        only_a, only_b = sorted(members_a[cls] - members_b[cls]), sorted(members_b[cls] - members_a[cls])
        if only_a or only_b:
            changed += 1
            print(f"~ {cls}")
            for line in only_a:
                print(f"    A only: {line}")
            for line in only_b:
                print(f"    B only: {line}")
    for cls in sorted(set(members_a) - set(members_b)):
        print(f"- {cls} (only in A)")
    for cls in sorted(set(members_b) - set(members_a)):
        print(f"+ {cls} (only in B)")
    print(f"{len(set(members_a) & set(members_b))} classes compared, {changed} differ")


def main(arguments):
    if len(arguments) == 3 and arguments[0] == "minecraft":
        pairs = renames()

        def to_older(text):
            for older, newer in pairs:
                text = text.replace(newer, older)
            return text

        compare(named_classes("net.minecraft.", ["src"]), [arguments[1]], [arguments[2]], to_older)
    elif len(arguments) == 5 and arguments[0] == "loader":
        prefix, directory, jars_a, jars_b = arguments[1:]
        compare(named_classes(prefix, [directory]), jars_a.split(";"), jars_b.split(";"))
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
