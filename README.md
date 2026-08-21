# Meshline 📡

**Professional Decentralized End-to-End Encrypted Bluetooth Mesh Messenger**

Meshline is a production-grade, offline-first communication platform that enables secure messaging and Walkie-Talkie functionality without any reliance on internet, cellular data, or central servers.

---

## 🚀 Key Features (Real & Authentic)

### 🔗 Automatic Mesh Discovery
* **Zero Configuration**: Simply enable Bluetooth, and Meshline automatically discovers all nearby nodes.
* **Proactive Visibility**: Users who have the app open and Bluetooth enabled are instantly visible to each other for pairing.
* **Instant Handshake**: Secure P-256 ECDH session keys are exchanged automatically upon request acceptance.

### 🔒 Military-Grade Security
* **E2EE Architecture**: Every message, voice note, and GPS coordinate is encrypted end-to-end using AES-256-GCM.
* **Passphrase-Derived Master Keys**: Uses PBKDF2 with 250,000 iterations to secure your local database and identity vault.
* **Panic Destruction**: A single button to permanently zeroize all cryptographic material and chat logs.

### 🎙️ Advanced Communication
* **Walkie-Talkie (PTT)**: Low-latency off-grid voice transmission with live waveform visualization.
* **Multi-Hop Mesh**: Support for up to 7 relay hops to extend range beyond point-to-point Bluetooth limits.
* **Interactive UI**: Telegram-inspired dark mode, message reactions, quote-replies, and double-check delivery indicators (✓/✓✓).

### 👤 Profile Management
* **Update Details**: Easily change your Display Name and Avatar directly from the Settings screen.
* **Identity Portability**: Export your unique cryptographic identity to move your node to a new device.

---

## 🛠️ Tech Stack
* **Language**: 100% Kotlin
* **UI**: Jetpack Compose (Material 3)
* **Storage**: Encrypted Room Database
* **Networking**: BLE (Bluetooth Low Energy) GATT & Advertising
* **Encryption**: ECDH (P-256) + AES-256-GCM

---

## 📦 Getting Started
1. **Build APK**: `.\gradlew.bat assembleDebug`
2. **Install**: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. **Run**: Launch "Meshline" from your app drawer.
4. **Permissions**: Grant Bluetooth and Location permissions to enable mesh discovery.

---

## 🛡️ License
Released under the [MIT License](LICENSE).
