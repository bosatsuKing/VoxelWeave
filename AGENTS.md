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
- Prefer conservative feature classification at uncertain boundaries.
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
- feature-preservation decisions must not treat `UNKNOWN_BOUNDARY` as a confident face/edge/corner/tip classification;
- failed export must not corrupt the only valid copy;
- missing/unsupported optional integrations fail with a readable message;
- ambiguous Litematica capture must fail closed rather than mix placements;
- do not mix unrelated refactors into feature work.

Bug fixes: observation → reproduction → root cause → fix → regression test.

## Implementation loop

For the active issue, follow:

Discover → Plan → Implement → Verify → Self-review → Document → PR → CI → Update issue → Stop.

- Discover: inspect branch/status and compare the active issue with current code. When requested to start from latest main, fetch and synchronize safely before editing; preserve existing work.
- Plan: identify the smallest affected files, reusable APIs, safety invariants, tests and docs. Do not reimplement existing behavior or expand the issue scope.
- Implement: make focused changes that preserve the product boundaries.
- Verify: for code/build changes, run `./gradlew test`, `./gradlew build` and `./gradlew build -PwithLitematica=true`, plus relevant isolation/write-API scans. Always run `git diff --check`. For documentation-only changes, check consistency and links; builds are not required.
- Self-review: review the diff against the base branch, including new files, for correctness, scope, safety, determinism and unnecessary work.
- Document: update only affected docs and record verified behavior and remaining limitations. Recheck the final diff; rerun affected verification if code changes.
- PR/CI: when authorized, commit, push and open a focused PR. Verify test, default-build and Litematica-enabled-build CI results for the current PR head. Missing, pending or skipped checks are not green; report them unless the user explicitly accepts local verification instead.
- Update issue: when authorized, mark only verified acceptance criteria complete. Close only after implementation, tests, docs, self-review and required CI gates pass, or an explicit user-approved exception applies.
- Stop at the requested boundary. Do not merge or start the next issue without authorization.

On a verification, review or CI failure: inspect evidence → identify root cause → focused fix → regression test where relevant → rerun affected verification → self-review. Repeat until the applicable gates pass. Do not repeat speculative fixes or treat unavailable verification as success. Report external blockers that cannot be resolved within the authorized scope.

This loop does not itself authorize commits, pushes, branch creation/deletion, PR/issue mutations or merges. Follow the user's current authorization and stop condition; omit stages outside that scope.

## Context discipline

- Read `AGENTS.md` and the active issue (or its supplied contents) first, then only relevant source, tests and docs.
- Use current repository code/docs as implementation context and the active issue as the acceptance criteria. Do not reconstruct completed steps from old prompts; explicit user instructions retain priority.
- Report material conflicts instead of silently changing the issue's goal.
- Do not reread unchanged references or scan the whole repository without a concrete reason.
- Keep plans and progress updates short; focus on new findings, decisions and blockers.

## Read only when relevant

- Product scope: `docs/PRODUCT.md`
- Architecture/package boundaries: `docs/ARCHITECTURE.md`
- Selection/snapshot semantics: `docs/SELECTION_DOMAIN.md`
- Surface/feature analysis semantics: `docs/SURFACE_ANALYSIS.md`
- Codex local setup: `docs/CODEX_SETUP.md`
- Current implementation unit: the active issue/PR; read parent issues only when needed

Do not reread every reference on every turn. Read the smallest relevant source first, then inspect code before changing dependencies or package structure.

## Current implementation order

Completed/merged foundation:

1. Verify exact target dependency versions for Minecraft 26.1.2-era tooling.
2. Establish a reproducible build/dev client.
3. Add a narrow Litematica/MaLiLib adapter and world-space selection/placement mapping.
4. Implement bounded block replacement as the first deterministic transformation.
5. Add preview → commit and undo/redo.
6. Capture the current bounded Litematica target into an immutable `SchematicSnapshot`.
7. Add conservative six-neighbor surface topology and connected-component analysis.

Current shape-analysis sequence:

8. Add local feature-preservation descriptors for face/edge/corner/thin-feature/tip/unknown-boundary evidence.
9. Conservative disconnected-island cleanup planning is implemented as a pure `ChangeSet` planner. Reuse existing topology/features, require source-bound analysis and preserve incomplete/unknown or protected components as a whole. Connected-surface/spike cleanup remains future work.
10. Add feature-preserving smoothing/relaxation and contour correction; add larger-neighborhood curvature descriptors only where needed by preservation policy.
11. Add palette/color tools: palette mapping, gradients, patterns and dithering.
12. Add Minecraft preview rendering/UI over pending `ChangeSet` data.
13. Add safe Litematica schematic commit/write-back with explicit recovery boundaries.
14. Add safe `.litematic` export and validation/recovery.

Pull requests must pass the JDK 25 GitHub Actions Gradle gates (`test`, default `build`, and Litematica-enabled `build`) before merge. Interactive Minecraft/Litematica smoke checks remain manual where required.

Step 5 implementation is merged, but GitHub Issue #5 remains open until interactive Litematica dev-client verification evidence is actually produced.
