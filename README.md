# Mob Lasso (Polymer)

A server-side Fabric mod that lets you capture and transport mobs using a lasso — powered by the [Polymer](https://github.com/Patbox/polymer) framework.

**No client mod required!** Vanilla players see the lasso as a custom-textured item.

## Features

- **Universal mob lasso** — works on any mob type (animals, villagers, hostile mobs, etc.)
- **100% server-side** — powered by Polymer, vanilla clients work without any mods
- **Configurable holding timer** — mobs auto-release after a configurable time
- **Hostile mob support** — capture weakened hostile mobs (configurable health threshold)
- **Hostile damage** — carrying hostile mobs damages the player periodically
- **Tamed mob protection** — only the owner can capture their tamed pets
- **Boss protection** — boss mobs cannot be captured by default
- **YACL config** — in-game configuration screen via [YetAnotherConfigLib](https://github.com/isXander/YetAnotherConfigLib)

## How It Works

1. **Capture**: Right-click a mob while holding the Mob Lasso
2. **Transport**: Carry the mob in your inventory (timer counts down)
3. **Release**: Right-click a block to release the mob at that location

The lasso shows a durability bar indicating remaining holding time.

## Configuration

Edit `config/mob-lasso-polymer.json` or use the YACL config screen:

| Option | Default | Description |
|--------|---------|-------------|
| `holdingTime` | 120 | Seconds the lasso can hold a mob (-1 = infinite) |
| `allowBosses` | false | Allow capturing boss mobs |
| `allowTamedMobs` | true | Allow capturing tamed mobs (owner only) |
| `allowHostileMobs` | true | Allow capturing hostile mobs |
| `hostileMobMaxHealth` | 0.5 | Max health % for hostile capture |
| `hostileDamageRate` | 5 | Seconds between damage ticks (-1 = disabled) |

## Requirements

- Minecraft 26.2+
- Fabric Loader 0.16+
- [Polymer Core](https://modrinth.com/mod/polymer) 0.17+
- [Polymer Resource Pack](https://modrinth.com/mod/polymer) 0.17+
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [YetAnotherConfigLib](https://modrinth.com/mod/yacl) (optional, for config screen)

## Building

```bash
./gradlew build
```

The mod JAR will be in `build/libs/`.

## Texture

The included texture (`mob_lasso.png`) is a placeholder using the vanilla lead texture. Replace it with your own 16x16 PNG at:
```
src/main/resources/assets/moblassopolymer/textures/item/mob_lasso.png
```
