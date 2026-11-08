"use strict";

const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const { HttpsError, onCall } = require("firebase-functions/v2/https");
const { normalizeOrderRequest } = require("./src/order-validation");
const { priceOrderItems } = require("./src/order-pricing");

setGlobalOptions({ region: "asia-south1" });
initializeApp();

const db = getFirestore();

exports.placeOrder = onCall({ enforceAppCheck: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in before placing an order.");
  }

  let order;
  try {
    order = normalizeOrderRequest(request.data);
  } catch (error) {
    if (error.code === "invalid-argument") {
      throw new HttpsError("invalid-argument", error.message);
    }
    throw error;
  }

  const uid = request.auth.uid;
  const orderRef = db.collection("orders").doc(`${uid}_${order.requestId}`);
  return db.runTransaction(async (transaction) => {
    const existing = await transaction.get(orderRef);
    if (existing.exists) {
      const saved = existing.data();
      if (saved.uid !== uid) {
        throw new HttpsError("permission-denied", "This order request is not available.");
      }
      return {
        orderId: existing.id,
        orderNumber: saved.orderNumber,
        status: saved.status,
        totalPkr: saved.totalPkr,
        paymentMethod: saved.paymentMethod
      };
    }

    const productRefs = order.items.map((item) => db.collection("products").doc(item.productId));
    const productSnapshots = await Promise.all(productRefs.map((ref) => transaction.get(ref)));
    const productsById = new Map(productSnapshots.map((snapshot) => [
      snapshot.id,
      snapshot.exists ? snapshot.data() : null
    ]));
    let pricedOrder;
    try {
      pricedOrder = priceOrderItems(order.items, productsById);
    } catch (error) {
      if (error.code === "failed-precondition" || error.code === "invalid-argument") {
        throw new HttpsError(error.code, error.message);
      }
      throw error;
    }

    transaction.create(orderRef, {
      uid,
      requestId: order.requestId,
      orderNumber: order.requestId.substring(0, 8).toUpperCase(),
      customer: order.customer,
      items: pricedOrder.orderItems,
      currency: "PKR",
      totalPkr: pricedOrder.totalPkr,
      status: "confirmed",
      paymentMethod: "cod",
      paymentStatus: "pending_collection",
      createdAt: FieldValue.serverTimestamp()
    });

    return {
      orderId: orderRef.id,
      orderNumber: order.requestId.substring(0, 8).toUpperCase(),
      status: "confirmed",
      totalPkr: pricedOrder.totalPkr,
      paymentMethod: "cod"
    };
  });
});
