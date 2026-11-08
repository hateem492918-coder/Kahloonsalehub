"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const { matchesOrderRequest, normalizeOrderRequest } = require("../src/order-validation");
const { priceOrderItems } = require("../src/order-pricing");

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

test("recognizes an idempotent retry but rejects changed checkout data", () => {
  const request = normalizeOrderRequest(validOrder());
  const saved = {
    customer: request.customer,
    items: [{ productId: "1", quantity: 2, unitPricePkr: 2499 }]
  };
  assert.equal(matchesOrderRequest(saved, request), true);

  const changedRequest = normalizeOrderRequest({
    ...validOrder(),
    items: [{ productId: "2", quantity: 2 }]
  });
  assert.equal(matchesOrderRequest(saved, changedRequest), false);
});

test("prices order lines from the server catalog instead of client prices", () => {
  const request = normalizeOrderRequest(validOrder());
  const priced = priceOrderItems(request.items, new Map([
    ["1", { active: true, name: "Wireless Earbuds", pricePkr: 2499 }]
  ]));
  assert.equal(priced.totalPkr, 4998);
  assert.equal(priced.orderItems[0].unitPricePkr, 2499);
  assert.equal(priced.orderItems[0].lineTotalPkr, 4998);
});

test("rejects inactive products and invalid server prices", () => {
  const request = normalizeOrderRequest(validOrder());
  assert.throws(() => priceOrderItems(request.items, new Map([
    ["1", { active: false, name: "Wireless Earbuds", pricePkr: 2499 }]
  ])), { code: "failed-precondition" });
  assert.throws(() => priceOrderItems(request.items, new Map([
    ["1", { active: true, name: "Wireless Earbuds", pricePkr: 0 }]
  ])), { code: "failed-precondition" });
});
