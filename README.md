# XMRpos Client
**Free and Open Source Monero Point of Sale (POS) Android App**

<img width="500" src="https://github.com/user-attachments/assets/ac8414eb-4d28-4609-a4fe-4144e17d46f7" alt="XMRpos screenshot" />
<br>
<a href="https://xmrpos.twed.org/fdroid" target="_blank" rel="noopener noreferrer">
  <img width="200" height="77" alt="fdroid" src="https://github.com/user-attachments/assets/125217c5-1bfb-4ba3-85e4-dc69ab78637b" />
</a>

---

**XMRpos** is a FOSS Android point-of-sale (POS) system for accepting Monero (XMR) payments. It provides a **self-hosted**, **trustless**, and **secure** payment solution for vendors and merchants.

---

## Features
- **Open Source:** Licensed under GNU GPL v3.0.  
- **Trustless Architecture:** Operate your own backend; no reliance on third parties.  
- **Device-Agnostic:** Works on any Android device — no proprietary hardware.  
- **Receipt Printing:** Supports Bluetooth ESC/POS printers.  
- **Scalable:** Unlimited POS clients and vendors, centrally managed.  
- **Secure:** No wallet keys stored or exposed on client devices.  
- **Integrated Backend:** Backend API interfaces with `monerod` and **MoneroPay**.  
- **Fast Payments:** Supports 0-confirmation Monero transactions in 5–10 seconds.

---

## Compatibility
- **Client:** Any Android device, with or without a Bluetooth mobile printer.  
- **Backend:** Ubuntu LTS VPS or LAN environment (low-spec compatible).  

---

## Backend Setup

### Prerequisites
- Clean **Ubuntu LTS VPS**
- Sudo privileges

### Installation
```bash
wget https://raw.githubusercontent.com/MoneroKon/XMRpos/refs/heads/main/install.sh -O install.sh
chmod +x install.sh
sudo ./install.sh
````

This installs **MoneroPay** and **XMRpos-backend** using Docker.
It automatically configures environment variables, secrets, health checks, and wallet setup.

### Uninstall / Cleanup

```bash
sudo ./install.sh clean
```

Removes all containers, cloned repositories, and `~/wallets`.
**Always back up your wallets** before cleaning.

> For detailed backend API usage, see [`XMRpos-backend/README.md`](XMRpos/XMRpos-backend/README.md).

---

## Taking Payments

1. Enter the amount in your `primaryFiatCurrency` and tap the **green button**.
2. The app generates a Monero payment QR or NFC tag with address, amount, and note prefilled.
   The customer scans it and sends funds using any Monero wallet.
3. Once the payment reaches the configured confirmation threshold (0, 1, or 10),
   the app automatically advances to the receipt screen where you can:

   * Print a receipt
   * Start a new order

---

## Settings Overview

### Company Information

* Upload a logo (appears on receipts)
* Edit company name and contact details
* Customize footer text

### Fiat Currencies

* Configure the `primaryFiatCurrency`
* Add multiple `referenceFiatCurrencies`

### Security

* Enable PIN protection for app startup or settings access
* **Note:** PINs cannot currently be reset — choose carefully

### Printer Settings

* Select connection type (Bluetooth tested and supported)
* Adjust printer parameters if needed
* Use **Test Print** to verify output

---

## Building XMRpos Client from Source

### Prerequisites

* [Android Studio](https://developer.android.com/studio)

### 1. Clone the Repository

```bash
git clone https://github.com/MoneroKon/XMRpos
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Select **Open an Existing Project**
3. Navigate to `XMRpos/XMRpos` and open it

### 3. Install Dependencies

Android Studio auto-installs dependencies.
If not, manually sync Gradle:

```
File > Sync Project with Gradle Files
```

### 4. Choose Build Variant

```
View > Tool Windows > Build Variants
```

Select `debug` or `release`.

### 5. Build the APK

#### GUI method:

```
Build > Build APK(s)
```

#### Command line:

```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build
```

Output: `app/build/outputs/apk/`

---

## Building with Docker

### Requirements

* Docker Engine ≥ 24
* Docker Compose plugin
* 8 GB RAM, ~10 GB free disk space
* User added to `docker` group

### Build

```bash
git clone https://github.com/MoneroKon/XMRpos
cd XMRpos/XMRpos
docker compose build --no-cache
docker compose up --abort-on-container-exit
```

### Using Prebuilt Image

```bash
git clone https://github.com/MoneroKon/XMRpos
cd XMRpos/XMRpos
docker run --rm \
  -v "$PWD":/workspace \
  -v xmrpos-gradle:/home/gradle-cache \
  -v xmrpos-android-sdk:/opt/android-sdk \
  ghcr.io/ajs-xmr/xmrpos-android-builder:df7af4d
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Donations

Support the project with Monero (XMR):

```
88zkpYQRJPmeuycSN7Jx3UHq9vH1u2dD8eE1rECvCAouPj75Cdnu1eUacQ5p7ZMvdr4e6BRe2FShv4HoatSs9HcwEeZCupZ
```

---

## License

Licensed under the **GNU General Public License v3.0**.
See [LICENSE](LICENSE) for details.
