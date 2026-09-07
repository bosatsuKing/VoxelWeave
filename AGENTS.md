# AGENTS.md — VoxelWeave

VoxelWeave is a client-side Minecraft mod for refining Litematica schematics created from Blender / 3D-model conversion workflows.

## Priority

Explicit user instructions override this file. If instructions conflict or a requirement is ambiguous enough to change the result, state the conflict briefly; otherwise proceed.

## Product boundaries

- Start after 3D → schematic conversion; do not recreate proprietary converters.
- Preserve the original schematic and provide a recovery path for writes.
- Preview and commit are separate states.
- Editing operations must support reversible change sets / undo-redo.
- Keep Minecraft/Litematica integration separate from pure analysis/transformation logic.
- Preserve creator intent: do not make one generic procedural smoothing/gradient style the default answer to every shape.
- Treat missing analysis context as unknown, not automatically as air/empty geometry.
- Avoid expensive full-schematic work on render ticks.
- No hidden networking, packet manipulation, forced chunk loading, or server automation.
- Do not copy proprietary code, assets, UI, or reverse-engineered algorithms.

## Quality gates

For each focused change:

- build must pass;
- pure logic gets deterministic tests where practical;
- edits must stay inside the selected region;
- analysis must not mutate source snapshots;
- cleanup candidates that depend on complete connectivity must fail conservative when boundary data is unknown;
- failed export must not corrupt the only valid copy;
- missing/unsupported optional integrations fail with a readable message;
- ambiguous Litematica capture must fail closed rather than mix placements;
- do not mix unrelated refactors into feature work.

Bug fixes: observation → reproduction → root cause → fix → regression test.

## Read only when relevant

- Product scope: `docs/PRODUCT.md`
- Architecture/package boundaries: `docs/ARCHITECTURE.md`
- Selection/snapshot semantics: `docs/SELECTION_DOMAIN.md`
- Surface analysis semantics: `docs/SURFACE_ANALYSIS.md`
- Codex local setup: `docs/CODEX_SETUP.md`
- Current implementation unit: the open GitHub issue and PR being worked on

Do not reread every reference on every turn. Read the smallest relevant source first, then inspect code before changing dependencies or package structure.

## Current implementation order

Completed/merged foundation:

1. Verify exact target dependency versions for Minecraft 26.1.2-era tooling.
2. Establish a reproducible build/dev client.
3. Add a narrow Litematica/MaLiLib adapter and world-space selection/placement mapping.
4. Implement bounded block replacement as the first deterministic transformation.
5. Add preview → commit and undo/redo.
6. Capture the current bounded Litematica target into an immutable `SchematicSnapshot`.

Current shape-analysis sequence:

7. Add conservative six-neighbor surface topology and connected-component analysis.
8. Add higher-order feature descriptors such as ridge/edge strength and curvature-like local measures.
9. Add shape refinement tools: cleanup, spike removal, smoothing/relaxation and contour correction.
10. Add palette/color tools: palette mapping, gradients, patterns and dithering.
11. Add Minecraft preview rendering/UI over pending `ChangeSet` data.
12. Add safe Litematica schematic commit/write-back with explicit recovery boundaries.
13. Add safe `.litematic` export and validation/recovery.

Step 5 implementation is merged, but GitHub Issue #5 remains open until local Gradle/dev-client integration verification evidence is actually produced.
