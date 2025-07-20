# XMRpos
FOSS Monero Point of Sale (POS) Android App

![pay](xmrpos-pay.gif)

# Compatability
The app is designed to run specifically on the **Alacrity MJ-Q50Plus** device but should work on any **Noryox NB55** based devices.

# Usage
> [!WARNING]  
> You should only communicate with the MoneroPay instance through a private and secure network

## Initial required configuration
When running XMRpos for the first time you need to at least configure the MoneroPay endpoint. This can be done by:
1. Tap on settings icon on the top right corner
2. Tap on MoneroPay
3. Change the MoneroPay address to the one you want to use.

If your device has a printer you have to configure it like this:
1. Tap on settings icon on the top right corner
2. Tap on printer settings
3. Select the printer connection that your printer uses (NOTE: only Bluetooth is tested right now)
4. Adjust the printer parameters if necessary 

## Taking payments
The payment flow is a 3 step process. 
1. Enter the amount in the `primaryFiatCurrency` and press the green button
2. Tell customers to scan the QR code or tap their phone against the card reader (which in this case emulates a NFC tag). The address, amount, and private note will get prefilled for most Monero wallet apps. The customer then sends the funds. Here you can also reference the `referenceFiatCurrencies`.
3. When the payment is done with either 0, 1, or 10 confirmations (configurable in settings) the app will automatically transfer you to this third screen where you have the choice to print out a receipt.

## Settings

### Company information
Here you can upload a logo which will be shown on the top of the receipt.

You can also change the company name which will appear just below the logo.

The contact information is also changeable from here and will appear just below the company name

At the bottom of the receipt is the receipt footer text which can also be changed here.

### Fiat currencies
Here you can change the `primaryFiatCurrency` and the `referenceFiatCurrencies`

### Security
Here you can enable and disable PIN codes on app start or to open settings. After enableing them, please use a PIN code you can remember because right now there is no way to reset them if you forget them.

### Export transactions
All transactions which are successful are recorded to an internal database.

You can choose to export all of them as a CSV file (txId, xmrAmount, fiatValue, timestamp).

You can also choose to delete all transactions that are currently saved.

### MoneroPay
Here you enter the server address for your MoneroPay instance.

You can also choose to change the request interval which is how often the app will manually check the MoneroPay instance to look for changes instead of waiting for a callback

The number of required confirmations can also be changed here (0-conf, 1-conf or 10-conf)

### Printer settings
Here you can choose the printer connection type of your printer (NOTE: Only Bluetooth is tested)

There are many parameters here that are changeable if your printer does not work out of the box.

The print test button prints a receipt the same way as it would be printed after a transaction, but with static values.


# Building XMRpos from source
This guide will help you build and run **XMRpos** from source.

## Prerequisites
- **Android Studio**: Download and install Android Studio from [here](https://developer.android.com/studio).

## Steps to build the app

### 1. Clone the repository
Start by cloning the repository to your local machine. Open a terminal and run the following command:

```bash
git clone https://github.com/MoneroKon/XMRpos
```

### 2. Open the project in Android Studio
1. Open Android Studio.
2. Select **Open an Existing Project**.
3. Navigate to the cloned XMRpos directory and then to the XMRpos subfolder and open the project.

### 3. Install dependencies
Android Studio should automatically download the necessary dependencies when you open the project. If this doesn’t happen, manually sync the project with Gradle by following these steps:
1. Go to **File > Sync Project with Gradle Files**. 
2. Wait for the sync to complete

### 4. Configure the build variant
If you are building for a specific variant (e.g., debug or release), you can configure the build variant:
1. In Android Studio, open the **Build Variants** tab.
2. Select the desired build variant (e.g., `debug` or `release`).

### 5. Build the APK
1. Go to **Build > Build APK(s)**.
2. Wait for Android Studio to complete the build process.

Alternatively, you can build the APK using the terminal:
```bash
./gradlew assembleDebug  # For Debug build
./gradlew assembleRelease  # For Release build
```
The APK will be available in the app/build/outputs/apk/ directory.

# Donate XMR

**Address**:

```
88zkpYQRJPmeuycSN7Jx3UHq9vH1u2dD8eE1rECvCAouPj75Cdnu1eUacQ5p7ZMvdr4e6BRe2FShv4HoatSs9HcwEeZCupZ
``` 
