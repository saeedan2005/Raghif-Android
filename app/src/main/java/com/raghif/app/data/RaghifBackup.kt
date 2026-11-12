package com.raghif.app.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Offline backup of all bakeries and daily records.
 * Import is all-or-nothing: parse and validate before touching the database,
 * then restore inside one Room transaction (rollback on any insert failure).
 */
object RaghifBackup {
    private const val FORMAT = "raghif-backup"
    private const val VERSION = 1

    suspend fun export(database: AppDatabase): ByteArray {
        val root = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("createdAt", System.currentTimeMillis())
        }
        val bakeries = JSONArray()
        database.bakeryDao().all().forEach { b ->
            bakeries.put(JSONObject().apply {
                put("id", b.id)
                put("number", b.number)
                put("normalizedNumber", b.normalizedNumber)
                put("name", b.name)
                put("normalizedName", b.normalizedName)
                put("address", b.address)
                put("phone", b.phone)
                put("supervisorMobile", b.supervisorMobile)
                put("unitRevenue", b.unitRevenue)
                put("piecesPerBag", b.piecesPerBag)
                put("flourBagWeight", b.flourBagWeight)
                put("createdAt", b.createdAt)
            })
        }
        val records = JSONArray()
        database.dailyDao().allRecords().forEach { r ->
            records.put(JSONObject().apply {
                put("id", r.id)
                put("bakeryId", r.bakeryId)
                put("date", r.date)
                put("flourKg", r.flourKg)
                put("pieces", r.pieces)
                put("workersMeal", r.workersMeal)
                put("waste", r.waste)
                put("creditSold", r.creditSold)
                put("revenue", r.revenue)
                put("note", r.note)
                put("unitRevenueUsed", r.unitRevenueUsed)
                put("piecesPerBagUsed", r.piecesPerBagUsed)
                put("flourBagWeightUsed", r.flourBagWeightUsed)
            })
        }
        root.put("bakeries", bakeries)
        root.put("dailyRecords", records)
        return root.toString(2).toByteArray(Charsets.UTF_8)
    }

    suspend fun restore(database: AppDatabase, bytes: ByteArray): Pair<Int, Int> {
        require(bytes.size <= 20 * 1024 * 1024) { "ملف النسخة الاحتياطية كبير جدًا" }
        val doc = JSONObject(bytes.toString(Charsets.UTF_8))
        require(doc.optString("format") == FORMAT && doc.optInt("version") == VERSION) {
            "هذا الملف ليس نسخة احتياطية متوافقة من رغيف"
        }
        val bakeryJson = doc.getJSONArray("bakeries")
        val recordJson = doc.getJSONArray("dailyRecords")
        val bakeries = (0 until bakeryJson.length()).map { index ->
            bakeryJson.getJSONObject(index).let { j ->
                Bakery(
                    id = j.getLong("id"),
                    number = j.getString("number"),
                    normalizedNumber = j.getString("normalizedNumber"),
                    name = j.getString("name"),
                    normalizedName = j.getString("normalizedName"),
                    address = j.getString("address"),
                    phone = j.getString("phone"),
                    supervisorMobile = j.getString("supervisorMobile"),
                    unitRevenue = j.getLong("unitRevenue"),
                    piecesPerBag = j.getLong("piecesPerBag"),
                    flourBagWeight = j.getDouble("flourBagWeight"),
                    createdAt = j.getLong("createdAt")
                )
            }
        }
        val records = (0 until recordJson.length()).map { index ->
            recordJson.getJSONObject(index).let { j ->
                DailyRecord(
                    id = j.getLong("id"),
                    bakeryId = j.getLong("bakeryId"),
                    date = j.getString("date"),
                    flourKg = j.getLong("flourKg"),
                    pieces = j.getLong("pieces"),
                    workersMeal = j.getLong("workersMeal"),
                    waste = j.getLong("waste"),
                    creditSold = j.getLong("creditSold"),
                    revenue = j.getLong("revenue"),
                    note = j.getString("note"),
                    unitRevenueUsed = j.getLong("unitRevenueUsed"),
                    piecesPerBagUsed = j.getLong("piecesPerBagUsed"),
                    flourBagWeightUsed = j.getDouble("flourBagWeightUsed")
                )
            }
        }

        val ids = HashSet<Long>()
        val numbers = HashSet<String>()
        val names = HashSet<String>()
        bakeries.forEach { b ->
            require(b.id > 0 && ids.add(b.id) &&
                b.number.isNotBlank() && b.name.isNotBlank() &&
                b.normalizedNumber.isNotBlank() && numbers.add(b.normalizedNumber) &&
                b.normalizedName.isNotBlank() && names.add(b.normalizedName) &&
                b.unitRevenue > 0 && b.piecesPerBag > 0 &&
                b.flourBagWeight > 0.0 && b.flourBagWeight.isFinite()
            ) { "بيانات المخابز في النسخة الاحتياطية غير صالحة أو مكررة" }
        }
        val recordIds = HashSet<Long>()
        val uniqueDates = HashSet<Pair<Long, String>>()
        records.forEach { r ->
            require(
                r.id > 0 && recordIds.add(r.id) &&
                r.bakeryId in ids && runCatching { LocalDate.parse(r.date) }.isSuccess &&
                uniqueDates.add(r.bakeryId to r.date) &&
                r.flourKg >= 0 && r.pieces >= 0 && r.workersMeal >= 0 &&
                r.waste >= 0 && r.creditSold >= 0 && r.revenue >= 0 &&
                r.unitRevenueUsed > 0 && r.piecesPerBagUsed > 0 &&
                r.flourBagWeightUsed > 0.0 && r.flourBagWeightUsed.isFinite()
            ) { "يوجد سجل غير صالح أو مكرر في النسخة الاحتياطية" }
        }

        database.withTransaction {
            database.dailyDao().clear()
            database.bakeryDao().clear()
            bakeries.forEach { database.bakeryDao().insert(it) }
            records.forEach { database.dailyDao().insert(it) }
        }
        return bakeries.size to records.size
    }
}
