# BabyzombieAddons

A quality-of-life client mod for Hypixel SkyBlock, built on [Fabric](https://fabricmc.net/).

[English](README.md) · [中文](README_CN.md)

> This project was developed with AI assistance (requirement analysis, implementation, review and iteration).

[GitHub](https://github.com/babyzombie/BabyzombieAddons) · [Gitee](https://gitee.com/Bluesky-kk/BabyzombieAddons)

## Overview

- **Purpose**: a client-side QoL / helper / automation mod for Hypixel SkyBlock, covering Dungeons, Kuudra, Slayers, Mining, Fishing, Garden, parties & chat, events and more.
- **Core features**: HUD displays, waypoint / beacon / light-pillar markers, chat and interaction enhancements, combat / skill / item timers, key-event alerts and safeguards, plus plenty of toggles and searchable config options.
- **Use cases**: daily grinding and runs, Dungeon / Kuudra party play, Slayer boss fights, Crystal Hollows / Glacite mining routes, rare sea creature fishing alerts, and Garden farming helpers (pests, greenhouses, signs and more).

## Quick start

- 🧩 **Install dependencies**: follow the "Installation" section below.
- ⚙️ **Open settings**: run `/bza` in-game to open the settings screen and enable the features you need per gameplay area.
- 🧭 **HUD & display features**: drag and reposition them in the HUD editor, and toggle display, colors, opacity, etc.
- 🔎 **Find options fast**: most features support search tags and category filtering.

## Installation

Requires [Fabric API](https://modrinth.com/mod/fabric-api)

Requires [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)

## Main features

- **General / Misc**: update checker, HUD editor, auto-reconnect & restore after disconnect, window title, custom ringtone / incoming-call alerts, view & interaction QoL, etc.
- **Chat / Party**: chat command responses and automation (`!allinv / !p / !warp / !join / !sc / !play / !pt`), channel detection, interactive popup notifications (party / friend / trade, etc.)
- **Dungeons**: auto-requeue, F4 spectator hiding, Storm thunder muting, daily run counter, chest closing / rare drop sound handling, Wither Cloak HUD & timers, etc.
- **Kuudra**: health & phase info, phase / stun timers, waypoints & beacons, ballista / supply progress HUDs, supply & arrow-poison / pearl refill reminders, shop & screen protection, etc.
- **Slayers**: boss detection with overlays / HUD, low-HP alerts, boss respawn alerts, item / armor / ability timers.
- **Mining**: Crystal Hollows / Glacite routing & teleport helpers, crystal auto-teleport, drill swing suppression, mineshaft waypoints & counters, powder sound & cooldown helpers, etc.
- **Fishing**: rare sea creature scanning & highlighting (light pillar / title, configurable scan radius, exclusion list, title repeat policy), instant-reel prevention, etc.
- **Garden**: pest markers, greenhouse detection & protection, Trevor auto-accept / call, sign auto-rotation, XP orb & sound tweaks, etc.
- **Hunting**: Safari / Torrhus Canyon helpers (e.g. entity highlighting, tracking and info).
- **Events**: Great Spook helper, fruit-digging helper, Raffle task helpers, etc.
- **Minigames**: Ravengard and other minigame helpers.

## Feature details

Below is a quick walkthrough for the core features, following "how to use → use cases → actual effect". No technical details involved.

### 🧰 General / Misc

- ✅ **How to use**: open `/bza` → enable what you need in the "General / Misc" category → for HUD features, adjust position and style in the HUD editor.
- 🧭 **Use cases**: daily wandering, long AFK / dungeon sessions, frequent scene / island switching, when you want fewer pointless clicks and less clutter.
- ⭐ **Key strengths**: turns frequent, repetitive, easy-to-forget details into toggles, letting you focus on the game itself.
- 👀 **Actual effect**: fewer misclicks and less information noise; a cleaner UI with more focused alerts.

### 💬 Chat / Party

- ✅ **How to use**: open `/bza` → enable chat/party toggles → use `!warp/!join/!p/...` directly in party chat (no need to switch to the command box).
- 🧩 **Use cases**: dungeon runs, party coordination, quick invites / warps / info sync.
- ⭐ **Key strengths**: less box-switching and retyping, smoother party commands, and clearer popups so invites / requests are never missed.
- 🎯 **Actual effect**: faster party response, fewer interruptions, smoother overall pace.

### 🏰 Dungeons

- ✅ **How to use**: enable the features in `/bza` before entering a dungeon → follow the alerts / counters in-run → place HUD elements where they are easy to scan.
- 🧭 **Use cases**: daily runs, speedruns, runs that need tight coordination, or players sensitive to sound / visual clutter.
- ⭐ **Key strengths**: presents key moments and easily-missed details more clearly, reducing missed actions and distraction.
- 🔔 **Actual effect**: less repeated communication and correction; steadier party pace and higher efficiency.

### 🔥 Kuudra

- ✅ **How to use**: enable Kuudra features in `/bza` before the run → place phase / stun timers and progress HUDs within easy sight → follow the alerts for supply, ballista and other mechanics.
- 🧭 **Use cases**: multi-phase Kuudra fights where you constantly need progress and resource status at a glance.
- ⭐ **Key strengths**: concentrates the important info so you see it at a glance, reducing missed cues and slow reactions.
- 🧠 **Actual effect**: clearer role coordination, smoother progression, higher tolerance for mistakes.

### ⚔️ Slayers

- ✅ **How to use**: enable boss info / HUD / alerts in the Slayer category of `/bza` → turn on timers for the gear and abilities you actually use → watch the HUD or alerts to time your windows.
- 🧭 **Use cases**: slayer grinding, ability rotations, when you need to time burst / survival windows.
- ⭐ **Key strengths**: turns "by feel" timing into a visual rhythm, reducing misjudgment and wasted windows.
- 💥 **Actual effect**: more stable ability chaining, more controlled damage / survival, easier grinding.

### ⛏️ Mining

- ✅ **How to use**: in the Mining category of `/bza`, enable what matches your area (Crystal Hollows / Glacite, etc.) → turn on the waypoints / counters / HUD you need → follow the markers for teleports and routes.
- 🧭 **Use cases**: powder grinding, route planning, counting stats, reducing wasted swings and misclicks.
- ⭐ **Key strengths**: makes routes and status more visible, cutting time lost to getting lost, backtracking and missing info.
- 📈 **Actual effect**: smoother movement, more consistent efficiency, less fatigue on long mining sessions.

### 🎣 Fishing

- ✅ **How to use**: `/bza` → Fishing → enable "rare sea creature highlight" and set it up:
  - 🔎 **Scan radius**: 1–25, higher means more aggressive alerts;
  - 🚫 **Exclusion list**: check the creatures you want to ignore (requires "exclude highlighted creatures" enabled);
  - 🔁 **Repeat title alerts**: repeat the title on a cooldown when enabled; otherwise each entity alerts only once.
- 🧭 **Use cases**: hotspot fishing, event fishing, when you need to judge "is it worth handling" the moment a rare spawns.
- ⭐ **Key strengths**: pulls rare targets out of the noise, with personal filtering; alerts are controllable — not spammy, not missed.
- ✨ **Actual effect**: spot rares faster, miss fewer windows, and mute targets you don't want.

### 🌿 Garden

- ✅ **How to use**: `/bza` → Garden → enable pest / greenhouse / sign helpers as needed → for display features, position them in the HUD editor.
- 🧭 **Use cases**: pest farming, greenhouse operations, Trevor tasks, daily garden maintenance.
- ⭐ **Key strengths**: reduces fumbling around, redirecting attention from finding targets / buttons back to the flow itself.
- 🌱 **Actual effect**: smoother operations, fewer detours, more continuous task progress.

### 🐾 Hunting

- ✅ **How to use**: `/bza` → Hunting → enable the helpers for your area (Safari, Torrhus Canyon, etc.) → play alongside your usual routes and tasks.
- 🧭 **Use cases**: Safari / Torrhus Canyon and other areas where you need to find targets, watch info and control pace.
- ⭐ **Key strengths**: better target visibility and information density, lowering the cost of "searching" and "waiting".
- 🎯 **Actual effect**: fewer missed targets, fewer wasted detours, smoother overall experience.

### 🎉 Events & 🎮 Minigames

- ✅ **How to use**: enable the relevant category in `/bza` before the event / minigame starts → follow the alerts and helper info in-game.
- 🧭 **Use cases**: limited-time events, or events / minigames with complex flows that you want to pick up quickly.
- ⭐ **Key strengths**: lower learning cost and fewer flow mistakes, letting you focus on rewards and pace.
- 🥇 **Actual effect**: get going faster, fewer flow stalls, more stable event runs.

## Commands

`/bza` opens the settings screen. More commands: `/bza play`, `/bza sc`, `/bza la`, `/bza yaw`, `/bza l`, `/bza wp`.

## Authors

baby__zombie · TryToFeel

## License

LGPL 3.0
