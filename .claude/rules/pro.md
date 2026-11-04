---
paths:
  - "app/src/main/java/com/alorma/contactnotes/ui/**"
  - "app/src/main/java/com/alorma/contactnotes/data/pro/**"
  - "docs/pro/**"
---

# Pro: what can be Pro and how a Pro feature behaves

Full picture in `docs/pro/README.md`. This rule is the checklist when a feature is new, changes, or might be Pro.

## Free or Pro

- **Never charge to access, protect or take out your own data:** people, entries, protected people, app lock, export and import stay free. Pro is what makes the app more comfortable or more powerful.
- Whether a new feature is free or Pro is Bernat's decision: ask, never assume.

## How a Pro feature behaves

- The UI reads `rememberIsPro()` (`ProRepository.isPro` is the only source); no feature flags for finished Pro features.
- Without Pro it is visible with the PRO badge and its entry point opens the paywall with its `ProBenefit` (`PaywallRoute(highlight)`). The paywall never opens on its own.
- A new benefit needs a new `ProBenefit`, its row in the paywall and its texts in en/ca/es.
- **Downgrade** (refund or revocation): what was made with Pro is kept and still shown; it just cannot be changed or created again. Never delete data.
- Protected people: a Pro feature must not show their data before unlocking (same as any feature).

## In the same task

Update `docs/pro/README.md` (the Pro table) and say whether the change deserves a release notes bullet (`release-notes.md`).
