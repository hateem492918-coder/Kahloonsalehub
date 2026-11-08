"use strict";

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function invalid(message) {
  const error = new Error(message);
  error.code = "invalid-argument";
  return error;
}

function normalizeOrderRequest(data) {
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    throw invalid("Order details are required.");
  }

  const requestId = typeof data.requestId === "string" ? data.requestId.trim() : "";
  if (!UUID_PATTERN.test(requestId)) {
    throw invalid("A valid order request ID is required.");
  }

  const customer = data.customer;
  if (!customer || typeof customer !== "object" || Array.isArray(customer)) {
    throw invalid("Delivery details are required.");
  }

  const name = typeof customer.name === "string" ? customer.name.trim() : "";
  const phone = typeof customer.phone === "string" ? customer.phone.trim() : "";
  const address = typeof customer.address === "string" ? customer.address.trim() : "";
  const phoneDigits = phone.replace(/\D/g, "");
  if (name.length < 2 || name.length > 100) {
    throw invalid("Enter a name between 2 and 100 characters.");
  }
  if (!/^\+?[0-9\s()-]+$/.test(phone) || phoneDigits.length < 10 || phoneDigits.length > 15) {
    throw invalid("Enter a valid phone number.");
  }
  if (address.length < 8 || address.length > 500) {
    throw invalid("Enter a delivery address between 8 and 500 characters.");
  }

  if (!Array.isArray(data.items) || data.items.length === 0 || data.items.length > 50) {
    throw invalid("Your cart must contain between 1 and 50 different products.");
  }

  const seenProductIds = new Set();
  const items = data.items.map((item) => {
    if (!item || typeof item !== "object" || Array.isArray(item)) {
      throw invalid("The cart contains an invalid item.");
    }
    const productId = typeof item.productId === "string" ? item.productId.trim() : "";
    const quantity = item.quantity;
    if (!/^[1-9][0-9]{0,8}$/.test(productId)) {
      throw invalid("The cart contains an invalid product.");
    }
    if (seenProductIds.has(productId)) {
      throw invalid("Each product must appear only once in the cart.");
    }
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 20) {
      throw invalid("Product quantities must be between 1 and 20.");
    }
    seenProductIds.add(productId);
    return { productId, quantity };
  });

  return {
    requestId,
    customer: { name, phone, address },
    items
  };
}

function matchesOrderRequest(saved, request) {
  if (!saved || !saved.customer || !Array.isArray(saved.items)
      || saved.customer.name !== request.customer.name
      || saved.customer.phone !== request.customer.phone
      || saved.customer.address !== request.customer.address
      || saved.items.length !== request.items.length) {
    return false;
  }

  return request.items.every((item) => saved.items.some((savedItem) =>
    savedItem.productId === item.productId && savedItem.quantity === item.quantity));
}

module.exports = { normalizeOrderRequest, matchesOrderRequest };
