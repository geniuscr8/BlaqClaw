# Fixed Linux terminal

BlackClaw includes a small local terminal without a package manager. It uses an
Alpine 3.21 root filesystem with `bash`, Python 3, Git, curl, jq, and development utilities.
It works without root, Shizuku, or ADB; it is not a bridge to the Android shell.

The root filesystem is installed once from the app assets into the app's private space. The
agent always uses that unprivileged session, with the virtual user
`blackclaw` (uid 1000), and cannot select privileged backends, ADB, or
remote devices. The manual console keeps its separate Pro option.

## Empaquetado y reproducibilidad

`tools/build_terminal_bootstrap.py` rebuilds the fixed root filesystems from
Alpine `v3.21`, minirootfs `3.21.7`, and the `main` APK indexes. It leaves no `apk`,
repositories, or package metadata inside the final image.

| Artefacto | SHA-256 |
| --- | --- |
| `assets/terminal/aarch64/rootfs.tar.gz` | `e742b3c02bdbc2a1c21a7602b50b6990c612d9a6b7a3d53ea1fcc13a91cfbae6` |
| `assets/terminal/x86_64/rootfs.tar.gz` | `4f9df468247c0e670fabf6c3528f9e8d5b8d2aeeeb44ea020d4da15c2527f090` |
| `jniLibs/arm64-v8a/libblackclaw_proot.so` | `297abc237247682a84a3fd4283b28f69506502b4b852faf71fd726fb5d955d60` |
| `jniLibs/x86_64/libblackclaw_proot.so` | `46ca97b11d67f63b7dfe49f5ba50da89ab69b255f00a911c36499377a58f40b0` |

PRoot is packaged as a JNI library so Android extracts it into the executable
native-library directory. This is required on Android 10+:
executables written into an app's data directory cannot be run when the target
API is 29 or higher. The PRoot binaries come from
`green-green-avk/build-proot-android` (MIT), version
`0.15_release`; they are used with their separate loaders.

## Licencia PRoot wrapper

Copyright (c) 2023 green-green-avk

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
