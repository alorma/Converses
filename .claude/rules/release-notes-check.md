# Release notes check at the end of every task

No `paths` on purpose: this rule applies to every task, not only when editing the notes.

At the end of every task or feature, say whether it deserves a bullet in the Google Play release notes (`app/src/main/play/release-notes/`) and why.

- **Yes** only if the user can see or measure it. **No** for refactors, docs, CI, dependency bumps, internal fixes, and anything behind a flag (Pro, `PersonColorFeature`, …).
- Propose it and ask. Do not edit the notes unless the user agrees.
- The notes are rewritten for every release, so a bullet agreed now can wait until the next release is prepared.
- Tone and format: `.claude/rules/release-notes.md`.
