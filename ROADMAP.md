# Roadmap

**2.0.0 on three Minecraft versions: 1.21.1, 26.1 and 26.2, with the same content.** Only the
plumbing differs where the game's API does. `main` follows the newest version, 26.2.

What follows is ordered by what actually moves the mod forward — which is no longer "what is
missing" but "what do players hit first". The pre-release checklist that used to live here is done;
its history is in the git log and the [changelog](CHANGELOG.md).

---

## Now

**Bump Curios on 26.2 once it stops using `logoFile`.** Curios 16.0.0+26.2 still does, and the 26.2
client opens on a "1 warning" screen for it - ours is fixed, theirs is not. Nothing to do but
change `curios_version` when a fixed 26.2 build appears.

**Release 2.0.0.** The jars, the description, the images and the per-file settings are listed in
[STORE.md](STORE.md)'s release checklist; the changelog section is `## 2.0.0`.

**Publish the rewritten wiki** with the release, not before: it describes 2.0.0.

**Decide on 1.20.1.** Feasible, at roughly twice the cost of the 1.21.1 backport - no data
components, an older capability and networking system, no GUI sprites - and most 1.20.1 packs run
Forge rather than NeoForge.

## Then — the first week

**Watch the issue tracker.** This is the whole point of shipping. Until reports come in, every
priority below is a guess, and a guess should lose to a real report every time.

**Tell people it exists.** r/feedthebeast and r/MinecraftMods on a weekend, the NeoForged Discord's
showcase channel. Modpack authors are the real growth channel — a small utility mod with a clean
GUI and datapack hooks is easy to include, and the permissions block already says yes for them.

## The 26.x line

**1.21.1 → 26.1 → 26.2 → 26.3.** One hop at a time, a published file for each, rather than one jump
to whatever is newest.

**26.1 is done.** Ten primers separated it from 1.21.1, and the full analysis of what broke and why
is in [PORTING.md](PORTING.md) — including where that analysis guessed wrong. What is left is play,
not compilation.

**26.2 branches from 26.1 once 26.1 has been played**, not before, or it is an empty copy that
drifts. EMI returns whenever a 26.x NeoForge build appears; the plugin is still in the tree.

`1.21.1` stays maintained throughout. Every extra branch multiplies each bug report by the number of
branches, and reports have only just begun.

## Still open

**One translation reviewed by a native speaker.** Fifteen locales ship and all fifteen are mine. The
first correction from a real player outranks anything I wrote.

**A balance pass on sharing.** 1.0.1 made Attunement matter by taking wide sharing away from every
tier that used to have it for free. That is the right shape, but the numbers have never been played
— only tested. Whether Beacon II spending its single augment slot on Attunement feels like a
decision or a tax is something only playing will say.

## Settled

**The rename.** Done fully: display name, mod id, Java package, every item id, the data component,
the item tag, the datapack paths, the repository and the CurseForge page. "BeaconPack" read as a
modpack, which is the one thing this is not.

I argued for changing only the display name and keeping the ids, because an id is baked into every
saved beacon and into any datapack written against it. That reasoning is right in general and was
wrong here twice over: the beta had been public a day, and 1.0.0 had not shipped at all. If the ids
ever have to move again, it belongs to a 2.0 with a migration, not to a patch.

**CurseForge only.** One page kept current beats two kept half-current, and the audience this mod is
aimed at is already there.

## Publishing mechanics

Manual uploads for now. Once CurseForge has accepted one by hand, `curseforge-gradle` can publish
from a local Gradle task — no CI needed, which is consistent with this project not having any.

Keep the changelog current as things land: the store page renders its release notes straight from it.
