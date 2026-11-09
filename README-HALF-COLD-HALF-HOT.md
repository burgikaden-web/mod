# Half-Cold Half-Hot — Fabric 1.20.1

A fan-made Minecraft Java mod inspired by Shoto Todoroki's ice-and-fire quirk from *My Hero Academia*.

## Requirements
- Minecraft Java Edition 1.20.1
- Fabric Loader 0.16.14 or newer
- Fabric API 0.92.12+1.20.1
- Java 17

No animation library or extra mod dependency is required.

## Abilities and controls
| Ability | Default key |
| --- | --- |
| Ice Wall | R |
| Ice Spikes | G |
| Ice Slide | H |
| Fire Blast | V |
| Jet Burn | B |
| Flashfreeze Heatwave | N |

Change keys in **Options → Controls → Key Binds → Half-Cold Half-Hot Abilities**.

## Ability behavior
- **Ice Wall:** forms a packed-ice barrier in front of the player.
- **Ice Spikes:** raises a line of blue-ice spikes and freezes nearby targets.
- **Ice Slide:** gives the player a forward burst and leaves an ice patch.
- **Fire Blast:** sends a visible flame burst forward and ignites targets in its path.
- **Jet Burn:** releases a close-range flame burst.
- **Flashfreeze Heatwave:** combines freezing, fire, knockback, and mixed ice/flame particles in an area.

Abilities are processed on the server in multiplayer. Install the mod and Fabric API on both server and client for multiplayer play.

## Build
GitHub Actions builds the mod with Java 17 and Gradle 8.7. Download the artifact named **half-cold-half-hot-fabric-1.20.1** from the completed workflow run.

The resulting mod JAR has a versioned, distinct name:
**HalfColdHalfHot-MC1.20.1-Fabric-1.0.0.jar**

## Notes
This first release focuses on functional abilities and vanilla particle/block effects. It does not include a custom player model, bespoke animations, or custom textures yet. The mod does not require Player Animator.
