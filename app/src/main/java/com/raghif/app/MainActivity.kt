package com.raghif.app

import android.content.Intent
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

private val brown = Color(0xFFA65B35)
private val cream = Color(0xFFFFF8F1)
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
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MaterialTheme(colorScheme = lightColorScheme(primary=brown, background=cream, surface=Color.White, secondary=Color(0xFFD8A15D))) { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { RaghifApp() } } } }
}

@Composable private fun RaghifApp() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val bakeries by db.bakeryDao().observeAll().collectAsState(initial=emptyList())
    var selected by remember { mutableStateOf<Bakery?>(null) }
    var page by remember { mutableStateOf("welcome") }
    var message by remember { mutableStateOf<String?>(null) }
    var editingBakery by remember { mutableStateOf<Bakery?>(null) }
    var currentRecord by remember { mutableStateOf<DailyRecord?>(null) }
    var reportRows by remember { mutableStateOf<List<DailyRecord>>(emptyList()) }

    Scaffold(containerColor=cream, topBar={
        TopAppBarRaghif(title=when(page) { "welcome"->"رغيف"; "bakeries"->"قائمة المخابز"; "bakeryForm"->if(editingBakery==null) "إنشاء مخبز جديد" else "تعديل المخبز"; "daily"->selected?.name ?: "البيانات اليومية"; "stats"->"الإحصاءات"; "financial"->"التقرير المالي"; "settings"->"الإعدادات"; else->"رغيف" }, onBack={ page=when(page){"bakeries"->"welcome";"daily","settings"->"bakeries";"stats","financial"->"daily";"bakeryForm"->"bakeries";else->"welcome"} })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when(page) {
                "welcome" -> {
                    Spacer(Modifier.height(28.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment=Alignment.Center) {
                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Box(Modifier.size(104.dp).background(Color(0xFFF7C77A), RoundedCornerShape(30.dp)), contentAlignment=Alignment.Center) { Icon(Icons.Default.BakeryDining, null, tint=brown, modifier=Modifier.size(68.dp)) }
                            Spacer(Modifier.height(12.dp)); Text("رغيف", style=MaterialTheme.typography.headlineLarge, fontWeight=FontWeight.Bold, color=brown)
                            Text("حسابات يومية... بال تعقيد", color=Color(0xFF745747))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    BigButton("تسجيل الدخول", Icons.Default.Login) { page="bakeries" }
                    BigButton("إنشاء مخبز جديد", Icons.Default.AddBusiness) { editingBakery=null; page="bakeryForm" }
                    Spacer(Modifier.weight(1f))
                    Text("تعمل البيانات محليًا على هذا الجهاز", modifier=Modifier.align(Alignment.CenterHorizontally), color=Color.Gray)
                }
                "bakeries" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={editingBakery=null;page="bakeryForm"}, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Add,null); Text("مخبز جديد") }
                        OutlinedButton(onClick={page="settings"}) { Icon(Icons.Default.Settings,null); Text("الإعدادات") }
                    }
                    if (bakeries.isEmpty()) EmptyMessage("لا توجد مخابز مضافة. أضف مخبزًا للبدء.")
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        items(bakeries, key={it.id}) { b ->
                            Card(Modifier.fillMaxWidth().clickable { selected=b; currentRecord=null; page="daily" }, shape=RoundedCornerShape(18.dp)) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { Text(b.name, fontWeight=FontWeight.Bold, fontSize=18.sp); Text("رقم المخبز: ${b.number}", color=Color.Gray); Text("إعدادات البيع: ${fmt(b.unitRevenue)} ريال / ${fmt(b.piecesPerBag)} حبات", fontSize=12.sp) }
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
                        val share = "رغيف | ${b.name}\nالتاريخ: ${prettyDate(record.date)}\nاليوم: ${dayName(record.date)}\nالملاحظة: ${record.note}\nكمية الدقيق: ${fmt(record.flourKg)} كجم\nعدد الأقراص: ${fmt(record.pieces)}\nعشاء العمال: ${fmt(record.workersMeal)}\nالتالف: ${fmt(record.waste)}\nالمباع آجل: ${fmt(record.creditSold)}\nالإيراد: ${fmt(record.revenue)} ريال\nالمباع نقدًا: ${fmt(cashSold(record))}\nالفرق: ${fmt(difference(record))}"
                        val intent=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,share)
                        context.startActivity(Intent.createChooser(intent,"مشاركة السجل"))
                    },message={message=it})
                }
                "stats" -> selected?.let { StatsScreen(db,it) }
                "financial" -> selected?.let { FinancialScreen(db,it) }
                "settings" -> SettingsScreen()
            }
        }
    }
    if(message!=null) AlertDialog(onDismissRequest={message=null}, confirmButton={TextButton(onClick={message=null}){Text("موافق")} }, title={Text("رغيف")}, text={Text(message.orEmpty())})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TopAppBarRaghif(title:String,onBack:()->Unit) {
    TopAppBar(title={Text(title,fontWeight=FontWeight.Bold)}, navigationIcon={ IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null)} }, colors=TopAppBarDefaults.topAppBarColors(containerColor=cream))
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
        Text("حسابات يومية... بال تعقيد",color=Color.Gray)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={scope.launch { history=db.dailyDao().all(bakery.id);showHistory=true }},modifier=Modifier.weight(1f)){Icon(Icons.Default.Search,null);Text("السجلات السابقة")}
            OutlinedButton(onClick=onStats,modifier=Modifier.weight(1f)){Icon(Icons.Default.BarChart,null);Text("الإحصاءات")}
        }
        OutlinedButton(onClick=onFinancial,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.ReceiptLong,null);Text("التقرير المالي")}
        Field("التاريخ (YYYY-MM-DD)",date,{date=it})
        Text("اليوم: ${dayName(date)}",color=brown,fontWeight=FontWeight.Bold)
        Field("المالحظة",note,{note=it},singleLine=false)
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
        Field("من تاريخ (YYYY-MM-DD)",from,{from=it});Field("إلى تاريخ (YYYY-MM-DD)",to,{to=it})
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

@Composable private fun FinancialScreen(db:AppDatabase,b:Bakery) {
    val scope= rememberCoroutineScope()
    var from by remember{mutableStateOf("")}
    var to by remember{mutableStateOf("")}
    var rows by remember{mutableStateOf<List<DailyRecord>?>(null)}
    var err by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("التقرير المالي — ${b.name}",fontWeight=FontWeight.Bold,color=brown,fontSize=18.sp)
        Field("من تاريخ (YYYY-MM-DD)",from,{from=it})
        Field("إلى تاريخ (YYYY-MM-DD)",to,{to=it})
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
        }
    }
}
@Composable private fun TableCell(value:String,width:Dp,bold:Boolean=false) {
    Box(Modifier.width(width).border(1.dp,Color(0xFFD9C8B9)).background(if(bold) Color(0xFFFFE8D3) else Color.White).padding(8.dp),contentAlignment=Alignment.Center) {
        Text(value,fontWeight=if(bold) FontWeight.Bold else FontWeight.Normal,fontSize=12.sp)
    }
}
@Composable private fun SettingsScreen() {
    var night by remember { mutableStateOf(false) }; var sound by remember { mutableStateOf(true) }; var biometric by remember { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Card { Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("الوضع الليلي");Switch(night,{night=it})} }
        Card { Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("صوت النقر");Switch(sound,{sound=it})} }
        Card { Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("الدخول بالبصمة (واجهة إعداد أولية)");Switch(biometric,{biometric=it})} }
        Text("رغيف — الإصدار 1.0.0\nحسابات يومية... بال تعقيد",color=Color.Gray)
    }
}
