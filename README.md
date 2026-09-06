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

The first pure editing vertical slice is complete and the read-only Litematica bridge now reaches real schematic block data.

Implemented foundation:

1. Fabric 26.1.2 client bootstrap and pinned optional Litematica/MaLiLib integration.
2. Immutable world-space selection / placement geometry and `selection × placement` targeting.
3. Deterministic bounded block replacement producing immutable `ChangeSet` values.
4. Preview → commit workspace separation with reversible undo/redo.
5. Read-only capture of the current bounded Litematica target into a world-space `SchematicSnapshot`.

The Step 5 capture includes air and non-air states inside the target, deduplicates overlapping target regions and reads the block state as Litematica presents it in world space. If another schematic placement or multiple selected-placement subregions overlap the same captured coordinate, capture fails closed instead of silently mixing ambiguous schematic-world data.

Still not implemented: Litematica schematic write-back, Minecraft preview rendering/UI, surface analysis, smoothing/cleanup/contour tools, palette/gradient/pattern/dithering tools, and safe `.litematic` export/recovery.

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

See [AGENTS.md](AGENTS.md) for implementation rules, [docs/PRODUCT.md](docs/PRODUCT.md) for the product definition, and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for package boundaries.

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

When the integration is ready, `ReadOnlySchematicIntegration.captureSnapshot()` exposes the current bounded target and immutable `SchematicSnapshot` to the VoxelWeave editing workspace. Capture is read-only and is not automatically run every render tick.

Pure domain and transformation/history tests run with:

```text
./gradlew test
```

Before merging Litematica integration changes, also verify:

```text
./gradlew build -PwithLitematica=true
```

See [selection and snapshot domain](docs/SELECTION_DOMAIN.md) for coordinate and capture semantics.
