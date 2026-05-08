# Modelle

**Modelle** is a Minecraft Forge mod that lets you display custom 3D models (OBJ format) directly in your world.

---

## Features

- Load `.obj` + `texture.png` models into the game world
- Server-side model storage and synchronization
- Per-model settings: rotation (Yaw/Pitch/Roll), scale, position offset
- Owner protection with optional edit/copy permissions
- Automatic model caching and chunk-based network transfer

---

## Installation

1. Install **Minecraft Forge 1.20.1** (build 47.1.0 or newer).
2. Download the latest `modelle-X.X.X.jar`.
3. Place the `.jar` into your `.minecraft/mods` folder.
4. Launch the game.

---

## How to Use

### 1. Prepare Your Model

Inside your `.minecraft/config/modelle/models/` folder, create a new folder for your model, e.g. `mymodel/`.

Place two files inside:
- `model.obj` — your 3D model
- `texture.png` — the texture (PNG format)

Example structure:
```
.minecraft/
└── config/
    └── modelle/
        └── models/
            └── mymodel/
                ├── model.obj
                └── texture.png
```

### 2. Place the Block

Craft or give yourself the **Model Block** (ID: `modelle:model_block`).
Place it in the world.

### 3. Configure the Model

Right-click the block to open the GUI.

- **Path field**: type the folder name (e.g. `mymodel`) and press **Save**.
  The mod will convert your OBJ into an optimized `.mbm` format and generate a unique hash.
- **Hash field**: if you already know the model hash (64-character SHA-256), paste it directly and save.

Use the +/- buttons to adjust:
- **X / Y / Z** — position offset
- **Yaw / Pitch / Roll** — rotation
- **Scale** — model size

### 4. Permissions (Owner Only)

If you are the block owner, you can toggle:
- **Allow Copy** — lets other players copy the model hash
- **Allow Edit** — lets other players change transforms
- **Always Render** — disables frustum culling (useful for very large models)

### 5. Copy Hash

Click **Copy Hash** to copy the model's SHA-256 hash to your clipboard. You can share this hash with others so they can load the same model on their blocks.

---

## Model Requirements

- Format: Wavefront `.obj`
- Texture: `.png`
- Maximum model size: **50 MB**
- Only triangulated faces are supported (quads will be auto-converted if possible)
- Materials inside OBJ are ignored — only the single `texture.png` is used

---

## Multiplayer

- Models are stored in the world folder (`modelle_data/`).
- When a player loads a model by hash, the server sends it to other players automatically.
- If the server does not have the model file, it will request it from the client who placed it.

---

## License

This project is licensed under the **MIT License**.
