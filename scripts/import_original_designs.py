#!/usr/bin/env python3
"""
Lossless import of the nine ORIGINAL user-supplied Raghif screenshots.
No cropping, recompression, resampling or aspect-ratio changes. These are
reference assets only (screens include example figures); live editable Compose
fields must render actual database values instead of baking in screenshot text.

Usage:
  python scripts/import_original_designs.py /path/to/the/9/original/images
  python scripts/import_original_designs.py /path/to/unmodified_refs.zip

Destination:
  app/src/main/assets/original_refs/*.png

All files must match docs/original_assets_manifest.json exactly.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import struct
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "docs" / "original_assets_manifest.json"
DEST = ROOT / "app" / "src" / "main" / "assets" / "original_refs"


def png_dimensions(data: bytes) -> tuple[int, int]:
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValueError("Source is not an original PNG file")
    return struct.unpack(">II", data[16:24])


def load_bytes(source: Path, name: str) -> bytes:
    if source.is_dir():
        path = source / name
        if not path.is_file():
            raise ValueError(f"Missing original image: {name}")
        return path.read_bytes()
    with zipfile.ZipFile(source) as archive:
        matches = [path for path in archive.namelist() if Path(path).name == name]
        if len(matches) != 1:
            raise ValueError(f"Expected exactly one copy of {name} in ZIP")
        return archive.read(matches[0])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path, help="Folder or ZIP with unmodified original images")
    args = parser.parse_args()
    refs = json.loads(MANIFEST.read_text(encoding="utf-8"))["entries"]

    # Validate EVERYTHING before copying anything.
    validated = []
    for entry in refs:
        raw = load_bytes(args.source, entry["file"])
        digest = hashlib.sha256(raw).hexdigest()
        size = png_dimensions(raw)
        if digest != entry["sha256"]:
            raise ValueError(f"Image bytes differ from user's original: {entry['file']}")
        if size != (entry["width"], entry["height"]):
            raise ValueError(f"Image dimensions changed: {entry['file']}, got {size}")
        validated.append((entry["file"], raw))

    DEST.mkdir(parents=True, exist_ok=True)
    for name, raw in validated:
        (DEST / name).write_bytes(raw)
        assert hashlib.sha256((DEST / name).read_bytes()).hexdigest() == hashlib.sha256(raw).hexdigest()
    print(f"Successfully imported {len(validated)} UNMODIFIED PNGs to {DEST}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, zipfile.BadZipFile) as e:
        print(f"Original image import failed: {e}", file=sys.stderr)
        sys.exit(1)
