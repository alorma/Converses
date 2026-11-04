# External privacy: the policy and Play must match what the app sends

No `paths` on purpose: data can start leaving the phone from a dependency, a manifest or a build file, not only from app code.

## When it applies

Any change to what leaves the phone:

- a new SDK or library that talks to a server, or a new permission (check the merged manifest: libraries add permissions);
- a new identifier, a new common tracking property, new event data or a new Firebase or RevenueCat feature;
- a change in who receives data, how long it is kept, or whether it is optional.

## What to do

1. Say it in the proposal, before writing code: what new data leaves the phone, to whom, and whether it is optional.
2. Review the privacy policy in the **ConversesLanding** repo (`privacy/`, `ca/privacitat/`, `es/privacidad/`): all three languages, same content, new «Last updated» date. Propose the text and ask before editing; push only with Bernat's approval.
3. Tell Bernat which answers of the **Play data safety form** change (data type, collected or shared, optional or required, purpose). Only Bernat edits Play Console.
4. Retention claims in the policy must match the Firebase and RevenueCat settings; if one changes, the other changes too.

Never: an advertising ID, data shared with third parties that are not service providers, or anything sent before the user opts in when the feature is opt-in.
