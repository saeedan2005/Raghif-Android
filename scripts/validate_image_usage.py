#!/usr/bin/env python3
"""CI guardrail: do not reintroduce stretching or cropping of original artwork.

This check does not claim complete pixel-for-pixel UI fidelity; a human still
needs to compare editable Compose screens with the nine design references.
"""
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
brand = (root / "app/src/main/java/com/raghif/app/ui/RaghifBrand.kt").read_text()
pdf = (root / "app/src/main/java/com/raghif/app/reports/RaghifPdf.kt").read_text()
icon = (root / "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml").read_text()

errors = []
if "ContentScale.Fit" not in brand:
    errors.append("Original branding must use ContentScale.Fit")
if "ContentScale.FillBounds" in brand or "ContentScale.Crop" in brand:
    errors.append("Original branding must not use FillBounds or Crop")
if "ImageFit.contain(" not in pdf:
    errors.append("PDF image placement must preserve the source aspect ratio")
if "@drawable/raghif_launcher_safe_foreground" not in icon:
    errors.append("Launcher must keep its foreground within the safe zone")
if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("Raghif image-safe layout checks passed.")
