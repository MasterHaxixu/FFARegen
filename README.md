# FFARegen

FFARegen is a Spigot plugin that regenerates FFA arena worlds by cloning a configured *model* world into a *reset* world using **Multiverse-Core**.

It can regenerate arenas:
- Automatically on a timer (per arena)
- Manually via a command

## Requirements

- Spigot/Paper (built against `spigot-api` 1.19.2)
- Multiverse-Core (declared as a dependency)

## Usage

- Command: `/regenffa <arena|all>`
- Permission: `ffaregen.regen`

## Configuration

Configured in `config.yml`:

- `duration`: default regen interval in minutes
- `arenas.<id>`: arena entries with:
  - `enabled`: enable/disable automatic regen
  - `duration`: override interval for this arena
  - `modelworld`: source world to clone from
  - `resetworld`: world name to delete + recreate from `modelworld` (destructive)
  - `world-between-regen`: temporary world players are teleported to during regen
  - `x`, `y`, `z`: fallback teleport location in the temp world

## Build

- `mvn package`

The built plugin JAR will be under `target/`.
