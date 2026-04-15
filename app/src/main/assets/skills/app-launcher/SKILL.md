---
name: app-launcher
description: Open apps installed on the phone by package name.
---

# App Launcher

## Examples

- "Open YouTube"
- "Launch Chrome"
- "Open Spotify"
- "Start WhatsApp"

## Instructions

Call the `run_intent` tool with the following exact parameters:

- intent: open_app
- parameters: A JSON string with the following field:
  - package_name: The Android package name of the app. String.

Common package names:
- YouTube: com.google.android.youtube
- Chrome: com.android.chrome
- WhatsApp: com.whatsapp
- Spotify: com.spotify.music
- Gmail: com.google.android.gm
- Google Maps: com.google.android.apps.maps
- Instagram: com.instagram.android
- Twitter/X: com.twitter.android
- Facebook: com.facebook.katana
- Camera: com.android.camera2
- Settings: com.android.settings
- Calculator: com.google.android.calculator
- Clock: com.google.android.deskclock
- Photos: com.google.android.apps.photos

If the user asks to open an app not in this list, make a reasonable guess at the package name (usually com.[company].[appname]).
