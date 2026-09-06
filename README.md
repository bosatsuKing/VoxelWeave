# VoxelWeave

VoxelWeave is a Minecraft client-side tool for refining voxelized 3D models after conversion to Litematica schematics.

## Product goal

The target workflow is:

```text
Blender / 3D model
        ↓
3D-to-voxel / schematic converter
        ↓
.litematic
        ↓
Minecraft + Litematica
        ↓
VoxelWeave
 ├─ inspect conversion artifacts
 ├─ block replacement
 ├─ palette / color adjustment
 ├─ smoothing
 ├─ dithering
 ├─ noise cleanup
 ├─ contour correction
 ├─ local region editing
 ├─ before / after preview
 └─ safe schematic export
        ↓
refined .litematic
```

VoxelWeave does **not** aim to clone a proprietary converter. Its primary value is the post-conversion editing and refinement workflow inside Minecraft, with export stability treated as a first-class quality requirement.

## Status

Early prototype / repository bootstrap.

The existing prototype targets the Minecraft 26.1.2 generation, but exact Minecraft, Fabric Loader, Fabric API, Java, Gradle and Litematica/MaLiLib versions must be verified before build configuration is finalized.

## Initial MVP scope

1. Load/read the active Litematica schematic context.
2. Select a bounded region for editing.
3. Replace one block type/palette entry with another.
4. Preview edits before commit.
5. Undo/redo for VoxelWeave operations.
6. Export/save without corrupting the source schematic.
7. Fail safely: original schematic must remain recoverable if an operation or export fails.

## Non-goals for the first MVP

- Reimplementing Blender or a full 3D voxel converter.
- Copying proprietary algorithms, code, assets, or UI from commercial tools.
- Server-side automation or hidden packet manipulation.
- Destructive edits without an undo/recovery path.

## Development handoff

See [AGENTS.md](AGENTS.md) for implementation rules and [docs/PRODUCT.md](docs/PRODUCT.md) for the current product definition.
