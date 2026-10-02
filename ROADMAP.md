# Roadmap

**1.1.0 on two Minecraft versions, and a 26.1 line pulling ahead.** 1.21.1 is the maintained line.
26.1 carries 1.1.0 plus what is under *Unreleased* in the [changelog](CHANGELOG.md): four economy
bugs fixed, upgrades that keep their augments, a new screen on FactoryIO's layout, new item art, and
a beacon that waits when it runs dry instead of switching off. Those have not reached 1.21.1 yet.

What follows is ordered by what actually moves the mod forward — which is no longer "what is
missing" but "what do players hit first". The pre-release checklist that used to live here is done;
its history is in the git log and the [changelog](CHANGELOG.md).

---

## Now

**Upload 1.0.1** to CurseForge as release type *Release*, for game version 1.21.1. The project is
live at <https://www.curseforge.com/minecraft/mc-mods/portables-beacons>; the changelog section and
the listing copy are ready in [CHANGELOG.md](CHANGELOG.md) and [STORE.md](STORE.md).

**Say the id change out loud on the page.** Anyone who played 1.0.0 or the 0.9.0 beta loses their
saved beacons — the item ids, the data component and the item tag all moved. That is worth one line
at the top of the description, not a footnote.

**Play the 26.1 build.** It compiles and passes 43 unit tests and 15 gametests, and
`BEACON_GUI_PREVIEW=1 ./gradlew runClient` captures the screen in its states - but the screen, the
item art and the tooltips are exactly what no test judges. It should not be published on a green
build alone.

**Bring the Unreleased work back to 1.21.1**, or decide that 26.1 is where it lives. Note that
1.21.1's `copyDevMods` is still a Copy: it leaves 26.1's jars in the shared `run/mods`, the same way
26.1 used to keep 1.21.1's and crash on EMI.

## Then — the first week

**Watch the issue tracker.** This is the whole point of shipping. Until reports come in, every
priority below is a guess, and a guess should lose to a real report every time.

**Screenshots.** Still the single biggest thing standing between the page and a download. Three
carry it: the beacon screen with a drawer open, the effect picker mid-search, and the JEI fuel
category. A short GIF of a drawer opening would do more than any paragraph on the page.

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
