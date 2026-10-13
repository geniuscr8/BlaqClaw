# Production release checklist

## Before tagging

- Increase `versionCode` and set the intended `versionName` in `app/build.gradle.kts`.
- Add a matching `### vX.Y.Z` entry to `README.md`.
- Review permissions, privacy text, remote providers, and release notes.
- Confirm no API keys, keystores, `local.properties`, generated APKs, or model
  credentials are tracked by Git.
- Run `./gradlew testDebugUnitTest lintDebug assembleDebug` locally when JDK 17
  and the Android SDK are available.

## Required GitHub configuration

Configure these repository or environment secrets before publishing:

- `ANDROID_KEYSTORE_B64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The release workflow fails closed when any signing secret is missing. Keep the
keystore backed up offline and restrict who can create release tags.

## Release process

```bash
git tag -a vX.Y.Z -m "vX.Y.Z"
git push origin vX.Y.Z
```

The tag workflow builds and tests the signed release APK and AAB, runs release
lint, verifies APK signatures, publishes SHA-256 checksums, and uploads the
obfuscation mapping file needed for crash diagnosis.

The `universal-latest` workflow is for a stable direct-download APK. It also
requires the signing secrets and replaces the previous universal asset.

## After publishing

- Verify the GitHub release asset signatures and checksums.
- Install over the previous production build on at least one device to confirm
  in-place upgrade compatibility.
- Test onboarding, accessibility, notification access, voice, local/cloud model
  switching, backup restore, automations, and update links.
- Review CodeQL, unit-test, lint, emulator, and Firebase Test Lab results.
- Monitor crash and ANR dashboards for the first staged rollout window.
- If a rollback is necessary, publish a higher `versionCode` using the same key;
  Android does not support downgrading an installed package.
