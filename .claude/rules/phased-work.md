# Phased work: phases → tasks → commits → validation

No `paths` on purpose: this is how every piece of work is done, not only the Wear OS plan (`docs/wear/fases.md`). For work outside a numbered plan (a backlog item, a bugfix), apply the same loop at small scale: short task list, approval, validated commits, then ask for validation.

## Rules

1. **One phase, one session.** Once a phase is executed, validated and approved, the session is not reused: the next phase starts in a new session or after `/clear`. What must be remembered goes into the plan docs, not the chat.
2. **Propose the tasks and wait for approval.** Read the plan and the phase, propose the concrete task list (adapting the plan's if needed) and do not start until Bernat approves it.
3. **Tasks run in series and autonomously**, one at a time, validating each before the next.
4. **Each task declares how it is validated**, without depending on Bernat when possible:
   - Unit tests (JVM): `./gradlew testDebugUnitTest` and the task's new tests.
   - `./gradlew ktlintCheck`.
   - Screenshots: previews and captures compared with the references; Claude validates them.
   - Emulator: Claude validates UI changes on the Wear + phone emulator pair (or the phone emulator).
   - Human validation only for what cannot be done alone (real watch, biometric unlock, TalkBack, feel of use).
5. **Say how the phase is executed**, in the proposal (Bernat approves it with the rest):
   - **Model:** a smaller one (e.g. Haiku) for mechanical tasks, a more capable one for delicate ones (security, sync, Keystore and unlock, migrations).
   - **Agents:** whether a piece benefits from a subagent (exploration, docs research) or all is done in the main session.
   - **Parallelism:** tasks that touch different files and do not depend on each other run in parallel with agents, each in its own isolated worktree. Mark in the proposal which are parallel and which serial; integrate and validate each agent's result before continuing.
   - **Agent model:** for each implementation agent, assess whether Haiku is enough (mechanical, well-bounded tasks: components and previews from a design, strings in three languages, Gradle or CI plumbing, tests from a clear pattern). Keep a more capable model for privacy, sync, Keystore, migrations. If Haiku fails the first time, reassign instead of insisting. The main session's model always does the validation, never the agent that implemented.
6. **Mark progress in the plan.** When a task is done and validated, tick it (`- [x]`) with its commit hash: `- [x] Task — \`abc1234\``.
7. **Commits.** The proposal states the strategy: one commit per task (default, when the task makes sense alone) or one closing commit for the phase. The approval authorizes those commits and only those. When a task is validated (tests, ktlint, screenshots, emulator as relevant), commit it without asking again; if a validation fails, do not commit. Messages in English, small, with the usual attribution.
8. **Close the phase with Bernat's validation.** Present what was done, how it was validated and what is pending. Do not consider the phase closed, nor update its status in the plan, until Bernat approves.
9. **Push only with the phase validated**, never mid-task on your own unless Bernat asks explicitly (e.g. to test on a real device). No Google Play change without permission.
10. **Open decisions are asked, not invented.** If a phase depends on a decision or a missing design, ask before starting.
11. **Design is requested, not improvised.** When a phase needs it, ask for it from Claude Design.

## Proposal template

Before touching anything, present:

| Field | Content |
| --- | --- |
| Phase | Number and title |
| Decisions and design needed | Which decisions and which designs are missing |
| Tasks (in order) | Numbered list; each with its **validation** (tests / screenshots / emulator / human) |
| Execution | Model, agents, and which tasks are parallel or serial, and why |
| Commits | One per task or one closing commit |
| Risks | What can go wrong and how it is detected |
| Output | What can be tried when it is done |
