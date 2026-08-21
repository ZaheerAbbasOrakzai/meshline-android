# Meshline 📡

**Offline-First End-to-End Encrypted Bluetooth Mesh Messenger & Walkie-Talkie for Android**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Encryption](https://img.shields.io/badge/Encryption-ECDH%20%2B%20AES--256--GCM-red.svg)](https://en.wikipedia.org/wiki/Authenticated_encryption)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📌 Repository Overview

* **Repository Name:** `meshline-android`
* **Short Description:** An offline-first, zero-infrastructure Bluetooth Mesh messenger with E2E encryption (ECDH + AES-256-GCM), Push-To-Talk (PTT) Walkie-Talkie, and live network topology mapping.
* **Topics / Tags:** `android`, `kotlin`, `jetpack-compose`, `bluetooth-mesh`, `ble`, `e2ee`, `cryptography`, `walkie-talkie`, `ptt`, `room-database`, `material3`, `offline-first`, `mesh-networking`

---

## ✨ Key Features

### 🔒 1. Cryptographic Security & Privacy
* **Elliptic-Curve Diffie-Hellman (ECDH)** key agreement paired with **AES-256-GCM** authenticated encryption.
* **SAS Verification**: Compare Short Authentication Strings (Numeric Fingerprint & QR Matrix) out-of-band to prevent MITM attacks.
* **Emergency Panic Wipe**: Zeroize and sanitize local databases instantly during emergencies.

### 🎙️ 2. PTT Walkie-Talkie & Rich Media
* **Hold-to-Talk Transceiver**: Animated pulse rings, live audio waveform canvas visualizer, and 12ms low-latency hop indicators.
* **Off-Grid Attachments**: Send encrypted voice notes and off-grid GPS location pins directly through nearby nodes.

### 💬 3. Next-Gen Messaging Suite
* **Telegram-Style "Saved Messages" Vault**: Securely store encrypted notes, keys, and bookmarks locally.
* **Off-Grid Broadcast Channels**: Create multi-member channels relayed across up to 25 mesh hop nodes.
* **Interactive Reactions & Pinning**: Tap to attach emojis (`👍`, `❤️`, `🔥`, `😂`, `😮`, `🚨`), pin priority messages, and configure disappearing timers (30s, 5m, 24h).
* **Smart Filtering**: Categorize channels by `All`, `Direct`, `Groups`, or `Saved`.

### 🌐 4. Network Operations & Topology
* **Live Node Map**: Visual radar overview showing direct (1-hop) and relayed (multi-hop) BLE peers.
* **Route Inspector**: Monitor hop latency, packet transit logs, and relay node stats.
* **Network Admin Access**: Built-in administrator authority controls for network management.

---

## 🛠️ Architecture & Tech Stack

* **Language:** 100% Kotlin
* **UI Framework:** Jetpack Compose with Material Design 3 (M3)
* **Architecture:** Clean Architecture + MVVM (`MainViewModel`, `MeshRepository`)
* **Database:** Room Database with KSP and custom type converters
* **Asynchronous Execution:** Kotlin Coroutines & `StateFlow` / `collectAsStateWithLifecycle`
* **Networking Protocol:** BLE (Bluetooth Low Energy) GATT & Advertising Multi-Hop Relay Engine

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio:** Ladybug (2024.2.1) or higher
* **JDK:** Version 17+
* **Min SDK:** 26 (Android 8.0)
* **Target SDK:** 34 (Android 14)

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/meshline-android.git
   cd meshline-android
   ```
2. Open the project in Android Studio.
3. Sync Gradle and build the APK:
   ```bash
   gradle assembleDebug
   ```

---

## 🛡️ License

This project is released under the [MIT License](LICENSE).
