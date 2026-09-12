# Codex setup for VoxelWeave

VoxelWeave keeps persistent agent instructions deliberately small so Codex can spend context on the current task, code and evidence instead of replaying project history.

This guidance follows the September 2026 OpenAI recommendation to revisit skills, `AGENTS.md`, and task prompts for GPT-6 Astra: keep triggers narrow, use progressive disclosure, read only what the task needs, avoid redundant scaffolding, and define the completion boundary clearly.

Reference: https://developers.openai.com/blog/rethinking-skills-and-prompts-for-gpt-6-astra

## Repository instructions

`AGENTS.md` contains only durable repository-wide rules. It should not become a full project map, historical changelog, or mandatory preflight reading list.

The task/request and active Issue/PR provide volatile scope and acceptance criteria. Domain-specific details stay in their corresponding docs and should be read only when the current decision needs them.

Do not require Codex to read every design document before every edit. Point it to the relevant source instead:

- product decisions → `docs/PRODUCT.md`
- package/layer boundaries → `docs/ARCHITECTURE.md`
- selection/snapshot semantics → `docs/SELECTION_DOMAIN.md`
- surface/feature/protrusion analysis → `docs/SURFACE_ANALYSIS.md`

## Model and local configuration

Do not make VoxelWeave depend on one Codex model, one reasoning-effort setting, or an experimental context-management flag. Model availability and client defaults change independently of this repository.

Use the best available Codex model for the task. When GPT-6 Astra is selected, avoid adding extra handholding solely because older models needed it. Preserve user-level configuration unless a specific task requires a local setting change and the user explicitly requests it.

If local Codex configuration must be changed, inspect the effective config first, preserve unrelated settings, make the edit recoverable/idempotent, validate the result, and do not modify system-managed or plugin-cache files.

## Skills

There is intentionally no repository `SKILL.md` today.

Add a project skill only when VoxelWeave gains a repeatable specialized workflow that benefits from a distinct trigger. A useful skill should follow all of these rules:

- the description is short and says exactly when the skill applies;
- the trigger is narrow enough that ordinary adjacent work does not load it;
- the root skill is a minimal router when multiple workflows exist;
- supporting docs/scripts are loaded through progressive disclosure;
- it does not repeat `AGENTS.md`, architecture docs, or active-Issue acceptance criteria;
- it does not encode a long itinerary that a capable model can infer from the task itself.

For user/global skills, audit only user-maintained files. Prefer deleting duplicate or broad trigger prose over adding another layer of exceptions.

## Task prompts

A good VoxelWeave task prompt normally needs only four things:

1. the goal or active Issue;
2. the important acceptance criteria or unusual constraints;
3. the requested completion state;
4. any explicit permission boundary that differs from the repository default.

Do not paste the repository map, full architecture, completed-step history, or `AGENTS.md` back into every prompt. Codex can inspect the relevant files itself.

Example for issue-scoped implementation:

```text
Open bosatsuKing/VoxelWeave and use AGENTS.md plus Issue #<N> as the source of truth.

Implement Issue #<N> only. Read additional design docs only when the current decision requires them. Reuse existing APIs and preserve the repository invariants.

Continue through implementation, proportional verification, final diff review, and affected documentation. Fix failures caused by this change and rerun affected checks without asking for approval at each step.

Stop at PR ready. Do not merge or start the next Issue.

Report the branch/commit/PR, verification evidence, remaining limitations, and any blocker.
```

For a documentation-only task, omit code/build language that does not apply. For a research spike, state the evidence required and explicitly say whether production implementation is out of scope.

## Autonomy and decision boundaries

Inside an authorized task, Codex should be allowed to inspect, edit, test, diagnose and fix the requested change without pausing for routine confirmation. Safe local tests use repository fixtures and have no production access; run affected tests, fix failures caused by the task, and rerun them as needed.

Keep meaningful boundaries meaningful. GitHub/external mutations and work beyond the requested stop condition still require task authorization. Do not add defensive “ask before every step” language merely because older models once overreached; it can make Astra stop prematurely.

## Verification

Do not turn verification into a fixed ceremony for every task.

- documentation-only work: consistency, links/references, and final diff;
- pure logic: focused deterministic tests first;
- runtime/integration/build changes: relevant builds and manual checks where the contract changed;
- code/build PR readiness: repository CI remains the final full gate for the current PR head.

The goal is evidence that matches the changed behavior, not maximum command count.

## Persistence and completion

GPT-6 Astra may be more conservative about deciding when a task is finished, so prompts should name the desired end state.

If the user asks for implementation, verification and PR readiness, Codex should continue until all three are complete or a real blocker is found. Do not return after the first implementation merely because it compiles. Conversely, if the user asks for a design review or research spike only, stop before production code.

Use the completion definitions in `AGENTS.md`: **Implementation complete**, **PR ready**, and **Merged / delivered**.

## Periodic instruction audit

When model behavior or the development workflow changes materially, audit these instructions instead of appending new rules indefinitely:

- remove instructions the model already handles reliably;
- shorten broad skill descriptions and triggers;
- replace mandatory “read all docs” rules with contextual routing;
- remove duplicate testing requirements;
- keep explicit safety/product invariants and real permission boundaries;
- verify examples and active-Issue references are not stale.

The preferred direction is less persistent context with clearer task-specific acceptance criteria.
