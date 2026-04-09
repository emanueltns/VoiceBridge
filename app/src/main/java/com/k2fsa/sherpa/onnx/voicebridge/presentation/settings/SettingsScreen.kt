package com.k2fsa.sherpa.onnx.voicebridge.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallGreen
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextPrimary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextTertiary

private val TerminalBg = Color(0xFF0D1117)
private val TerminalHeaderBg = Color(0xFF161B22)
private val DotRed = Color(0xFFFF5F56)
private val DotYellow = Color(0xFFFFBD2E)
private val DotGreen = Color(0xFF27C93F)
private val TerminalDim = Color(0xFF555577)
private val TerminalInputBg = Color(0xFF0D1117)

// Voice data
private data class Voice(val sid: Int, val name: String, val gender: String)
private data class Language(val code: String, val label: String, val flag: String, val voices: List<Voice>)

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
        Voice(20, "Alice", "F"), Voice(21, "Emma", "F"), Voice(22, "Isabella", "F"), Voice(23, "Lily", "F"),
        Voice(24, "Daniel", "M"), Voice(25, "Fable", "M"), Voice(26, "George", "M"), Voice(27, "Lewis", "M"),
    )),
    Language("es", "Spanish", "\uD83C\uDDEA\uD83C\uDDF8", listOf(Voice(28, "Dora", "F"), Voice(29, "Alex", "M"), Voice(30, "Santa", "M"))),
    Language("fr", "French", "\uD83C\uDDEB\uD83C\uDDF7", listOf(Voice(31, "Siwis", "F"))),
    Language("hi", "Hindi", "\uD83C\uDDEE\uD83C\uDDF3", listOf(Voice(32, "Alpha", "F"), Voice(33, "Beta", "F"), Voice(34, "Omega", "M"), Voice(35, "Psi", "M"))),
    Language("it", "Italian", "\uD83C\uDDEE\uD83C\uDDF9", listOf(Voice(36, "Sara", "F"), Voice(37, "Nicola", "M"))),
    Language("ja", "Japanese", "\uD83C\uDDEF\uD83C\uDDF5", listOf(Voice(38, "Alpha", "F"), Voice(39, "Gongitsune", "F"), Voice(40, "Nezumi", "F"), Voice(41, "Tebukuro", "F"), Voice(42, "Kumo", "M"))),
    Language("pt", "Portuguese", "\uD83C\uDDE7\uD83C\uDDF7", listOf(Voice(43, "Dora", "F"), Voice(44, "Alex", "M"), Voice(45, "Santa", "M"))),
    Language("zh", "Chinese", "\uD83C\uDDE8\uD83C\uDDF3", listOf(Voice(46, "Xiaobei", "F"), Voice(47, "Xiaoni", "F"), Voice(48, "Xiaoxiao", "F"), Voice(49, "Xiaoyi", "F"), Voice(50, "Yunjian", "M"), Voice(51, "Yunxi", "M"), Voice(52, "Yunyang", "M"))),
)

private fun findLanguageForSid(sid: Int): Language =
    LANGUAGES.firstOrNull { lang -> lang.voices.any { it.sid == sid } } ?: LANGUAGES.first()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedLanguage by remember(state.voiceId) { mutableStateOf(findLanguageForSid(state.voiceId)) }
    var languageDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Terminal header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalHeaderBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(DotRed))
                Box(Modifier.size(8.dp).clip(CircleShape).background(DotYellow))
                Box(Modifier.size(8.dp).clip(CircleShape).background(DotGreen))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("settings", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalDim)
            Spacer(modifier = Modifier.weight(1f))
            // Save
            IconButton(onClick = { viewModel.save(); onNavigateBack() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = CallGreen, modifier = Modifier.size(20.dp))
            }
        }

        // Settings body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // User
            SectionHeader("# User")
            Spacer(modifier = Modifier.height(8.dp))
            TerminalTextField(
                value = state.userName,
                onValueChange = viewModel::onUserNameChanged,
                label = "name",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // VPS
            SectionHeader("# VPS Connection")
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TerminalTextField(
                    value = state.host,
                    onValueChange = viewModel::onHostChanged,
                    label = "host",
                    modifier = Modifier.weight(3f),
                    isPassword = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TerminalTextField(
                    value = state.port,
                    onValueChange = viewModel::onPortChanged,
                    label = "port",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                    isPassword = true,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ASR
            SectionHeader("# Speech Recognition")
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.useAndroidAsr) "engine=android" else "engine=nemotron",
                        fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TextPrimary,
                    )
                    Text(
                        text = if (state.useAndroidAsr) "# Best accuracy, great with accents" else "# Private, offline, cross-platform",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalDim,
                    )
                }
                Switch(checked = state.useAndroidAsr, onCheckedChange = { viewModel.onAsrEngineChanged(it) })
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fun facts
            SectionHeader("# While Waiting")
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.funFactsEnabled) "fun_facts=true" else "fun_facts=false",
                        fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TextPrimary,
                    )
                    Text("# Speak fun facts if response > 3s", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalDim)
                }
                Switch(checked = state.funFactsEnabled, onCheckedChange = { viewModel.onFunFactsChanged(it) })
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Overlay
            SectionHeader("# Display")
            Spacer(modifier = Modifier.height(8.dp))
            val context = androidx.compose.ui.platform.LocalContext.current
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            var overlayEnabled by remember { mutableStateOf(android.provider.Settings.canDrawOverlays(context)) }
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        overlayEnabled = android.provider.Settings.canDrawOverlays(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (overlayEnabled) "overlay=true" else "overlay=false",
                        fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TextPrimary,
                    )
                    Text("# Show orb over other apps", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalDim)
                }
                Switch(
                    checked = overlayEnabled,
                    onCheckedChange = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}"),
                        )
                        context.startActivity(intent)
                    },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Voice
            SectionHeader("# Voice")
            Spacer(modifier = Modifier.height(8.dp))

            Text("language=", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TerminalDim)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { languageDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text("${selectedLanguage.flag} ${selectedLanguage.label}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = languageDropdownExpanded, onDismissRequest = { languageDropdownExpanded = false }) {
                for (lang in LANGUAGES) {
                    DropdownMenuItem(
                        text = { Text("${lang.flag} ${lang.label} (${lang.voices.size})", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            selectedLanguage = lang; languageDropdownExpanded = false
                            if (lang.voices.none { it.sid == state.voiceId }) viewModel.onVoiceIdChanged(lang.voices.first().sid)
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val femaleVoices = selectedLanguage.voices.filter { it.gender == "F" }
            val maleVoices = selectedLanguage.voices.filter { it.gender == "M" }

            if (femaleVoices.isNotEmpty()) {
                Text("# Female", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalDim)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (v in femaleVoices) {
                        FilterChip(
                            selected = state.voiceId == v.sid, onClick = { viewModel.onVoiceIdChanged(v.sid) },
                            label = { Text(v.name, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CallGreen.copy(alpha = 0.2f)),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (maleVoices.isNotEmpty()) {
                Text("# Male", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalDim)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (v in maleVoices) {
                        FilterChip(
                            selected = state.voiceId == v.sid, onClick = { viewModel.onVoiceIdChanged(v.sid) },
                            label = { Text(v.name, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CallGreen.copy(alpha = 0.2f)),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = CallGreen)
}

@Composable
private fun TerminalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(text = "$label=", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalDim)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TerminalInputBg, unfocusedContainerColor = TerminalInputBg,
                focusedBorderColor = CallGreen.copy(alpha = 0.3f), unfocusedBorderColor = TerminalDim.copy(alpha = 0.3f),
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = CallGreen,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
        )
    }
}
