# TaleAPI

TaleAPI is a library for creating Hytale mods.

## Why?

I honestly think the best part of hytale is the modding support it will have. We will have access day 1 to the source, and we won't need an API on top of it (like fabric or spigot), but I also want to start building some stuff before the game comes out, to create some minigames day 1.

This project ensures that we have a solid foundation to build on, and we can start implementing hytale logic as soon as possible, so all the abstractions work correctly.

That way, we can also have a stable API that will adapt to constant breaking changes from the game, so we don't have to worry about each update.

## Features

This API aims to be:

- Clean
- Robust
- Performant
- Easy to use

with those principles in mind, this API features:

- Event system
- Entity abstraction
- Block & Item abstractions
- Code generation (JSON files from annotations)

## Documentation

- [Events](docs/events.md) — Event system with priorities and cancellation
- [Codegen](docs/codegen.md) — Generate JSON files from annotated classes

## Roadmap

We know that a lot of hytale modding will be based on json files, models, and a node-based editor. Future plans include:

- Model codegen (`@Model`) for blockbench
- Node editor abstractions
- Inventories
- UI
