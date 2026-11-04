"""Check the geometry submitted by real client renderers, including animated resource packs.

An open lid's visible outer face must clear the drawn torso. Coplanar side faces with overlapping
areas are rejected. Leg/body overlaps are reported separately: an animated paw crossing a bag
is a different problem from a lid disappearing into the torso or two surfaces z-fighting.
"""
import json
import math
from pathlib import Path
import re
import sys


def bounds(cube):
    points = cube["c"]
    return [(min(points[axis::3]), max(points[axis::3])) for axis in range(3)]


def volume(cube):
    points = cube["c"]
    a, b, c = [[points[3 * corner + axis] - points[axis] for axis in range(3)] for corner in (1, 2, 4)]
    return abs(a[0] * (b[1] * c[2] - b[2] * c[1])
               - a[1] * (b[0] * c[2] - b[2] * c[0])
               + a[2] * (b[0] * c[1] - b[1] * c[0]))


def usable(cube):
    return cube["vis"] and all(math.isfinite(value) for value in cube["c"]) and volume(cube) > 0.001


def side_planes(cube):
    low, high = bounds(cube)[0]
    if all(min(abs(x - low), abs(x - high)) < 0.002 for x in cube["c"][0::3]):
        return (low, high)
    return ()  # A rotated face cannot z-fight with an axis-aligned lid face over an area.


def main():
    rows = {}
    for line in Path(sys.argv[1]).read_text(errors="replace").splitlines():
        if "REVIVED_QA_GEOM " in line:
            row = json.loads(line.split("REVIVED_QA_GEOM ", 1)[1])
            rows[row["scene"], row["lid"]] = row
    expected = {name + baby + sitting for name in ("cat", "wolf")
                for baby in ("", "_baby") for sitting in ("", "_sitting")}
    assert set(rows) == {(scene, lid) for scene in expected for lid in (0.0, 1.0)}, "Missing posed renderer samples"
    problems, overlaps = [], set()
    smallest_gap = math.inf
    for (scene, openness), row in rows.items():
        assert not row.get("missing"), f"{scene}: renderer did not submit saddlebags"
        pet = [cube for cube in row["pet"] if usable(cube)]
        torsos = [cube for cube in pet if (cube["path"] == "/body" or cube["path"].startswith("/body/"))
                  and not re.search(r"(?:^|[/_])(head|mane)\d*(?:$|[/_])", cube["path"], re.I)]
        assert torsos, f"{scene}: no visible torso geometry"
        torso = bounds(max(torsos, key=volume))
        for chest in row["chest"]:
            box = bounds(chest)
            if chest["path"].endswith("_body"):
                for limb in pet:
                    if "leg" not in limb["path"].lower():
                        continue
                    other = bounds(limb)
                    intersection = math.prod(max(0, min(box[axis][1], other[axis][1])
                                                 - max(box[axis][0], other[axis][0])) for axis in range(3))
                    if intersection > 0.005:
                        overlaps.add((scene, limb["path"], round(intersection, 3)))
            if openness != 1.0 or not chest["path"].endswith("_lid"):
                continue
            gap = box[0][1] - torso[0][1] if "left" in chest["path"] else torso[0][0] - box[0][0]
            smallest_gap = min(smallest_gap, gap)
            if gap < 0.01:
                problems.append(f"{scene} {chest['path']}: visible lid face is {gap:.3f} outside the torso")
            for solid in pet:
                other = bounds(solid)
                if any(min(box[axis][1], other[axis][1]) - max(box[axis][0], other[axis][0]) < 0.03
                       for axis in (1, 2)):
                    continue
                if any(abs(a - b) < 0.004 for a in side_planes(chest) for b in side_planes(solid)):
                    problems.append(f"{scene} {chest['path']}: coplanar side face at {solid['path']}")
    for scene, path, intersection in sorted(overlaps):
        print(f"Attachment overlap to inspect: {scene}, {path}, bounding-box volume {intersection}")
    assert not problems, "\n".join(problems)
    print(f"MODEL_PACK_GEOMETRY_PASSED: 8 posed pets, both lids outside the torso, minimum gap {smallest_gap:.3f}")


if __name__ == "__main__":
    main()
