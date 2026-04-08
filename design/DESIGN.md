# VoiceBridge — "Aurora" Design System

## The Vibe

VoiceBridge is about **human-AI voice connection**. The design should feel warm,
alive, and intimate — like talking with a friend in a softly-lit room at night.
Not a cold tool. Not a generic chat app. A **voice experience**.

---

## Color Palette

### Brand Colors (replacing current)

| Name | Hex | Current | Purpose |
|------|-----|---------|---------|
| **Deep Navy** | `#0A0E27` | `#121212` | Background — rich, not flat black |
| **Dark Slate** | `#151A3A` | `#1E1E1E` | Surface cards, message area |
| **Warm Indigo** | `#4C3BCF` | `#1A73E8` | Primary — user bubbles, accents |
| **Soft Coral** | `#FF6B6B` | `#CF6679` | Error / End call — warmer red |
| **Mint** | `#00D9A6` | `#00BFA5` | Secondary — call button, success |
| **Muted Silver** | `#8892b0` | — | Secondary text, descriptions |
| **Ghost White** | `#e0e0ff` | — | Primary text on dark |

### Pipeline State Colors (updated)

| State | Hex | Current | Feel |
|-------|-----|---------|------|
| **Listening** | `#00E676` | `#4CAF50` | Brighter emerald with glow |
| **Transcribing** | `#448AFF` | `#2196F3` | Electric blue |
| **Sending** | `#FFD740` | `#FFC107` | Warm amber gold |
| **Speaking** | `#18FFFF` | `#00BCD4` | Vivid cyan pulse |
| **Entertaining** | `#E040FB` | `#9C27B0` | Bright violet |
| **Idle** | `#90A4AE` | `#757575` | Warmer silver (not dead gray) |

---

## Typography

| Style | Font | Size | Weight | Usage |
|-------|------|------|--------|-------|
| App Title | System Sans | 16sp | Medium | Top center, subtle |
| Status Text | System Sans | 18sp | Regular | Below orb |
| Transcript | System Sans | 14sp | Regular | Real-time speech text |
| Message Body | System Sans | 14sp | Regular | Chat bubble text |
| Message Label | System Sans | 11sp | Medium | "Claude" sender label |
| Timer | Monospace | 14sp | Regular | Call duration |
| Card Title | System Sans | 16sp | Medium | History card titles |
| Card Preview | System Sans | 12sp | Regular | History card previews |
| Section Label | System Sans | 12sp | Medium | "Today", "Yesterday" |

---

## Screen 1: Conversation (The Star)

### Layout — IDLE state
```
+----------------------------------+
|          (status bar)            |
|  [clock]              [gear]     |
|                                  |
|         VoiceBridge              |  <- small, centered, 60% opacity
|                                  |
|                                  |
|           .-"""-.                |
|          / . . . \               |  <- Dormant orb: silver, no pulse
|         |  (   )  |              |     3 concentric circles
|          \ .___.  /              |     subtle shadow, no glow
|           '-...-'                |
|                                  |
|    Tap to start talking          |  <- 18sp, muted text
|                                  |
|  +----------------------------+  |
|  | Hey! I'm your voice bridge |  |  <- Welcome card, glass-morphism
|  | to Claude. Let's talk.     |  |     only shown when no messages
|  +----------------------------+  |
|                                  |
|            ( Call )              |  <- Mint circle button, centered
|                                  |     NOT a floating FAB
+----------------------------------+
```

### Layout — LISTENING state
```
+----------------------------------+
|          (status bar)            |
|  [clock]              [gear]     |
|                                  |
|       VoiceBridge · 0:42         |  <- Timer appears, monospace
|                                  |
|        .·*°*·.    .·*°*·.        |
|       / .:::. \  (outer glow)    |  <- ALIVE: 4 rings pulsing
|      |  .:::.  | emerald green   |     outer glow bleeds into bg
|      |  (:::)  | breathing       |     gradient shifts the whole
|       \ .:::.  / animation       |     background slightly green
|        '·.,,.·'                  |
|                                  |
|         Listening...             |  <- Emerald green text
|                                  |
|  "Tell me about the weather..."  |  <- Partial transcript, fading in
|                                  |
|  +----------------------------+  |
|  |          Hi Claude! [user] |  |  <- Indigo bubble, right-aligned
|  |                            |  |
|  | [Claude]                   |  |  <- Slate card, left-aligned
|  | Hey! What's on your mind?  |  |     "Claude" label in mint
|  +----------------------------+  |
|                                  |
|            ( End )               |  <- Coral circle button
|                                  |
+----------------------------------+
```

### Key Design Decisions

1. **No TopAppBar** — Wastes 56dp of vertical space. In a voice app,
   every pixel matters for the orb and messages.

2. **Centered call button** — The FAB (bottom-right) is for secondary
   actions. The call IS the primary action. Center it. Make it big.

3. **Background state tint** — When listening, a very subtle green
   gradient washes over the top of the screen (6% opacity). Makes the
   whole screen feel alive without being distracting.

4. **Call timer** — Users need to know how long they've been talking.
   Small monospace timer next to the app name.

5. **Glass message area** — Messages sit in a translucent card that
   floats over the background. Subtle border, slight blur effect.

---

## Screen 2: History

### Layout
```
+----------------------------------+
|          (status bar)            |
|                                  |
|  < Conversations                 |  <- Back arrow + title
|                                  |
|  +----------------------------+  |
|  | Search conversations...    |  |  <- Search bar, slate bg
|  +----------------------------+  |
|                                  |
|  Today                           |  <- Section header, muted
|                                  |
|  +----------------------------+  |
|  | * Weather chat        2:30 |  |  <- Green dot = recent
|  | "It'll be sunny tomor..."  |  |     Preview of last message
|  | 12 messages                |  |     Message count in indigo
|  +----------------------------+  |
|                                  |
|  +----------------------------+  |
|  | * Cooking ideas       1:15 |  |  <- Purple dot = long session
|  | "Try a Romanian ciorba..." |  |
|  | 8 messages                 |  |
|  +----------------------------+  |
|                                  |
|  Yesterday                       |
|                                  |
|  +----------------------------+  |
|  | Code review help     11:00 |  |
|  | "That function could..."   |  |
|  | 24 messages                |  |
|  +----------------------------+  |
+----------------------------------+
```

### Key Design Decisions

1. **Search** — Not in the current design. Users will accumulate
   conversations. They need to find past ones.

2. **Time grouping** — "Today" / "Yesterday" / "Last week" headers
   give temporal context without cluttering each card.

3. **Conversation preview** — Show the last message snippet. Users
   recognize conversations by content, not timestamps.

4. **Color dot** — Quick visual indicator. Green = recent/active,
   purple = long session, gray = old.

5. **Swipe to delete** — Instead of a delete button on each card.
   Cleaner. Add a confirmation dialog.

---

## Voice Orb — The Soul of the App

### Current (3 circles)
```
  Outer glow    (15% alpha, 130% radius)
  Middle ring   (30% alpha, 110% radius)
  Core circle   (100% alpha, 70% radius)
```

### Proposed (4 layers + effects)
```
  Outer bloom   (6% alpha, 160% radius, blur 40dp)   <- NEW: soft ambient glow
  Outer glow    (12% alpha, 130% radius)
  Middle ring   (25% alpha, 110% radius)
  Core circle   (75% alpha, 70% radius, gradient)     <- CHANGED: gradient fill
```

### Animation Upgrades

| State | Current | Proposed |
|-------|---------|----------|
| Idle | Static gray | Subtle breathing (scale 0.98-1.02, 3000ms) |
| Listening | Pulse 0.85-1.15, 1200ms | Pulse + outer bloom expands, color ripple |
| Transcribing | Pulse 1000ms | Rapid micro-pulse + particles flowing inward |
| Sending | Slow pulse 1800ms | Orbiting ring animation (like a loading spinner) |
| Speaking | Fast pulse 600ms | Scale-sync with audio amplitude + cyan ripples |
| Entertaining | Pulse 1000ms | Color cycling through fun-fact colors |

---

## Component Specs

### Message Bubble (User)
- Background: `#4C3BCF` (Warm Indigo)
- Text: `#ffffff`
- Corner radius: 16dp top, 16dp top, 4dp bottom-end, 16dp bottom-start
- Max width: 280dp
- Padding: 12dp
- Alignment: right

### Message Bubble (Claude)
- Background: `#1a2744` (slightly lighter than surface)
- Border: 1dp `#2a2a5a`
- Label: "Claude" in `#00D9A6` (Mint), 11sp
- Text: `#c8d6e5`
- Corner radius: 16dp top, 16dp top, 16dp bottom-end, 4dp bottom-start
- Max width: 280dp
- Alignment: left

### Call Button (Start)
- Shape: Circle, 80dp diameter
- Background: `#00D9A6` (Mint)
- Icon: Phone, `#0A0E27` (Navy)
- Shadow: 8dp elevation with mint tint
- Position: Bottom center, 32dp from bottom

### Call Button (End)
- Shape: Circle, 80dp diameter
- Background: `#FF6B6B` (Coral)
- Icon: Phone Off, `#ffffff`
- Shadow: 8dp elevation with coral tint

### History Card
- Background: `#151A3A` (Dark Slate)
- Border: 1dp `#2a2a5a`
- Corner radius: 16dp
- Padding: 16dp
- Title: 16sp, `#e0e0ff`
- Preview: 12sp, `#8892b0`, max 1 line, ellipsis
- Time: 10sp monospace, `#5a5a8a`
- Message count: 11sp, `#4C3BCF`
- Gap between cards: 12dp

---

## Animations & Transitions

### Screen transitions
- Conversation -> History: Slide left + fade (300ms)
- History -> Conversation: Slide right + fade (300ms)

### Message animations
- New message: Slide up 20dp + fade in (200ms)
- Auto-scroll: Smooth animated scroll

### State transitions
- Orb color change: 600ms spring animation
- Background tint change: 1000ms ease-in-out
- Status text: Crossfade 300ms

### Micro-interactions
- Call button tap: Scale down 0.9 -> spring back (tactile feel)
- History card tap: Brief highlight ripple
- Swipe to delete: Slide + red background reveal

---

## What This Is NOT

- NOT a chat app — messages are secondary to voice
- NOT a phone app — no dial pad, no contacts
- NOT a dark theme slapped on Material defaults — custom dark palette with warmth
- NOT static — every state should feel different through color, animation, glow
