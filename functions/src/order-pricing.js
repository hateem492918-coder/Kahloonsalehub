"use strict";

function priceOrderItems(items, productsById) {
  const orderItems = [];
  let totalPkr = 0;

  for (const item of items) {
    const product = productsById.get(item.productId);
    if (!product || product.active !== true) {
      const error = new Error("A product in your cart is no longer available.");
      error.code = "failed-precondition";
      throw error;
    }
    if (typeof product.name !== "string" || product.name.trim().length === 0
        || !Number.isSafeInteger(product.pricePkr) || product.pricePkr < 1) {
      const error = new Error("A product price is not configured correctly.");
      error.code = "failed-precondition";
      throw error;
    }

    const lineTotalPkr = product.pricePkr * item.quantity;
    if (!Number.isSafeInteger(lineTotalPkr)) {
      const error = new Error("The cart total is too large.");
      error.code = "invalid-argument";
      throw error;
    }
    totalPkr += lineTotalPkr;
    if (!Number.isSafeInteger(totalPkr)) {
      const error = new Error("The cart total is too large.");
      error.code = "invalid-argument";
      throw error;
    }
    orderItems.push({
      productId: item.productId,
      name: product.name,
      unitPricePkr: product.pricePkr,
      quantity: item.quantity,
      lineTotalPkr
    });
  }

  return { orderItems, totalPkr };
}

module.exports = { priceOrderItems };
