#!/usr/bin/env python3
"""
Google Play 商店素材生成器（English listing）。

产出三张图，全部是 Play Console 直接可上传的规格：
  images/icon.png                          512×512    应用图标
  images/featureGraphic.png                1024×500   特色图形（横幅）
  images/phoneScreenshots/dashboard.png    1080×1920  手机截图 · 主面板
  images/phoneScreenshots/connection.png   1080×1920  手机截图 · 配对

为什么不直接用浏览器渲染 HTML/CSS：本机开发环境下 Chrome 被沙箱拦住，它的
ProcessSingleton 与 Crashpad 坚持写 ~/Library/Application Support/Google/Chrome，
--user-data-dir 也绕不过。PIL 是纯文件 I/O，不依赖任何外部进程，且像素级可控。
同目录下的 feature-graphic.html 与 screenshot.html 是同一套设计的 CSS 版本，
在有可用 Chrome 的机器上可以渲染出排版更精细的版本，二者保留其一即可。

字体：标题用 Chakra Petch（与文档站 index.css 的 --font-display 一致），
正文用系统字体。Chakra Petch 未随仓库分发，脚本会在缺失时按 URL 下载到
FONT_DIR；也可以自行放置到「本目录/fonts/」下。
"""
import os
import sys
import urllib.request

from PIL import Image, ImageDraw, ImageFont, ImageFilter

# --- 路径 ---
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(REPO, "fastlane", "metadata", "android", "en-US", "images")
SHOT_DIR = os.path.join(REPO, "docs", "images", "mobile_app_screenshot")
FONT_DIR = "/tmp/play-fonts"

DASHBOARD_SHOT = os.path.join(
    SHOT_DIR,
    "Screenshot_2026-09-15-13-00-45-590_xin.ctkqiang.burpsuite.remote.mobileapp.jpg",
)
CONNECTION_SHOT = os.path.join(
    SHOT_DIR,
    "Screenshot_2026-09-15-13-00-53-661_xin.ctkqiang.burpsuite.remote.mobileapp.jpg",
)

# --- 品牌色：与 index.css 的 --accent / --bg / --surface 完全一致 ---
BRAND = (255, 102, 51)
BG = (10, 11, 13)
SURFACE = (18, 21, 25)
WHITE = (255, 255, 255)
TEXT = (232, 234, 237)
DIM = (154, 163, 173)

DISPLAY_URL = "https://raw.githubusercontent.com/google/fonts/main/ofl/chakrapetch/ChakraPetch-SemiBold.ttf"
DISPLAY_NAME = "ChakraPetch-SemiBold.ttf"
DISPLAY_CANDIDATES = [
    os.path.join(HERE, "fonts", DISPLAY_NAME),
    os.path.join(FONT_DIR, DISPLAY_NAME),
]
BODY_CANDIDATES = [
    "/System/Library/Fonts/SFNS.ttf",
    "/System/Library/Fonts/HelveticaNeue.ttc",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
]


def resolve_display_font():
    """返回可用的显示字体路径；本地没有就下载，下载失败则退回系统字体。"""
    for path in DISPLAY_CANDIDATES:
        if os.path.isfile(path):
            return path
    try:
        os.makedirs(FONT_DIR, exist_ok=True)
        target = os.path.join(FONT_DIR, DISPLAY_NAME)
        urllib.request.urlretrieve(DISPLAY_URL, target)
        return target
    except Exception as error:  # 网络不可用时不该让整个脚本失败
        print("  ! Chakra Petch 获取失败（%s），改用系统字体" % error, file=sys.stderr)
        return None


def load(path, size):
    return ImageFont.truetype(path, size)


DISPLAY_PATH = resolve_display_font()
BODY_PATH = next((p for p in BODY_CANDIDATES if os.path.isfile(p)), None)


def tracked(draw, xy, text, font, fill, tracking=0.0):
    """逐字绘制以实现字距；PIL 没有 letter-spacing，标题不加字距会显得很挤。"""
    x, y = xy
    for char in text:
        draw.text((x, y), char, font=font, fill=fill)
        x += draw.textlength(char, font=font) + tracking
    return x


def tracked_width(draw, text, font, tracking=0.0):
    if not text:
        return 0
    return sum(draw.textlength(c, font=font) for c in text) + tracking * (len(text) - 1)


def radial_glow(size, center, radius, color, peak):
    """低分辨率算好再放大，避免逐像素遍历整张画布。"""
    small = (max(2, size[0] // 4), max(2, size[1] // 4))
    grad = Image.new("L", small, 0)
    pixels = grad.load()
    cx = center[0] * small[0] / size[0]
    cy = center[1] * small[1] / size[1]
    scaled = radius * small[0] / size[0]
    for y in range(small[1]):
        for x in range(small[0]):
            distance = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            falloff = max(0.0, 1.0 - distance / scaled)
            pixels[x, y] = int(peak * falloff * falloff)
    grad = grad.resize(size, Image.BICUBIC)
    layer = Image.new("RGBA", size, color + (0,))
    layer.putalpha(grad)
    return layer


def grid_overlay(size, step=64, alpha=9):
    overlay = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    for x in range(0, size[0], step):
        draw.line([(x, 0), (x, size[1])], fill=(255, 255, 255, alpha), width=1)
    for y in range(0, size[1], step):
        draw.line([(0, y), (size[0], y)], fill=(255, 255, 255, alpha), width=1)
    return overlay


def rounded_mask(size, radius, top_only=False):
    mask = Image.new("L", size, 0)
    corners = (True, True, False, False) if top_only else (True, True, True, True)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255, corners=corners
    )
    return mask


def screenshot_card(path, width, height, radius):
    """等比缩放到 target 宽度后，从顶部裁到目标高度。

    只裁底部：手机截图的底部是列表尾部，裁掉不丢信息；顶部有状态栏和导航，
    裁顶部会让画面显得像是坏的。
    """
    shot = Image.open(path).convert("RGB")
    ratio = width / shot.width
    shot = shot.resize((width, max(1, round(shot.height * ratio))), Image.LANCZOS)
    if shot.height < height:
        canvas = Image.new("RGB", (width, height), SURFACE)
        canvas.paste(shot, (0, 0))
        shot = canvas
    else:
        shot = shot.crop((0, 0, width, height))

    mask = rounded_mask((width, height), radius, top_only=True)
    card = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    card.paste(shot, (0, 0), mask)

    # 描边只画上边缘与两侧，底部出血处不画，否则会出现一条水平硬边
    edge = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(edge).rounded_rectangle(
        [0, 0, width - 1, height - 1],
        radius=radius,
        outline=(255, 255, 255, 34),
        width=1,
        corners=(True, True, False, False),
    )
    card = Image.alpha_composite(card, edge)
    return card, mask


def brand_mark(size, radius_ratio=0.22):
    """把 icon.svg 栅格化后套圆角，用作素材里的小图标。

    这里重新栅格化而不是复用 icon.png：icon.png 是给商店用的满幅方图，
    放进画布需要圆角，直接缩放会把直角带进去。
    """
    import subprocess
    import tempfile

    svg = os.path.join(HERE, "icon.svg")
    big = size * 4
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as handle:
        temp = handle.name
    subprocess.run(
        ["rsvg-convert", "-w", str(big), "-h", str(big), svg, "-o", temp],
        check=True,
        capture_output=True,
    )
    mark = Image.open(temp).convert("RGBA")
    os.unlink(temp)
    mask = rounded_mask((big, big), int(big * radius_ratio))
    mark.putalpha(mask)
    return mark.resize((size, size), Image.LANCZOS)


def feature_graphic():
    width, height = 1024, 500
    canvas = Image.new("RGBA", (width, height), BG)
    canvas = Image.alpha_composite(canvas, grid_overlay((width, height)))
    canvas = Image.alpha_composite(canvas, radial_glow((width, height), (820, 230), 470, BRAND, 78))

    display = load(DISPLAY_PATH or BODY_PATH, 15)
    headline = load(DISPLAY_PATH or BODY_PATH, 52)
    body = load(BODY_PATH, 17)

    draw = ImageDraw.Draw(canvas)

    # 品牌行
    mark = brand_mark(26)
    canvas.alpha_composite(mark, (64, 148))
    draw = ImageDraw.Draw(canvas)
    tracked(draw, (64 + 26 + 12, 155), "BURPSUITE REMOTE", display, BRAND, tracking=4.0)

    # 标题：第二行用品牌色，把「不受束缚」这个卖点压在过去分词上
    draw.text((64, 196), "Burp Suite,", font=headline, fill=WHITE)
    draw.text((64, 196 + 60), "untethered.", font=headline, fill=BRAND)

    draw.text((64, 330), "Proxy history · Intercept decisions · Repeater", font=body, fill=DIM)
    draw.text((64, 356), "streamed live to your phone.", font=body, fill=DIM)

    # 截图卡片：只给到画布下沿，于是底部自然出血
    card_x, card_y, card_w, card_h = width - 58 - 296, 72, 296, 428
    card, mask = screenshot_card(DASHBOARD_SHOT, card_w, card_h, 30)

    glow = Image.new("RGBA", (card_w, card_h), BRAND + (0,))
    glow.putalpha(mask.filter(ImageFilter.GaussianBlur(22)).point(lambda v: int(v * 0.42)))
    canvas.alpha_composite(glow, (card_x - 12, card_y - 12))
    canvas.alpha_composite(card, (card_x, card_y))

    canvas.convert("RGB").save(os.path.join(OUT, "featureGraphic.png"))


def phone_shot(name, eyebrow, headline_lines, sub_lines, shot_path):
    width, height = 1080, 1920
    canvas = Image.new("RGBA", (width, height), BG)
    canvas = Image.alpha_composite(canvas, radial_glow((width, height), (540, -60), 1150, BRAND, 86))

    display = load(DISPLAY_PATH or BODY_PATH, 25)
    headline = load(DISPLAY_PATH or BODY_PATH, 72)
    body = load(BODY_PATH, 31)

    draw = ImageDraw.Draw(canvas)
    tracked(draw, (96, 126), eyebrow.upper(), display, BRAND, tracking=6.0)

    y = 196
    for line in headline_lines:
        draw.text((96, y), line, font=headline, fill=WHITE)
        y += 78

    y += 24
    for line in sub_lines:
        draw.text((96, y), line, font=body, fill=DIM)
        y += 46

    card_x, card_y, card_w, card_h = 96, 640, 888, 1280
    card, mask = screenshot_card(shot_path, card_w, card_h, 40)

    glow = Image.new("RGBA", (card_w, card_h), BRAND + (0,))
    glow.putalpha(mask.filter(ImageFilter.GaussianBlur(30)).point(lambda v: int(v * 0.38)))
    canvas.alpha_composite(glow, (card_x - 16, card_y - 16))
    canvas.alpha_composite(card, (card_x, card_y))

    canvas.convert("RGB").save(os.path.join(OUT, "phoneScreenshots", name + ".png"))


def main():
    os.makedirs(os.path.join(OUT, "phoneScreenshots"), exist_ok=True)
    print("显示字体 :", DISPLAY_PATH or "（回落）") 
    print("正文字体 :", BODY_PATH)

    feature_graphic()
    print("  featureGraphic.png  1024×500")

    phone_shot(
        "dashboard",
        "Real-time dashboard",
        ["Your proxy,", "on your phone."],
        ["Requests, intercepts and saves —", "counted the moment they happen."],
        DASHBOARD_SHOT,
    )
    phone_shot(
        "connection",
        "Device pairing",
        ["Scan once,", "you are in."],
        ["One-time code, five-minute expiry,", "nothing leaves your local network."],
        CONNECTION_SHOT,
    )
    print("  phoneScreenshots/dashboard.png  1080×1920")
    print("  phoneScreenshots/connection.png 1080×1920")


if __name__ == "__main__":
    main()
