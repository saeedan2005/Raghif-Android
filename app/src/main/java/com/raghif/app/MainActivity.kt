package com.raghif.app

import android.content.Intent
import android.app.DatePickerDialog
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextDirectionHeuristics
import androidx.core.content.FileProvider
import java.io.File
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.room.withTransaction
import com.raghif.app.data.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val brown = Color(0xFF87472F)
private val cream = Color(0xFFFFF9F3)
private val wheat = Color(0xFFE4B66E)
private val ink = Color(0xFF35251E)
private val ar = Locale("ar")
private val dbFmt = DateTimeFormatter.ISO_LOCAL_DATE
private val uiFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private fun today() = LocalDate.now().format(dbFmt)
private fun prettyDate(s: String) = runCatching { LocalDate.parse(s).format(uiFmt) }.getOrDefault(s)
private fun fmt(n: Number): String = NumberFormat.getNumberInstance(ar).format(n)
private fun dayName(s: String) = runCatching { LocalDate.parse(s).dayOfWeek.getDisplayName(TextStyle.FULL, ar) }.getOrDefault("")
private fun normalizedDigits(s: String) = s.trim().map { if (it in '٠'..'٩') ('0'.code + it.code - '٠'.code).toChar() else it }.joinToString("")
private fun cashSold(r: DailyRecord): Double = if (r.unitRevenueUsed <= 0) 0.0 else r.revenue.toDouble() / r.unitRevenueUsed * r.piecesPerBagUsed
private fun difference(r: DailyRecord) = r.pieces.toDouble() - r.workersMeal - r.waste - r.creditSold - cashSold(r)
private fun avgBag(r: DailyRecord): Double? = if (r.flourKg <= 0 || r.flourBagWeightUsed <= 0) null else r.pieces.toDouble() / (r.flourKg / r.flourBagWeightUsed)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { RaghifApp() } }
}

@Composable private fun RaghifApp() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    var nightMode by remember { mutableStateOf(context.getSharedPreferences("raghif_settings", android.content.Context.MODE_PRIVATE).getBoolean("night_mode", false)) }
    val scheme = if (nightMode) darkColorScheme(primary=Color(0xFFE7B47A), secondary=Color(0xFFD6A15D), background=Color(0xFF171411), surface=Color(0xFF241F1A), surfaceVariant=Color(0xFF342A22), onSurface=Color(0xFFFFF4E8)) else lightColorScheme(primary=brown, secondary=wheat, tertiary=Color(0xFF6F8060), background=cream, surface=Color.White, surfaceVariant=Color(0xFFF4E8DC), onSurface=ink)
    MaterialTheme(colorScheme=scheme, shapes=Shapes(small=RoundedCornerShape(12.dp), medium=RoundedCornerShape(18.dp), large=RoundedCornerShape(24.dp))) {
    val scope = rememberCoroutineScope()
    val bakeries by db.bakeryDao().observeAll().collectAsState(initial=emptyList())
    var selected by remember { mutableStateOf<Bakery?>(null) }
    var page by remember { mutableStateOf("bakeries") }
    var confirmExit by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var editingBakery by remember { mutableStateOf<Bakery?>(null) }
    var currentRecord by remember { mutableStateOf<DailyRecord?>(null) }
    var reportRows by remember { mutableStateOf<List<DailyRecord>>(emptyList()) }

    androidx.activity.compose.BackHandler(enabled=true) {
        if (page == "bakeries") confirmExit = true
        else page = when(page) { "daily","settings" -> "bakeries"; "stats","financial" -> "daily"; "bakeryForm" -> "bakeries"; else -> "bakeries" }
    }
    Scaffold(containerColor=MaterialTheme.colorScheme.background, topBar={
        TopAppBarRaghif(title=when(page) { "bakeries"->"قائمة المخابز"; "bakeryForm"->if(editingBakery==null) "إنشاء مخبز جديد" else "تعديل المخبز"; "daily"->selected?.name ?: "البيانات اليومية"; "stats"->"الإحصاءات"; "financial"->"التقرير المالي"; "settings"->"الإعدادات"; else->"رغيف" }, onBack={ if(page=="bakeries") confirmExit=true else page=when(page){"daily","settings"->"bakeries";"stats","financial"->"daily";"bakeryForm"->"bakeries";else->"bakeries"} })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when(page) {
                "welcome" -> { /* removed: bakery list is the launch screen */
                    Spacer(Modifier.height(28.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment=Alignment.Center) {
                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Box(Modifier.size(104.dp).background(Color(0xFFF7C77A), RoundedCornerShape(30.dp)), contentAlignment=Alignment.Center) { Icon(Icons.Default.BakeryDining, null, tint=brown, modifier=Modifier.size(68.dp)) }
                            Spacer(Modifier.height(12.dp)); Text("رغيف", style=MaterialTheme.typography.headlineLarge, fontWeight=FontWeight.Bold, color=brown)
                            Text("حسابات بلا تعقيد", color=Color(0xFF745747))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    BigButton("تسجيل الدخول", Icons.Default.Login) { page="bakeries" }
                    BigButton("إنشاء مخبز جديد", Icons.Default.AddBusiness) { editingBakery=null; page="bakeryForm" }
                    Spacer(Modifier.weight(1f))
                    Text("تعمل البيانات محليًا على هذا الجهاز", modifier=Modifier.align(Alignment.CenterHorizontally), color=Color.Gray)
                }
                "bakeries" -> {
                    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).background(Color(0xFFFFE7C5),RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) { Icon(Icons.Default.BakeryDining,null,tint=brown,modifier=Modifier.size(34.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text("مخابزك",fontSize=21.sp,fontWeight=FontWeight.Bold); Text("${bakeries.size} مخبز مسجل",color=Color.Gray,fontSize=13.sp) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={editingBakery=null;page="bakeryForm"}, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Add,null); Text("مخبز جديد") }
                        OutlinedButton(onClick={page="settings"}) { Icon(Icons.Default.Settings,null); Text("الإعدادات") }
                    }
                    if (bakeries.isEmpty()) EmptyMessage("لا توجد مخابز مضافة. أضف مخبزًا للبدء.")
                    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=12.dp)) {
                        items(bakeries, key={it.id}) { b ->
                            Card(Modifier.fillMaxWidth().clickable { selected=b; currentRecord=null; page="daily" },shape=RoundedCornerShape(20.dp),elevation=CardDefaults.cardElevation(2.dp)) {
                                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Box(Modifier.size(48.dp).background(Color(0xFFFFE7C5),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) { Icon(Icons.Default.Storefront,null,tint=brown,modifier=Modifier.size(27.dp)) }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) { Text(b.name,fontWeight=FontWeight.Bold,fontSize=17.sp); Text("رقم المخبز: ${b.number}",color=Color.Gray,fontSize=12.sp); Text("${fmt(b.unitRevenue)} ريال / ${fmt(b.piecesPerBag)} حبات",fontSize=12.sp,color=brown) }
                                    IconButton(onClick={editingBakery=b;page="bakeryForm"}) { Icon(Icons.Default.Edit,null) }
                                    IconButton(onClick={scope.launch { if(db.dailyDao().all(b.id).isNotEmpty()) message="لا يمكن حذف مخبز يحتوي على سجلات يومية في هذه النسخة التجريبية." else { db.bakeryDao().delete(b); message="تم حذف المخبز." } }}) { Icon(Icons.Default.Delete,null,tint=Color.Red) }
                                }
                            }
                        }
                    }
                }
                "bakeryForm" -> BakeryForm(editingBakery, onCancel={page="bakeries"}, onSave={b ->
                    scope.launch {
                        val old=editingBakery
                        val duplicateNum=db.bakeryDao().numberExists(normalizedDigits(b.number), old?.id ?: 0L)>0
                        val duplicateName=db.bakeryDao().nameExists(b.name.trim().lowercase(Locale.ROOT), old?.id ?: 0L)>0
                        if(duplicateNum || duplicateName) message="رقم المخبز أو اسمه مستخدم مسبقًا."
                        else {
                            if(old==null) db.bakeryDao().insert(b) else db.bakeryDao().update(b.copy(id=old.id,createdAt=old.createdAt))
                            page="bakeries"; message="تم حفظ بيانات المخبز."
                        }
                    }
                })
                "daily" -> selected?.let { b ->
                    DailyScreen(db,b,currentRecord,onChoose={currentRecord=it},onNew={currentRecord=null},onSaved={currentRecord=it;message="تم حفظ السجل اليومي.";},onStats={page="stats"},onFinancial={page="financial"},onShare={record ->
                        val activity=context as? ComponentActivity ?: return@DailyScreen
                        shareDailyRecord(activity,b,record)
                    },message={message=it})
                }
                "stats" -> selected?.let { StatsScreen(db,it) }
                "financial" -> selected?.let { b -> FinancialScreen(db,b,onExport={ rows,from,to -> (context as? ComponentActivity)?.let { activity -> shareFinancialReport(activity,b,rows,from,to) } }) }
                "settings" -> SettingsScreen(nightMode=nightMode,onNightModeChange={ enabled -> nightMode=enabled; context.getSharedPreferences("raghif_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("night_mode",enabled).apply() })
            }
        }
    }
    if(confirmExit) AlertDialog(onDismissRequest={confirmExit=false},title={Text("تأكيد الخروج")},text={Text("هل تريد الخروج من التطبيق؟")},confirmButton={TextButton(onClick={confirmExit=false;(context as? ComponentActivity)?.finish()}){Text("خروج")}},dismissButton={TextButton(onClick={confirmExit=false}){Text("إلغاء")}})
    if(message!=null) AlertDialog(onDismissRequest={message=null}, confirmButton={TextButton(onClick={message=null}){Text("موافق")} }, title={Text("رغيف")}, text={Text(message.orEmpty())})
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TopAppBarRaghif(title:String,onBack:()->Unit) {
    TopAppBar(title={Text(title,fontWeight=FontWeight.Bold)}, navigationIcon={ IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null)} }, colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.surface))
}
@Composable private fun BigButton(label:String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick:()->Unit) { Button(onClick=onClick, modifier=Modifier.fillMaxWidth().height(58.dp), shape=RoundedCornerShape(16.dp)) { Icon(icon,null); Spacer(Modifier.width(8.dp)); Text(label,fontSize=17.sp) } }
@Composable private fun EmptyMessage(text:String) { Card(Modifier.fillMaxWidth()) { Text(text,Modifier.padding(16.dp),color=Color.Gray) } }

@Composable private fun Field(label:String,value:String,onValue:(String)->Unit, numeric:Boolean=false, singleLine:Boolean=true) {
    OutlinedTextField(value=value,onValueChange=onValue,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=singleLine,keyboardOptions=KeyboardOptions(keyboardType=if(numeric) KeyboardType.Number else KeyboardType.Text))
}
@Composable private fun BakeryForm(existing:Bakery?,onCancel:()->Unit,onSave:(Bakery)->Unit) {
    var number by remember(existing){mutableStateOf(existing?.number.orEmpty())}; var name by remember(existing){mutableStateOf(existing?.name.orEmpty())}
    var address by remember(existing){mutableStateOf(existing?.address.orEmpty())}; var phone by remember(existing){mutableStateOf(existing?.phone.orEmpty())}; var mobile by remember(existing){mutableStateOf(existing?.supervisorMobile.orEmpty())}
    var revenue by remember(existing){mutableStateOf((existing?.unitRevenue?:500).toString())}; var pieces by remember(existing){mutableStateOf((existing?.piecesPerBag?:9).toString())}; var weight by remember(existing){mutableStateOf((existing?.flourBagWeight?:50.0).toString())}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Field("رقم المخبز",number,{number=it},true); Field("اسم المخبز",name,{name=it}); Field("العنوان (اختياري)",address,{address=it}); Field("رقم هاتف المخبز (اختياري)",phone,{phone=it},true); Field("جوال المشرف (اختياري)",mobile,{mobile=it},true)
        Text("إعدادات المبيعات",fontWeight=FontWeight.Bold,color=brown,fontSize=18.sp)
        Field("قيمة وحدة البيع (افتراضي 500)",revenue,{revenue=it},true); Field("عدد الحبات في الكيس (افتراضي 9)",pieces,{pieces=it},true); Field("وزن كيس الدقيق بالكيلو (افتراضي 50)",weight,{weight=it},true)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={val n=normalizedDigits(number);val rv=revenue.toLongOrNull();val pc=pieces.toLongOrNull();val wt=weight.toDoubleOrNull()
                if(n.isBlank()||!n.all{it.isDigit()}||name.isBlank()||rv==null||rv<=0||pc==null||pc<=0||wt==null||wt<=0) return@Button
                onSave(Bakery(number=n,normalizedNumber=n.trimStart('0').ifBlank{"0"},name=name.trim(),normalizedName=name.trim().lowercase(Locale.ROOT),address=address.trim(),phone=phone.trim(),supervisorMobile=mobile.trim(),unitRevenue=rv,piecesPerBag=pc,flourBagWeight=wt)) },modifier=Modifier.weight(1f)){Text("حفظ")}
            OutlinedButton(onClick=onCancel,modifier=Modifier.weight(1f)){Text("إلغاء")}
        }
    }
}

@Composable private fun DailyScreen(db:AppDatabase,bakery:Bakery,initial:DailyRecord?,onChoose:(DailyRecord?)->Unit,onNew:()->Unit,onSaved:(DailyRecord)->Unit,onStats:()->Unit,onFinancial:()->Unit,onShare:(DailyRecord)->Unit,message:(String)->Unit) {
    val scope= rememberCoroutineScope()
    var date by remember(initial){mutableStateOf(initial?.date ?: today())}
    var note by remember(initial){mutableStateOf(initial?.note.orEmpty())}
    var flour by remember(initial){mutableStateOf(initial?.flourKg?.toString().orEmpty())}
    var pieces by remember(initial){mutableStateOf(initial?.pieces?.toString().orEmpty())}
    var meal by remember(initial){mutableStateOf(initial?.workersMeal?.toString().orEmpty())}
    var waste by remember(initial){mutableStateOf(initial?.waste?.toString().orEmpty())}
    var credit by remember(initial){mutableStateOf(initial?.creditSold?.toString().orEmpty())}
    var revenue by remember(initial){mutableStateOf(initial?.revenue?.toString().orEmpty())}
    var history by remember { mutableStateOf<List<DailyRecord>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }
    val f=flour.toLongOrNull()?:0; val p=pieces.toLongOrNull()?:0; val m=meal.toLongOrNull()?:0; val w=waste.toLongOrNull()?:0; val c=credit.toLongOrNull()?:0; val rev=revenue.toLongOrNull()?:0
    val preview=DailyRecord(id=initial?.id?:0,bakeryId=bakery.id,date=date,flourKg=f,pieces=p,workersMeal=m,waste=w,creditSold=c,revenue=rev,note=note,unitRevenueUsed=initial?.unitRevenueUsed?:bakery.unitRevenue,piecesPerBagUsed=initial?.piecesPerBagUsed?:bakery.piecesPerBag,flourBagWeightUsed=initial?.flourBagWeightUsed?:bakery.flourBagWeight)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(bakery.name,Modifier.fillMaxWidth(),fontSize=23.sp,fontWeight=FontWeight.Bold,color=brown)
        Text("حسابات بلا تعقيد",color=Color.Gray)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={scope.launch { history=db.dailyDao().all(bakery.id);showHistory=true }},modifier=Modifier.weight(1f)){Icon(Icons.Default.Search,null);Text("السجلات السابقة")}
            OutlinedButton(onClick=onStats,modifier=Modifier.weight(1f)){Icon(Icons.Default.BarChart,null);Text("الإحصاءات")}
        }
        OutlinedButton(onClick=onFinancial,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.ReceiptLong,null);Text("التقرير المالي")}
        Field("التاريخ (YYYY-MM-DD)",date,{date=it})
        Text("اليوم: ${dayName(date)}",color=brown,fontWeight=FontWeight.Bold)
        Field("الملاحظة",note,{note=it},singleLine=false)
        Field("كمية الدقيق (كجم)",flour,{flour=it},true); Field("عدد الأقراص",pieces,{pieces=it},true)
        Field("عشاء العمال (اختياري)",meal,{meal=it},true); Field("التالف (اختياري)",waste,{waste=it},true)
        Field("المباع آجل (اختياري)",credit,{credit=it},true); Field("الإيراد (ريال)",revenue,{revenue=it},true)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text("المباع نقدًا: ${fmt(cashSold(preview))} حبة")
            Text("الفرق: ${fmt(difference(preview))} حبة",color=if(difference(preview)<0) Color.Red else brown)
            Text("متوسط إنتاج الكيس: ${avgBag(preview)?.let{fmt(it)} ?: "غير متاح"} حبة/كيس")
        } }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={
                val dt=runCatching{LocalDate.parse(date)}.getOrNull()
                val nums=listOf(flour,pieces,meal,waste,credit,revenue)
                if(dt==null||dt.isAfter(LocalDate.now())||flour.toLongOrNull()==null||pieces.toLongOrNull()==null||revenue.toLongOrNull()==null||listOf(flour,pieces,revenue).any{(it.toLongOrNull()?:-1)<0}||listOf(meal,waste,credit).any{it.isNotBlank()&&(it.toLongOrNull()?:-1)<0}) { message("تحقق من التاريخ والحقول الرقمية؛ يجب أن تكون القيم أعدادًا صحيحة غير سالبة.");return@Button }
                scope.launch {
                    val duplicate=db.dailyDao().byDate(bakery.id,date)
                    if(duplicate!=null&&duplicate.id!=(initial?.id?:0L)) { message("يوجد سجل لهذا المخبز في هذا التاريخ.");return@launch }
                    val record=preview.copy(id=initial?.id?:0,workersMeal=m,waste=w,creditSold=c,unitRevenueUsed=initial?.unitRevenueUsed?:bakery.unitRevenue,piecesPerBagUsed=initial?.piecesPerBagUsed?:bakery.piecesPerBag,flourBagWeightUsed=initial?.flourBagWeightUsed?:bakery.flourBagWeight)
                    if(initial==null) { val id=db.dailyDao().insert(record); onSaved(record.copy(id=id)) } else { db.dailyDao().update(record);onSaved(record) }
                }
            },modifier=Modifier.weight(1f)){Text("حفظ")}
            OutlinedButton(onClick={onNew},modifier=Modifier.weight(1f)){Text("سجل جديد")}
        }
        if(initial!=null) Button(onClick={onShare(initial.copy(note=note))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Share,null);Text("مشاركة السجل المحفوظ")}
    }
    if(showHistory) AlertDialog(onDismissRequest={showHistory=false},title={Text("السجلات السابقة")},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState())){history.forEach { r -> TextButton(onClick={showHistory=false;onChoose(r)}){Text("${prettyDate(r.date)} — الإيراد ${fmt(r.revenue)} ريال")} } }},confirmButton={TextButton(onClick={showHistory=false}){Text("إغلاق")}})
}

@Composable private fun StatsScreen(db:AppDatabase,b:Bakery) {
    val scope= rememberCoroutineScope(); var from by remember{mutableStateOf("")};var to by remember{mutableStateOf("")};var rows by remember{mutableStateOf<List<DailyRecord>?>(null)};var err by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        DateField("من تاريخ",from,{from=it});DateField("إلى تاريخ",to,{to=it})
        Button(onClick={val a=runCatching{LocalDate.parse(from)}.getOrNull();val z=runCatching{LocalDate.parse(to)}.getOrNull();if(a==null||z==null||a.isAfter(z)||z.isAfter(LocalDate.now()))err="تحقق من فترة التاريخ." else scope.launch{rows=db.dailyDao().between(b.id,from,to);err=if(rows!!.isEmpty())"لا توجد سجلات في الفترة المحددة." else null}},modifier=Modifier.fillMaxWidth()){Text("بحث")}
        err?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        rows?.takeIf{it.isNotEmpty()}?.let{list->
            Stat("عدد السجلات",fmt(list.size));Stat("إجمالي كمية الدقيق",fmt(list.sumOf{it.flourKg})+" كجم")
            Stat("عدد أكياس الدقيق",fmt(list.sumOf{if(it.flourBagWeightUsed>0)it.flourKg/it.flourBagWeightUsed else 0.0}))
            Stat("إجمالي عدد الأقراص",fmt(list.sumOf{it.pieces}));Stat("إجمالي عشاء العمال",fmt(list.sumOf{it.workersMeal}))
            Stat("إجمالي التالف",fmt(list.sumOf{it.waste}));Stat("إجمالي المباع آجل",fmt(list.sumOf{it.creditSold}))
            Stat("إجمالي المباع نقدًا",fmt(list.sumOf{cashSold(it)}));Stat("إجمالي الإيراد",fmt(list.sumOf{it.revenue})+" ريال")
            Stat("مجموع الفرق",fmt(list.sumOf{difference(it)}));Stat("متوسط إنتاج الكيس",if(list.sumOf{it.flourKg}==0L)"غير متاح" else fmt(list.sumOf{it.pieces}.toDouble()/list.sumOf{it.flourKg}*list.map{it.flourBagWeightUsed}.average())+" حبة/كيس")
        }
    }
}
@Composable private fun Stat(label:String,value:String){Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(label);Text(value,fontWeight=FontWeight.Bold,color=brown)}}}

@Composable private fun FinancialScreen(db:AppDatabase,b:Bakery,onExport:(List<DailyRecord>,String,String)->Unit) {
    val scope= rememberCoroutineScope()
    var from by remember{mutableStateOf("")}
    var to by remember{mutableStateOf("")}
    var rows by remember{mutableStateOf<List<DailyRecord>?>(null)}
    var err by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("التقرير المالي — ${b.name}",fontWeight=FontWeight.Bold,color=brown,fontSize=18.sp)
        DateField("من تاريخ",from,{from=it})
        DateField("إلى تاريخ",to,{to=it})
        Button(onClick={
            val a=runCatching{LocalDate.parse(from)}.getOrNull()
            val z=runCatching{LocalDate.parse(to)}.getOrNull()
            when {
                a==null || z==null -> err="يرجى تحديد تاريخ البداية والنهاية بصيغة YYYY-MM-DD."
                a.isAfter(z) -> err="تاريخ البداية يجب ألا يتجاوز تاريخ النهاية."
                z.isAfter(LocalDate.now()) -> err="لا يمكن اختيار تاريخ مستقبلي."
                java.time.temporal.ChronoUnit.DAYS.between(a,z)+1 > 10 -> err="يجب ألا تتجاوز فترة التقرير 10 أيام."
                else -> scope.launch {
                    rows=db.dailyDao().between(b.id,from,to)
                    err=if(rows!!.isEmpty()) "لا توجد سجلات في الفترة المحددة." else null
                }
            }
        },modifier=Modifier.fillMaxWidth()){Text("بحث")}
        err?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        rows?.takeIf{it.isNotEmpty()}?.let{list->
            val start=LocalDate.parse(from)
            val end=LocalDate.parse(to)
            val days=(0L..java.time.temporal.ChronoUnit.DAYS.between(start,end)).map{start.plusDays(it)}
            val byDate=list.associateBy{it.date}
            Text("الفترة: ${prettyDate(from)} — ${prettyDate(to)}",fontWeight=FontWeight.SemiBold)
            Text("مرّر الجدول أفقيًا لرؤية جميع الأيام والإجمالي.",color=Color.Gray,fontSize=12.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Column {
                    Row {
                        TableCell("البند",110.dp,true)
                        days.forEach { d -> TableCell("${d.dayOfMonth}/${d.monthValue}\n${d.dayOfWeek.getDisplayName(TextStyle.SHORT,ar)}",88.dp,true) }
                        TableCell("الإجمالي",105.dp,true)
                    }
                    Row {
                        TableCell("الإيراد",110.dp,true)
                        days.forEach { d -> TableCell(byDate[d.format(dbFmt)]?.let{fmt(it.revenue)} ?: "—",88.dp) }
                        TableCell(fmt(list.sumOf{it.revenue}),105.dp,true)
                    }
                    Row {
                        TableCell("المباع آجل",110.dp,true)
                        days.forEach { d -> TableCell(byDate[d.format(dbFmt)]?.let{fmt(it.creditSold)} ?: "—",88.dp) }
                        TableCell(fmt(list.sumOf{it.creditSold}),105.dp,true)
                    }
                }
            }
            Text("يحتوي التقرير على بندي الإيراد والمباع آجل فقط.",color=Color.Gray,fontSize=12.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick={onExport(list,from,to)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) { Icon(Icons.Default.PictureAsPdf,null); Spacer(Modifier.width(8.dp)); Text("تصدير التقرير المالي PDF") }
        }
    }
}
@Composable private fun TableCell(value:String,width:Dp,bold:Boolean=false) {
    Box(Modifier.width(width).border(1.dp,Color(0xFFD9C8B9)).background(if(bold) Color(0xFFFFE8D3) else Color.White).padding(8.dp),contentAlignment=Alignment.Center) {
        Text(value,fontWeight=if(bold) FontWeight.Bold else FontWeight.Normal,fontSize=12.sp)
    }
}

@Composable private fun DateField(label:String,value:String,onValue:(String)->Unit) {
    val context=LocalContext.current
    val selected=runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }
    OutlinedTextField(value=if(value.isBlank()) "" else prettyDate(value),onValueChange={},readOnly=true,label={Text(label)},placeholder={Text("اختر التاريخ")},modifier=Modifier.fillMaxWidth(),
        trailingIcon={ IconButton(onClick={ DatePickerDialog(context,{_,year,month,day -> onValue(LocalDate.of(year,month+1,day).format(dbFmt)) },selected.year,selected.monthValue-1,selected.dayOfMonth).apply { datePicker.maxDate=System.currentTimeMillis() }.show() }) { Icon(Icons.Default.CalendarMonth,contentDescription="اختيار التاريخ") } })
}
private fun drawRtlPdfText(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, width: Int, paint: TextPaint): Int {
    if (text.isBlank()) return 0
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
        .setTextDirection(TextDirectionHeuristics.RTL)
        .setIncludePad(false)
        .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
    return layout.height
}

private fun createRaghifPdf(
    activity: ComponentActivity,
    bakery: Bakery,
    title: String,
    subtitle: String,
    rows: List<Pair<String, String>>,
    fileName: String
) {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 38f
    val contentWidth = (pageWidth - margin * 2).toInt()
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(105, 53, 34)
        textSize = 22f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val bakeryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(52, 37, 30)
        textSize = 18f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val normalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(55, 48, 43)
        textSize = 12.5f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val labelPaint = TextPaint(normalPaint).apply { typeface = Typeface.create("sans-serif", Typeface.BOLD) }
    val footerPaint = TextPaint(normalPaint).apply {
        color = android.graphics.Color.rgb(120, 105, 94)
        textSize = 9.5f
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var y = 0f

    fun startNewPage() {
        page?.let { document.finishPage(it) }
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        val canvas = page!!.canvas
        canvas.drawColor(android.graphics.Color.WHITE)
        fillPaint.color = android.graphics.Color.rgb(135, 71, 47)
        canvas.drawRoundRect(margin, 28f, pageWidth - margin, 34f, 3f, 3f, fillPaint)
        y = 52f
        y += drawRtlPdfText(canvas, "رغيف", margin, y, contentWidth, titlePaint) + 5f
        y += drawRtlPdfText(canvas, bakery.name, margin, y, contentWidth, bakeryPaint) + 4f
        y += drawRtlPdfText(canvas, title, margin, y, contentWidth, titlePaint) + 3f
        y += drawRtlPdfText(canvas, subtitle, margin, y, contentWidth, normalPaint) + 15f
        fillPaint.color = android.graphics.Color.rgb(246, 237, 226)
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 25f, 6f, 6f, fillPaint)
        drawRtlPdfText(canvas, "البيان", pageWidth - margin - 255f, y + 6f, 245, labelPaint)
        drawRtlPdfText(canvas, "القيمة", margin + 10f, y + 6f, 185, labelPaint)
        y += 32f
    }

    startNewPage()
    rows.forEachIndexed { index, row ->
        val canvas = page!!.canvas
        val labelLayout = StaticLayout.Builder.obtain(row.first, 0, row.first.length, normalPaint, 245)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).build()
        val valueLayout = StaticLayout.Builder.obtain(row.second, 0, row.second.length, normalPaint, 185)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).setMaxLines(4).build()
        val rowHeight = maxOf(30, maxOf(labelLayout.height, valueLayout.height) + 12).toFloat()
        if (y + rowHeight > pageHeight - 58f) startNewPage()
        val target = page!!.canvas
        if (index % 2 == 0) {
            fillPaint.color = android.graphics.Color.rgb(252, 249, 245)
            target.drawRect(margin, y, pageWidth - margin, y + rowHeight, fillPaint)
        }
        drawRtlPdfText(target, row.first, pageWidth - margin - 255f, y + 6f, 245, normalPaint)
        drawRtlPdfText(target, row.second, margin + 10f, y + 6f, 185, normalPaint)
        fillPaint.color = android.graphics.Color.rgb(230, 220, 210)
        fillPaint.strokeWidth = 0.6f
        target.drawLine(margin, y + rowHeight, pageWidth - margin, y + rowHeight, fillPaint)
        y += rowHeight
    }
    val canvas = page!!.canvas
    fillPaint.color = android.graphics.Color.rgb(135, 71, 47)
    canvas.drawLine(margin, pageHeight - 42f, pageWidth - margin, pageHeight - 42f, fillPaint)
    drawRtlPdfText(canvas, "إعداد التطبيق عبدالعزيز سعيدان", margin, pageHeight - 34f, contentWidth / 2, footerPaint)
    val pageLabel = "صفحة $pageNumber"
    val pageLayout = StaticLayout.Builder.obtain(pageLabel, 0, pageLabel.length, footerPaint, 130)
        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).build()
    canvas.save(); canvas.translate(pageWidth - margin - 130f, pageHeight - 34f); pageLayout.draw(canvas); canvas.restore()
    document.finishPage(page!!)
    val directory = File(activity.cacheDir, "shared_reports").apply { mkdirs() }
    val pdf = File(directory, fileName)
    pdf.outputStream().use { document.writeTo(it) }
    document.close()
    val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", pdf)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(activity.contentResolver, "تقرير رغيف", uri)
    }
    activity.startActivity(Intent.createChooser(intent, "مشاركة تقرير PDF"))
}

private fun shareDailyRecord(activity: ComponentActivity, bakery: Bakery, record: DailyRecord) {
    val rows = listOf(
        "التاريخ" to prettyDate(record.date),
        "اليوم" to dayName(record.date),
        "الملاحظة" to record.note.ifBlank { "لا توجد" },
        "كمية الدقيق" to "${fmt(record.flourKg)} كجم",
        "عدد الأقراص" to fmt(record.pieces),
        "عشاء العمال" to fmt(record.workersMeal),
        "التالف" to fmt(record.waste),
        "المباع آجل" to fmt(record.creditSold),
        "الإيراد" to "${fmt(record.revenue)} ريال",
        "المباع نقدًا" to "${fmt(cashSold(record))} حبة",
        "الفرق" to "${fmt(difference(record))} حبة",
        "متوسط إنتاج الكيس" to "${avgBag(record)?.let { fmt(it) } ?: "غير متاح"} حبة/كيس"
    )
    createRaghifPdf(activity, bakery, "تقرير السجل اليومي", "تاريخ السجل: ${prettyDate(record.date)}", rows, "raghif_daily_${record.date}.pdf")
}

private fun shareFinancialReport(activity: ComponentActivity, bakery: Bakery, records: List<DailyRecord>, from: String, to: String) {
    val rows = mutableListOf<Pair<String, String>>()
    rows += "الفترة" to "${prettyDate(from)} إلى ${prettyDate(to)}"
    records.sortedBy { it.date }.forEach { record ->
        rows += "الإيراد — ${prettyDate(record.date)}" to "${fmt(record.revenue)} ريال"
        rows += "المباع آجل — ${prettyDate(record.date)}" to fmt(record.creditSold)
    }
    rows += "إجمالي الإيراد" to "${fmt(records.sumOf { it.revenue })} ريال"
    rows += "إجمالي المباع آجل" to fmt(records.sumOf { it.creditSold })
    createRaghifPdf(activity, bakery, "التقرير المالي", "الفترة المحددة: ${prettyDate(from)} — ${prettyDate(to)}", rows, "raghif_financial_${from}_${to}.pdf")
}

@Composable private fun SettingsScreen(nightMode:Boolean,onNightModeChange:(Boolean)->Unit) {
    var sound by remember { mutableStateOf(true) }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("الوضع الليلي"); Switch(checked=nightMode,onCheckedChange=onNightModeChange) } }
        Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("صوت النقر"); Switch(sound,{sound=it}) } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("رغيف — الإصدار 1.0.0"); Text("حسابات بلا تعقيد",color=brown); Text("إعداد التطبيق عبدالعزيز سعيدان",color=Color.Gray) } }
    }
}
