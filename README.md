# AuTedium – Grindfulness

fabric mod for minecraft 26.2. makes grinding suck less without making the game easier. i have the attention span of a baby zombie so i built this.

the one rule: **less tedium, never less cost.** full durability, full hunger, vanilla loot rates. the mod only deletes the boring parts — the clicking, the searching, the waiting, the forgetting where you died.

## what's in it

everything is a toggle. mining stuff:

- **vein miner** — sneak-break one ore/log, the connected vein pops. full cost per block, tool breaks mid-vein = chain stops.
- **flow state** — keep breaking the same block family, stack haste I→III. stop or switch and it fades.
- **pity ping** — 800 deep blocks with no diamond? nearest *existing* vein glows through walls for a bit. reveal only, never spawns ore.
- **prospector** — sneak + right-click with a pick, nearby ores glow. costs durability, has a cooldown.
- **torch cadence** — sneak with torches in hand and dark spots get lit as you walk. every torch paid from your stack.

around the base:

- **vigil** — skyrim wait button. press V, pick how many hours, kneel. crops grow, furnaces run, copper ages — and your hunger drains to match, daytime only, and a zombie will absolutely end you mid-wait. food for time.
- **brew queue** — click a busy brewing stand with more ingredients, they queue and auto-load. same ingredients, same blaze powder, same time.
- **encore** — animal on breeding cooldown? feed it double to skip the wait. costs triple the items per baby vs waiting.
- **auto graze** — designated hotbar slot, plain food gets eaten when you're hungry. hard locked in combat, effect foods stay manual.

and the brain stuff:

- **grind ledger** — action bar stats: milestone flashes while mining, session summary when you take a breather.
- **last breath** — died? coords + distance + a glow column at your death spot, despawn countdown included. pure info, corpse run is still yours to survive.
- **pin book** — `/pin add mine` and the action bar points you there. no teleports, just memory.
- **toasts** — milestone popups. zero balance impact, pure dopamine.

## fairness meter

the config screen (mod menu, YACL) judges your combo live as you flip toggles — from "vanilla purist." to "wow kinda unfair!". heavier features (vigil, encore, auto graze) push the meter harder. the mod is honest about how much you're cheating.

## running it

- minecraft 26.2, fabric loader 0.19.3+, fabric api
- YACL comes bundled, mod menu optional

build: `JAVA_HOME=<jdk 25+> ./gradlew build`, jar lands in `build/libs/`. tunables live in `config/autedium_grindfulness.json` or the config screen sliders.

MIT
