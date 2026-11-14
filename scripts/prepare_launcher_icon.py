#!/usr/bin/env python3
"""Prepare the launcher icon from the ORIGINAL Raghif artwork, without beige halo.

Run during CI before Gradle: source WebP stays untouched in Git history, output
PNG replaces it only in the temporary GitHub Actions workspace. This does not
redraw, regenerate, or modify any visible details inside the golden icon frame.
"""
from collections import deque
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/"app/src/main/res/drawable-nodpi"
SOURCE=RES/"raghif_logo_icon.webp"
TARGET=RES/"raghif_logo_icon.png"

def make_icon(source:Path, target:Path) -> None:
    image=Image.open(source).convert("RGBA")
    w,h=image.size
    px=image.load()
    todo=deque([(x,0) for x in range(w)] +
               [(x,h-1) for x in range(w)] +
               [(0,y) for y in range(h)] +
               [(w-1,y) for y in range(h)])
    visited=set()
    while todo:
        x,y=todo.popleft()
        if (x,y) in visited or x<0 or y<0 or x>=w or y>=h:
            continue
        visited.add((x,y))
        r,g,b,a=px[x,y]
        # Only remove the CONNECTED light beige background, never the gold
        # lines, bread or lettering that are inside the icon.
        if r>=210 and g>=160 and b>=115 and r>=g and g>=b:
            px[x,y]=(r,g,b,0)
            todo.extend(((x-1,y),(x+1,y),(x,y-1),(x,y+1)))
    bounds=image.getbbox()
    if not bounds: raise RuntimeError("No icon pixels remain")
    trimmed=image.crop(bounds)
    dimension=max(trimmed.size)
    centered=Image.new("RGBA",(dimension,dimension),(0,0,0,0))
    centered.alpha_composite(trimmed,((dimension-trimmed.width)//2,(dimension-trimmed.height)//2))
    centered=centered.resize((432,432),Image.Resampling.LANCZOS)
    centered.save(target,"PNG",optimize=True)
    # Android AAPT rejects two different image formats with the same resource name.
    source.unlink()

if __name__=="__main__":
    make_icon(SOURCE,TARGET)
    print(f"Created borderless Raghif icon: {TARGET}, {TARGET.stat().st_size} bytes")
