# BlaqClaw privacy overview

BlaqClaw is designed as a local-first Android assistant. The app does not have a
central BlaqClaw account or a mandatory BlaqClaw cloud. What leaves the device
depends on the provider, channel, and feature the user enables.

## Local data

Settings, memories, task history, automation drafts, credentials, and debug logs
are stored in app-private storage. Credentials and sensitive structured data use
Android Keystore-backed encryption where supported. The in-app encrypted backup
uses a passphrase chosen by the user; BlaqClaw does not receive that passphrase.

## Optional network data

When a cloud model or remote channel is configured, prompts and the context
needed to answer the request may be sent to that provider. Web search, weather,
maps, update checks, Telegram, Discord, WeChat, SSH, and smart-home webhooks
also contact their respective services when explicitly used or enabled.

Review the provider's privacy terms before sending screen content, messages,
contacts, location, or other sensitive information to it.

## Android permissions

Accessibility, notification access, overlay, microphone, screen capture,
location, camera, SMS, contacts, calendar, storage, Wireless Debugging, and
Shizuku are optional capabilities. Grant only the permissions required for the
features you use. Android privacy indicators and system confirmation dialogs
remain authoritative for microphone, camera, and screen capture use.

## Retention and deletion

The Memory & Privacy screen can inspect and delete stored memories and related
local context. Clearing app data removes app-private data. Remote providers and
channels may retain data under their own policies; delete it there as well.

## Security reporting

Do not include API keys, backup passphrases, contacts, private messages, or
screen captures in public issues. Report suspected security vulnerabilities
privately through the repository's configured security contact before public
disclosure.
