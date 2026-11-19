"""生成 6 个形象占位图（像素风，256x256 透明底）。

为什么用脚本画而不是找图：picture/ 下那批是残缺的图集切片，不能直接用；
先用统一规格的占位图把"解锁 / 切换"的逻辑跑通，后面换真素材只要替换同名文件。
运行：python tools/gen_characters.py
"""
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "android-app", "app", "src", "main", "res", "drawable-nodpi")
SCALE = 8          # 32x32 逻辑像素，放大 8 倍 = 256
GRID = 32


def px(d, x, y, w, h, color):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def draw_character(body, belly, ear, accent, ear_type="round", muzzle=None):
    img = Image.new("RGBA", (GRID, GRID), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # 耳朵（先画，压在头后面）
    if ear_type == "round":
        px(d, 8, 3, 5, 5, ear)
        px(d, 19, 3, 5, 5, ear)
    elif ear_type == "point":
        px(d, 8, 2, 4, 6, ear)
        px(d, 20, 2, 4, 6, ear)
        px(d, 9, 1, 2, 2, ear)
        px(d, 21, 1, 2, 2, ear)
    elif ear_type == "long":
        px(d, 10, 1, 3, 8, ear)
        px(d, 19, 1, 3, 8, ear)
    elif ear_type == "none":
        pass

    # 头
    px(d, 8, 5, 16, 12, body)
    px(d, 9, 4, 14, 14, body)

    # 身体
    px(d, 10, 16, 12, 12, body)
    px(d, 9, 19, 14, 8, body)

    # 肚子
    px(d, 12, 19, 8, 7, belly)

    # 脚
    px(d, 10, 27, 5, 2, accent)
    px(d, 17, 27, 5, 2, accent)

    # 眼睛
    px(d, 12, 9, 2, 3, (30, 30, 34, 255))
    px(d, 18, 9, 2, 3, (30, 30, 34, 255))
    px(d, 12, 9, 1, 1, (255, 255, 255, 255))
    px(d, 18, 9, 1, 1, (255, 255, 255, 255))

    # 嘴 / 鼻子
    if muzzle:
        px(d, 14, 13, 4, 2, muzzle)
    px(d, 15, 15, 2, 1, accent)

    # 腮红
    px(d, 10, 12, 2, 1, (255, 170, 170, 200))
    px(d, 20, 12, 2, 1, (255, 170, 170, 200))

    return img.resize((GRID * SCALE, GRID * SCALE), Image.NEAREST)


CHARACTERS = {
    "char_panda": dict(body=(245, 245, 248, 255), belly=(255, 255, 255, 255),
                       ear=(38, 38, 44, 255), accent=(38, 38, 44, 255),
                       ear_type="round", muzzle=(38, 38, 44, 255)),
    "char_duck": dict(body=(255, 214, 92, 255), belly=(255, 236, 170, 255),
                      ear=(255, 214, 92, 255), accent=(240, 150, 40, 255),
                      ear_type="none", muzzle=(240, 150, 40, 255)),
    "char_frog": dict(body=(108, 196, 108, 255), belly=(198, 236, 178, 255),
                      ear=(108, 196, 108, 255), accent=(60, 140, 70, 255),
                      ear_type="none", muzzle=(60, 140, 70, 255)),
    "char_penguin": dict(body=(58, 74, 96, 255), belly=(250, 250, 252, 255),
                         ear=(58, 74, 96, 255), accent=(240, 160, 60, 255),
                         ear_type="none", muzzle=(240, 160, 60, 255)),
    "char_bear": dict(body=(168, 122, 84, 255), belly=(226, 190, 152, 255),
                      ear=(140, 98, 66, 255), accent=(120, 82, 54, 255),
                      ear_type="round", muzzle=(226, 190, 152, 255)),
    "char_zombie": dict(body=(132, 168, 122, 255), belly=(160, 190, 148, 255),
                        ear=(132, 168, 122, 255), accent=(88, 120, 84, 255),
                        ear_type="point", muzzle=(88, 120, 84, 255)),
}


def placeholder():
    img = Image.new("RGBA", (GRID, GRID), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    px(d, 8, 5, 16, 12, (206, 212, 218, 255))
    px(d, 10, 16, 12, 12, (206, 212, 218, 255))
    px(d, 12, 9, 2, 3, (255, 255, 255, 255))
    px(d, 18, 9, 2, 3, (255, 255, 255, 255))
    return img.resize((GRID * SCALE, GRID * SCALE), Image.NEAREST)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, cfg in CHARACTERS.items():
        draw_character(**cfg).save(os.path.join(OUT, name + ".png"))
        print("生成", name)
    placeholder().save(os.path.join(OUT, "char_placeholder.png"))
    print("生成 char_placeholder")


if __name__ == "__main__":
    main()
