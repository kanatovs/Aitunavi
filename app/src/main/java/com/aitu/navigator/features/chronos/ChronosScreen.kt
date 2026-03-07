package com.aitu.navigator.features.chronos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private sealed class ChronosPage {
    data object Home : ChronosPage()
    data object Clubs : ChronosPage()
    data class ClubDetails(val club: Club) : ChronosPage()
    data object QaSoon : ChronosPage()
}

@Composable
fun ChronosScreen() {
    var page by remember { mutableStateOf<ChronosPage>(ChronosPage.Home) }

    when (val current = page) {
        ChronosPage.Home -> ChronosHome(
            onOpenClubs = { page = ChronosPage.Clubs },
            onOpenQa = { page = ChronosPage.QaSoon }
        )

        ChronosPage.Clubs -> ClubsScreen(
            clubs = demoClubs,
            onBack = { page = ChronosPage.Home },
            onOpenClub = { club -> page = ChronosPage.ClubDetails(club) }
        )

        is ChronosPage.ClubDetails -> ClubDetailsScreen(
            club = current.club,
            onBack = { page = ChronosPage.Clubs }
        )

        ChronosPage.QaSoon -> QaComingSoonScreen(
            onBack = { page = ChronosPage.Home }
        )
    }
}

@Composable
private fun ChronosHome(
    onOpenClubs: () -> Unit,
    onOpenQa: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Chronos",
            style = MaterialTheme.typography.headlineSmall
        )

        ChronosMenuCard(
            title = "Клубы",
            subtitle = "Список клубов, описание и соцсети",
            onClick = onOpenClubs
        )

        ChronosMenuCard(
            title = "Вопросы и ответы",
            subtitle = "Раздел FAQ и полезные ответы",
            onClick = onOpenQa
        )
    }
}

@Composable
private fun ChronosMenuCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ClubsScreen(
    clubs: List<Club>,
    onBack: () -> Unit,
    onOpenClub: (Club) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Назад")
            }
            Text("Клубы", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            clubs.forEach { club ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenClub(club) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(club.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(club.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubDetailsScreen(
    club: Club,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Назад")
            }
            Text(club.name, style = MaterialTheme.typography.headlineSmall)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Описание", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(club.description)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Соцсети", style = MaterialTheme.typography.titleMedium)

                if (club.telegram != null) {
                    OutlinedButton(
                        onClick = { /* потом откроем ссылку */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Telegram: ${club.telegram}")
                    }
                }

                if (club.instagram != null) {
                    OutlinedButton(
                        onClick = { /* потом откроем ссылку */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Instagram: ${club.instagram}")
                    }
                }
            }
        }
    }
}

@Composable
private fun QaComingSoonScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Назад")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Скоро будет",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Раздел вопросов и ответов пока находится в разработке.",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}