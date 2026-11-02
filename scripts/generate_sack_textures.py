"""Generate the compact 22x11 saddlebag textures used for special pet chests.

Requires Pillow. Run from anywhere:

    python scripts/generate_sack_textures.py            # textures only
    python scripts/generate_sack_textures.py --preview  # also refresh docs/special-saddlebags-preview.png

The texture is the UV layout of an 8x8x3 box (top/bottom strips, two thin sides, two
identical 8x8 faces). Rows 0-2 of every face are the lid, rows 3-7 the body, so the
renderer can swing the lid open. Copper variants recolour the project's own chest
silhouette with colours sampled from the vanilla 1.21.11 copper chests; the Ender
saddlebag is drawn from scratch after the vanilla Ender chest (dark teal body, studded
lid, golden clasp and green eye).
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/resources/assets/chestedcompanions/textures/chest/chest.png"
OUTPUT = ROOT / "src/main/resources/assets/chestedcompanions/textures/chest/special"
PREVIEW = ROOT / "docs/special-saddlebags-preview.png"

# Colours of the wooden chest silhouette -> shading roles shared by every copper palette.
ROLE_OF = {
    (31, 28, 23): "C", (42, 37, 29): "G", (54, 48, 38): "B", (69, 60, 47): "A",
    (114, 84, 26): "L", (128, 92, 34): "I", (143, 105, 29): "D", (167, 110, 31): "E", (171, 121, 45): "F",
    (165, 165, 165): "H", (113, 113, 113): "J", (134, 134, 134): "K",
}
# Vanilla copper chests keep the plain grey latch of the wooden chest.
GREY_LATCH = {"H": (165, 165, 165), "J": (113, 113, 113), "K": (134, 134, 134)}

# Shades from darkest (C) to lightest (F), sampled from entity/chest/copper*.png of Minecraft 1.21.11.
COPPER = {
    "copper": dict(C="651c16", G="7d3b26", B="904931", A="9a5038", L="a75a40", I="b26247", D="c26b4c", E="d67b5b", F="e3826c"),
    "copper_exposed": dict(C="50422f", G="61503f", B="796454", A="7f7257", L="947661", I="a87762", D="ba8277", E="c68a7d", F="ce8d83"),
    "copper_weathered": dict(C="284a3f", G="375c50", B="497164", A="6a7147", L="748d57", I="6c975c", D="64a077", E="66a977", F="7ab799"),
    "copper_oxidized": dict(C="28483c", G="3b6655", B="396e59", A="3e816b", L="4c9484", I="4fab90", D="59b292", E="63bc98", F="6ec59f"),
}

ENDER_COLORS = dict(
    a="020e14", b="05151c", c="082a2b", d="10232d", e="132223", f="192328", g="1e2c2e", h="233032",
    j="34494b", l="4b645c", n="274839", o="3c6b60", p="629b6a", q="74b76b", r="17311e", s="cbfcdd",
    t="b69228", u="d0b924", v="e7d143", w="faf1b5", x="decb51",
)
# 8x8 face (front and back): grey studded lid, golden clasp straddling the seam, green eye.
ENDER_FACE = ["fffffffa", "jlhwvhlb", "gbbuxbba", "gdetudea", "gdnppnda", "gcqrrqca", "gdnosnda", "aaaaaaaa"]
ENDER_SIDE = ["ffa", "jhb", "gba", "gda", "gda", "gca", "gda", "aaa"]
ENDER_TOP = ["fffffffa", "jhlhhlhb", "bbbbbbba"]

LABELS = {"ender": "Ender", "copper": "Copper", "copper_exposed": "Exposed",
          "copper_weathered": "Weathered", "copper_oxidized": "Oxidized"}


def rgb(value):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def build_copper(roles):
    result = Image.open(SOURCE).convert("RGBA")
    pixels = result.load()
    colors = {role: rgb(value) for role, value in roles.items()}
    colors.update(GREY_LATCH)
    for y in range(result.height):
        for x in range(result.width):
            r, g, b, alpha = pixels[x, y]
            if alpha and (r, g, b) in ROLE_OF:
                pixels[x, y] = (*colors[ROLE_OF[(r, g, b)]], alpha)
    return result


def build_ender():
    result = Image.new("RGBA", (22, 11), (0, 0, 0, 0))
    pixels = result.load()
    colors = {key: rgb(value) for key, value in ENDER_COLORS.items()}
    for offset_x, offset_y, rows in ((3, 0, ENDER_TOP), (11, 0, ENDER_TOP), (0, 3, ENDER_SIDE),
                                     (3, 3, ENDER_FACE), (11, 3, ENDER_SIDE), (14, 3, ENDER_FACE)):
        for dy, row in enumerate(rows):
            for dx, key in enumerate(row):
                pixels[offset_x + dx, offset_y + dy] = (*colors[key], 255)
    return result


def write_preview(textures):
    scale, columns, margin = 10, 2, 14
    cell_w, cell_h = 22 * scale + margin * 2, 11 * scale + margin * 2 + 12
    rows = (len(textures) + columns - 1) // columns
    sheet = Image.new("RGB", (cell_w * columns, cell_h * rows), (30, 33, 40))
    draw = ImageDraw.Draw(sheet)
    for index, (name, image) in enumerate(textures.items()):
        x, y = index % columns * cell_w + margin, index // columns * cell_h + margin
        draw.text((x, y), LABELS[name], fill=(224, 228, 236))
        big = image.resize((22 * scale, 11 * scale), Image.NEAREST)
        sheet.paste(big, (x, y + 12), big)
    sheet.save(PREVIEW)


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    textures = {"ender": build_ender()}
    textures.update({name: build_copper(roles) for name, roles in COPPER.items()})
    for name, image in textures.items():
        image.save(OUTPUT / f"{name}.png", optimize=False)
    if "--preview" in sys.argv[1:]:
        write_preview(textures)


if __name__ == "__main__":
    main()
