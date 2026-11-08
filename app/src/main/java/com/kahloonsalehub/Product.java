package com.kahloonsalehub;

final class Product {
    final long id;
    final String name;
    final String category;
    final String emoji;
    final long price;
    final long originalPrice;
    final double rating;
    final String description;

    Product(long id, String name, String category, String emoji, long price,
            long originalPrice, double rating, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.emoji = emoji;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rating = rating;
        this.description = description;
    }
}
