#!/usr/bin/env python3
"""Guardrails for the user-supplied November UI review; no database/schema edits."""
from pathlib import Path
import re
import subprocess
import sys

root=Path(__file__).resolve().parents[1]
files={
    "main":root/"app/src/main/java/com/raghif/app/MainActivity.kt",
    "list":root/"app/src/main/java/com/raghif/app/ui/BakerySelectionScreen.kt",
    "dashboard":root/"app/src/main/java/com/raghif/app/ui/RaghifDashboard.kt",
    "brand":root/"app/src/main/java/com/raghif/app/ui/RaghifBrand.kt",
    "actions":root/"app/src/main/java/com/raghif/app/reports/ReportActions.kt",
    "chart":root/"app/src/main/java/com/raghif/app/reports/RecordCharts.kt",
    "icon":root/"app/src/main/res/drawable/raghif_launcher_safe_foreground.xml",
    "manifest":root/"app/src/main/AndroidManifest.xml",
}
s={key:path.read_text(encoding="utf-8") for key,path in files.items()}
issues=[]

def require(condition:bool,error:str):
    if not condition: issues.append(error)

require("onNightToggle" not in s["list"],"No home-screen night toggle allowed")
require('onNightChange(!night)' not in s["main"],"Night toggle must be settings-only")
require('containerColor = if(night) Color.Black else RaghifPalette.Paper' in s["main"],
        "Night mode must change only the application background")
require("darkColorScheme(" not in s["main"],"Do not recolor buttons in night mode")
require("Alignment.CenterEnd" in s["list"],"Settings gear must be at left edge in RTL")
settings=s["main"].split("@Composable private fun SettingsScreen(",1)[-1]
finance=s["main"].split("@Composable private fun FinancialScreen(",1)[-1].split("@Composable private fun TableCell(",1)[0]
require("تحديث البيانات تلقائيًا" not in settings,"Remove redundant automatic updates setting")
require("774527928" in settings,"Author phone number required")
require('RaghifSectionTitle("إعدادات التطبيق",sideAligned=true)' in settings,
        "Section headers in Settings should align to right")
require("التقرير خاص بالمخبز المحدد فقط. المباع آجل" not in finance,
        "Financial internal instruction must not be displayed")
for token in ["إجمالي الإنتاج","كمية الدقيق","أعلى يوم إنتاجًا","RecordCharts(list)"]:
    require(token not in finance,"Financial screen should not contain "+token)
require("FinancialRevenueChart(list)" in finance,"Financial screen should show revenue only")
require("Icons.Default.Save" in s["actions"] and "Icons.Default.Print" in s["actions"],
        "PDF controls need meaningful icons")
require("24dp" not in s["icon"] or 'android:left="24dp"' not in s["icon"],
        "Artificial icon padding must not be restored")
require('android:icon="@mipmap/ic_launcher"' in s["manifest"],"Use the adaptive Raghif icon")
require("DashboardTile(" in s["dashboard"],"Bakery dashboard must preserve live navigation")

formula=root/"app/src/main/java/com/raghif/app/reports/ReportMath.kt"
sha=subprocess.run(["git","hash-object",str(formula)],capture_output=True,text=True,cwd=root)
require(sha.returncode==0 and sha.stdout.strip()=="e8011a4ca5d292f4a26c5e01f3aa10dc269ba2af",
        "ReportMath.kt changed: formulas must remain untouched")

if issues:
    print("\n".join(issues),file=sys.stderr)
    sys.exit(1)
print("Raghif November review checks passed; original financial formulas unchanged.")
