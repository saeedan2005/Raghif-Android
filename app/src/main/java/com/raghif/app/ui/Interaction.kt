package com.raghif.app.ui

import android.app.DatePickerDialog
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * A real selectable calendar with today as the default.
 * The date is saved as yyyy-MM-dd, consistent with the Room database.
 */
@Composable
fun RaghifDateField(
    title: String,
    date: String,
    onDateSelected: (String) -> Unit,
    allowFuture: Boolean = false
) {
    val context = LocalContext.current
    val clickSound = LocalRaghifSound.current
    val selected = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
    val display = selected.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    OutlinedButton(
        onClick = {
            clickSound()
            val dialog = DatePickerDialog(
                context,
                { _, year, month, day ->
                    onDateSelected(LocalDate.of(year, month + 1, day).toString())
                },
                selected.year,
                selected.monthValue - 1,
                selected.dayOfMonth
            )
            if (!allowFuture) {
                val end = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                dialog.datePicker.maxDate = end.timeInMillis
            }
            dialog.show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null)
        Text("  $title: $display")
    }
}

/** Audio feedback is optional and persisted by the parent settings screen. */
val LocalRaghifSound = staticCompositionLocalOf<() -> Unit> { {} }

fun playRaghifClick(context: Context) {
    runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 55)
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 65)
        // ToneGenerator.stopTone releases when the short tone is finished by Android.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            tone.stopTone()
            tone.release()
        }, 110)
    }
}
