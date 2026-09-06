# AGENTS.md — VoxelWeave

This file is the working contract for Codex and other coding agents operating in this repository.

## Goal

Build VoxelWeave as a Minecraft client-side MOD that refines Litematica schematics produced from Blender / 3D-model conversion workflows.

The product starts **after conversion**. Focus on reliable, reversible in-game refinement rather than recreating proprietary 3D conversion products.

## Required engineering principles

- Preserve the user's original schematic. Destructive in-place writes are not acceptable without a recovery path.
- Every editing operation must be representable as an explicit command/change set so undo/redo is possible.
- Preview and commit must be separate states.
- Export must be atomic where feasible: write a temporary output, validate it, then replace/finalize the destination.
- A failed export must never leave the only valid copy corrupted.
- Keep Minecraft/Litematica adapters separate from pure transformation logic.
- Prefer deterministic, testable transformation code.
- Avoid performing expensive full-schematic work every render tick.
- Avoid hidden network behavior, packet manipulation, forced chunk loading, or server automation.
- Do not copy proprietary code, assets, UI, or reverse-engineered algorithms from commercial tools.

## Target architecture

```text
Minecraft / Litematica integration
          ↓
  SchematicWorkspace
          ↓
  Selection / Preview
          ↓
  Transformation commands
          ↓
  ChangeSet + Undo/Redo
          ↓
  Validation
          ↓
  Safe Export
```

Suggested package boundaries (final names may change after prototype inspection):

```text
voxelweave/
  client/        Minecraft/Fabric lifecycle and keybind integration
  integration/   Litematica/MaLiLib adapters
  workspace/     active schematic, selection, preview state
  transform/     pure block/palette transformations
  history/       commands, change sets, undo/redo
  export/        safe/atomic output and validation
  ui/            screens, panels, tool feedback
  config/        local configuration
```

## MVP implementation order

1. Verify exact Minecraft 26.1.2-era dependency versions and import the existing prototype if available.
2. Establish a build that launches a development client.
3. Detect/read the active schematic or selected schematic region through a narrow integration adapter.
4. Implement one deterministic transformation: block replacement in a bounded selection.
5. Add preview → commit separation.
6. Add undo/redo.
7. Add safe export with validation and recovery.
8. Add automated tests for pure transformation/history/export-policy code.
9. Only then add smoothing, dithering, palette/color refinement and contour tools.

## Quality gates

At minimum for MVP:

- `./gradlew build` succeeds.
- Pure transformation tests cover normal, empty, boundary and invalid inputs.
- Undo followed by redo restores the expected state.
- Export failure leaves the original file intact.
- Unsupported/missing optional integration fails with a user-readable message rather than a crash.
- No operation performs unbounded work on the render thread.

## Change discipline

For bug fixes, prefer:

```text
observation → reproduction → root cause → fix → regression test
```

Do not refactor unrelated code while implementing a focused feature unless the refactor is necessary to make the change safe or testable.

## First task for Codex

Read this file, `README.md`, `docs/PRODUCT.md`, and `docs/ARCHITECTURE.md`. Then inspect the repository and any imported prototype before changing dependency versions or package structure. Produce a short implementation plan and start with the smallest runnable vertical slice.
