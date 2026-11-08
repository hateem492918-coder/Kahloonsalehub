package com.kahloonsalehub;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int ORANGE = Color.rgb(245, 114, 36);
    private static final int INK = Color.rgb(36, 39, 43);
    private static final int MUTED = Color.rgb(116, 121, 128);
    private static final int CANVAS = Color.rgb(247, 248, 250);
    private static final int BORDER = Color.rgb(232, 234, 237);
    private static final String[] CATEGORIES = {"All", "Electronics", "Fashion", "Home", "Beauty", "Sports"};
    private static final String CHECKOUT_PREFS = "checkout";
    private static final String CHECKOUT_REQUEST_ID = "request_id";

    private StoreDatabase database;
    private FirebaseRepository firebaseRepository;
    private List<Product> remoteProducts = new ArrayList<>();
    private boolean remoteCatalogLoaded;
    private String screen = "home";
    private String selectedCategory = "All";
    private String searchQuery = "";
    private long selectedProductId;
    private String lastOrderNumber = "";
    private long lastOrderTotalPkr;
    private String checkoutRequestId;
    private boolean placingOrder;
    private LinearLayout root;
    private LinearLayout content;
    private TextView headerCartCount;
    private TextView footerCartCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new StoreDatabase(this);
        checkoutRequestId = getSharedPreferences(CHECKOUT_PREFS, MODE_PRIVATE)
                .getString(CHECKOUT_REQUEST_ID, null);
        try {
            firebaseRepository = new FirebaseRepository(this);
            firebaseRepository.loadProducts(products -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                remoteProducts = products;
                remoteCatalogLoaded = true;
                database.syncProducts(products);
                if ("home".equals(screen)) {
                    render();
                }
            }, error -> {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, "Could not load the online catalog: " +
                            error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (IllegalStateException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
        }
        render();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if ("checkout".equals(screen)) {
            if (placingOrder) {
                return;
            }
            clearCheckoutRequestId();
            screen = "cart";
        } else if ("detail".equals(screen) || "cart".equals(screen)) {
            screen = "home";
        } else {
            super.onBackPressed();
            return;
        }
        render();
    }

    private void render() {
        headerCartCount = null;
        footerCartCount = null;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CANVAS);
        setContentView(root);

        if ("success".equals(screen)) {
            showOrderSuccess();
            return;
        }

        if ("home".equals(screen)) {
            showHome();
        } else if ("detail".equals(screen)) {
            showProductDetail();
        } else if ("cart".equals(screen)) {
            showCart();
        } else if ("checkout".equals(screen)) {
            showCheckout();
        }
    }

    private void showHome() {
        root.addView(buildBrandBar(false));
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(10), dp(16), dp(20));
        scroll.setFillViewport(true);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView welcome = text("Good finds, close to home", 21, INK, true);
        content.addView(welcome);
        content.addView(text("Shop your everyday favourites in Lahore.", 14, MUTED, false),
                marginParams(-1, -2, 0, 4, 0, 14));
        addSearchBox();
        addCategories();
        addPromoBanner();

        LinearLayout heading = row();
        heading.addView(text(searchQuery.isEmpty() ? "Popular picks" : "Search results", 19, INK, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView count = text(getVisibleProducts().size() + " items", 13, MUTED, false);
        heading.addView(count);
        content.addView(heading, marginParams(-1, -2, 0, 18, 0, 10));
        addProductGrid(getVisibleProducts());
        root.addView(buildBottomBar());
    }

    private void addSearchBox() {
        LinearLayout search = row();
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(12), 0, dp(12), 0);
        search.setBackground(roundRect(Color.WHITE, BORDER, 1, 12));
        TextView icon = text("⌕", 23, MUTED, true);
        search.addView(icon, marginParams(-2, -2, 0, 0, 8, 0));
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setHint("Search products");
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setText(searchQuery);
        input.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchQuery = view.getText().toString();
                render();
                return true;
            }
            return false;
        });
        search.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView go = text("Go", 14, ORANGE, true);
        go.setOnClickListener(v -> {
            searchQuery = input.getText().toString();
            render();
        });
        search.addView(go);
        content.addView(search, marginParams(-1, -2, 0, 0, 0, 16));
    }

    private void addCategories() {
        ScrollView horizontal = new ScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = row();
        for (String category : CATEGORIES) {
            boolean selected = category.equals(selectedCategory);
            TextView chip = text(category, 13, selected ? Color.WHITE : INK, selected);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(15), 0, dp(15), 0);
            chip.setBackground(roundRect(selected ? ORANGE : Color.WHITE, selected ? ORANGE : BORDER, 1, 24));
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                render();
            });
            chips.addView(chip, marginParams(-2, 38, 0, 0, 9, 0));
        }
        horizontal.addView(chips);
        content.addView(horizontal, marginParams(-1, 38, 0, 0, 0, 16));
    }

    private void addPromoBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(dp(18), dp(15), dp(18), dp(15));
        banner.setBackground(roundRect(Color.rgb(255, 237, 222), Color.rgb(255, 237, 222), 0, 16));
        banner.addView(text("A little more joy, for less", 17, INK, true));
        banner.addView(text("Handpicked deals with cash on delivery", 13, MUTED, false),
                marginParams(-1, -2, 0, 5, 0, 0));
        content.addView(banner, marginParams(-1, -2, 0, 0, 0, 20));
    }

    private void addProductGrid(List<Product> products) {
        if (products.isEmpty()) {
            TextView empty = text("No products found. Try another search or category.", 15, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            content.addView(empty, marginParams(-1, 120, 0, 4, 0, 0));
            return;
        }
        for (int i = 0; i < products.size(); i += 2) {
            LinearLayout row = row();
            row.setGravity(Gravity.TOP);
            row.addView(buildProductCard(products.get(i)), new LinearLayout.LayoutParams(0, -2, 1));
            if (i + 1 < products.size()) {
                row.addView(buildProductCard(products.get(i + 1)), new LinearLayout.LayoutParams(0, -2, 1));
            } else {
                row.addView(new TextView(this), new LinearLayout.LayoutParams(0, 1, 1));
            }
            content.addView(row, marginParams(-1, -2, 0, 0, 0, 12));
        }
    }

    private LinearLayout buildProductCard(Product product) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(11));
        card.setBackground(roundRect(Color.WHITE, BORDER, 1, 14));
        card.setOnClickListener(v -> {
            selectedProductId = product.id;
            screen = "detail";
            render();
        });
        TextView image = text(product.emoji, 40, INK, false);
        image.setGravity(Gravity.CENTER);
        image.setBackground(roundRect(Color.rgb(250, 243, 237), Color.rgb(250, 243, 237), 0, 12));
        card.addView(image, new LinearLayout.LayoutParams(-1, dp(108)));
        card.addView(text(product.category.toUpperCase(Locale.ROOT), 10, MUTED, true),
                marginParams(-1, -2, 0, 10, 0, 3));
        TextView name = text(product.name, 14, INK, true);
        name.setMaxLines(2);
        card.addView(name, new LinearLayout.LayoutParams(-1, dp(38)));
        card.addView(text("★ " + String.format(Locale.US, "%.1f", product.rating), 12,
                Color.rgb(195, 128, 28), true), marginParams(-1, -2, 0, 5, 0, 5));

        LinearLayout priceRow = row();
        priceRow.setGravity(Gravity.CENTER_VERTICAL);
        priceRow.addView(text(formatPrice(product.price), 15, ORANGE, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = text("+ Add", 12, ORANGE, true);
        add.setGravity(Gravity.CENTER);
        add.setPadding(dp(8), dp(6), dp(8), dp(6));
        add.setBackground(roundRect(Color.rgb(255, 243, 235), Color.rgb(255, 243, 235), 0, 10));
        add.setOnClickListener(v -> {
            if (!database.addToCart(product.id)) {
                Toast.makeText(this, "You have reached the 20-item limit for this product.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            refreshCartCount();
            Toast.makeText(this, "Added to your cart", Toast.LENGTH_SHORT).show();
        });
        priceRow.addView(add);
        card.addView(priceRow);
        return card;
    }

    private void showProductDetail() {
        Product product = findProduct(selectedProductId);
        if (product == null) {
            screen = "home";
            render();
            return;
        }
        root.addView(buildBrandBar(true));
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(16), dp(18), dp(20));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView image = text(product.emoji, 82, INK, false);
        image.setGravity(Gravity.CENTER);
        image.setBackground(roundRect(Color.rgb(250, 243, 237), Color.rgb(250, 243, 237), 0, 20));
        body.addView(image, marginParams(-1, 230, 0, 0, 0, 20));
        body.addView(text(product.category.toUpperCase(Locale.ROOT), 12, MUTED, true));
        body.addView(text(product.name, 25, INK, true), marginParams(-1, -2, 0, 5, 0, 7));
        body.addView(text("★ " + String.format(Locale.US, "%.1f", product.rating) + "  ·  In stock",
                14, Color.rgb(195, 128, 28), true));
        body.addView(text(formatPrice(product.price), 24, ORANGE, true),
                marginParams(-1, -2, 0, 13, 0, 5));
        body.addView(text("Was " + formatPrice(product.originalPrice), 14, MUTED, false));
        body.addView(text("About this item", 17, INK, true), marginParams(-1, -2, 0, 22, 0, 7));
        body.addView(text(product.description, 15, MUTED, false));
        body.addView(text("Cash on delivery available in Lahore.", 14, INK, false),
                marginParams(-1, -2, 0, 20, 0, 0));
        Button add = primaryButton("Add to cart");
        add.setOnClickListener(v -> {
            if (!database.addToCart(product.id)) {
                Toast.makeText(this, "You have reached the 20-item limit for this product.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Added to your cart", Toast.LENGTH_SHORT).show();
            screen = "cart";
            render();
        });
        root.addView(add, marginParams(-1, 52, 18, 0, 18, 14));
    }

    private void showCart() {
        root.addView(buildBrandBar(true));
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(20));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content.addView(text("Your cart", 23, INK, true), marginParams(-1, -2, 0, 0, 0, 14));
        List<CartLine> lines = database.getCart();
        if (lines.isEmpty()) {
            TextView empty = text("Your cart is waiting for something lovely.", 15, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            content.addView(empty, marginParams(-1, 180, 0, 0, 0, 12));
            Button shop = primaryButton("Browse products");
            shop.setOnClickListener(v -> {
                screen = "home";
                render();
            });
            content.addView(shop);
        } else {
            for (CartLine line : lines) {
                content.addView(buildCartLine(line), marginParams(-1, -2, 0, 0, 0, 10));
            }
            long total = cartTotal(lines);
            LinearLayout summary = new LinearLayout(this);
            summary.setOrientation(LinearLayout.VERTICAL);
            summary.setPadding(dp(16), dp(15), dp(16), dp(15));
            summary.setBackground(roundRect(Color.WHITE, BORDER, 1, 14));
            LinearLayout subtotal = row();
            subtotal.addView(text("Subtotal", 15, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
            subtotal.addView(text(formatPrice(total), 16, INK, true));
            summary.addView(subtotal);
            summary.addView(text("Free delivery within Lahore", 12, MUTED, false),
                    marginParams(-1, -2, 0, 7, 0, 13));
            Button checkout = primaryButton("Continue to checkout");
            checkout.setOnClickListener(v -> {
                if (checkoutRequestId == null) {
                    createCheckoutRequestId();
                }
                screen = "checkout";
                render();
            });
            summary.addView(checkout);
            content.addView(summary, marginParams(-1, -2, 0, 10, 0, 0));
        }
        root.addView(buildBottomBar());
    }

    private LinearLayout buildCartLine(CartLine line) {
        LinearLayout card = row();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(roundRect(Color.WHITE, BORDER, 1, 14));
        TextView emoji = text(line.product.emoji, 28, INK, false);
        emoji.setGravity(Gravity.CENTER);
        emoji.setBackground(roundRect(Color.rgb(250, 243, 237), Color.rgb(250, 243, 237), 0, 10));
        card.addView(emoji, marginParams(54, 54, 0, 0, 12, 0));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.addView(text(line.product.name, 14, INK, true));
        details.addView(text(formatPrice(line.product.price) + " each", 12, MUTED, false),
                marginParams(-1, -2, 0, 4, 0, 0));
        LinearLayout qtyRow = row();
        qtyRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView minus = quantityButton("−");
        minus.setOnClickListener(v -> {
            database.setCartQuantity(line.product.id, line.quantity - 1);
            render();
        });
        TextView qty = text(String.valueOf(line.quantity), 14, INK, true);
        qty.setGravity(Gravity.CENTER);
        qtyRow.addView(minus);
        qtyRow.addView(qty, new LinearLayout.LayoutParams(dp(32), -2));
        TextView plus = quantityButton("+");
        plus.setOnClickListener(v -> {
            if (line.quantity >= 20) {
                Toast.makeText(this, "You have reached the 20-item limit for this product.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            database.setCartQuantity(line.product.id, line.quantity + 1);
            render();
        });
        qtyRow.addView(plus);
        details.addView(qtyRow, marginParams(-1, -2, 0, 8, 0, 0));
        card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(text(formatPrice(line.getSubtotal()), 14, INK, true));
        return card;
    }

    private void showCheckout() {
        root.addView(buildBrandBar(true));
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(16), dp(18), dp(20));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content.addView(text("Delivery details", 23, INK, true), marginParams(-1, -2, 0, 0, 0, 6));
        content.addView(text("Free delivery within Lahore.", 14, MUTED, false),
                marginParams(-1, -2, 0, 0, 0, 18));
        EditText name = formField("Full name");
        EditText phone = formField("Phone number");
        phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        EditText address = formField("Delivery address in Lahore");
        address.setMinLines(3);
        address.setGravity(Gravity.TOP | Gravity.START);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        content.addView(name, marginParams(-1, 54, 0, 0, 0, 10));
        content.addView(phone, marginParams(-1, 54, 0, 0, 0, 10));
        content.addView(address, marginParams(-1, 94, 0, 0, 0, 18));
        LinearLayout payment = row();
        payment.setGravity(Gravity.CENTER_VERTICAL);
        payment.setPadding(dp(14), dp(14), dp(14), dp(14));
        payment.setBackground(roundRect(Color.WHITE, BORDER, 1, 12));
        payment.addView(text("◉", 18, ORANGE, true), marginParams(-2, -2, 0, 0, 10, 0));
        LinearLayout paymentText = new LinearLayout(this);
        paymentText.setOrientation(LinearLayout.VERTICAL);
        paymentText.addView(text("Cash on delivery", 15, INK, true));
        paymentText.addView(text("Pay when your order arrives", 13, MUTED, false));
        payment.addView(paymentText);
        content.addView(payment, marginParams(-1, -2, 0, 0, 0, 18));
        content.addView(text("Easypaisa online payment will be enabled after the merchant API and callback details are configured.",
                13, MUTED, false), marginParams(-1, -2, 0, 0, 0, 14));
        long total = cartTotal(database.getCart());
        LinearLayout totalRow = row();
        totalRow.addView(text("Order total", 16, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        totalRow.addView(text(formatPrice(total), 18, ORANGE, true));
        content.addView(totalRow, marginParams(-1, -2, 0, 0, 0, 16));
        Button placeOrder = primaryButton("Place order");
        placeOrder.setOnClickListener(v -> {
            String customerName = name.getText().toString().trim();
            String customerPhone = phone.getText().toString().trim();
            String customerAddress = address.getText().toString().trim();
            if (customerName.length() < 2 || customerName.length() > 100
                    || customerAddress.length() < 8 || customerAddress.length() > 500) {
                Toast.makeText(this, "Please complete all delivery details.", Toast.LENGTH_SHORT).show();
                return;
            }
            String phoneDigits = customerPhone.replaceAll("\\D", "");
            if (!customerPhone.matches("\\+?[0-9\\s()-]+")
                    || phoneDigits.length() < 10 || phoneDigits.length() > 15) {
                phone.setError("Enter a valid phone number");
                return;
            }
            if (firebaseRepository == null) {
                Toast.makeText(this, "Connect this app to Firebase before placing an online order.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            List<CartLine> cart = database.getCart();
            if (cart.isEmpty()) {
                Toast.makeText(this, "Your cart is empty.", Toast.LENGTH_SHORT).show();
                screen = "cart";
                render();
                return;
            }
            if (checkoutRequestId == null) {
                createCheckoutRequestId();
            }
            placeOrder.setEnabled(false);
            placingOrder = true;
            firebaseRepository.placeOrder(checkoutRequestId, customerName, customerPhone, customerAddress, cart,
                    result -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                placingOrder = false;
                Object orderNumber = result.get("orderNumber");
                Object total = result.get("totalPkr");
                if (!(orderNumber instanceof String) || !(total instanceof Number)) {
                    placeOrder.setEnabled(true);
                    Toast.makeText(this, "The order service returned an invalid response.", Toast.LENGTH_LONG).show();
                    return;
                }
                lastOrderNumber = (String) orderNumber;
                lastOrderTotalPkr = ((Number) total).longValue();
                database.clearCart();
                clearCheckoutRequestId();
                screen = "success";
                render();
            }, error -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                placingOrder = false;
                placeOrder.setEnabled(true);
                String message = error.getLocalizedMessage();
                Toast.makeText(this, "Order could not be placed" +
                        (message == null ? "." : ": " + message), Toast.LENGTH_LONG).show();
            });
        });
        content.addView(placeOrder, marginParams(-1, 52, 0, 0, 0, 12));
    }

    private void showOrderSuccess() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER);
        body.setPadding(dp(28), dp(24), dp(28), dp(24));
        body.setBackgroundColor(Color.WHITE);
        TextView icon = text("✓", 50, Color.WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(roundRect(Color.rgb(47, 160, 112), Color.rgb(47, 160, 112), 0, 50));
        body.addView(icon, marginParams(88, 88, 0, 0, 0, 22));
        body.addView(text("Order placed!", 25, INK, true), marginParams(-1, -2, 0, 0, 0, 8));
        TextView message = text("Thanks for shopping with Kahloon Sale Hub. Your cash-on-delivery order has been saved online.",
                15, MUTED, false);
        message.setGravity(Gravity.CENTER);
        body.addView(message, marginParams(-1, -2, 0, 0, 0, 10));
        TextView order = text("Order #" + lastOrderNumber + "  ·  " + formatPrice(lastOrderTotalPkr), 14, INK, true);
        order.setGravity(Gravity.CENTER);
        body.addView(order, marginParams(-1, -2, 0, 0, 0, 24));
        Button home = primaryButton("Continue shopping");
        home.setOnClickListener(v -> {
            screen = "home";
            render();
        });
        body.addView(home, marginParams(-1, 52, 0, 0, 0, 0));
        root.addView(body, new LinearLayout.LayoutParams(-1, -1));
    }

    private LinearLayout buildBrandBar(boolean back) {
        LinearLayout bar = row();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(10), dp(16), dp(10));
        bar.setBackgroundColor(Color.WHITE);
        if (back) {
            TextView backButton = text("‹", 32, INK, false);
            backButton.setGravity(Gravity.CENTER);
            backButton.setOnClickListener(v -> onBackPressed());
            bar.addView(backButton, marginParams(36, 42, 0, 0, 10, 0));
        }
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(text("kahloon", 21, ORANGE, true));
        brand.addView(text("SALE HUB  ·  LAHORE", 9, MUTED, true));
        bar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        headerCartCount = text("🛒 " + database.getCartCount(), 15, INK, true);
        headerCartCount.setGravity(Gravity.CENTER);
        headerCartCount.setPadding(dp(10), dp(8), dp(10), dp(8));
        headerCartCount.setBackground(roundRect(Color.rgb(255, 243, 235), Color.rgb(255, 243, 235), 0, 12));
        headerCartCount.setOnClickListener(v -> {
            screen = "cart";
            render();
        });
        bar.addView(headerCartCount);
        return bar;
    }

    private LinearLayout buildBottomBar() {
        LinearLayout nav = row();
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(dp(16), dp(8), dp(16), dp(8));
        TextView home = text("⌂  Home", 14, "home".equals(screen) ? ORANGE : MUTED, true);
        home.setGravity(Gravity.CENTER);
        home.setOnClickListener(v -> {
            screen = "home";
            render();
        });
        footerCartCount = text("🛒  Cart  (" + database.getCartCount() + ")", 14,
                "cart".equals(screen) ? ORANGE : MUTED, true);
        footerCartCount.setGravity(Gravity.CENTER);
        footerCartCount.setOnClickListener(v -> {
            screen = "cart";
            render();
        });
        nav.addView(home, new LinearLayout.LayoutParams(0, dp(42), 1));
        nav.addView(footerCartCount, new LinearLayout.LayoutParams(0, dp(42), 1));
        return nav;
    }

    private void refreshCartCount() {
        int count = database.getCartCount();
        if (headerCartCount != null) {
            headerCartCount.setText("🛒 " + count);
        }
        if (footerCartCount != null) {
            footerCartCount.setText("🛒  Cart  (" + count + ")");
        }
    }

    private EditText formField(String hint) {
        EditText field = new EditText(this);
        field.setTextSize(15);
        field.setTextColor(INK);
        field.setHintTextColor(MUTED);
        field.setHint(hint);
        field.setPadding(dp(14), dp(10), dp(14), dp(10));
        field.setBackground(roundRect(Color.WHITE, BORDER, 1, 12));
        return field;
    }

    private TextView quantityButton(String label) {
        TextView button = text(label, 18, ORANGE, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(Color.rgb(255, 243, 235), Color.rgb(255, 243, 235), 0, 8));
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(30), dp(28)));
        return button;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(roundRect(ORANGE, ORANGE, 0, 12));
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private GradientDrawable roundRect(int color, int strokeColor, int strokeWidth, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (strokeWidth > 0) {
            shape.setStroke(dp(strokeWidth), strokeColor);
        }
        return shape;
    }

    private LinearLayout.LayoutParams marginParams(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                width < 0 ? width : dp(width), height < 0 ? height : dp(height));
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private long cartTotal(List<CartLine> lines) {
        long total = 0;
        for (CartLine line : lines) {
            total += line.getSubtotal();
        }
        return total;
    }

    private List<Product> getVisibleProducts() {
        List<Product> catalog = remoteCatalogLoaded
                ? remoteProducts
                : database.getProducts("All", "");
        List<Product> visible = new ArrayList<>();
        for (Product product : catalog) {
            if (!"All".equals(selectedCategory) && !selectedCategory.equals(product.category)) {
                continue;
            }
            if (!searchQuery.trim().isEmpty()) {
                String query = searchQuery.trim().toLowerCase(Locale.ROOT);
                if (!product.name.toLowerCase(Locale.ROOT).contains(query)
                        && !product.category.toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }
            }
            visible.add(product);
        }
        return visible;
    }

    private Product findProduct(long id) {
        if (remoteCatalogLoaded) {
            for (Product product : remoteProducts) {
                if (product.id == id) {
                    return product;
                }
            }
            return null;
        }
        return database.getProduct(id);
    }

    private void createCheckoutRequestId() {
        checkoutRequestId = UUID.randomUUID().toString();
        getSharedPreferences(CHECKOUT_PREFS, MODE_PRIVATE)
                .edit()
                .putString(CHECKOUT_REQUEST_ID, checkoutRequestId)
                .apply();
    }

    private void clearCheckoutRequestId() {
        checkoutRequestId = null;
        getSharedPreferences(CHECKOUT_PREFS, MODE_PRIVATE)
                .edit()
                .remove(CHECKOUT_REQUEST_ID)
                .apply();
    }

    private String formatPrice(long amount) {
        return "Rs " + NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
