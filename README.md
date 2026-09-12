# VoxelWeave

**English** | [日本語](README_JA.md)

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

The first pure editing vertical slice is complete, the read-only Litematica bridge reaches real schematic block data, and shape analysis supports conservative disconnected-island cleanup planning and bounded connected-protrusion evidence.

Implemented foundation:

1. Fabric 26.1.2 client bootstrap and pinned optional Litematica/MaLiLib integration.
2. Immutable world-space selection / placement geometry and `selection × placement` targeting.
3. Deterministic bounded block replacement producing immutable `ChangeSet` values.
4. Preview → commit workspace separation with reversible undo/redo.
5. Read-only capture of the current bounded Litematica target into a world-space `SchematicSnapshot`.
6. Pure six-neighbor surface analysis with exposed/interior/unknown topology and deterministic connected-component sizing.
7. Pure feature-preservation descriptors for face, edge, corner, thin feature, tip, isolated, interior and unknown-boundary cells.
8. Pure disconnected-island cleanup planning with size limits, whole-component feature protection and stale-analysis rejection, producing an immutable `ChangeSet` for the existing workspace.
9. Pure bounded TIP-origin path analysis with separate attachment/support evidence, explicit termination/context, canonical complete TIP-to-TIP paths and source binding. This does not classify artifacts or generate edits.

The Step 5 capture includes air and non-air states inside the target, deduplicates overlapping target regions and reads the block state as Litematica presents it in world space. If another schematic placement or multiple selected-placement subregions overlap the same captured coordinate, capture fails closed instead of silently mixing ambiguous schematic-world data.

Shape analysis does not mutate geometry. It records conservative structural evidence for later cleanup and smoothing: known exposed faces, unknown capture-boundary faces, occupied-neighbor counts, complete/incomplete 6-connected components, local feature kinds, discrete exposure direction and nearby surface context. Missing neighbor data is never silently treated as air.

Cleanup is limited to fully-known disconnected components within the analyzed target. Callers explicitly supply replacement state and protected feature kinds. Incomplete/unknown components are preserved; no schematic or world write occurs.

Connected-protrusion analysis measures only one-voxel-wide terminal paths using six-neighbor topology and an explicit positive visit budget. Empty results mean no evidence in this limited model, not artifact-free geometry. Connected cleanup policy (Step 7B-2) remains unimplemented.

Still not implemented: Litematica schematic write-back, Minecraft preview rendering/UI, larger-neighborhood curvature/feature fitting, connected-surface/spike cleanup, smoothing/relaxation/contour transforms, palette/gradient/pattern/dithering tools, and safe `.litematic` export/recovery.

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

Domain semantics:

- [selection and snapshot domain](docs/SELECTION_DOMAIN.md)
- [surface analysis domain](docs/SURFACE_ANALYSIS.md)

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

Pure domain, analysis and transformation/history tests run with:

```text
./gradlew test
```

Pull requests also run JDK 25 GitHub Actions checks for tests, the default build and the Litematica-enabled build. Interactive Minecraft/Litematica smoke verification remains manual.
