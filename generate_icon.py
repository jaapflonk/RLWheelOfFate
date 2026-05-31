"""Generate icon.png for the Wheel of Fate plugin (48x48 wheel logo)."""
from PIL import Image, ImageDraw
import math

SIZE = 48
# Render at 4x then downscale for smooth edges
SCALE = 4
W = SIZE * SCALE

img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# Wheel center / radius (leave room for pointer at top)
cx, cy = W // 2, W // 2 + 1 * SCALE
r_outer = (W // 2) - 2 * SCALE

# 8 segments — colorful fortune-wheel palette
colors = [
    (231, 76, 60),    # red
    (241, 196, 15),   # yellow
    (46, 204, 113),   # green
    (52, 152, 219),   # blue
    (155, 89, 182),   # purple
    (230, 126, 34),   # orange
    (26, 188, 156),   # teal
    (236, 240, 241),  # off-white
]
n = len(colors)
seg = 360 / n
# Start at -90 so the first segment top is at 12 o'clock
start = -90 - seg / 2

bbox = (cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer)
for i, c in enumerate(colors):
    a0 = start + i * seg
    a1 = a0 + seg
    d.pieslice(bbox, a0, a1, fill=c, outline=(30, 30, 30, 255), width=1 * SCALE)

# Outer ring
d.ellipse(bbox, outline=(20, 20, 20, 255), width=2 * SCALE)

# Center hub
hub_r = 5 * SCALE
d.ellipse(
    (cx - hub_r, cy - hub_r, cx + hub_r, cy + hub_r),
    fill=(40, 40, 40, 255),
    outline=(200, 200, 200, 255),
    width=1 * SCALE,
)

# Pointer / indicator at top (triangle)
ptip_y = cy - r_outer - 1 * SCALE
ptri = [
    (cx - 4 * SCALE, cy - r_outer - 5 * SCALE),
    (cx + 4 * SCALE, cy - r_outer - 5 * SCALE),
    (cx, cy - r_outer + 3 * SCALE),
]
d.polygon(ptri, fill=(220, 30, 30, 255), outline=(20, 20, 20, 255))

# Downscale with Lanczos for smooth anti-aliased edges
final = img.resize((SIZE, SIZE), Image.LANCZOS)
final.save("icon.png", "PNG")
print("Wrote icon.png ({}x{})".format(SIZE, SIZE))
