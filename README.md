# Kahloon Sale Hub

An Android storefront starter for shoppers in Lahore, Pakistan. The app includes a browsable sample catalog, category filtering, search, product details, a persistent cart, and a cash-on-delivery checkout flow.

## Run

Open the repository in Android Studio and let Gradle sync, or build from the repository root:

```sh
./gradlew :app:assembleDebug
```

Install the debug APK on a connected device or emulator with:

```sh
./gradlew :app:installDebug
```

The project requires Android SDK 35 and Java 17. The minimum supported Android version is API 23.

## Local-first demo

The catalog and sample prices are stored in the app, while cart items and placed orders are saved in a local SQLite database on the device. Checkout records an order locally and demonstrates cash on delivery; it does not contact a server, accept online payments, or send an order to a merchant. Replace the sample catalog and connect an authorized backend before using this as a live store.
