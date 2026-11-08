package com.kahloonsalehub;

import android.content.Context;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class FirebaseRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseFunctions functions;

    FirebaseRepository(Context context) {
        FirebaseApp app = FirebaseApp.initializeApp(context);
        if (app == null) {
            throw new IllegalStateException("Firebase is not configured. Add google-services.json for this app.");
        }

        AppCheckInstaller.install(app);

        auth = FirebaseAuth.getInstance(app);
        firestore = FirebaseFirestore.getInstance(app);
        functions = FirebaseFunctions.getInstance(app, "asia-south1");
    }

    void loadProducts(OnSuccessListener<List<Product>> onSuccess, OnFailureListener onFailure) {
        firestore.collection("products")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> products = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            products.add(readProduct(document));
                        } catch (IllegalArgumentException exception) {
                            onFailure.onFailure(exception);
                            return;
                        }
                    }
                    onSuccess.onSuccess(products);
                })
                .addOnFailureListener(onFailure);
    }

    void placeOrder(String requestId, String name, String phone, String address, List<CartLine> cart,
                    OnSuccessListener<Map<String, Object>> onSuccess, OnFailureListener onFailure) {
        Runnable submit = () -> {
            Map<String, Object> customer = new HashMap<>();
            customer.put("name", name);
            customer.put("phone", phone);
            customer.put("address", address);

            List<Map<String, Object>> items = new ArrayList<>();
            for (CartLine line : cart) {
                Map<String, Object> item = new HashMap<>();
                item.put("productId", String.valueOf(line.product.id));
                item.put("quantity", line.quantity);
                items.add(item);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("requestId", requestId);
            payload.put("customer", customer);
            payload.put("items", items);

            functions.getHttpsCallable("placeOrder")
                    .call(payload)
                    .addOnSuccessListener(result -> onSuccess.onSuccess(readOrderResult(result)))
                    .addOnFailureListener(onFailure);
        };

        if (auth.getCurrentUser() != null) {
            submit.run();
        } else {
            auth.signInAnonymously()
                    .addOnSuccessListener(authResult -> submit.run())
                    .addOnFailureListener(onFailure);
        }
    }

    private Product readProduct(com.google.firebase.firestore.DocumentSnapshot document) {
        Long id = parseProductId(document.getId());
        Long price = document.getLong("pricePkr");
        Long originalPrice = document.getLong("originalPricePkr");
        Double rating = document.getDouble("rating");
        String name = document.getString("name");
        String category = document.getString("category");
        String emoji = document.getString("emoji");
        String description = document.getString("description");
        if (id == null || price == null || originalPrice == null || rating == null
                || name == null || category == null || emoji == null || description == null) {
            throw new IllegalArgumentException("The online catalog contains an invalid product.");
        }
        return new Product(id, name, category, emoji, price, originalPrice, rating, description);
    }

    private Long parseProductId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Map<String, Object> readOrderResult(HttpsCallableResult result) {
        Object data = result.getData();
        if (!(data instanceof Map<?, ?>)) {
            throw new IllegalStateException("The order service returned an invalid response.");
        }
        Map<?, ?> rawResponse = (Map<?, ?>) data;
        Map<String, Object> response = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawResponse.entrySet()) {
            if (entry.getKey() instanceof String) {
                response.put((String) entry.getKey(), entry.getValue());
            }
        }
        Object orderId = response.get("orderId");
        Object totalPkr = response.get("totalPkr");
        if (!(orderId instanceof String) || !(totalPkr instanceof Number)) {
            throw new IllegalStateException("The order service returned an incomplete response.");
        }
        return response;
    }
}
