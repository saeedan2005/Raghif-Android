package com.raghif.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Tap anywhere to choose a date. ISO dates are retained for Room queries. */
@Composable
fun CalendarDateField(
    label: String,
    isoDate: String,
    onDateSelected: (String) -> Unit,
    limitToToday: Boolean = true
) {
    val context = LocalContext.current
    val date = runCatching { LocalDate.parse(isoDate) }.getOrDefault(LocalDate.now())
    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onDateSelected(LocalDate.of(year, month + 1, day).format(DateTimeFormatter.ISO_LOCAL_DATE))
                },
                date.year, date.monthValue - 1, date.dayOfMonth
            ).apply {
                if (limitToToday) datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
        Text(
            text = label + ": " + date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale("ar"))),
            fontWeight = FontWeight.SemiBold
        )
    }
}
