# One For All — Fabric 1.20.1

A server-authoritative Fabric mod that adds a progression-based One For All quirk to Minecraft Java Edition 1.20.1.

## Features

- **Vestige progression:** earn vestiges by defeating hostile mobs and high-health boss entities. Progress is saved per player in the server world's persistent state.
- **Scaling power:** One For All percentage increases with vestiges and scales attack damage, movement speed, and Jump Boost.
- **Blackwhip:** unlock at 10 vestiges. Grapple toward the block you are looking at, and briefly slow/pull nearby hostile mobs for crowd control.
- **Danger Sense:** unlock at 20 vestiges. Toggle client-side glowing outlines on nearby hostile mobs.
- **Detroit Smash:** unlock at 5 vestiges. Spend energy to damage and knock back nearby hostile mobs with a ring-shaped particle shockwave. The effect does not destroy blocks.
- **Energy:** abilities consume energy; energy regenerates automatically.
- **HUD and keybinds:** shows percentage, vestiges, energy, and activation state. Keybinds can be changed in Minecraft's Controls menu.
- **Client/server split:** progression, damage, cooldown costs, and ability effects are handled by the server; the client handles input, HUD, and Danger Sense outlines.

## Default controls

| Action | Default key | Unlock |
| --- | --- | ---: |
| Toggle One For All | `O` | Always |
| Blackwhip | `R` | 10 vestiges |
| Toggle Danger Sense | `G` | 20 vestiges |
| Detroit Smash | `V` | 5 vestiges |

Change these under **Options → Controls → Key Binds → One For All**.

## Progression and balance

| Defeated target | Vestiges |
| --- | ---: |
| Hostile mob | +1 |
| High-health target (80+ max health) | +3 |
| Boss-scale target (200+ max health) | +6 |

Vestiges are capped at 50. The displayed percentage is `min(100, 5 + vestiges × 2)`. Energy is capped at 100 and regenerates by 1 every 10 player ticks. Blackwhip costs 20 energy; Detroit Smash costs 40 energy.

The ability thresholds, progression rewards, and energy costs are constants in `OFAPlayerProgress` and `OFAAbilities`, so they can be adjusted in source.

## Build the mod on Windows (no Gradle setup required)

You do **not** need to install Gradle yourself. Double-click `BUILD-MOD.bat` in the extracted project folder. The script automatically downloads a compatible Gradle version and, if needed, a portable Java 17 JDK into the project's `.tools` folder, then builds the mod.

The first run needs an internet connection and may download Minecraft/Fabric development libraries. When it finishes, install `build\libs\one-for-all-1.0.0.jar` in your Minecraft 1.20.1 Fabric instance's `mods` folder.

Do not put the `-sources.jar` file in your mods folder. The automated build script is for Windows. On other operating systems, the project still requires Java 17+ and Gradle 8.6.

### Install

1. Install Fabric Loader for Minecraft 1.20.1.
2. Put the built `one-for-all-1.0.0.jar` in the Minecraft instance's `mods` folder.
3. Install the matching Fabric API build in the same `mods` folder.
4. For multiplayer, install the mod and Fabric API on both the dedicated server and each connecting client.
5. Launch the Fabric 1.20.1 profile.

## Compatibility notes

The mod uses vanilla player attributes, status effects, Fabric events, and namespaced networking. It does not replace player models, overwrite vanilla entity classes, or add mandatory dependencies on other superhero mods. That design minimizes conflicts, but it is **not a guarantee of compatibility** with every superhero/combat mod; attribute modifiers, keybind defaults, or custom entity types can still overlap. Test modpacks in a separate world before using them in an established save.

## GitHub setup

The project is ready to place in a Git repository. Create an empty repository, extract this folder into it, then commit the complete project. The connected GitHub account had no repositories available when this project was generated, so no remote repository or commit was created from this session.

## License and fan content

Source code is MIT-licensed. One For All, Blackwhip, Danger Sense, and Detroit Smash are fan references to *My Hero Academia* and are not affiliated with or endorsed by the rights holders. Add your own original or properly licensed textures, sounds, and branding if you expand the mod.
