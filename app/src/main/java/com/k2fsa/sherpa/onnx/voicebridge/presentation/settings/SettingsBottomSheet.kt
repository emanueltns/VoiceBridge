package com.k2fsa.sherpa.onnx.voicebridge.presentation.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Kokoro multi-lang v1_0: 53 speakers
 * sid -> (name, group)
 */
private data class Voice(val sid: Int, val name: String)

private data class VoiceGroup(val label: String, val voices: List<Voice>)

private val VOICE_GROUPS = listOf(
    VoiceGroup("American English - Female", listOf(
        Voice(0, "Alloy"), Voice(1, "Aoede"), Voice(2, "Bella"),
        Voice(3, "Heart"), Voice(4, "Jessica"), Voice(5, "Kore"),
        Voice(6, "Nicole"), Voice(7, "Nova"), Voice(8, "River"),
        Voice(9, "Sarah"), Voice(10, "Sky"),
    )),
    VoiceGroup("American English - Male", listOf(
        Voice(11, "Adam"), Voice(12, "Echo"), Voice(13, "Eric"),
        Voice(14, "Fenrir"), Voice(15, "Liam"), Voice(16, "Michael"),
        Voice(17, "Onyx"), Voice(18, "Puck"), Voice(19, "Santa"),
    )),
    VoiceGroup("British English - Female", listOf(
        Voice(20, "Alice"), Voice(21, "Emma"),
        Voice(22, "Isabella"), Voice(23, "Lily"),
    )),
    VoiceGroup("British English - Male", listOf(
        Voice(24, "Daniel"), Voice(25, "Fable"),
        Voice(26, "George"), Voice(27, "Lewis"),
    )),
    VoiceGroup("Spanish", listOf(
        Voice(28, "Dora"), Voice(29, "Alex"), Voice(30, "Santa"),
    )),
    VoiceGroup("French", listOf(
        Voice(31, "Siwis"),
    )),
    VoiceGroup("Hindi", listOf(
        Voice(32, "Alpha"), Voice(33, "Beta"),
        Voice(34, "Omega"), Voice(35, "Psi"),
    )),
    VoiceGroup("Italian", listOf(
        Voice(36, "Sara"), Voice(37, "Nicola"),
    )),
    VoiceGroup("Japanese", listOf(
        Voice(38, "Alpha"), Voice(39, "Gongitsune"),
        Voice(40, "Nezumi"), Voice(41, "Tebukuro"), Voice(42, "Kumo"),
    )),
    VoiceGroup("Portuguese", listOf(
        Voice(43, "Dora"), Voice(44, "Alex"), Voice(45, "Santa"),
    )),
    VoiceGroup("Chinese (Mandarin)", listOf(
        Voice(46, "Xiaobei"), Voice(47, "Xiaoni"),
        Voice(48, "Xiaoxiao"), Voice(49, "Xiaoyi"),
        Voice(50, "Yunjian"), Voice(51, "Yunxi"),
        Voice(52, "Yunyang"),
    )),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

            // VPS Connection
            Text(
                text = "VPS Connection",
                style = MaterialTheme.typography.titleLarge,
            )
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

            // Voice selection
            Text(
                text = "Voice",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "53 speakers, 9 languages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            for (group in VOICE_GROUPS) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (voice in group.voices) {
                        FilterChip(
                            selected = state.voiceId == voice.sid,
                            onClick = { viewModel.onVoiceIdChanged(voice.sid) },
                            label = { Text(voice.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

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
