# VoxelWeave Product Definition

## Problem

3D models converted from Blender or other modeling tools into Minecraft/Litematica schematics often need substantial manual cleanup after conversion. Typical post-conversion problems include palette mismatch, stair-stepping/noisy surfaces, awkward contours, isolated artifacts and repeated block substitutions.

Existing high-precision conversion tools demonstrate that users will pay for quality in this workflow, but VoxelWeave is intentionally scoped to the **post-conversion refinement stage** rather than cloning a commercial converter.

Export reliability is a core product requirement. The workflow must assume that large schematics and conversion-derived data can stress tools and that users need recovery when an operation fails.

## Primary user

A Minecraft builder who:

- creates or receives 3D models outside Minecraft;
- converts them to a schematic;
- loads the schematic with Litematica;
- wants to refine the result inside Minecraft without repeatedly round-tripping through Blender or manually replacing thousands of blocks.

## Core jobs to be done

1. Select a meaningful part of a converted schematic.
2. Understand what blocks/palette entries are present.
3. Preview a proposed transformation.
4. Apply the transformation only to the intended region.
5. Undo mistakes quickly.
6. Export/save a valid result without losing the original.

## MVP

### P0

- Workspace around an active schematic/selection.
- Bounded block replacement.
- Preview before commit.
- Undo/redo.
- Safe export/recovery.
- User-readable errors instead of hard crashes where recovery is possible.

### P1

- Palette inspection and bulk mapping.
- Color-oriented replacement assistance.
- Noise/island cleanup.
- Basic surface smoothing.
- Contour correction tools.

### P2

- Dithering strategies.
- Material-aware palette presets.
- Batch transformation recipes.
- Diff/compare views between source and refined schematic.

## Product principles

### Reversible
Every edit should be recoverable during a session. Source schematic safety takes priority over convenience.

### Previewable
A user should be able to see the impact of a substantial edit before it is committed.

### Local and bounded
Tools should act on explicit selections or bounded schematic data, not on unrelated world state.

### Stable
A failed operation should degrade gracefully and preserve recoverable data.

### Fast enough for builders
Large builds are expected. Avoid designs that rescan the entire schematic every frame or copy large block arrays unnecessarily.

### Interoperable
Litematica/MaLiLib integration should be isolated behind adapters so transformation logic remains independently testable and maintainable.

## Success criteria for prototype → alpha

- Development client launches consistently.
- A real converted `.litematic` can be opened/identified through the supported workflow.
- A user can select a region and preview/apply block replacement.
- Undo/redo works across multiple VoxelWeave edits.
- Exported output can be reopened successfully.
- Simulated export failure does not destroy the source schematic.
- No common editing action produces a Minecraft client crash in normal test scenarios.
