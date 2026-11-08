# Copilot instructions for Kahloonsalehub

## Build, test, and lint commands

- Build debug APK: `./gradlew :app:assembleDebug`
- Run app unit tests: `./gradlew :app:testDebugUnitTest`
- Run one unit test class: `./gradlew :app:testDebugUnitTest --tests 'com.kahloonsalehub.SomeTest'`
- Run Android lint: `./gradlew :app:lintDebug`
- The project uses Android Gradle Plugin 8.7.3, compile/target SDK 35, min SDK 23, and Java 17.
- Tests and lint tasks may need to be added alongside future features; there are no tests or CI workflows in the current project.

## High-level architecture

- This is a native Java Android app with a single `app` module and a programmatic, single-activity storefront UI in `MainActivity`.
- `StoreDatabase` is the local source of truth: it seeds the sample catalog and persists cart quantities and placed cash-on-delivery orders in SQLite. `Product` and `CartLine` are the small data models used by the UI and database.
- The activity swaps between home/search/category browsing, product detail, cart, checkout, and order confirmation. Cart and order data survive app restarts; sample product data does not come from a network service.
- This is a local-first starter, not a connected marketplace backend. Do not imply that checkout transmits an order or performs payment.

## Key conventions

- Keep Android code under package `com.kahloonsalehub` and follow the existing Java/Gradle module structure.
- Keep catalog/cart/order persistence in `StoreDatabase`; when changing its schema, increment `DATABASE_VERSION` and implement a migration rather than dropping user data.
- The UI is built with platform Android views without a UI framework dependency. Preserve the existing orange/neutral visual palette and keep currency display in PKR (`Rs`).
- Treat current product records as demonstration content. Product/category/search filtering should continue to use parameterized SQLite queries.
