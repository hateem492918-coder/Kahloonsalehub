"use strict";

const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { products } = require("../src/catalog");

const projectId = process.argv[2] || process.env.GCLOUD_PROJECT;
if (!projectId) {
  throw new Error("Pass a Firebase project ID or set GCLOUD_PROJECT.");
}

initializeApp({ projectId });
const db = getFirestore();

async function seedProducts() {
  const batch = db.batch();
  for (const product of products) {
    batch.set(db.collection("products").doc(product.id), {
      name: product.name,
      category: product.category,
      emoji: product.emoji,
      pricePkr: product.pricePkr,
      originalPricePkr: product.originalPricePkr,
      rating: product.rating,
      description: product.description,
      active: true
    });
  }
  await batch.commit();
  console.log(`Seeded ${products.length} products in Firebase project ${projectId}.`);
}

seedProducts().catch((error) => {
  console.error("Could not seed the product catalog:", error);
  process.exitCode = 1;
});
