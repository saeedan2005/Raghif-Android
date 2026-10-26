package com.raghif.app.data

import androidx.room.*

@Entity(tableName = "bakeries", indices = [Index(value=["normalizedNumber"], unique=true), Index(value=["normalizedName"], unique=true)])
data class Bakery(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val number: String,
    val normalizedNumber: String,
    val name: String,
    val normalizedName: String,
    val address: String = "",
    val phone: String = "",
    val supervisorMobile: String = "",
    val unitRevenue: Long = 500,
    val piecesPerBag: Long = 9,
    val flourBagWeight: Double = 50.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName="daily_records",
    foreignKeys=[ForeignKey(entity=Bakery::class, parentColumns=["id"], childColumns=["bakeryId"], onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["bakeryId","date"], unique=true)]
)
data class DailyRecord(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val bakeryId: Long,
    val date: String,
    val flourKg: Long,
    val pieces: Long,
    val workersMeal: Long = 0,
    val waste: Long = 0,
    val creditSold: Long = 0,
    val revenue: Long,
    val note: String = "",
    val unitRevenueUsed: Long,
    val piecesPerBagUsed: Long,
    val flourBagWeightUsed: Double
)
