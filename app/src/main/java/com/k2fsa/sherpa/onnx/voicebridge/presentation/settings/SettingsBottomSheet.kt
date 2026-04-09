package com.k2fsa.sherpa.onnx.voicebridge.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class Voice(val sid: Int, val name: String, val gender: String)

private data class Language(
    val code: String,
    val label: String,
    val flag: String,
    val voices: List<Voice>,
)

private val LANGUAGES = listOf(
    Language("en-US", "American English", "\uD83C\uDDFA\uD83C\uDDF8", listOf(
        Voice(0, "Alloy", "F"), Voice(1, "Aoede", "F"), Voice(2, "Bella", "F"),
        Voice(3, "Heart", "F"), Voice(4, "Jessica", "F"), Voice(5, "Kore", "F"),
        Voice(6, "Nicole", "F"), Voice(7, "Nova", "F"), Voice(8, "River", "F"),
        Voice(9, "Sarah", "F"), Voice(10, "Sky", "F"),
        Voice(11, "Adam", "M"), Voice(12, "Echo", "M"), Voice(13, "Eric", "M"),
        Voice(14, "Fenrir", "M"), Voice(15, "Liam", "M"), Voice(16, "Michael", "M"),
        Voice(17, "Onyx", "M"), Voice(18, "Puck", "M"), Voice(19, "Santa", "M"),
    )),
    Language("en-GB", "British English", "\uD83C\uDDEC\uD83C\uDDE7", listOf(
        Voice(20, "Alice", "F"), Voice(21, "Emma", "F"),
        Voice(22, "Isabella", "F"), Voice(23, "Lily", "F"),
        Voice(24, "Daniel", "M"), Voice(25, "Fable", "M"),
        Voice(26, "George", "M"), Voice(27, "Lewis", "M"),
    )),
    Language("es", "Spanish", "\uD83C\uDDEA\uD83C\uDDF8", listOf(
        Voice(28, "Dora", "F"), Voice(29, "Alex", "M"), Voice(30, "Santa", "M"),
    )),
    Language("fr", "French", "\uD83C\uDDEB\uD83C\uDDF7", listOf(
        Voice(31, "Siwis", "F"),
    )),
    Language("hi", "Hindi", "\uD83C\uDDEE\uD83C\uDDF3", listOf(
        Voice(32, "Alpha", "F"), Voice(33, "Beta", "F"),
        Voice(34, "Omega", "M"), Voice(35, "Psi", "M"),
    )),
    Language("it", "Italian", "\uD83C\uDDEE\uD83C\uDDF9", listOf(
        Voice(36, "Sara", "F"), Voice(37, "Nicola", "M"),
    )),
    Language("ja", "Japanese", "\uD83C\uDDEF\uD83C\uDDF5", listOf(
        Voice(38, "Alpha", "F"), Voice(39, "Gongitsune", "F"),
        Voice(40, "Nezumi", "F"), Voice(41, "Tebukuro", "F"), Voice(42, "Kumo", "M"),
    )),
    Language("pt", "Portuguese", "\uD83C\uDDE7\uD83C\uDDF7", listOf(
        Voice(43, "Dora", "F"), Voice(44, "Alex", "M"), Voice(45, "Santa", "M"),
    )),
    Language("zh", "Chinese", "\uD83C\uDDE8\uD83C\uDDF3", listOf(
        Voice(46, "Xiaobei", "F"), Voice(47, "Xiaoni", "F"),
        Voice(48, "Xiaoxiao", "F"), Voice(49, "Xiaoyi", "F"),
        Voice(50, "Yunjian", "M"), Voice(51, "Yunxi", "M"),
        Voice(52, "Yunyang", "M"),
    )),
)

/** Find which language a voice sid belongs to */
private fun findLanguageForSid(sid: Int): Language {
    return LANGUAGES.firstOrNull { lang -> lang.voices.any { it.sid == sid } }
        ?: LANGUAGES.first()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Track selected language (auto-detect from current voice)
    var selectedLanguage by remember(state.voiceId) {
        mutableStateOf(findLanguageForSid(state.voiceId))
    }
    var languageDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            // Header + save checkmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    viewModel.save()
                    onDismiss()
                }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── VPS Connection ──
            Text("VPS Connection", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.host,
                    onValueChange = viewModel::onHostChanged,
                    label = { Text("Tailscale IP") },
                    singleLine = true,
                    modifier = Modifier.weight(3f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = state.port,
                    onValueChange = viewModel::onPortChanged,
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Speech Recognition ──
            Text("Speech Recognition", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.useAndroidAsr) "Android (Google)" else "On-device (Nemotron)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (state.useAndroidAsr)
                            "Best accuracy, great with accents"
                        else
                            "Private, works offline, cross-platform",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.useAndroidAsr,
                    onCheckedChange = { viewModel.onAsrEngineChanged(it) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Voice ──
            Text("Voice", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            // Language dropdown
            Text(
                text = "Language",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = { languageDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${selectedLanguage.flag}  ${selectedLanguage.label}",
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = languageDropdownExpanded,
                onDismissRequest = { languageDropdownExpanded = false },
            ) {
                for (lang in LANGUAGES) {
                    DropdownMenuItem(
                        text = {
                            Text("${lang.flag}  ${lang.label}  (${lang.voices.size} voices)")
                        },
                        onClick = {
                            selectedLanguage = lang
                            languageDropdownExpanded = false
                            // Auto-select first voice of this language if current isn't in it
                            if (lang.voices.none { it.sid == state.voiceId }) {
                                viewModel.onVoiceIdChanged(lang.voices.first().sid)
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voices for selected language
            val femaleVoices = selectedLanguage.voices.filter { it.gender == "F" }
            val maleVoices = selectedLanguage.voices.filter { it.gender == "M" }

            if (femaleVoices.isNotEmpty()) {
                Text(
                    text = "Female",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (voice in femaleVoices) {
                        FilterChip(
                            selected = state.voiceId == voice.sid,
                            onClick = { viewModel.onVoiceIdChanged(voice.sid) },
                            label = { Text(voice.name, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (maleVoices.isNotEmpty()) {
                Text(
                    text = "Male",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (voice in maleVoices) {
                        FilterChip(
                            selected = state.voiceId == voice.sid,
                            onClick = { viewModel.onVoiceIdChanged(voice.sid) },
                            label = { Text(voice.name, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.save()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
