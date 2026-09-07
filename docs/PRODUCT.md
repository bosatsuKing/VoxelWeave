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

## Current implementation state

The repository currently contains the non-destructive editing and shape-analysis foundation:

- pinned Fabric / Litematica / MaLiLib integration for Minecraft 26.1.2;
- immutable world-space selection and placement targeting;
- read-only capture of the bounded selected Litematica target into `SchematicSnapshot`;
- deterministic bounded block replacement;
- preview/commit separation inside the VoxelWeave workspace;
- reversible `ChangeSet` history with undo/redo;
- pure six-neighbor surface topology analysis with exposed/interior/unknown distinction and deterministic component sizing;
- pure local feature descriptors that distinguish broad faces, edges, corners, thin geometry, tips, isolated cells, interiors and unknown boundaries.

The current `commit` is internal workspace state only. It does **not** write to the Litematica schematic or Minecraft world.

Shape analysis intentionally reports structural evidence before editing geometry. Missing neighbor data remains unknown instead of being treated as air. Disconnected-component cleanup candidates are only considered safe when connectivity is complete, and local feature descriptors keep unknown-boundary cells out of confident face/edge/corner/tip classes.

The next shape-refinement work should consume these descriptors so broad conversion noise can be treated differently from deliberate ridges, corners, thin ornament and spires. Larger-neighborhood curvature fitting may be added where it materially improves preservation decisions, but one generic smoothing style must not become the default visual signature of VoxelWeave.

## MVP

### P0

- Workspace around an active schematic/selection.
- Bounded block replacement.
- Preview before commit.
- Undo/redo.
- Safe Litematica write-back boundary.
- Safe export/recovery.
- User-readable errors instead of hard crashes where recovery is possible.

### P1

- Surface / shape analysis of captured schematic data.
- Palette inspection and bulk mapping.
- Color-oriented replacement assistance.
- Noise/island cleanup.
- Feature-preserving surface smoothing.
- Contour correction tools.

### P2

- Dithering strategies.
- Gradient and pattern composition.
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

### Preserve creator intent
Shape tools should remove conversion artifacts and repetitive procedural noise without forcing every build toward one recognizable smoothing, gradient or contour style. Large forms, important edges, thin features and intentional detail should be preservable independently from cleanup strength.

Analysis should prefer uncertainty over destructive guessing: unknown capture-boundary data is not equivalent to exposed air, incomplete components are not safe island-removal candidates, and uncertain local geometry is not promoted to a confident feature class.

### Stable
A failed operation should degrade gracefully and preserve recoverable data. Ambiguous schematic input must fail closed instead of silently editing the wrong placement.

### Fast enough for builders
Large builds are expected. Avoid designs that rescan the entire schematic every frame or copy large block arrays unnecessarily. Analysis should be explicit and cacheable by snapshot/workspace revision.

### Interoperable
Litematica/MaLiLib integration should be isolated behind adapters so transformation and analysis logic remain independently testable and maintainable.

## Success criteria for prototype → alpha

- Development client launches consistently.
- A real converted `.litematic` can be identified and captured through the supported workflow.
- A user can select a region and preview/apply block replacement in VoxelWeave workspace state.
- Undo/redo works across multiple VoxelWeave edits.
- Surface analysis can distinguish exposed surface, interior, isolated/noisy candidates and uncertain boundaries.
- Feature analysis can distinguish broad faces from edges, corners, tips and thin features well enough to support preservation-aware refinement.
- Shape refinement can remove obvious conversion artifacts without flattening protected structural detail.
- Litematica write-back revalidates the source/target before mutation.
- Exported output can be reopened successfully.
- Simulated export failure does not destroy the source schematic.
- No common editing action produces a Minecraft client crash in normal test scenarios.
