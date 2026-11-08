package com.kahloonsalehub;

final class CartLine {
    final Product product;
    final int quantity;

    CartLine(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    long getSubtotal() {
        return product.price * quantity;
    }
}
