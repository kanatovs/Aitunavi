package com.aitu.navigator.features.atlas

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AtlasScreen(vm: AtlasViewModel = viewModel()) {

    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val history by vm.history.collectAsState()
    val building by vm.selectedBuilding.collectAsState()
    val floor by vm.selectedFloor.collectAsState()
    val mapUrl by vm.mapUrl.collectAsState()
    val reloadTick by vm.reloadTick.collectAsState()

    var showHistory by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // --- SEARCH ROW ---
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = { Text("Поиск: аудитория / POI") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = vm::search) { Text("Поиск") }
        }

        // --- HISTORY BUTTON ---
        OutlinedButton(
            onClick = { showHistory = !showHistory },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showHistory) "Скрыть историю" else "История")
        }

        // --- HISTORY PANEL ---
        if (showHistory && history.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(history) { h ->
                        AssistChip(
                            onClick = {
                                vm.setQuery(h)
                                vm.search()
                                showHistory = false
                            },
                            label = { Text(h) }
                        )
                    }
                }
            }
        }

        // --- RESULTS PANEL ---
        if (results.isNotEmpty()) {
            Text("Результаты:", style = MaterialTheme.typography.titleMedium)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { r ->
                        Card(
                            onClick = { vm.pickResult(r) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(r.title, style = MaterialTheme.typography.titleMedium)
                                if (r.subtitle.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(r.subtitle, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- MAP CONTROLS (в отдельной карточке, аккуратно) ---
        Text("Карта", style = MaterialTheme.typography.titleMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Buildings row (горизонтальный скролл на маленьких экранах)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = building == "C1", onClick = { vm.setBuilding("C1") }, label = { Text("C1") })
                    FilterChip(selected = building == "C2", onClick = { vm.setBuilding("C2") }, label = { Text("C2") })
                    FilterChip(selected = building == "C3", onClick = { vm.setBuilding("C3") }, label = { Text("C3") })
                }

                // Floors + refresh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = floor == 1, onClick = { vm.setFloor(1) }, label = { Text("1") })
                        FilterChip(selected = floor == 2, onClick = { vm.setFloor(2) }, label = { Text("2") })
                        FilterChip(selected = floor == 3, onClick = { vm.setFloor(3) }, label = { Text("3") })
                    }

                    Button(onClick = vm::refreshMap) { Text("Обновить") }
                }
            }
        }

        // --- WEBVIEW (занимает остаток высоты всегда) ---

        var webViewRef by remember { mutableStateOf<WebView?>(null) }
        var webError by remember { mutableStateOf<String?>(null) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(Modifier.fillMaxSize()) {

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this

                            // --- Web settings ---
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true

                            // zoom + gestures
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false

                            // нормальный viewport
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true

                            // чтобы скролл работал адекватно
                            isVerticalScrollBarEnabled = true
                            isHorizontalScrollBarEnabled = true

                            // чтобы WebView не блокировал скролл родителя
                            setOnTouchListener { v, event ->
                                when (event.actionMasked) {
                                    android.view.MotionEvent.ACTION_DOWN,
                                    android.view.MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                                    android.view.MotionEvent.ACTION_UP,
                                    android.view.MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                                false
                            }

                            // cache (на эмуляторе иногда помогает NO_CACHE)
                            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    webError = null
                                    view?.let { injectViewportFix(it) } // <-- важно (см. ниже)
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    webError = description ?: "Ошибка загрузки"
                                }
                            }

                            loadUrl(mapUrl)
                        }
                    },
                    update = { wv ->
                        // если URL поменялся — грузим
                        if (wv.url != mapUrl) wv.loadUrl(mapUrl)
                    }
                )

                // reload по тикеру (кнопка "Обновить")
                LaunchedEffect(reloadTick) {
                    webViewRef?.apply {
                        clearCache(true)
                        reload()
                    }
                }

                // Оверлей ошибки + кнопка повторить
                if (webError != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Карта не загрузилась", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(webError ?: "")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.refreshMap() }) {
                            Text("Повторить")
                        }
                    }
                }
            }
        }
    }
}
private fun injectViewportFix(wv: WebView) {
    val js = """
        (function() {
          var meta = document.querySelector('meta[name=viewport]');
          if(!meta){
            meta = document.createElement('meta');
            meta.name = 'viewport';
            document.head.appendChild(meta);
          }
          meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
          document.documentElement.style.width = '100%';
          document.body.style.width = '100%';
          document.body.style.margin = '0';
          document.body.style.padding = '0';
        })();
    """.trimIndent()
    wv.evaluateJavascript(js, null)
}