# AGENTS.md — VoxelWeave

VoxelWeave is a client-side Minecraft mod for refining Litematica schematics created from Blender / 3D-model conversion workflows.

## Priority

Explicit user instructions override this file. If instructions conflict or a requirement is ambiguous enough to change the result, state the conflict briefly; otherwise proceed.

## Product boundaries

- Start after 3D → schematic conversion; do not recreate proprietary converters.
- Preserve the original schematic and provide a recovery path for writes.
- Preview and commit are separate states.
- Editing operations must support reversible change sets / undo-redo.
- Keep Minecraft/Litematica integration separate from pure transformation logic.
- Avoid expensive full-schematic work on render ticks.
- No hidden networking, packet manipulation, forced chunk loading, or server automation.
- Do not copy proprietary code, assets, UI, or reverse-engineered algorithms.

## Quality gates

For each focused change:

- build must pass;
- pure logic gets deterministic tests where practical;
- edits must stay inside the selected region;
- failed export must not corrupt the only valid copy;
- missing/unsupported optional integrations fail with a readable message;
- do not mix unrelated refactors into feature work.

Bug fixes: observation → reproduction → root cause → fix → regression test.

## Read only when relevant

- Product scope: `docs/PRODUCT.md`
- Architecture/package boundaries: `docs/ARCHITECTURE.md`
- Codex local setup: `docs/CODEX_SETUP.md`
- Current implementation unit: GitHub Issue #1 and the issue/PR being worked on

Do not reread every reference on every turn. Read the smallest relevant source first, then inspect code before changing dependencies or package structure.

## Current implementation order

1. Verify exact target dependency versions for Minecraft 26.1.2-era tooling.
2. Establish a reproducible build/dev client.
3. Add a narrow Litematica/MaLiLib adapter.
4. Implement bounded block replacement as the first vertical slice.
5. Add preview → commit and undo/redo.
6. Add safe export and recovery.
7. Then add palette/color tools, smoothing, dithering, cleanup and contour refinement.
