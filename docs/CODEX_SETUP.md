# Codex setup for VoxelWeave

This repository keeps project instructions small so Codex can spend context on the code and current task.

## Recommended local settings

Use ChatGPT sign-in rather than an API key when you want Codex usage to come from the ChatGPT plan allowance.

In the effective user Codex config (normally `~/.codex/config.toml`), preserve existing settings and add/merge:

```toml
model_reasoning_effort = "low"

[features.context_management]
experimental_mode = true
```

Do not duplicate an existing `[features.context_management]` table or `experimental_mode` key. Back up the file before changing it.

Do not force a model name in this repository. Select GPT-6 Astra in Codex only when it is available to the signed-in account; otherwise use the best available Codex model and keep the same workflow.

## Local setup prompt

Give Codex this prompt once on the development machine:

```text
Inspect the effective Codex configuration on this PC and identify the config.toml actually being used (normally ~/.codex/config.toml). Read it before editing.

Create a recoverable backup if a change is required. Preserve all unrelated settings and make the update idempotent: do not create duplicate TOML tables or keys.

Ensure:
- model_reasoning_effort = "low"
- [features.context_management].experimental_mode = true

Validate the resulting TOML and verify whether the running Codex client actually recognizes the settings. Report:
1. which config file is effective,
2. whether each setting was already present or changed,
3. backup location,
4. validation result,
5. whether a new thread or client restart is required.

Do not modify system-managed or plugin cache files.
```

## Skills / instruction audit

There is intentionally no project `SKILL.md` yet. Do not add one merely to save tokens. Add a project skill only when VoxelWeave has a repeatable specialized workflow that benefits from an explicit trigger.

For user/global Skills, let Codex audit only user-maintained files. Remove duplicated prose and keep triggers narrow, but preserve safety boundaries, acceptance criteria, permissions and failure conditions.

## Working pattern for Plus

Use Astra for bounded units of work rather than one giant implementation request:

1. State the current goal and acceptance criteria.
2. Let Codex inspect only the relevant files and ask only questions that materially change the result.
3. Implement the smallest complete vertical slice.
4. Build/test it.
5. Review the diff.
6. Start the next small unit in a fresh task/thread when the previous unit is complete.

For VoxelWeave, GitHub Issue #1 is the first vertical slice. Do not ask Astra to implement smoothing, dithering, palette optimization, contour correction and export recovery all at once.

## First VoxelWeave prompt

```text
Open bosatsuKing/VoxelWeave. Read AGENTS.md first, then GitHub Issue #1. Read docs/PRODUCT.md or docs/ARCHITECTURE.md only when needed for the current decision.

Inspect the repository before changing anything. Implement the smallest runnable vertical slice from Issue #1. Keep the scope bounded, preserve the product boundaries in AGENTS.md, run the available build/tests, and report the resulting diff, verification evidence, and remaining blockers.
```
