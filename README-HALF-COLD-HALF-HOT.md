# Half-Cold Half-Hot — Fabric 1.20.1

A fan-made Minecraft mod inspired by Shoto Todoroki's ice-and-fire quirk. This overhaul adds a thermal resource system, three combat stances, cooldowns, mobility, defense, crowd control, and combo finishers.

## Requirements
- Minecraft Java Edition 1.20.1
- Fabric Loader 0.16.14 or newer
- Fabric API 0.92.12+1.20.1
- Java 17
- No Player Animator or other animation dependency

## Core mechanics

### Temperature resources
Heat and cold are separate 0–100 resources. Abilities consume the relevant resource and regenerate over time. Every ability has its own cooldown. Your current stance changes which resource regenerates faster.

### Three stances
- **Ice Focus (C):** Ice abilities are available and cold regenerates faster.
- **Fire Focus (C):** Fire abilities are available and heat regenerates faster.
- **Temperature Balance (C):** Both elements are available. Staying within 15 points of equal heat/cold grants the Balanced Bonus indicator. The two signature combo moves require this stance.

## Controls

| Key | Ability |
|---|---|
| C | Cycle Ice Focus → Fire Focus → Temperature Balance |
| R | Ice Wall — create a defensive packed-ice barrier |
| G | Ice Lance — raise a forward line of spikes, damage and freeze targets |
| H | Ice Slide — burst forward with a frost trail |
| V | Fire Burst — ranged flame cone with knockback |
| B | Jet Burn — launch upward/forward and scorch nearby targets |
| N | Flashfreeze Heatwave — large mixed-element finisher |
| M | Steam Nova — close-range blast that pushes enemies away |
| J | Ice Armor — gain temporary absorption shielding |

All keys can be rebound under **Options → Controls → Key Binds → Half-Cold Half-Hot Abilities**.

## Multiplayer and server behavior
Ability activation and combat effects are handled server-side. Install Fabric API and the mod on both client and server for multiplayer. Thermal resources and cooldowns are currently session-based and reset when a player leaves or the server restarts.

## Build and install
GitHub Actions builds with Java 17 and Gradle 8.7. Download the artifact named **half-cold-half-hot-fabric-1.20.1** from a completed workflow run, then place the JAR in your Minecraft `mods` folder.

Expected JAR:
`HalfColdHalfHot-MC1.20.1-Fabric-1.0.0.jar`

## Current scope
This version uses vanilla blocks and particles. It does not yet include custom player animations, custom textures, a bespoke HUD, or permanent saved progression. Those are separate future features; this release does not require an animation library.
