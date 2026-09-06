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

Step 2 adds immutable world-space selection/placement geometry to the Step 1
optional Litematica/MaLiLib integration. Schematic editing is not implemented yet.

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

## Development

VoxelWeave targets Minecraft 26.1.2 and requires JDK 25. Build with the checked-in Gradle Wrapper:

```text
./gradlew build
```

The default development client intentionally starts without Litematica to verify that the optional integration fails safely:

```text
./gradlew runClient
```

To include the pinned Litematica and MaLiLib versions in the development runtime only:

```text
./gradlew runClient -PwithLitematica=true
```

Press `V` in a world to show the read-only integration diagnostic. It reports dependency presence,
selected-placement presence, current-selection presence and whether their world-space intersection
is non-empty. It does not modify schematics, worlds or configuration.

Pure domain and boundary-conversion tests run with `./gradlew test` without starting Minecraft.
See [selection domain](docs/SELECTION_DOMAIN.md) for coordinate and snapshot semantics.
