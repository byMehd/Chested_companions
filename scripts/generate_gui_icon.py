"""Generate the 12x12 remove-chest button sprites of the pet chest screen.

Requires Pillow. Run from anywhere:

    python scripts/generate_gui_icon.py

A small chest with a red cross on the usual grey button; the highlighted sprite is the hovered state.
"""
from pathlib import Path
from PIL import Image, ImageColor

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/chestedcompanions/textures/gui/sprites"

CHEST = [  # (x, y, colour key) on the 12x12 button
    *[(x, 4, "L") for x in range(2, 8)],
    *[(x, 5, "B") for x in range(2, 8)],
    *[(x, 6, "g" if x in (4, 5) else "B") for x in range(2, 8)],
    *[(x, 7, "B") for x in range(2, 8)],
    *[(x, 8, "D") for x in range(2, 8)],
    (7, 3, "R"), (9, 3, "R"), (8, 4, "R"), (7, 5, "R"), (9, 5, "R"),
]
COLOURS = {"L": "#b97f45", "B": "#8a5a2b", "D": "#5c3a1c", "g": "#d9d9d9", "R": "#d32f2f"}


def rgba(colour: str) -> tuple:
    return ImageColor.getcolor(colour, "RGBA")


def sprite(highlighted: bool) -> Image.Image:
    border, light, shade, face = ("#7fb0ff", "#ffffff", "#8b8b8b", "#dbe8ff") if highlighted else ("#373737", "#ffffff", "#8b8b8b", "#c6c6c6")
    image = Image.new("RGBA", (12, 12), rgba(face))
    for i in range(12):
        for x, y in ((i, 0), (i, 11), (0, i), (11, i)):
            image.putpixel((x, y), rgba(border))
    for i in range(1, 11):
        for x, y, colour in ((i, 1, light), (1, i, light), (i, 10, shade), (10, i, shade)):
            image.putpixel((x, y), rgba(colour))
    for x, y, key in CHEST:
        image.putpixel((x, y), rgba(COLOURS[key]))
    return image


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    sprite(False).save(OUT / "remove_chest.png")
    sprite(True).save(OUT / "remove_chest_highlighted.png")
    print("wrote", OUT)
