package com.kahloonsalehub;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StoreDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "kahloon_store.db";
    private static final int DATABASE_VERSION = 1;

    StoreDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products (" +
                "_id INTEGER PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, " +
                "emoji TEXT NOT NULL, price INTEGER NOT NULL, original_price INTEGER NOT NULL, " +
                "rating REAL NOT NULL, description TEXT NOT NULL)");
        db.execSQL("CREATE TABLE cart (" +
                "product_id INTEGER PRIMARY KEY REFERENCES products(_id), quantity INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE orders (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_name TEXT NOT NULL, " +
                "phone TEXT NOT NULL, address TEXT NOT NULL, total INTEGER NOT NULL, " +
                "created_at TEXT NOT NULL)");
        seedProducts(db);
    }

    private void seedProducts(SQLiteDatabase db) {
        insertProduct(db, 1, "Wireless Earbuds", "Electronics", "🎧", 2499, 3999, 4.7,
                "Comfortable wireless earbuds with a charging case, clear calls, and all-day listening.");
        insertProduct(db, 2, "Everyday Backpack", "Fashion", "🎒", 1890, 2690, 4.5,
                "A lightweight everyday backpack with a roomy main compartment and practical pockets.");
        insertProduct(db, 3, "Cotton Kurta", "Fashion", "👕", 1599, 2299, 4.6,
                "Soft breathable cotton kurta for comfortable everyday wear.");
        insertProduct(db, 4, "Skin Care Set", "Beauty", "🧴", 1299, 1799, 4.4,
                "A gentle daily skin care set for a simple morning and evening routine.");
        insertProduct(db, 5, "Ceramic Mug Set", "Home", "☕", 999, 1499, 4.8,
                "Set of two sturdy ceramic mugs, ideal for tea and coffee at home.");
        insertProduct(db, 6, "Sports Water Bottle", "Sports", "🥤", 799, 1199, 4.3,
                "Reusable leak-resistant bottle for workouts, commutes, and everyday hydration.");
        insertProduct(db, 7, "Bluetooth Speaker", "Electronics", "🔊", 3299, 4599, 4.6,
                "Compact portable speaker with rich sound for music at home or on the go.");
        insertProduct(db, 8, "Decorative Cushion", "Home", "🛋️", 1199, 1699, 4.5,
                "A soft decorative cushion cover to add a fresh touch to your living space.");
    }

    private void insertProduct(SQLiteDatabase db, long id, String name, String category, String emoji,
                               long price, long originalPrice, double rating, String description) {
        ContentValues values = new ContentValues();
        values.put("_id", id);
        values.put("name", name);
        values.put("category", category);
        values.put("emoji", emoji);
        values.put("price", price);
        values.put("original_price", originalPrice);
        values.put("rating", rating);
        values.put("description", description);
        db.insertOrThrow("products", null, values);
    }

    List<Product> getProducts(String category, String search) {
        List<Product> products = new ArrayList<>();
        StringBuilder selection = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();
        if (category != null && !"All".equals(category)) {
            selection.append(" AND category = ?");
            args.add(category);
        }
        if (search != null && !search.trim().isEmpty()) {
            selection.append(" AND (name LIKE ? OR category LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            args.add(pattern);
            args.add(pattern);
        }
        try (Cursor cursor = getReadableDatabase().query("products", null, selection.toString(),
                args.toArray(new String[0]), null, null, "name COLLATE NOCASE")) {
            while (cursor.moveToNext()) {
                products.add(readProduct(cursor));
            }
        }
        return products;
    }

    Product getProduct(long id) {
        try (Cursor cursor = getReadableDatabase().query("products", null, "_id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst() ? readProduct(cursor) : null;
        }
    }

    List<CartLine> getCart() {
        List<CartLine> lines = new ArrayList<>();
        String sql = "SELECT p.*, c.quantity FROM cart c " +
                "JOIN products p ON p._id = c.product_id ORDER BY p.name COLLATE NOCASE";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                lines.add(new CartLine(readProduct(cursor), cursor.getInt(cursor.getColumnIndexOrThrow("quantity"))));
            }
        }
        return lines;
    }

    int getCartCount() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(quantity), 0) FROM cart", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    void syncProducts(List<Product> products) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Product product : products) {
                ContentValues values = new ContentValues();
                values.put("_id", product.id);
                values.put("name", product.name);
                values.put("category", product.category);
                values.put("emoji", product.emoji);
                values.put("price", product.price);
                values.put("original_price", product.originalPrice);
                values.put("rating", product.rating);
                values.put("description", product.description);
                db.insertWithOnConflict("products", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void clearCart() {
        getWritableDatabase().delete("cart", null, null);
    }

    void addToCart(long productId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            android.database.sqlite.SQLiteStatement statement = db.compileStatement(
                    "UPDATE cart SET quantity = quantity + 1 WHERE product_id = ?");
            statement.bindLong(1, productId);
            int updated;
            try {
                updated = statement.executeUpdateDelete();
            } finally {
                statement.close();
            }
            if (updated == 0) {
                ContentValues values = new ContentValues();
                values.put("product_id", productId);
                values.put("quantity", 1);
                db.insertOrThrow("cart", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void setCartQuantity(long productId, int quantity) {
        SQLiteDatabase db = getWritableDatabase();
        if (quantity <= 0) {
            db.delete("cart", "product_id = ?", new String[]{String.valueOf(productId)});
            return;
        }
        ContentValues values = new ContentValues();
        values.put("quantity", quantity);
        db.update("cart", values, "product_id = ?", new String[]{String.valueOf(productId)});
    }

    long placeOrder(String customerName, String phone, String address) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long total = 0;
            try (Cursor cursor = db.rawQuery(
                    "SELECT COALESCE(SUM(p.price * c.quantity), 0) FROM cart c " +
                            "JOIN products p ON p._id = c.product_id", null)) {
                if (cursor.moveToFirst()) {
                    total = cursor.getLong(0);
                }
            }
            if (total <= 0) {
                throw new IllegalStateException("Your cart is empty.");
            }
            ContentValues values = new ContentValues();
            values.put("customer_name", customerName);
            values.put("phone", phone);
            values.put("address", address);
            values.put("total", total);
            values.put("created_at", String.format(Locale.US, "%tF %tT", System.currentTimeMillis(),
                    System.currentTimeMillis()));
            long orderId = db.insertOrThrow("orders", null, values);
            db.delete("cart", null, null);
            db.setTransactionSuccessful();
            return orderId;
        } finally {
            db.endTransaction();
        }
    }

    private Product readProduct(Cursor cursor) {
        return new Product(
                cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("category")),
                cursor.getString(cursor.getColumnIndexOrThrow("emoji")),
                cursor.getLong(cursor.getColumnIndexOrThrow("price")),
                cursor.getLong(cursor.getColumnIndexOrThrow("original_price")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("rating")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Database migration required from version " + oldVersion);
    }
}
