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
- Codex local setup: `docs/CODEX_SETUP.md`
- Current implementation unit: the active issue/PR; read parent issues only when needed

Do not reread every reference on every turn. Read the smallest relevant source first, then inspect code before changing dependencies or package structure.

## Current implementation order

1. Verify exact target dependency versions for Minecraft 26.1.2-era tooling.
2. Establish a reproducible build/dev client.
3. Add a narrow Litematica/MaLiLib adapter.
4. Implement bounded block replacement as the first vertical slice.
5. Add preview → commit and undo/redo.
6. Add safe export and recovery.
7. Then add palette/color tools, smoothing, dithering, cleanup and contour refinement.
