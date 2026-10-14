# Releasing NodeBox

Installers are built in GitHub Actions by [`.github/workflows/release.yml`](../.github/workflows/release.yml),
which mirrors the approach used by the [figment](https://github.com/figmentapp/figment) project:

| Trigger | Result |
| --- | --- |
| Push to `master` | macOS `.dmg` + Windows `.msi` uploaded as **workflow artifacts** (nightly) |
| Tag `v*` (e.g. `v3.0.53`) | Same installers attached to a **GitHub Release** |
| Manual `workflow_dispatch` | Build on demand |

Both runners use the **same Temurin JDK** (`JAVA_VERSION` in the workflow). `jpackage`
bundles a runtime matching the runner architecture — so `macos-latest` yields a **native
arm64** app (no Rosetta) and `windows-latest` yields an x64 `.msi`.

## Cutting a release

1. Bump `nodebox.version` in `src/main/resources/version.properties`.
2. Add an item for the new version at the top of [`appcast/public/app/nodebox/appcast.xml`](../appcast/README.md),
   with the release notes. Running copies of NodeBox read it to offer the update.
3. Commit, then tag and push:
   ```sh
   git tag v3.0.53
   git push origin v3.0.53
   ```
4. The workflow signs + notarizes the macOS build and creates the GitHub Release.

## Required repository secrets (macOS signing & notarization)

Set these under **Settings → Secrets and variables → Actions**. Windows is currently
shipped unsigned, so no Windows secrets are needed. If the macOS secrets are absent the
build still succeeds but produces an **unsigned** `NodeBox-unsigned.zip` instead of a `.dmg`.

| Secret | What it is |
| --- | --- |
| `MACOS_CERTIFICATE` | Base64 of your *Developer ID Application* certificate exported as `.p12` |
| `MACOS_CERTIFICATE_PWD` | The password set when exporting the `.p12` |
| `KEYCHAIN_PASSWORD` | Any throwaway string; used to create a temporary keychain on the runner |
| `MACOS_SIGN_IDENTITY` | Identity name, e.g. `Developer ID Application: Frederik De Bleser (5X78EYG9RH)` |
| `APPLE_ID` | Apple ID email used for notarization |
| `APPLE_APP_SPECIFIC_PASSWORD` | App-specific password generated at <https://appleid.apple.com> |
| `APPLE_TEAM_ID` | Developer Team ID, e.g. `5X78EYG9RH` |

### Exporting the certificate

In **Keychain Access**, find *Developer ID Application: …*, right-click → **Export** as
`.p12` (set a password — that's `MACOS_CERTIFICATE_PWD`). Then base64-encode it for the
`MACOS_CERTIFICATE` secret:

```sh
base64 -i Certificates.p12 | pbcopy
```

## Building locally

The same Ant targets run locally (signing uses your login keychain and the env vars above):

```sh
ant dist-mac sign-mac   # macOS: app image -> sign -> dmg -> notarize -> staple
ant dist-win            # Windows: .msi (requires WiX Toolset v3 on PATH)
```

Override the JDK used for packaging with `-Djpackage=/path/to/jdk/bin/jpackage`; by default
it uses the JDK running Ant (`${java.home}/bin/jpackage`).

## Bundled ffmpeg

`platform/mac/bin/ffmpeg` is a **static arm64** build (ffmpeg 8.1 from
[osxexperts.net](https://www.osxexperts.net), the Apple Silicon counterpart to the
evermeet.cx Intel builds). It links only system frameworks, so it bundles without extra
dylibs. To update it, download the latest `ffmpegNNarm.zip`, confirm it's self-contained
(`otool -L ffmpeg` shows only `/usr/lib` and `/System`), and replace the file. It must
keep the libx264 (h264/mp4) and libvpx (webm) encoders that NodeBox invokes.

The Windows (`platform/windows/bin/ffmpeg.exe`) and Linux binaries are still x86_64.
