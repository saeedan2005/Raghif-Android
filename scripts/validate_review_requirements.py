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
require("onSettings:" not in s["list"],"Home settings icon must no longer appear above bakery image")
require('onSettings={clickSound();navigate("settings")}' in s["main"],
        "Home settings icon must be connected to footer navigation")
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
require('android:icon="@drawable/raghif_logo_icon"' in s["manifest"],"Launcher icon must equal About-screen image resource")
require("DashboardTile(" in s["dashboard"],"Bakery dashboard must preserve live navigation")

formula=root/"app/src/main/java/com/raghif/app/reports/ReportMath.kt"
sha=subprocess.run(["git","hash-object",str(formula)],capture_output=True,text=True,cwd=root)
require(sha.returncode==0 and sha.stdout.strip()=="e8011a4ca5d292f4a26c5e01f3aa10dc269ba2af",
        "ReportMath.kt changed: formulas must remain untouched")

# Version 1.0.3 requirements
require("RaghifExitDialog" in s["main"],"Use branded exit confirmation")
require("RaghifNoticeDialog" in s["main"],"Use branded save notice")
require("سجل مستقل جديد للتاريخ المختار" not in s["main"],"Remove temporary-new-record banner")
require('RaghifSectionTitle("حفظ السجل")' not in s["main"],"Remove extra save title")
require('ReportFileNames.daily' in s["main"],"Daily PDF must use Arabic filename")
require("RaghifSectionTitle(\"نوع التقرير\")" in
        (root/"app/src/main/java/com/raghif/app/ui/RaghifExportScreen.kt").read_text(),
        "Use singular report type title")

# Version 1.0.4: visual polish and display precision, without changing data.
require('RaghifSectionTitle(title, sideAligned = false)' in s["brand"],
        "Only top-level page headings must be centered")
require('horizontalArrangement=if(sideAligned) Arrangement.Start else Arrangement.Center' in s["brand"],
        "Secondary section titles must remain right-aligned in RTL")
require('RaghifFooter(' in s["main"] and 'onSettings={clickSound();navigate("settings")}' in s["main"],
        "Settings icon should be in the home footer next to exit")
require('RaghifGoldWaves()' not in s["list"] and 'BakeryListDivider()' in s["list"],
        "Use redesigned golden divider above saved bakeries")
require(s["list"].index("RaghifUncroppedImage(") < s["list"].index('Text("اختر المخبز"'),
        "Original bakery hero image must appear before heading")
require('RaghifSectionTitle("بيانات المبيعات")' in s["main"] and
        "بيانات المبيعات والمصروفات" not in s["main"],
        "Sales section should use the approved shorter name")
dialogs=(root/"app/src/main/java/com/raghif/app/ui/RaghifDialogs.kt").read_text()
require("Modifier.padding(horizontal=5.dp)" in dialogs,
        "Exit dialog buttons must have a small horizontal gap")
formatter=(root/"app/src/main/java/com/raghif/app/ui/RaghifDisplayNumber.kt").read_text()
pdf=(root/"app/src/main/java/com/raghif/app/reports/RaghifPdf.kt").read_text()
require("maximumFractionDigits = 1" in formatter and
        "maximumFractionDigits = 1" in pdf,
        "All displayed numbers (including PDF) must have no more than one decimal")

if issues:
    print("\n".join(issues),file=sys.stderr)
    sys.exit(1)
print("Raghif November review checks passed; original financial formulas unchanged.")

