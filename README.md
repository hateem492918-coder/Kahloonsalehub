# Kahloon Sale Hub

An Android storefront starter for shoppers in Lahore, Pakistan. It includes a browsable catalog, category filtering, search, product details, a persistent local cart, and Firebase-backed cash-on-delivery orders.

## Android app

Open the repository in Android Studio and let Gradle sync, or build from the repository root:

```sh
./gradlew :app:assembleDebug
```

Install on a connected device or emulator with:

```sh
./gradlew :app:installDebug
```

The project requires Android SDK 35 and Java 17, with API 23 as its minimum supported Android version.
The Firebase Functions backend uses Node 22.

## Firebase setup

1. Create a Firebase project and register an Android app with package name `com.kahloonsalehub`.
2. Download its `google-services.json` into `app/`. This file is ignored by Git and must not be committed.
3. Enable **Anonymous** sign-in in Firebase Authentication and create the default Cloud Firestore database in `asia-south1` to match the order function region.
4. Install Firebase CLI, sign in with `firebase login`, then deploy the backend and Firestore rules:

   ```sh
   firebase deploy --only firestore:rules,firestore:indexes,functions --project YOUR_FIREBASE_PROJECT_ID
   ```

   Cloud Functions deployment requires a Firebase project on the Blaze plan.
5. Seed the sample catalog using Application Default Credentials for the target project:

   ```sh
   cd functions
   npm ci
   gcloud auth application-default login
   node scripts/seed-products.js YOUR_FIREBASE_PROJECT_ID
   ```

6. Configure Firebase App Check for the Android app. Debug builds use the App Check debug provider; add its Logcat token to the Firebase console for local testing. Register the release app with Play Integrity before publishing. The order function enforces App Check.

Product data is read from Firestore. Orders are created by the `placeOrder` callable function, which authenticates the guest user, validates item IDs and quantities, calculates totals from the server catalog, and writes an idempotent order record. Firestore rules prevent clients from writing orders or changing prices. The cart remains local on the device.

## Firebase backend checks

Run backend validation from the `functions/` directory:

```sh
npm test
npm run lint
```

Tests use Node's built-in test runner; run one test file with:

```sh
node --test test/order-validation.test.js
```

## Checkout and payments

Cash on delivery is available, and accepted orders are saved remotely in Firestore. Easypaisa online payment is not active yet: its merchant API contract, credentials, and callback/signature verification must come from the merchant integration materials. Do not place gateway secrets in the Android app or source control. The checkout currently explains this and offers cash on delivery until the verified Easypaisa flow is configured.
