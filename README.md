## Android · GitHub 自建更新通道

**[下载最新签名 APK](https://github.com/b1ghawk119/hapi/releases/latest/download/HAPI.apk)** · [发行记录](https://github.com/b1ghawk119/hapi/releases) · [自动构建](https://github.com/b1ghawk119/hapi/actions/workflows/android-channel.yml)

首次安装后，打开 App 即可配对自己的 Hub。新版本会在 App 处于前台时提示，点「下载并安装」，首次允许 HAPI 安装未知应用，再由 Android 确认升级。覆盖安装保留配对和应用数据。与 Play 版包名不同，可以并存；首次使用本通道需要单独配对。本构建未配置 Firebase，不提供 FCM 推送；官方通行密钥也不适用于此签名。

`android-channel` 是本 fork 的默认发布分支。Actions 每三小时检查 `tiann/hapi:main` 的 `android/` 和 `shared/fixtures/`，有变化才同步；手动运行同一工作流可立即检查。每次发布包含对应源码、签名 APK 和应用读取的 `update.json`。同步冲突时保留现有发行版：在发布分支手动合并对应上游改动、更新 `.github/channel/upstream` 至已整合的上游 Android 提交，再运行工作流。失败的构建会在下次检查时重试。GitHub 可能会在公共仓库连续 60 天没有活动后暂停定时工作流；可在 Actions 重新启用。请保留签名密钥备份，勿重置发布分支历史（版本号采用提交计数）。

---

# HAPI

Run official Claude Code / Codex / Cursor Agent / Grok Build / OpenCode / Kimi / Copilot / Antigravity / Pi / DeepSeek Harness sessions and control them remotely through native iOS / Android apps, Web / PWA, or Telegram Mini App.

> **Why HAPI?** HAPI is a local-first alternative to Happy. See [Why Not Happy?](docs/guide/why-hapi.md) for the key differences.

## Features

- **Seamless Handoff** - Work locally, switch to remote when needed, switch back anytime. No context loss, no session restart.
- **Shared Codex Sessions** - Use Codex from your terminal and phone at the same time. Requires Codex 0.154.0+. [Usage and limits](docs/guide/codex-shared-sessions.md).
- **Native First** - HAPI wraps your AI agent instead of replacing it. Same terminal, same experience, same muscle memory.
- **AFK Without Stopping** - Step away from your desk? Approve AI requests from your phone with one tap.
- **Your AI, Your Choice** - Claude Code, Codex, Cursor Agent, Grok Build, OpenCode, Kimi, Copilot, Antigravity, Pi, DeepSeek Harness—different agents, one unified workflow.
- **Terminal Anywhere** - Run commands from your phone's browser or desktop web app, directly connected to the working machine.
- **Voice Control** - Use dictation in native apps, or talk to your AI agent hands-free with the web voice assistant.
- **Workspace Browser** - Opt-in via one or more `hapi runner start --workspace-root <path>` flags: browse scoped file trees from the web and start sessions in allowed subdirectories.

## Demo

https://github.com/user-attachments/assets/38230353-94c6-4dbe-9c29-b2a2cc457546

## Getting Started

```bash
npx @twsxtd/hapi hub --relay     # start hub with E2E encrypted relay
npx @twsxtd/hapi                 # choose an agent and start a session
```

`hapi server` remains supported as an alias.

Use `hapi <agent> [options]` to start an agent directly, for example `hapi claude`
or `hapi codex`. Scripts must specify the agent explicitly. `hapi --help` shows
HAPI's commands and supported agents.

The hub displays a URL and two QR codes. Then connect a client:

- **Web / PWA (free):** open the web URL in a browser, or scan the web QR code. [Install it as a PWA](docs/guide/pwa.md) for a home-screen app and push notifications.
- **Native apps (paid):** install HAPI from the [App Store](https://apps.apple.com/app/id6805699104) or [Google Play](https://play.google.com/store/apps/details?id=run.hapi.companion), then scan the companion QR. See [Native apps](docs/guide/native-apps.md) for pairing.

> The relay uses WireGuard + TLS for end-to-end encryption. Your data is encrypted from your device to your machine.

For self-hosted options (Cloudflare Tunnel, Tailscale), see [Installation](docs/guide/installation.md)

## Docs

- [Native apps (iOS / Android)](docs/guide/native-apps.md)
- [Web / PWA](docs/guide/pwa.md)
- [How it Works](docs/guide/how-it-works.md)
- [Supported Agents](docs/guide/agents.md)
- [Voice Assistant](docs/guide/voice-assistant.md)
- [Why HAPI](docs/guide/why-hapi.md)
- [FAQ](docs/guide/faq.md)

## Clients

- **Web / PWA (free)** - The full-featured web app, including the remote terminal and voice assistant. Works in any browser and installs as a PWA. See [Web / PWA](docs/guide/pwa.md).
- **Native iOS / Android apps (paid)** - SwiftUI/UIKit and Kotlin Compose clients with chat, approvals, session creation, files, dictation, and native push notifications. See the [native app guide](docs/guide/native-apps.md) for capabilities and pairing.

<a href="https://apps.apple.com/app/id6805699104"><img src="https://developer.apple.com/assets/elements/badges/download-on-the-app-store.svg" alt="Download on the App Store" height="48"></a>
<a href="https://play.google.com/store/apps/details?id=run.hapi.companion"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="72"></a>

Developers can build the apps from source: [iOS](ios/README.md), [Android](android/README.md). Protocol: [client contract](docs/api/client-contract/index.md).

## Build from source

Requires Bun 1.4.4.

```bash
bun install
bun run build:single-exe
```

## Credits

HAPI means "哈皮" a Chinese transliteration of [Happy](https://github.com/slopus/happy). Great credit to the original project.
