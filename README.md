# spoofified

> A Minecraft Fabric mod that completely spoofs your client identity and mod list to hide your presence from multiplayer servers.

## Overview

**spoofified** is a fork of [ClientSpoofer](https://github.com/FabiPunktExe/clientspoofer) by FabiPunktExe—a client-sided mod designed to achieve 100% client spoofing. It protects your privacy by intercepting and modifying network packets, preventing servers from detecting your client type, installed mods, resource packs, and other identifying information.

## Features

### 1. **Client Brand Spoofing**
Disguises your Minecraft client by modifying the client brand identifier sent to servers.

**Spoof Modes:**
- **VANILLA** - Reports as vanilla Minecraft client
- **MODDED** - Reports as Fabric (with selective mod hiding)
- **CUSTOM** - Reports as a custom client name of your choice
- **OFF** - Disables all spoofing

**Example Use Case:**
> Connect to anti-cheat servers that detect modded clients. A server checking your client brand will see "vanilla" instead of your actual client, preventing automatic kicks.

### 2. **Custom Payload Packet Filtering**
Blocks custom payload packets sent to servers that may contain mod identification data.

**How it works:**
- Intercepts all `ServerboundCustomPayloadPacket` before sending to server
- In VANILLA mode: Blocks all custom payloads (except brand payload)
- In MODDED mode: Allows whitelisted mods while blocking others
- In CUSTOM mode: Blocks payloads unless explicitly allowed
- Configurable allowed channels via JSON config

**Example Use Case:**
> Fabric mods often send custom packets that identify themselves (e.g., "fabric:register", "modname:data"). These packets are silently dropped, making mod detection impossible for servers scanning network traffic.

### 3. **Translation Exploit Protection**
Fixes an advanced server exploit where servers inject custom text into signs and anvil screens containing mod names encoded in special formatting.

**How it works:**
- Intercepts sign and anvil UI component rendering
- Uses sophisticated language parsing to detect when servers attempt to inject translatable text
- Converts malicious formatted text to plaintext, stripping out server-injected mod identification
- Shows a toast notification when a server attempts this exploit

**Example Use Case:**
> A server like HugoSMP could place a sign with hidden text that reads `{"translate":"modname.loaded"}`. When you look at the sign, it would reveal your installed mods. This feature strips that information before it reaches your client.

### 4. **Known Packs Manager Spoofing**
Hides Fabric-related resource packs and mod packs from server detection.

**How it works:**
- Intercepts the `KnownPacksManager.trySelectingPacks()` method
- Prevents servers from identifying Fabric packs unless whitelisted
- Maintains functionality for whitelisted mods and vanilla packs

**Example Use Case:**
> Servers can query which packs your client recognizes. By hiding Fabric pack identifiers, the server can't determine if you're running a modded client vs vanilla.

### 5. **Fingerprinting Prevention**
Advanced protection against creative server attempts to identify your client through multiple detection vectors simultaneously.

**Example Use Case:**
> Anti-cheat systems might try multiple detection methods: checking brand, scanning packets, reading pack IDs, and analyzing UI components. Fingerprinting prevention ensures all vectors are blocked consistently.

## Configuration

The mod is configured via `config/clientspoofer.json`:

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

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `spoof-mode` | string | `vanilla` | Spoofing mode: `vanilla`, `modded`, `custom`, or `off` |
| `custom-client` | string | `fabric` | Custom client name to report in CUSTOM mode |
| `hide-mods` | boolean | `true` | Whether to hide mods in CUSTOM mode |
| `disable-custom-payloads` | boolean | `true` | Block custom payload packets in CUSTOM mode |
| `prevent-fingerprinting` | boolean | `true` | Enable advanced fingerprinting protection |
| `allowed-mods` | array | `[]` | List of mod names to allow (in MODDED mode) |
| `allowed-custom-payload-channels` | array | `[]` | Custom payload channels to allow (in CUSTOM mode) |

## Tested Servers

- **Cytooxien** (`cytooxien.de`)
- **HugoSMP** (`hugosmp.net`)
- **DonutSMP** (`donutsmp.net`)

> These servers are known to use client detection. spoofified successfully prevents detection on them.

## Installation

### Requirements
- **Minecraft Version**: 1.21+ (Java 21+)
- **Modloader**: Fabric
- **Optional**: Mod Menu (for GUI configuration)

### Steps
1. Download the latest JAR from [Releases](https://github.com/igameshat/spoofified/releases)
2. Place in your `.minecraft/mods` folder
3. Launch Minecraft with Fabric
4. (Optional) Configure via `config/clientspoofer.json` or use Mod Menu GUI

## Technical Implementation

The mod uses **Mixin** bytecode manipulation to intercept Minecraft internals:

| Mixin Target | Purpose |
|---|---|
| `ClientBrandRetrieverMixin` | Spoofs `getClientModName()` return value |
| `ConnectionMixin` | Filters outgoing custom payload packets |
| `AbstractSignEditScreenMixin` | Strips server-injected mod identifiers from signs |
| `AnvilScreenMixin` | Strips server-injected mod identifiers from anvils |
| `KnownPacksManagerMixin` | Hides Fabric pack identifiers |
| `DownloadQueueMixin` | Prevents pack download detection |

## Advanced Scenarios

### Scenario 1: Pure Vanilla Appearance
```json
{
  "spoof-mode": "vanilla",
  "hide-mods": true,
  "disable-custom-payloads": true,
  "prevent-fingerprinting": true
}
```
Perfect for servers with strict anti-cheat that ban any non-vanilla client.

### Scenario 2: Selective Mod Compatibility
```json
{
  "spoof-mode": "modded",
  "allowed-mods": ["optifine", "sodium", "iris"],
  "prevent-fingerprinting": true
}
```
Hide problematic mods while allowing performance/compatibility mods that servers don't care about.

### Scenario 3: Custom Client Branding
```json
{
  "spoof-mode": "custom",
  "custom-client": "lunar",
  "hide-mods": true,
  "allowed-custom-payload-channels": ["lunar:receive"]
}
```
Spoof as a specific client while maintaining limited functionality through whitelisted channels.

## License

MIT License © 2026

## Disclaimer

This mod is provided for privacy protection purposes. Use responsibly:
- Some servers may prohibit mod usage—check their Terms of Service
- This mod does NOT provide cheating capabilities; it only hides client information
- Server administrators may implement additional detection methods not covered by this mod
- Use at your own discretion and assume responsibility for any consequences

## Credits

- **Original Author**: [FabiPunktExe](https://github.com/FabiPunktExe/clientspoofer)
- **Forked by**: [igameshat](https://github.com/igameshat)
- **Modding Framework**: Fabric, Mixin

---

**Repository**: https://github.com/igameshat/spoofified
