package com.kahloonsalehub;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

final class AppCheckInstaller {
    private AppCheckInstaller() {
    }

    static void install(FirebaseApp app) {
        FirebaseAppCheck.getInstance(app)
                .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());
    }
}
