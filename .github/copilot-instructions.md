# Copilot instructions for Kahloonsalehub

## Build, test, and lint commands

- Build debug APK: `./gradlew :app:assembleDebug`
- Run app unit tests: `./gradlew :app:testDebugUnitTest`
- Run one unit test class: `./gradlew :app:testDebugUnitTest --tests 'com.kahloonsalehub.SomeTest'`
- Run Android lint: `./gradlew :app:lintDebug`
- The project uses Android Gradle Plugin 8.7.3, compile/target SDK 35, min SDK 23, and Java 17.
- Firebase backend checks: from `functions/`, run `npm test` and `npm run lint`.
- Run one backend test file from `functions/` with `node --test test/order-validation.test.js`.
- The Firebase Functions runtime is Node 22 (`.nvmrc`).
- No CI workflow is configured.

## High-level architecture

- This is a native Java Android app with a single `app` module and a programmatic, single-activity storefront UI in `MainActivity`.
- `FirebaseRepository` reads active products from Firestore and submits checkout through the `placeOrder` HTTPS callable in `functions/index.js`. Anonymous Firebase Auth identifies guest orders; App Check protects the order function.
- `StoreDatabase` remains the local catalog fallback and persists cart quantities in SQLite. `Product` and `CartLine` are shared app models.
- The Functions backend uses Firestore as the source for product prices and order records. `functions/src/order-validation.js` validates client input; `functions/src/order-pricing.js` calculates totals from server catalog data. `functions/scripts/seed-products.js` initializes the demo catalog from `functions/src/catalog.js`.
- Firestore rules expose active products as read-only and restrict order reads to the authenticated owner. Client-side order writes are denied; Cloud Functions uses the Admin SDK to create orders.
- Order submission currently supports remote cash-on-delivery orders. Easypaisa is not active until merchant API documentation and a verified callback/signature contract are available; never invent provider protocol details or put secrets in the Android app.

## Key conventions

- Keep Android code under package `com.kahloonsalehub` and follow the existing Java/Gradle module structure.
- Keep cart persistence in `StoreDatabase`; when changing its schema, increment `DATABASE_VERSION` and implement a migration rather than dropping user data.
- The UI is built with platform Android views without a UI framework dependency. Preserve the existing orange/neutral visual palette and keep currency display in PKR (`Rs`).
- Treat the sample catalog as demonstration content. Product IDs in Firestore must remain numeric strings because the Android cart stores product IDs as SQLite integers.
- Keep order totals authoritative on the server. Callable input contains product IDs and quantities only; do not trust a client-supplied price or total.
- Keep Firebase project configuration (`app/google-services.json`) and local credentials out of version control. Update Firestore rules with any new client access paths.
