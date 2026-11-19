"""生成森系主题所需的图标：饥饿值鸡腿（满/半/空）+ 4 种食物。

风格与 themes.xml 保持一致（奶油底 + 陶土橙 #E8A87C + 草绿 #7BA05B）。
运行：python tools/gen_food_icons.py
"""
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "android-app", "app", "src", "main", "res", "drawable-nodpi")

MEAT = (232, 168, 124, 255)
MEAT_DARK = (201, 133, 90, 255)
BONE = (255, 248, 236, 255)
EMPTY = (233, 222, 200, 255)
EMPTY_LINE = (208, 195, 170, 255)


def drumstick(size=64, filled=True, half=False):
    """一根鸡腿：椭圆肉 + 圆骨头，向左下伸出。"""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = size / 64.0

    def box(x1, y1, x2, y2):
        return [x1 * s, y1 * s, x2 * s, y2 * s]

    # 骨头
    d.ellipse(box(2, 38, 22, 58), fill=BONE, outline=EMPTY_LINE, width=max(1, int(1.5 * s)))
    d.rectangle(box(10, 32, 22, 46), fill=BONE)

    # 肉（下半部先画，再叠上半部形成"半格"效果）
    d.ellipse(box(14, 6, 58, 42), fill=EMPTY, outline=EMPTY_LINE, width=max(1, int(1.5 * s)))
    if filled or half:
        d.pieslice(box(14, 6, 58, 42), start=90, end=270, fill=MEAT)
        if not half:
            d.ellipse(box(14, 6, 58, 42), fill=MEAT)
    if filled:
        d.ellipse(box(14, 6, 58, 42), fill=MEAT)
        d.arc(box(20, 12, 52, 36), start=200, end=320, fill=MEAT_DARK, width=max(1, int(2 * s)))
    elif half:
        d.arc(box(20, 12, 52, 36), start=200, end=320, fill=MEAT_DARK, width=max(1, int(2 * s)))

    d.ellipse(box(14, 6, 58, 42), outline=MEAT_DARK if filled else EMPTY_LINE,
              width=max(1, int(2 * s)))
    return img


def carrot(size=96):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = size / 96.0
    d.polygon([(48 * s, 90 * s), (30 * s, 34 * s), (66 * s, 34 * s)], fill=(232, 140, 72, 255))
    for i, x in enumerate((40, 48, 56)):
        d.line([(x * s, 44 * s), (x * s + 6 * s, 52 * s)], fill=(206, 116, 54, 255),
               width=max(1, int(2 * s)))
    for dx, dy in ((-8, -14), (0, -20), (8, -14)):
        d.ellipse([(48 + dx - 7) * s, (30 + dy) * s, (48 + dx + 7) * s, (30 + dy + 16) * s],
                  fill=(123, 160, 91, 255))
    return img


def bread(size=96):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = size / 96.0
    d.rounded_rectangle([12 * s, 30 * s, 84 * s, 74 * s], radius=22 * s, fill=(214, 160, 98, 255))
    d.rounded_rectangle([12 * s, 26 * s, 84 * s, 52 * s], radius=16 * s, fill=(232, 186, 130, 255))
    for x in (32, 48, 64):
        d.line([(x * s, 34 * s), (x * s - 4 * s, 48 * s)], fill=(198, 142, 84, 255),
               width=max(1, int(3 * s)))
    return img


def beef(size=96):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = size / 96.0
    d.rounded_rectangle([10 * s, 26 * s, 86 * s, 74 * s], radius=18 * s, fill=(150, 76, 62, 255))
    d.rounded_rectangle([18 * s, 34 * s, 78 * s, 66 * s], radius=12 * s, fill=(176, 96, 78, 255))
    for x1, y1, x2, y2 in ((28, 42, 44, 58), (50, 38, 66, 62)):
        d.line([(x1 * s, y1 * s), (x2 * s, y2 * s)], fill=(214, 168, 150, 255),
               width=max(1, int(3 * s)))
    return img


def golden_apple(size=96):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = size / 96.0
    d.ellipse([16 * s, 28 * s, 80 * s, 88 * s], fill=(240, 190, 74, 255))
    d.ellipse([24 * s, 36 * s, 44 * s, 56 * s], fill=(252, 226, 150, 255))
    d.line([(48 * s, 32 * s), (48 * s, 18 * s)], fill=(140, 104, 60, 255), width=max(1, int(3 * s)))
    d.ellipse([50 * s, 14 * s, 72 * s, 30 * s], fill=(123, 160, 91, 255))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    drumstick(64, filled=True).save(os.path.join(OUT, "ic_hunger_full.png"))
    drumstick(64, filled=False, half=True).save(os.path.join(OUT, "ic_hunger_half.png"))
    drumstick(64, filled=False).save(os.path.join(OUT, "ic_hunger_empty.png"))
    carrot().save(os.path.join(OUT, "food_carrot.png"))
    bread().save(os.path.join(OUT, "food_bread.png"))
    beef().save(os.path.join(OUT, "food_beef.png"))
    golden_apple().save(os.path.join(OUT, "food_golden_apple.png"))
    print("已生成饥饿图标 3 张 + 食物图标 4 张 ->", OUT)


if __name__ == "__main__":
    main()
