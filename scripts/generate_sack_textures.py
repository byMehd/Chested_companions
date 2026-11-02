"""Generate the compact 22x11 saddlebag palettes used for special pet chests.

Requires Pillow. The source silhouette is the project's own chest texture;
the output keeps its UV layout and only changes the palette/details.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/resources/assets/chestedcompanions/textures/chest/chest.png"
OUTPUT = ROOT / "src/main/resources/assets/chestedcompanions/textures/chest/special"

SOURCE_COLORS = [
    (31, 28, 23), (42, 37, 29), (54, 48, 38), (69, 60, 47),
    (171, 121, 45), (167, 110, 31), (143, 105, 29), (128, 92, 34), (114, 84, 26),
    (165, 165, 165), (113, 113, 113), (134, 134, 134),
]

PALETTES = {
    "ender": [
        (12, 15, 22), (17, 24, 32), (24, 34, 42), (36, 48, 53),
        (52, 79, 84), (43, 67, 74), (36, 58, 68), (28, 47, 59), (20, 35, 47),
        (104, 225, 151), (41, 126, 89), (66, 180, 119),
    ],
    "copper": [
        (44, 32, 25), (60, 42, 29), (79, 54, 35), (105, 70, 43),
        (207, 139, 72), (184, 113, 54), (160, 96, 46), (130, 76, 39), (100, 57, 31),
        (235, 177, 103), (159, 99, 48), (205, 142, 75),
    ],
    "copper_exposed": [
        (39, 35, 29), (53, 47, 36), (69, 63, 45), (91, 83, 53),
        (182, 137, 80), (139, 119, 68), (103, 126, 82), (77, 116, 80), (58, 94, 69),
        (217, 190, 119), (92, 142, 102), (156, 174, 111),
    ],
    "copper_weathered": [
        (28, 39, 40), (35, 52, 50), (43, 68, 63), (53, 89, 77),
        (116, 153, 117), (77, 137, 111), (55, 126, 112), (43, 106, 101), (34, 82, 82),
        (171, 209, 154), (56, 139, 127), (108, 180, 139),
    ],
    "copper_oxidized": [
        (24, 37, 42), (31, 49, 51), (37, 64, 62), (45, 82, 73),
        (109, 177, 139), (70, 151, 125), (52, 134, 119), (41, 110, 103), (32, 86, 83),
        (180, 229, 190), (53, 144, 130), (109, 199, 158),
    ],
}

OUTPUT.mkdir(parents=True, exist_ok=True)
source = Image.open(SOURCE).convert("RGBA")
for name, palette in PALETTES.items():
    colors = dict(zip(SOURCE_COLORS, palette, strict=True))
    result = source.copy()
    pixels = result.load()
    for y in range(result.height):
        for x in range(result.width):
            r, g, b, alpha = pixels[x, y]
            if alpha and (r, g, b) in colors:
                pixels[x, y] = (*colors[(r, g, b)], alpha)

    if name == "ender":
        # A few restrained purple glints make the Ender saddlebag read clearly
        # at in-game scale without copying the vanilla block's oversized atlas.
        for x, y in ((5, 6), (8, 9), (16, 6), (19, 9)):
            pixels[x, y] = (171, 105, 221, 255)

    result.save(OUTPUT / f"{name}.png", optimize=False)
