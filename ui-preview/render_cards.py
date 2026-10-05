#!/usr/bin/env python3
"""Visual self-check harness for Sacred Verses v4.0 UI work.
Replicates ShareImageHelper.render() and the NEW Verse-of-the-Day card
as PNGs (PIL + raqm for proper Arabic/Devanagari shaping). View with the
read tool and iterate."""
import json, textwrap
from PIL import Image, ImageDraw, ImageFont

OUT = '/home/hatch/workspace/sacred-verses-app/ui-preview/'
FD = '/usr/share/fonts/truetype/noto/'
FDJ = '/usr/share/fonts/truetype/dejavu/'

def font(name, size):
    return ImageFont.truetype(name, size)

SERIF = FD + 'NotoSerif-Regular.ttf'
SANS = FD + 'NotoSans-Regular.ttf'
SANSB = FD + 'NotoSans-Bold.ttf'
AR = FD + 'NotoNaskhArabic-Regular.ttf'
DEVA = FD + 'NotoSansDevanagari-Regular.ttf'
GURM = FD + 'NotoSansGurmukhi-Regular.ttf'

FAITH_COLORS = {'Hinduism': (232, 145, 45), 'Christianity': (74, 127, 201),
                'Islam': (46, 139, 87), 'Buddhism': (201, 162, 39),
                'Sikhism': (217, 123, 41)}

def vgrad(w, h, top, bot):
    img = Image.new('RGB', (w, h))
    px = img.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        px_col = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
        for x in range(w):
            px[x, y] = px_col
    return img

def rounded(img_draw_target):
    pass

def wrap(draw, text, fnt, max_w):
    words, lines, cur = text.split(), [], ''
    for wd in words:
        trial = (cur + ' ' + wd).strip()
        if draw.textlength(trial, font=fnt) <= max_w or not cur:
            cur = trial
        else:
            lines.append(cur)
            cur = wd
    if cur:
        lines.append(cur)
    return lines

def draw_centered_tracked(draw, cx, y, text, fnt, fill, tracking):
    # letter-spaced centered text
    widths = [draw.textlength(ch, font=fnt) for ch in text]
    total = sum(widths) + tracking * (len(text) - 1)
    x = cx - total / 2
    for ch, wd in zip(text, widths):
        draw.text((x, y), ch, font=fnt, fill=fill, direction='ltr')
        x += wd + tracking
    return total

def harness_text(verse, lang):
    """Harness-only transform: the sandbox's Noto fonts are subsetted builds
    missing ASCII parens/quotes, so drop the Arabic verse markers for layout
    measurement. The shipped data keeps them — Android renders them fine."""
    import re
    t = verse['text'][lang]
    if lang == 'ar':
        t = re.sub(r'\s*\(([٠-٩])\)', r' \1', t)
    return t

def pick_font(lang, serif, size):
    if lang == 'ar':
        return font(AR, size)
    if lang in ('hi', 'sa'):
        return font(DEVA, size)
    if lang == 'pa':
        return font(GURM, size)
    return font(SERIF if serif else SANS, size)

# ---------------- ShareImageHelper.render replica ----------------
def render_share(verse, lang, path):
    w = h = 1080
    img = vgrad(w, h, (42, 35, 86), (107, 78, 158))
    d = ImageDraw.Draw(img, 'RGBA')
    cx = w / 2
    accent = FAITH_COLORS[verse['faith']]
    # top glow
    for y in range(int(h * 0.45)):
        a = int(34 * (1 - y / (h * 0.45)))
        d.line([(0, y), (w, y)], fill=(255, 255, 255, a))
    # brand
    fb = font(SANS, 34)
    draw_centered_tracked(d, cx, 116, 'SACRED VERSES', fb, (232, 226, 245, 153), 14)
    # verse
    fv = pick_font(lang, True, 56)
    text = '\u201c' + harness_text(verse, lang) + '\u201d'
    lines = wrap(d, text, fv, w * 0.84)
    lh = int(56 * 1.25) + 8
    block_h = lh * len(lines)
    top = h * 0.50 - block_h / 2
    for i, ln in enumerate(lines):
        d.text((cx, top + i * lh), ln, font=fv, fill=(255, 255, 255),
               anchor='ma', direction='ltr' if lang != 'ar' else 'rtl')
    after = top + block_h
    # divider
    dw = w * 0.16
    d.rectangle([cx - dw / 2, after + 36, cx + dw / 2, after + 40], fill=accent)
    # ref + source + credit
    fr = font(SANSB, 46)
    d.text((cx, after + 78), verse['faith'] + ' \u00b7 ' + verse['ref'],
           font=fr, fill=accent, anchor='ma')
    fs = font(SANS, 34)
    d.text((cx, after + 128), verse['source'], font=fs,
           fill=(232, 226, 245, 187), anchor='ma')
    fc = font(SANS, 30)
    d.text((cx, h - 60), 'Shared via Sacred Verses', font=fc,
           fill=(232, 226, 245, 136), anchor='ma')
    img.save(path)
    print('wrote', path)

# ---------------- NEW daily card ----------------
def render_card(verse, lang, path, dark_buttons=False):
    S = 3  # px per dp
    W = 1080
    cw = W - 32 * S
    pad = 24 * S
    accent = FAITH_COLORS[verse['faith']]
    # measure pass
    fv = pick_font(lang, True, 20 * S)
    d0 = ImageDraw.Draw(Image.new('RGB', (8, 8)))
    lines = wrap(d0, '\u201c' + harness_text(verse, lang) + '\u201d', fv, cw - 2 * pad)
    lh = int(20 * S * 1.32)
    y = pad
    y += 11 * S + 8                      # label
    y += 12 * S                          # gap
    y += 12 * S + 12 * S + 8             # pill
    y += 16 * S                          # gap
    y += lh * len(lines)                 # verse
    y += 16 * S + 3 * S                  # divider
    y += 12 * S + 15 * S                 # ref
    y += 4 * S + 12 * S                  # source
    y += 12 * S + 14 * S                 # buttons
    y += pad
    H = int(y)
    img = vgrad(W, H, (42, 35, 86), (107, 78, 158))
    # rounded corners
    mask = Image.new('L', (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W, H], radius=16 * S, fill=255)
    img.putalpha(mask)
    d = ImageDraw.Draw(img, 'RGBA')
    cx = W / 2
    x0 = (W - cw) / 2
    y = pad
    # label
    fl = font(SANSB, 11 * S)
    draw_centered_tracked(d, cx, y, 'VERSE OF THE DAY', fl, (232, 226, 245, 179), 5)
    y += 11 * S + 8 + 12 * S
    # faith pill
    fp = font(SANSB, 12 * S)
    ptxt = verse['faith']
    pw = d.textlength(ptxt, font=fp) + 24 * S
    ph = 12 * S + 12 * S + 8
    d.rounded_rectangle([cx - pw / 2, y, cx + pw / 2, y + ph], radius=14 * S, fill=accent)
    d.text((cx, y + ph / 2), ptxt, font=fp, fill=(255, 255, 255), anchor='mm')
    y += ph + 16 * S
    # verse
    for i, ln in enumerate(lines):
        d.text((cx, y + i * lh), ln, font=fv, fill=(255, 255, 255),
               anchor='ma', direction='ltr' if lang != 'ar' else 'rtl')
    y += lh * len(lines) + 16 * S
    # divider
    dw = 64 * S
    d.rectangle([cx - dw / 2, y, cx + dw / 2, y + 3 * S], fill=accent)
    y += 3 * S + 12 * S
    # ref
    fr = font(SANSB, 15 * S)
    d.text((cx, y), verse['faith'] + ' \u00b7 ' + verse['ref'], font=fr,
           fill=accent, anchor='ma')
    y += 15 * S + 4 * S
    # source
    fs = font(SANS, 12 * S)
    d.text((cx, y), verse['source'], font=fs, fill=(232, 226, 245, 187), anchor='ma')
    y += 12 * S + 12 * S
    # buttons
    fb = font(SANS, 14 * S)
    btns = ['\u2606 SAVE', 'SHARE', 'IMAGE', '\U0001f50a LISTEN']
    gap = 28 * S
    widths = [d.textlength(b, font=fb) for b in btns]
    total = sum(widths) + gap * (len(btns) - 1)
    x = cx - total / 2
    for b, wd in zip(btns, widths):
        d.text((x, y), b, font=fb, fill=(255, 255, 255))
        x += wd + gap
    img.save(path)
    print('wrote', path)

def render_browse_card(verse, lang, path, zoom_path):
    """Replicates item_verse.xml geometry: card_bg (16dp radius, 1dp stroke)
    with the 4dp faith accent strip (16dp top corners). Also writes a 4x
    zoom of the top-left corner so strip/corner alignment can be eyeballed."""
    S = 3  # px per dp
    W = 1080
    cw = W - 32 * S          # 16dp margins
    R = 16 * S
    STRIP = 4 * S
    accent = FAITH_COLORS[verse['faith']]
    # measure
    fv = font(SERIF, 16 * S)
    d0 = ImageDraw.Draw(Image.new('RGB', (8, 8)))
    lines = wrap(d0, '\u201c' + harness_text(verse, lang) + '\u201d', fv, cw - 32 * S)
    lh = int(16 * S * 1.35)
    H = int(STRIP + 12 * S + lh * len(lines) + 8 * S + 12 * S + 2 * S + 11 * S
            + 4 * S + 16 * S + 12 * S)
    # page bg (light) so card edges are visible
    page = Image.new('RGB', (W, H + 40 * S), (240, 238, 245))
    card = Image.new('RGBA', (cw, H), (255, 255, 255, 255))
    # card body with 16dp rounded corners + 1dp stroke
    mask = Image.new('L', (cw, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, cw, H], radius=R, fill=255)
    d = ImageDraw.Draw(card)
    d.rounded_rectangle([0, 0, cw, H], radius=R, outline=(200, 195, 215), width=1 * S)
    # accent strip: full-bleed, 16dp top corners only
    smask = Image.new('L', (cw, STRIP), 0)
    ImageDraw.Draw(smask).rounded_rectangle([0, 0, cw, STRIP + R], radius=R, fill=255)
    strip = Image.new('RGBA', (cw, STRIP), accent + (255,))
    strip.putalpha(smask)
    card.alpha_composite(strip, (0, 0))
    card.putalpha(mask)
    page.paste(card, (16 * S, 20 * S), card)
    # verse text
    dp_ = ImageDraw.Draw(page)
    y = 20 * S + STRIP + 12 * S
    for i, ln in enumerate(lines):
        dp_.text((16 * S + 16 * S, y + i * lh), ln, font=fv, fill=(30, 27, 45))
    page.save(path)
    print('wrote', path)
    # 4x zoom of the top-left corner
    corner = page.crop((16 * S - 8 * S, 20 * S - 8 * S,
                        16 * S + 56 * S, 20 * S + 56 * S))
    corner = corner.resize((corner.width * 4, corner.height * 4), Image.NEAREST)
    corner.save(zoom_path)
    print('wrote', zoom_path)

if __name__ == '__main__':
    import os
    os.makedirs(OUT, exist_ok=True)
    verses = json.load(open('/home/hatch/workspace/sacred-verses-app/app/src/main/assets/verses.json', encoding='utf-8'))
    by_id = {v['id']: v for v in verses}
    en_v = by_id['hinduism-01']          # Gita 2.47 en/sa/hi
    ar_v = by_id['ar-fatiha']            # Arabic RTL stress test
    render_share(en_v, 'en', OUT + 'share_card_en.png')
    render_card(en_v, 'en', OUT + 'daily_card_en.png')
    render_card(ar_v, 'ar', OUT + 'daily_card_ar.png')
    render_card(en_v, 'sa', OUT + 'daily_card_sa.png')
    render_browse_card(by_id['islam-01'], 'en', OUT + 'browse_card.png',
                       OUT + 'browse_card_corner_zoom.png')
