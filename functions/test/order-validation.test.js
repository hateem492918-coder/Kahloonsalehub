"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const { normalizeOrderRequest } = require("../src/order-validation");

const validOrder = () => ({
  requestId: "acde4802-1c70-4e8d-aef4-e8ee5de1bac2",
  customer: {
    name: "Ayesha Khan",
    phone: "+92 300 1234567",
    address: "House 10, Model Town, Lahore"
  },
  items: [{ productId: "1", quantity: 2 }]
});

test("normalizes valid customer and cart details", () => {
  assert.deepEqual(normalizeOrderRequest(validOrder()), {
    requestId: "acde4802-1c70-4e8d-aef4-e8ee5de1bac2",
    customer: {
      name: "Ayesha Khan",
      phone: "+92 300 1234567",
      address: "House 10, Model Town, Lahore"
    },
    items: [{ productId: "1", quantity: 2 }]
  });
});

test("rejects client-supplied price fields by normalizing only product IDs and quantities", () => {
  const request = validOrder();
  request.items[0].pricePkr = 1;
  assert.equal(normalizeOrderRequest(request).items[0].pricePkr, undefined);
});

test("rejects duplicate products", () => {
  const request = validOrder();
  request.items.push({ productId: "1", quantity: 1 });
  assert.throws(() => normalizeOrderRequest(request), /only once/);
});

test("rejects invalid quantities and incomplete addresses", () => {
  const request = validOrder();
  request.items[0].quantity = 21;
  assert.throws(() => normalizeOrderRequest(request), /between 1 and 20/);

  const otherRequest = validOrder();
  otherRequest.customer.address = "Lahore";
  assert.throws(() => normalizeOrderRequest(otherRequest), /delivery address/);
});

test("rejects non-UUID request IDs", () => {
  const request = validOrder();
  request.requestId = "not-a-uuid";
  assert.throws(() => normalizeOrderRequest(request), /request ID/);
});
