package com.aitu.navigator.features.chronos

data class Club(
    val id: String,
    val name: String,
    val description: String,
    val telegram: String? = null,
    val instagram: String? = null
)

val demoClubs = listOf(
    Club(
        id = "debate",
        name = "Debate Club",
        description = "Клуб для развития аргументации, публичных выступлений и критического мышления.",
        telegram = "https://t.me/",
        instagram = "https://instagram.com/"
    ),
    Club(
        id = "music",
        name = "Music Club",
        description = "Музыкальный клуб для вокала, инструментов и совместных выступлений.",
        telegram = "https://t.me/",
        instagram = "https://instagram.com/"
    ),
    Club(
        id = "it",
        name = "IT Club",
        description = "Сообщество студентов, интересующихся программированием, хакатонами и проектами.",
        telegram = "https://t.me/simply_formula",
        instagram = "https://instagram.com/"
    )
)