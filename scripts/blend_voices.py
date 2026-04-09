#!/usr/bin/env python3
"""
VoiceBridge Voice Blender
=========================
Creates custom blended voices by mixing Kokoro speaker embeddings
and appends them to voices.bin.

Usage:
    python3 scripts/blend_voices.py

Edit the RECIPES list below to define your custom voice blends.
Each recipe becomes a new speaker ID appended after the existing 53.

After running, the updated voices.bin is written in-place and the
SettingsBottomSheet voice list needs updating with the new entries.

Voice Name Reference (kokoro-multi-lang-v1_0, sid 0-52):
  American Female:   0-af_alloy  1-af_aoede  2-af_bella  3-af_heart
                     4-af_jessica  5-af_kore  6-af_nicole  7-af_nova
                     8-af_river  9-af_sarah  10-af_sky
  American Male:     11-am_adam  12-am_echo  13-am_eric  14-am_fenrir
                     15-am_liam  16-am_michael  17-am_onyx  18-am_puck
                     19-am_santa
  British Female:    20-bf_alice  21-bf_emma  22-bf_isabella  23-bf_lily
  British Male:      24-bm_daniel  25-bm_fable  26-bm_george  27-bm_lewis
  Spanish:           28-ef_dora  29-em_alex  30-em_santa
  French:            31-ff_siwis
  Hindi:             32-hf_alpha  33-hf_beta  34-hm_omega  35-hm_psi
  Italian:           36-if_sara  37-im_nicola
  Japanese:          38-jf_alpha  39-jf_gongitsune  40-jf_nezumi
                     41-jf_tebukuro  42-jm_kumo
  Portuguese:        43-pf_dora  44-pm_alex  45-pm_santa
  Chinese:           46-zf_xiaobei  47-zf_xiaoni  48-zf_xiaoxiao
                     49-zf_xiaoyi  50-zm_yunjian  51-zm_yunxi
                     52-zm_yunxia  53-zm_yunyang
"""

import struct
import os
import shutil
from dataclasses import dataclass

# ============================================================
# CONFIGURATION
# ============================================================

VOICES_BIN = os.path.join(
    os.path.dirname(__file__), "..",
    "app", "src", "main", "assets",
    "kokoro-multi-lang-v1_0", "voices.bin",
)

NUM_SPEAKERS = 53  # Original speaker count (v1_0 has indices 0-52... wait, let me check)
# Actually the Chinese section ends at index 52 (zm_yunyang), but that's 53 entries (0-52)
# However voices.bin has 53 speakers worth of data. Let me recount:
# 11 + 9 + 4 + 4 + 3 + 1 + 4 + 2 + 5 + 3 + 8 = 54? No...
# af: 11 (0-10), am: 9 (11-19), bf: 4 (20-23), bm: 4 (24-27)
# ef: 1 + em: 2 = 3 (28-30), ff: 1 (31), hf: 2 + hm: 2 = 4 (32-35)
# if: 1 + im: 1 = 2 (36-37), jf: 4 + jm: 1 = 5 (38-42)
# pf: 1 + pm: 2 = 3 (43-45), zf: 4 + zm: 4 = 8 (46-53)
# Total: 11+9+4+4+3+1+4+2+5+3+8 = 54? Let me just count: 0 through 53 = 54 speakers
# But the file has 6919680 floats. 6919680 / (510*256) = 53.0 exactly.
# So there are 53 speakers (indices 0-52). The Chinese section is 46-52 = 7 entries, not 8.

DIM0 = 510   # Temporal dimension
DIM1 = 256   # Embedding dimension
FLOATS_PER_SPEAKER = DIM0 * DIM1  # 130560

# Voice name -> sid mapping
VOICE_MAP = {
    "af_alloy": 0, "af_aoede": 1, "af_bella": 2, "af_heart": 3,
    "af_jessica": 4, "af_kore": 5, "af_nicole": 6, "af_nova": 7,
    "af_river": 8, "af_sarah": 9, "af_sky": 10,
    "am_adam": 11, "am_echo": 12, "am_eric": 13, "am_fenrir": 14,
    "am_liam": 15, "am_michael": 16, "am_onyx": 17, "am_puck": 18,
    "am_santa": 19,
    "bf_alice": 20, "bf_emma": 21, "bf_isabella": 22, "bf_lily": 23,
    "bm_daniel": 24, "bm_fable": 25, "bm_george": 26, "bm_lewis": 27,
    "ef_dora": 28, "em_alex": 29, "em_santa": 30,
    "ff_siwis": 31,
    "hf_alpha": 32, "hf_beta": 33, "hm_omega": 34, "hm_psi": 35,
    "if_sara": 36, "im_nicola": 37,
    "jf_alpha": 38, "jf_gongitsune": 39, "jf_nezumi": 40,
    "jf_tebukuro": 41, "jm_kumo": 42,
    "pf_dora": 43, "pm_alex": 44, "pm_santa": 45,
    "zf_xiaobei": 46, "zf_xiaoni": 47, "zf_xiaoxiao": 48,
    "zf_xiaoyi": 49, "zm_yunjian": 50, "zm_yunxi": 51, "zm_yunyang": 52,
    # Total: 53 speakers (indices 0-52)
}


@dataclass
class Recipe:
    """A custom voice blend recipe."""
    name: str           # Human-readable name (for the settings UI)
    blend: dict         # voice_name -> weight (auto-normalized)


# ============================================================
# DEFINE YOUR CUSTOM VOICE RECIPES HERE
# ============================================================
# Each recipe blends multiple voices by weight.
# Weights are auto-normalized (they don't need to sum to 1).
#
# Example: 50% Nicole + 30% Adam + 20% Isabella
#   Recipe("Warm Mix", {"af_nicole": 0.5, "am_adam": 0.3, "bf_isabella": 0.2})

RECIPES = [
    Recipe(
        name="Warm Narrator",
        blend={
            "af_sarah": 0.5,
            "af_nicole": 0.3,
            "bf_emma": 0.2,
        },
    ),
    Recipe(
        name="Deep Storyteller",
        blend={
            "am_adam": 0.4,
            "bm_george": 0.4,
            "am_michael": 0.2,
        },
    ),
    Recipe(
        name="Bright Assistant",
        blend={
            "af_nova": 0.4,
            "af_heart": 0.3,
            "af_sky": 0.3,
        },
    ),
    Recipe(
        name="British Charm",
        blend={
            "bf_alice": 0.4,
            "bf_lily": 0.3,
            "bm_fable": 0.3,
        },
    ),
]


# ============================================================
# BLENDING ENGINE
# ============================================================

def load_voices_bin(path: str) -> list[list[float]]:
    """Load all speaker embeddings from voices.bin."""
    with open(path, "rb") as f:
        data = f.read()

    total_floats = len(data) // 4
    num_speakers = total_floats // FLOATS_PER_SPEAKER
    print(f"Loaded {path}")
    print(f"  File size: {len(data):,} bytes")
    print(f"  Speakers: {num_speakers}")
    print(f"  Dims: {DIM0} x {DIM1} = {FLOATS_PER_SPEAKER} floats per speaker")

    all_floats = struct.unpack(f"{total_floats}f", data)
    speakers = []
    for i in range(num_speakers):
        start = i * FLOATS_PER_SPEAKER
        speakers.append(list(all_floats[start:start + FLOATS_PER_SPEAKER]))

    return speakers


def blend_speakers(
    speakers: list[list[float]],
    recipe: dict[str, float],
) -> list[float]:
    """Blend multiple speaker embeddings by weighted average."""
    # Resolve names to indices
    resolved = {}
    for name, weight in recipe.items():
        if name not in VOICE_MAP:
            raise ValueError(f"Unknown voice: '{name}'. Available: {sorted(VOICE_MAP.keys())}")
        resolved[VOICE_MAP[name]] = weight

    # Normalize weights
    total_weight = sum(resolved.values())
    if total_weight <= 0:
        raise ValueError("Weights must sum to > 0")

    normalized = {sid: w / total_weight for sid, w in resolved.items()}

    # Weighted average of embeddings
    blended = [0.0] * FLOATS_PER_SPEAKER
    for sid, weight in normalized.items():
        emb = speakers[sid]
        for j in range(FLOATS_PER_SPEAKER):
            blended[j] += emb[j] * weight

    return blended


def save_voices_bin(path: str, speakers: list[list[float]]):
    """Write all speaker embeddings back to voices.bin."""
    total_floats = len(speakers) * FLOATS_PER_SPEAKER
    flat = []
    for s in speakers:
        flat.extend(s)

    data = struct.pack(f"{total_floats}f", *flat)
    with open(path, "wb") as f:
        f.write(data)

    print(f"\nSaved {path}")
    print(f"  File size: {len(data):,} bytes")
    print(f"  Speakers: {len(speakers)}")


def main():
    print("=" * 60)
    print("VoiceBridge Voice Blender")
    print("=" * 60)
    print()

    if not os.path.exists(VOICES_BIN):
        print(f"ERROR: voices.bin not found at:\n  {VOICES_BIN}")
        print("Make sure you're running from the project root.")
        return

    # Backup
    backup = VOICES_BIN + ".backup"
    if not os.path.exists(backup):
        shutil.copy2(VOICES_BIN, backup)
        print(f"Backup saved: {backup}")
    else:
        print(f"Backup already exists: {backup}")

    # Load
    speakers = load_voices_bin(VOICES_BIN)
    original_count = len(speakers)

    if not RECIPES:
        print("\nNo recipes defined. Edit RECIPES in this script.")
        return

    # Blend each recipe
    print(f"\nBlending {len(RECIPES)} custom voice(s)...")
    for i, recipe in enumerate(RECIPES):
        new_sid = original_count + i
        print(f"\n  sid {new_sid}: {recipe.name}")

        # Show blend formula
        total_w = sum(recipe.blend.values())
        parts = []
        for name, weight in recipe.blend.items():
            parts.append(f"{weight/total_w:.2f} * {name}")
        print(f"    Formula: {' + '.join(parts)}")

        blended = blend_speakers(speakers, recipe.blend)
        speakers.append(blended)

    # Save
    save_voices_bin(VOICES_BIN, speakers)

    # Print summary for updating the Android code
    print("\n" + "=" * 60)
    print("UPDATE YOUR ANDROID CODE")
    print("=" * 60)
    print(f"\nTotal speakers: {len(speakers)} (was {original_count})")
    print("\nAdd these to SettingsBottomSheet.kt VOICE_GROUPS:")
    print()
    print('    VoiceGroup("Custom Blends", listOf(')
    for i, recipe in enumerate(RECIPES):
        sid = original_count + i
        comma = "," if i < len(RECIPES) - 1 else ""
        print(f'        Voice({sid}, "{recipe.name}"){comma}')
    print("    )),")
    print()
    print("Done! Rebuild the app to use the new voices.")


if __name__ == "__main__":
    main()
