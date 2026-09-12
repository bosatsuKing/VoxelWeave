# AGENTS.md — VoxelWeave

VoxelWeave is a client-side Minecraft mod for refining converted 3D-model / Litematica workflows inside Minecraft.

## Task contract

The explicit user request and, when present, the active Issue/PR define the current scope and acceptance criteria.

Within that authorized scope, proceed without repeatedly asking for approval to inspect files, edit code/docs, run safe local verification, diagnose failures, fix failures caused by the requested change, and rerun affected checks. Do not stop at the first compiling implementation when the requested completion state clearly includes verification, review, documentation, PR readiness, or delivery.

Repository or external mutations still follow the requested boundary. Creating/updating branches, commits, PRs, Issues, releases, or merging work requires that the current request authorizes that stage. Tool availability alone is not authorization.

If a requirement is genuinely ambiguous enough to change the result, surface the decision briefly. Otherwise use repository evidence and continue.

## Product invariants

- Preserve the original schematic and a recovery path for writes.
- Preview and commit are separate states; VoxelWeave edits must remain reversible through exact change history / undo-redo.
- Keep Minecraft, Litematica, MaLiLib and converter-specific integration behind adapters. Pure domain, analysis and transformation code must not depend on those APIs.
- Preserve creator intent. Do not turn one generic smoothing, gradient or procedural style into the default answer for every shape.
- Missing or incomplete analysis context stays unknown; never silently reinterpret it as air or confident geometry.
- Prefer conservative behavior at uncertain boundaries and fail closed on ambiguous capture/targeting.
- Keep editing bounded to explicit selections/targets and avoid full-schematic work on render ticks.
- No hidden networking, packet manipulation, forced chunk loading, or server automation.
- Do not copy proprietary code, assets, UI, or reverse-engineered algorithms.

## Read only what the task needs

Start with the current request and this file. For issue-scoped work, read the active Issue/PR next, then inspect only the source, tests and docs needed for the decision at hand.

Use these references when relevant rather than reading all of them by default:

- product scope: `docs/PRODUCT.md`
- architecture/package boundaries: `docs/ARCHITECTURE.md`
- selection/snapshot semantics: `docs/SELECTION_DOMAIN.md`
- surface/feature/protrusion analysis semantics: `docs/SURFACE_ANALYSIS.md`
- Codex setup/instruction guidance: `docs/CODEX_SETUP.md`

Do not reconstruct completed work from old prompts when current code, docs, Issues or PRs already provide the source of truth. Avoid broad repository scans without a concrete reason.

## Implementation workflow

Use the smallest workflow that completes the authorized task:

1. **Discover** — inspect current branch/status, active task, and the smallest relevant code/docs.
2. **Plan** — identify affected files, reusable APIs, invariants, tests and the requested stop condition.
3. **Implement** — make the smallest complete change; avoid unrelated refactors.
4. **Verify** — run checks proportional to the change, fix in-scope failures, and rerun affected checks.
5. **Self-review** — compare the final diff against the task and base branch for correctness, scope, safety and unnecessary work.
6. **Document** — update only docs affected by verified behavior or remaining limitations.
7. **Deliver** — only when authorized: commit/push/PR, wait for required CI, merge if authorized, then update/close the Issue when its completion boundary is met.

On an in-scope failure, diagnose from evidence and keep going until the requested completion state is reached. Stop only for a real blocker: a required user decision, missing permission, unavailable dependency/environment, or a failure that cannot be resolved safely inside scope. Report unverified gates explicitly; never label unavailable checks as passing.

## Verification

Verification should be proportional, not ritualized.

- Documentation-only changes: check content consistency, links/references and final diff. Gradle test/build is unnecessary unless executable configuration or active acceptance criteria are affected.
- Pure logic changes: run focused deterministic tests first; add regression tests for bugs or newly defined behavior.
- Integration/build/configuration changes: run the relevant build/integration checks and any manual verification required by the changed contract.
- Before a code/build PR is considered **PR ready**, the current PR head must pass the repository JDK 25 CI gates: `test`, default `build`, and Litematica-enabled `build`, unless the user explicitly approves an exception.
- Dev-client verification is required when capture/placement mapping, Minecraft renderer/UI behavior, or another runtime integration contract changes, or when the active acceptance criteria require it. Minecraft startup alone is not an interactive acceptance result.
- Use `git diff --check` for implementation changes and review the complete diff, including new files, before delivery.

Bug fixes follow: observation → reproduction → root cause → focused fix → regression test when practical.

## Completion states

Use the state the task actually requests:

- **Implementation complete** — requested change, applicable verification, affected docs and self-review are complete with no known blocker.
- **PR ready** — PR exists, required checks pass for its current head, and no known review blocker remains.
- **Merged / delivered** — the authorized PR is merged into the target branch and required delivery acceptance criteria are verified.

Do not equate PR creation with delivery. If the user asks for a complete implementation, continue through verification and self-review rather than returning after the first pass. If the user sets an explicit stop point, honor it.

## Instruction maintenance

Keep this file limited to durable repository-wide rules. Put changing implementation status in README/Issues/PRs and detailed algorithm semantics in the relevant domain docs.

Do not add a project `SKILL.md` merely to save prompt tokens. A repository skill is justified only for a repeatable specialized workflow with a narrow trigger. If one is added later, keep its description short, make the root skill a minimal router, and use progressive disclosure for supporting docs/scripts instead of loading a large recipe for every task.

When model capabilities or workflows change materially, remove redundant scaffolding rather than layering new instructions on top of old ones.
