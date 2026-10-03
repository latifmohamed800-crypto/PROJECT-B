# PROJECT-B — Offline Audiobook

Current Android target: **Offline Audiobook 0.4.0**, built from `projects/latif-ai-os-audiobook/preserved-offline-android`.

Bundled Nabra 82M Arabic INT8 voice with Sherpa-ONNX. SHA-256 render cache identifiers, atomic WAV assembly, resumable segments, bounded UTF-8 manuscript import and native engine shutdown after active synthesis finishes. ARM64 phones, Android 8 or newer.

Use `.github/workflows/original-source-apk.yml` to validate and build this target. Pull requests validate source and package an Actions artifact; manual builds from a feature branch publish a prerelease for review. Builds on the project branch publish the current stable version. Each app version has one APK release rather than a new release name for every workflow run. Releases include `SHA256SUMS.txt` and `BUILD_INFO.json` with the exact source commit.

The existing source collections remain preserved in this repository. They are not additional current APK targets.

These APKs use development debug signing. Physical-device operation, narration quality, and speed still need device testing; a stable private release-signing key is needed for production updates. Preserve app data before changing signing identities.
