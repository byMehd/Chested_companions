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


def penetration(first, second):
    # Separating-axis test on the actual posed cube corners, rather than their axis-aligned bounding boxes.
    def points(cube):
        return [cube["c"][offset:offset + 3] for offset in range(0, 24, 3)]
    def sub(a, b):
        return [a[i] - b[i] for i in range(3)]
    def dot(a, b):
        return sum(a[i] * b[i] for i in range(3))
    def cross(a, b):
        return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
    a, b = points(first), points(second)
    edges_a = [sub(a[index], a[0]) for index in (1, 2, 4)]
    edges_b = [sub(b[index], b[0]) for index in (1, 2, 4)]
    axes = [cross(edges[i], edges[(i + 1) % 3]) for edges in (edges_a, edges_b) for i in range(3)]
    axes.extend(cross(x, y) for x in edges_a for y in edges_b)
    depth = math.inf
    for axis in axes:
        length = math.sqrt(dot(axis, axis))
        if length < 0.00001:
            continue
        axis = [value / length for value in axis]
        values_a, values_b = [dot(point, axis) for point in a], [dot(point, axis) for point in b]
        overlap = min(max(values_a), max(values_b)) - max(min(values_a), min(values_b))
        if overlap <= 0:
            return 0.0
        depth = min(depth, overlap)
    return depth


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
                        depth = penetration(chest, limb)
                        if depth > 0.01:
                            overlaps.add((scene, limb["path"], round(depth, 3)))
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
    for scene, path, depth in sorted(overlaps):
        print(f"Attachment overlap to inspect: {scene}, {path}, posed-cube penetration {depth} model units")
    assert not problems, "\n".join(problems)
    print(f"MODEL_PACK_GEOMETRY_PASSED: 8 posed pets, both lids outside the torso, minimum gap {smallest_gap:.3f}")


if __name__ == "__main__":
    main()
