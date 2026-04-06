/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package dev.pthomain.android.dejavu.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class DemoActivity : ComponentActivity() {

    private val viewModel: DemoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DemoScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(viewModel: DemoViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DejaVu 3.0 Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButtons(
                isLoading = state.isLoading,
                onLoad = viewModel::loadCatFact,
                onRefresh = viewModel::refreshCatFact,
                onOffline = viewModel::goOffline,
                onInvalidate = viewModel::invalidateCache,
                onClear = viewModel::clearCache,
                onClearLog = viewModel::clearLog
            )

            SettingsSection(
                state = state,
                onHttpClientChange = viewModel::setHttpClient,
                onPersistenceChange = viewModel::setPersistence,
                onFreshOnlyChange = viewModel::setFreshOnly,
                onEncryptChange = viewModel::setEncrypt
            )

            HorizontalDivider()

            ResultsList(
                results = state.results,
                logLines = state.logLines,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ActionButtons(
    isLoading: Boolean,
    onLoad: () -> Unit,
    onRefresh: () -> Unit,
    onOffline: () -> Unit,
    onInvalidate: () -> Unit,
    onClear: () -> Unit,
    onClearLog: () -> Unit
) {
    if (isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onLoad, modifier = Modifier.weight(1f), enabled = !isLoading) {
            Text("Load")
        }
        Button(onClick = onRefresh, modifier = Modifier.weight(1f), enabled = !isLoading) {
            Text("Refresh")
        }
        OutlinedButton(onClick = onClearLog, modifier = Modifier.weight(1f)) {
            Text("Clear")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onOffline, modifier = Modifier.weight(1f), enabled = !isLoading) {
            Text("Offline")
        }
        FilledTonalButton(onClick = onInvalidate, modifier = Modifier.weight(1f), enabled = !isLoading) {
            Text("Invalidate")
        }
        FilledTonalButton(onClick = onClear, modifier = Modifier.weight(1f), enabled = !isLoading) {
            Text("Clear Cache")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSection(
    state: DemoUiState,
    onHttpClientChange: (HttpClientType) -> Unit,
    onPersistenceChange: (PersistenceType) -> Unit,
    onFreshOnlyChange: (Boolean) -> Unit,
    onEncryptChange: (Boolean) -> Unit
) {
    Text("HTTP Client", style = MaterialTheme.typography.labelMedium)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        HttpClientType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                selected = state.httpClient == type,
                onClick = { onHttpClientChange(type) },
                shape = SegmentedButtonDefaults.itemShape(index, HttpClientType.entries.size)
            ) {
                Text(
                    when (type) {
                        HttpClientType.RETROFIT_ANNOTATION -> "Annotation"
                        HttpClientType.RETROFIT_HEADER -> "Header"
                        HttpClientType.KTOR -> "Ktor"
                    }, style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            PersistenceType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = state.persistence == type,
                    onClick = { onPersistenceChange(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, PersistenceType.entries.size)
                ) {
                    Text(type.name, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.freshOnly, onCheckedChange = onFreshOnlyChange)
            Text("Fresh", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.encrypt, onCheckedChange = onEncryptChange)
            Text("Encrypt", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ResultsList(
    results: List<CacheResultItem>,
    logLines: List<String>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(results) { result ->
            ResultCard(result)
        }

        if (logLines.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(logLines) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ResultCard(result: CacheResultItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (result.source) {
                "FRESH" -> MaterialTheme.colorScheme.primaryContainer
                "STALE" -> MaterialTheme.colorScheme.tertiaryContainer
                "NETWORK" -> MaterialTheme.colorScheme.secondaryContainer
                "ERROR" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                result.status?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (result.duration.isNotEmpty()) {
                    Text(
                        text = result.duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (result.error != null) {
                Text(
                    text = result.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (result.fact != null) {
                Text(
                    text = result.fact,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
