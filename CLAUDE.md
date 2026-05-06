# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

- **Build**: `./gradlew build`
- **Run client**: `./gradlew runClient`
- **Run server**: `./gradlew runServer`
- **Run data generation**: `./gradlew runData`
- **Game tests**: `./gradlew runGameTestServer`
- **IntelliJ**: `./gradlew idea`

All Gradle tasks use the NeoForged moddev plugin. Java 21 required.

## Architecture

NeoForge 1.21.1 mod that caches EMI's stack list to disk, skipping the ~52s reload on subsequent launches.

### Mod Entry Points

- `EmiAccelerator.java` — Main mod class. Initializes config, registers `/emiacc` command on the client event bus.
- `EmiAcceleratorClient.java` — `@EventBusSubscriber` on the GAME bus. On first world join, triggers an EMI reload if no cache exists, or skips if cache is present.

### Key Packages

- **`config/`** — `ModConfig.java`: Simple `.properties`-file config (cache enabled/disabled, max file size, auto-clear on mod change, diagnostics toggle). Lives in `config/emi-accelerator/`.
- **`util/`**
  - `EmiStackCache.java` — Serializes `EmiStackList.stacks` to JSON (items with NBT, fluids, or bare items). Verifies cache integrity via version stamp + SHA-256 mod-list hash. Loads synchronously on hit, saves async on cache miss.
  - `ReloadTimer.java` — Diagnostics: records per-step timing of EMI reload, persists last 20 records to `config/emi-accelerator/reload-timings.json`.
- **`mixin/`**
  - `EmiStackListMixin.java` — Injects at `EmiStackList.reload()` HEAD (try cache load, cancel if hit) and RETURN (save async).
  - `EmiReloadManagerMixin.java` — Injects at both `EmiReloadManager.step()` overloads to feed `ReloadTimer`.
  - `EmiReloadWorkerMixin.java` — Main cache skip logic: at `ReloadWorker.run()` HEAD, tries cache load and if hit, uses reflection to reset `EmiReloadManager` internal state (status, thread, restart fields), calls `EmiScreenManager.forceRecalculate()` and search update, then cancels the original run. On RETURN (cache miss), records timing.
- **`command/`** — `EmiAcceleratorCommand.java`: `/emiacc status` (cache size, hit status, load time), `/emiacc clear` (delete cache), `/emiacc reload` (trigger `EmiReloadManager.reload()`).

### Dependency: EMI

EMI is a `compileOnly`/`runtimeOnly` dependency from `maven.terraformersmc.com`. Mixins target EMI internals:
- `dev.emi.emi.registry.EmiStackList` (reload method)
- `dev.emi.emi.runtime.EmiReloadManager` (step, status/thread/restart fields)
- `dev.emi.emi.runtime.EmiReloadManager$ReloadWorker` (run method)
- `dev.emi.emi.screen.EmiScreenManager` (forceRecalculate, search)

### Data Flow

1. Player joins world → `EmiAcceleratorClient` fires
2. If cache file exists → skip EMI reload entirely
3. If no cache → trigger `EmiReloadManager.reload()`
4. `EmiStackListMixin` tries cache at `reload()` HEAD: if valid, cancel reload and populate `EmiStackList.stacks` from JSON
5. `EmiReloadWorkerMixin` tries cache at `run()` HEAD: if valid, use reflection to finalize reload state, cancel worker
6. On cache miss after reload completes, `EmiStackListMixin` RETURN saves async
7. Cache invalidation: mod list SHA-256 hash mismatch or version bump → auto-delete cache
