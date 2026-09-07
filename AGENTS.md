# AGENTS.md — VoxelWeave

VoxelWeave is a client-side Minecraft mod for refining Litematica schematics created from Blender / 3D-model conversion workflows.

## Priority

Explicit user instructions override this file. If instructions conflict or a requirement is ambiguous enough to change the result, state the conflict briefly; otherwise proceed.

Perform only stages authorized by the explicit user request and active task. The implementation loop does not grant permission for Git mutations, external actions or work beyond the requested stop condition. Tool access or an existing command approval is not task authorization.

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

Apply the relevant verification gates to each focused change:

- code/build changes must pass the test/build gates in the implementation loop;
- documentation-only changes require consistency, link and diff checks; Gradle build/test is not required unless executable configuration is affected or the active acceptance criteria require it;
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

Dev-client verification is required for changes affecting Litematica capture/placement mapping or Minecraft renderer/UI behavior, and when required by the active acceptance criteria. Pure-only changes do not require client launch unless an integration contract is affected. Record observed behavior; Minecraft startup alone is not a passed UI or interactive test.

Before merge, pull requests must pass the JDK 25 GitHub Actions gates (`test`, default `build` and Litematica-enabled `build`) for the current PR head, unless the user explicitly approves an exception. Report exceptions as exceptions, never as passing checks.

## Implementation loop

For implementation work, use only the applicable, authorized stages:

Discover → Plan → Implement → Verify → Self-review → Document → PR → CI → Merge (if authorized) → Update issue → Stop.

- Discover: inspect branch/status and compare the current request, and active issue when one exists, with current code. When requested to start from latest main, fetch and synchronize safely before editing; preserve existing work.
- Plan: identify the smallest affected files, reusable APIs, safety invariants, tests and docs. Do not reimplement existing behavior or expand the task scope.
- Implement: make focused changes that preserve the product boundaries.
- Verify: for code/build changes, run `./gradlew test`, `./gradlew build` and `./gradlew build -PwithLitematica=true`, plus relevant isolation/write-API scans. Run `git diff --check` for every change. Apply the documentation-only and manual-verification conditions above.
- Self-review: review the diff against the base branch, including new files, for correctness, scope, safety, determinism and unnecessary work.
- Document: update only affected docs and record verified behavior and remaining limitations. Recheck the final diff; rerun affected verification if code changes.
- PR/CI: when authorized, commit, push and open a focused PR. Verify test, default-build and Litematica-enabled-build CI results for the current PR head. Missing, pending or skipped checks are not green; report them unless the user explicitly accepts local verification instead.
- Update issue: only when an issue exists and its update is authorized, mark verified acceptance criteria complete, including required manual verification. Close an implementation issue after merge and required gates pass, unless the user explicitly approves another completion boundary.
- Stop at the requested boundary. Do not merge or start the next issue without authorization.

On a verification, review or CI failure, diagnose from evidence:

- Resolvable within the authorized scope: make a focused fix, add a regression test where relevant, rerun affected verification and self-review.
- Cannot complete within the authorized scope because of a required user decision, unavailable environment/dependency or missing permission: report the exact blocker and unverified gates, then stop dependent work.

Do not repeat speculative fixes or treat unavailable verification as success.

## Completion states

- Implementation complete: requested changes, applicable local verification, affected docs and self-review are complete, with no known blocker.
- PR ready: the PR exists, required checks pass for its current head and no review blocker remains. Record any explicitly approved verification exception separately.
- Merged / delivered: the PR is merged into the target branch and any required delivery acceptance criteria are verified. Update/close the issue only when authorized.

Report the state actually reached. PR creation is not delivery. Tasks that do not request a PR stop at their own requested boundary; do not create an issue or PR merely to satisfy this loop.

## Context discipline

- Read `AGENTS.md` and the current request first. For issue-scoped work, read the active issue (or its supplied contents), then only relevant source, tests and docs.
- Use current repository code/docs as implementation context and the current request or active issue as the acceptance criteria. Do not reconstruct completed steps from old prompts; explicit user instructions retain priority.
- Report material conflicts instead of silently changing the task's goal.
- Keep implementation status in README and Issues/PRs, and algorithm-specific details in the relevant domain docs; do not add changing progress records to this file.
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
