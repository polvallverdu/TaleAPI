# TaleAPI Code Generation

TaleAPI includes an annotation processor that generates JSON definition files for blocks and items at compile time.

## Overview

Instead of manually writing JSON files, you can annotate your Java classes and let the processor generate them automatically. This provides:

- **Compile-time validation** — Errors appear during compilation, not at runtime
- **Type safety** — Your IDE can autocomplete and validate annotations
- **Single source of truth** — Define everything in Java, no JSON files to maintain

## Getting Started

Simply add TaleAPI as a dependency. The annotation processor is automatically registered and will run during compilation.

```groovy
dependencies {
    implementation("dev.polv.taleapi:taleapi:1.0-SNAPSHOT")
}
```

## Blocks

Use the `@Block` annotation to define a block type:

```java
import dev.polv.taleapi.codegen.annotation.Block;
import dev.polv.taleapi.block.TaleBlock;

@Block(id = "mymod:magic_stone")
public class MagicStone implements TaleBlock {

    @Override
    public String getId() {
        return "mymod:magic_stone";
    }
}
```

This generates `blocks/magic_stone.json`:

```json
{
  "id": "mymod:magic_stone"
}
```

## Items

Use the `@Item` annotation to define an item type:

```java
import dev.polv.taleapi.codegen.annotation.Item;
import dev.polv.taleapi.item.TaleItem;

@Item(id = "mymod:ruby_sword")
public class RubySword implements TaleItem {

    @Override
    public String getId() {
        return "mymod:ruby_sword";
    }
}
```

This generates `items/ruby_sword.json`:

```json
{
  "id": "mymod:ruby_sword"
}
```

## ID Format

All IDs must follow the `namespace:name` format:

| ID                  | Valid | Reason              |
| ------------------- | ----- | ------------------- |
| `mymod:ruby`        | ✅    | Correct format      |
| `mymod:magic_stone` | ✅    | Underscores allowed |
| `ruby`              | ❌    | Missing namespace   |
| `:ruby`             | ❌    | Empty namespace     |
| `mymod:`            | ❌    | Empty name          |

Invalid IDs will cause a **compile-time error** with a helpful message.

## Generated File Location

JSON files are generated to the class output directory:

```
build/classes/java/main/
├── blocks/
│   └── magic_stone.json
├── items/
│   └── ruby_sword.json
└── dev/
    └── mymod/
        └── ...
```

The processor extracts the filename from the ID (the part after the colon).

## Best Practices

### 1. Keep IDs Consistent

Match your annotation ID with the `getId()` return value:

```java
@Block(id = "mymod:obsidian")
public class Obsidian implements TaleBlock {

    private static final String ID = "mymod:obsidian";

    @Override
    public String getId() {
        return ID;
    }
}
```

### 2. Use Constants for Namespaces

For larger mods, define your namespace as a constant:

```java
public final class MyMod {
    public static final String NAMESPACE = "mymod";

    public static String id(String name) {
        return NAMESPACE + ":" + name;
    }
}
```

### 3. Organize by Type

Keep blocks and items in separate packages:

```
src/main/java/com/example/mymod/
├── block/
│   ├── MagicStone.java
│   └── GlowingOre.java
├── item/
│   ├── RubySword.java
│   └── MagicWand.java
└── MyMod.java
```

## Troubleshooting

### JSON files not generated

Make sure:

1. The annotation processor is on the classpath
2. You're not using `-proc:none` compiler flag
3. The ID format is valid (`namespace:name`)

### Compilation errors about missing annotations

Ensure TaleAPI is added as an `implementation` (not `compileOnly`) dependency.

## Future Extensions

The codegen system is designed to be extensible. Future versions may include:

- Additional block/item properties (hardness, stack size, etc.)
- Model generation (`@Model`)
- Recipe generation (`@Recipe`)
- Loot table generation (`@LootTable`)

The annotation API will remain stable — only the generated JSON format will be updated when Hytale's actual format is known.
