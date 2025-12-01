# TaleAPI

[![CI](https://github.com/polvallverdu/TaleAPI/actions/workflows/ci.yml/badge.svg)](https://github.com/polvallverdu/TaleAPI/actions/workflows/ci.yml)
[![Release](https://github.com/polvallverdu/TaleAPI/actions/workflows/release.yml/badge.svg)](https://github.com/polvallverdu/TaleAPI/actions/workflows/release.yml)
[![JitPack](https://jitpack.io/v/polvallverdu/TaleAPI.svg)](https://jitpack.io/#polvallverdu/TaleAPI)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java 17+](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

A modern, type-safe library for creating Hytale mods.

## Why?

The best part of Hytale is the modding support it will have. We'll have access day 1 to the source, and we won't need an API on top of it (like Fabric or Spigot), but we also want to start building before the game launches to have minigames ready day 1.

This project provides a solid foundation to build on, allowing us to start implementing Hytale logic as soon as possible, ensuring all abstractions work correctly. It also offers a stable API that adapts to constant breaking changes from the game, so you don't have to worry about each update.

## Features

This API aims to be **clean**, **robust**, **performant**, and **easy to use**.

With those principles in mind, this API features:

- 🎯 **Event system** — Priorities, cancellation, and async support
- 🎮 **Entity abstraction** — Type-safe entity handling
- 🧱 **Block & Item abstractions** — Simplified block and item management
- 🔧 **Code generation** — Automatically generate JSON files from annotations

## Installation

Add JitPack to your repositories:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.polvallverdu:TaleAPI:VERSION'
}
```

## Documentation

- [Events](docs/events.md) — Event system with priorities and cancellation
- [Codegen](docs/codegen.md) — Generate JSON files from annotated classes

## Roadmap

A lot of Hytale modding will be based on JSON files, models, and a node-based editor. Future plans include:

- Model codegen (`@Model`) for Blockbench
- Node editor abstractions
- Inventories & UI systems

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
