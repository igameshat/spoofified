# Spoofified

> A Minecraft Fabric mod that completely spoofs your client identity and mod list to hide your presence from multiplayer servers.

## What's This About?

Ever joined a server and instantly got kicked because you had mods installed? Or you're worried some server is logging what mods you run? Yeah, that sucks. 

**Spoofified** lets you play on servers however you want without them knowing what you're actually running. Want to hide all your mods? Done. Want to show a few but hide the problematic ones? You can do that too. Make your client pretend to be vanilla, Fabric, Lunar, whatever—it's all configurable.

The best part? It's all client-side. No server patches needed, no sketchy workarounds. Just pure client spoofing.

## What Can You Actually Do?

### Hide Your Mods
Servers can't see what you're running. Full stop. When they ask, your client tells them nothing (or only what you want them to know).

### Spoof Your Client Brand
Make Minecraft think you're running vanilla, Fabric, or literally any other client. Some servers check this first—now they'll see whatever you tell them to.

### Block Mod Identification Packets
Mods send packets to servers to announce themselves. This blocks all that noise before it even leaves your computer. Servers won't know you're running anything at all.

### Stop Servers From Fingerprinting You
Advanced servers try multiple detection methods at once: checking your brand, scanning packets, looking at pack IDs, analyzing UI elements. This mod stops all of it.

There's this nasty exploit where servers inject hidden text into signs and anvils to trick you into revealing your mods. Spoofified strips that out and shows you a notification when a server tries it.

### Keep Your Keybinds Hidden
If you hide a mod, its keybinds get hidden too. No accidental reveals in the controls menu.

## Getting Started

### Requirements
- Minecraft 1.21+
- Java 21+
- Fabric loader
- (Optional) Mod Menu for the GUI config

### Installation
1. Grab the latest JAR from [Releases](https://github.com/igameshat/spoofified/releases)
2. Drop it in `.minecraft/mods`
3. Launch the game
4. Configure it

## Configuration

### The Easy Way
If you have Mod Menu installed, just open it, find Client Spoofer, hit the config button, and adjust whatever you want. It saves automatically.

### The Manual Way
Edit `config/clientspoofer.json`:

```json
{
  "spoof-mode": "vanilla",
  "custom-client": "fabric",
  "hide-mods": true,
  "disable-custom-payloads": true,
  "prevent-fingerprinting": true,
  "allowed-mods": [],
  "allowed-custom-payload-channels": []
}
```

**spoof-mode** - How you want to present yourself:
- `vanilla` - Look like plain Minecraft
- `modded` - Say you're on Fabric but hide certain mods
- `custom` - Pretend to be any client you want
- `off` - Don't spoof anything

**custom-client** - What name to use in CUSTOM mode (e.g., "lunar", "badlion")

**hide-mods** - Whether to hide your mod list

**disable-custom-payloads** - Block those mod announcement packets

**prevent-fingerprinting** - Protect against all the weird detection tricks

**allowed-mods** - In MODDED mode, list which mods you want to allow (e.g., `["sodium", "iris"]`)

**allowed-custom-payload-channels** - In CUSTOM mode, which packet channels are allowed through

## Real-World Scenarios

### "I Just Want to Look Vanilla"
```json
{
  "spoof-mode": "vanilla",
  "hide-mods": true,
  "disable-custom-payloads": true,
  "prevent-fingerprinting": true
}
```
Use this on servers with strict anti-cheat. You'll look 100% vanilla.

### "I Want My Performance Mods But Hide Everything Else"
```json
{
  "spoof-mode": "modded",
  "allowed-mods": ["sodium", "iris", "optifine"],
  "prevent-fingerprinting": true
}
```
Servers see you're on Fabric and that you have a few safe mods, but nothing else.

### "I Want to Look Like I'm Using Lunar"
```json
{
  "spoof-mode": "custom",
  "custom-client": "lunar",
  "hide-mods": true,
  "allowed-custom-payload-channels": ["lunar:receive"]
}
```
Now you're a "lunar client" as far as the server cares.

## Tested On
- Cytooxien
- HugoSMP  
- DonutSMP

These servers actually use detection. Spoofified works on all of them.

## How It Actually Works

The mod uses Mixin to hook into Minecraft's internals and intercept the stuff servers use to detect you:

- **ClientBrandRetrieverMixin** - Changes what your client brand says
- **ConnectionMixin** - Filters outgoing packets before they're sent
- **AbstractSignEditScreenMixin** & **AnvilScreenMixin** - Strips malicious text servers try to inject
- **KnownPacksManagerMixin** - Hides Fabric pack signatures
- **MixinModMain** - Hides mods from Mod Menu
- **MixinKeyBindsList** - Removes keybinds from hidden mods

Everything happens client-side. No external tools. No sketchy stuff.

## Important: Read This

This mod is for **privacy** and **avoiding detection**. Use it responsibly:

**Don't use it to:**
- Break server rules (check their ToS)
- Cheat or gain unfair advantages
- Mess with servers' security systems

**Safe to use for:**
- Protecting your privacy on community/friend servers
- Using performance mods on servers that don't explicitly ban them
- Keeping your setup private

**Real talk:** This mod only hides client info. Servers might have other detection methods we don't know about. They could also add more detection later. Use it knowing that, and don't cry if something breaks.

## License

MIT License © 2026

## Credits

- Original mod by [FabiPunktExe](https://github.com/FabiPunktExe/clientspoofer)
- Forked and maintained by [igameshat](https://github.com/igameshat)

---

Got questions? Issues? Open a GitHub issue and let me know what's up.
