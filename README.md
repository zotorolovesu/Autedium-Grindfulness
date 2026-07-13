# AuTedium – Grindfulness

Anti-tedium QoL for people whose attention span dies mid-grind. Vein mining, flow haste, ore pings, pity streaks — every feature toggleable, and the mod rates its own fairness ("balanced." → "wow kinda unfair!").

**AuTedium – Grindfulness** is a Fabric mod for Minecraft 26.2 that makes grinding less boring without making the game easier. Built for ADHD brains: the tedium goes, the cost stays.

One rule governs every feature: **reduce tedium, never expected cost.** You pay full durability, full hunger, and get vanilla loot rates — the mod just deletes the boring parts: repetitive clicking, aimless searching, brutal dry streaks, invisible progress.

## Features (all individually toggleable)

- ⛏️ **Vein Miner** — sneak-break one ore or log and the whole connected vein/tree pops. Full vanilla cost per block; tool breaks mid-vein, chain stops.
- ⚡ **Flow State** — keep breaking the same block family and stack Haste I→III. Stop or switch, it fades. Rewards staying locked in.
- 💎 **Pity Ping** — 800 deep blocks without a diamond? The nearest *existing* vein glows through walls for 10s. Never spawns ore — reveal only.
- 🔍 **Prospector Ping** — sneak + right-click with a pickaxe: nearby ores glow for 5s. 45s cooldown.
- 🍞 **Dopamine Toasts** — milestone popups and dry-streak progress. Pure feedback, zero balance impact.

## The Fairness Meter

The config screen (YACL, opens from Mod Menu or `/grindfulness`) judges your active combo live as you toggle — from *"vanilla purist."* through *"balanced."* and *"pushing it."* to *"wow kinda unfair!"* The mod is honest about how much you're cheating.

## Requirements

- Minecraft 26.2 (Java 25)
- Fabric Loader 0.19.3+
- Fabric API
- YACL is bundled (jar-in-jar); Mod Menu optional but recommended

## Building

```
JAVA_HOME=<jdk 25+> ./gradlew build
```

Jar lands in `build/libs/`. Tunables (vein caps, pity threshold, cooldowns) live in `config/autedium_grindfulness.json`.

## License

MIT
