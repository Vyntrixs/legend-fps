# LEGEND FPS 2.2

Fabric client mod for Minecraft Java 1.21.11 (Java 21): LEGEND FPS menu, cosmetics, performance tools, friends, friend requests and world invites using bundled e4mc.

## Build
- GitHub Actions builds the Fabric JAR on pushes to `cosmetics-foundation`.
- Local build: `./gradlew --no-daemon clean build` with JDK 21.
- The build verifies compilation and packaging; it does not prove the mod works in a live Minecraft session.

## World invites
Read [WORLD_INVITES_AR.md](WORLD_INVITES_AR.md) and [the Arabic Relay deployment guide](legend-relay-server/README_AR.md).

### Deploy the friends/invites relay
[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/Vyntrixs/legend-fps)

The button opens Render's Blueprint deployment flow for `render.yaml`. A repository owner must sign in to Render and approve creation of the service; this repository cannot deploy into somebody else's hosting account. After deployment, copy the generated `LEGEND_RELAY_TOKEN` from the Render service environment and configure the same WebSocket URL and token in LEGEND FPS on every player's client. Full steps are in the Arabic guide.

The relay is only for friend lists and invitation messages. It does not host Minecraft worlds. e4mc exposes the host's own single-player world temporarily.

## Verification status
- CI compilation: verified on the latest successful GitHub Actions run.
- e4mc address capture: mixin observes e4mc's local chat HUD message, including the copy-to-clipboard click event.
- Relay deployment: requires the repository owner to approve the Render Blueprint deployment.
- Two-device / separate-network join: not yet tested in a live Minecraft session. Compilation is not proof of successful live multiplayer.

Minecraft account authentication remains Minecraft's responsibility; LEGEND FPS does not bypass account authentication.
