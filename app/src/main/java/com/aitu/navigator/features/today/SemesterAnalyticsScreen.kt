package com.aitu.navigator.features.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SemesterAnalyticsScreen(
    vm: TodayViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by vm.ui.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    val semesterStart = LocalDate.now().minusMonths(1)
    val semesterEnd = LocalDate.now().plusMonths(3)

    val totalLessons = when (val s = state) {
        is TodayUiState.Content -> s.groupData.schedule.size
        else -> 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Назад") }
        Text("Семестр в цифрах", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Оставшиеся пары с учётом дат семестра.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Аналитика", style = MaterialTheme.typography.titleMedium)
                Text("Осталось пар: $totalLessons", style = MaterialTheme.typography.headlineSmall)
                Text("Период: ${semesterStart.format(formatter)} — ${semesterEnd.format(formatter)}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Настройки семестра", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Начало")
                    Text(semesterStart.format(formatter))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Конец")
                    Text(semesterEnd.format(formatter))
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Добавить каникулы/перерывы")
                }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Добавить праздники/выходные")
                }
            }
        }
    }
}
