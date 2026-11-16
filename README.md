# LEGEND FPS 2.2

Fabric client mod for Minecraft Java 1.21.11 (Java 21): LEGEND FPS menu, cosmetics, performance tools, friends, friend requests and world invites using bundled e4mc.

## Build
- GitHub Actions builds the Fabric JAR on pushes to `cosmetics-foundation`.
- Local build: `./gradlew --no-daemon clean build` with JDK 21.
- The build verifies compilation and packaging; it does not prove the mod works in a live Minecraft session.

## World invites
Read [WORLD_INVITES_AR.md](WORLD_INVITES_AR.md) and [the Relay deployment guide](legend-relay-server/README_AR.md). The relay must be deployed and configured by the server owner. No public relay is hosted by this repository.

## Verification status
- CI compilation: verified on the latest successful GitHub Actions run.
- e4mc address capture: mixin added to observe e4mc's local chat HUD message, including the copy-to-clipboard click event.
- Relay deployment: not performed; configure the service using `render.yaml`.
- Two-device / separate-network join: not yet tested in a live Minecraft session. Do not treat the feature as fully verified until this test passes.

Minecraft account authentication remains Minecraft's responsibility; LEGEND FPS does not bypass account authentication.
