#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

// Configuration
const CUSTOMER_COUNT = 1000;
const TRANSACTIONS_PER_CUSTOMER = 1000; // Total: 1M transactions
const START_DATE = new Date('2024-01-01');
const END_DATE = new Date('2025-01-01');

// Merchant categories and MCC codes
const MERCHANT_CATEGORIES = {
  '5411': ['Grocery Store', 'Supermarket', 'Food Mart', 'Fresh Market'],
  '5812': ['Restaurant', 'Cafe', 'Fast Food', 'Diner', 'Pizza Place'],
  '4121': ['Taxi', 'Ride Share', 'Transportation', 'Cab Service'],
  '6011': ['ATM', 'Bank ATM', 'Cash Withdrawal'],
  '5311': ['Department Store', 'Retail Store', 'Shopping Mall'],
  '7995': ['Gaming', 'Entertainment', 'Arcade', 'Gaming Store'],
  '5814': ['Fast Food', 'Quick Service', 'Takeout'],
  '5999': ['Miscellaneous', 'General Store', 'Convenience Store'],
  '7011': ['Hotel', 'Accommodation', 'Resort'],
  '4511': ['Airlines', 'Travel', 'Flight Booking']
};

// Indian cities with coordinates
const INDIAN_CITIES = [
  { name: 'Mumbai', lat: 19.0760, lon: 72.8777, country: 'IN' },
  { name: 'Delhi', lat: 28.7041, lon: 77.1025, country: 'IN' },
  { name: 'Bangalore', lat: 12.9716, lon: 77.5946, country: 'IN' },
  { name: 'Hyderabad', lat: 17.3850, lon: 78.4867, country: 'IN' },
  { name: 'Chennai', lat: 13.0827, lon: 80.2707, country: 'IN' },
  { name: 'Kolkata', lat: 22.5726, lon: 88.3639, country: 'IN' },
  { name: 'Pune', lat: 18.5204, lon: 73.8567, country: 'IN' },
  { name: 'Ahmedabad', lat: 23.0225, lon: 72.5714, country: 'IN' },
  { name: 'Jaipur', lat: 26.9124, lon: 75.7873, country: 'IN' },
  { name: 'Surat', lat: 21.1702, lon: 72.8311, country: 'IN' }
];

// Amount ranges by category (in paise)
const AMOUNT_RANGES = {
  '5411': { min: 5000, max: 500000 },    // Grocery: ₹50-₹5000
  '5812': { min: 2000, max: 200000 },    // Restaurant: ₹20-₹2000
  '4121': { min: 1000, max: 100000 },    // Transport: ₹10-₹1000
  '6011': { min: 10000, max: 1000000 },  // ATM: ₹100-₹10000
  '5311': { min: 10000, max: 1000000 },  // Retail: ₹100-₹10000
  '7995': { min: 5000, max: 500000 },    // Gaming: ₹50-₹5000
  '5814': { min: 1000, max: 100000 },    // Fast Food: ₹10-₹1000
  '5999': { min: 1000, max: 200000 },    // Misc: ₹10-₹2000
  '7011': { min: 500000, max: 10000000 }, // Hotel: ₹5000-₹100000
  '4511': { min: 100000, max: 5000000 }   // Airlines: ₹1000-₹50000
};

function randomBetween(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomChoice(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function randomDate(start, end) {
  return new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));
}

function generateTransaction(transactionId, customerId, cardId) {
  const mcc = randomChoice(Object.keys(MERCHANT_CATEGORIES));
  const merchant = randomChoice(MERCHANT_CATEGORIES[mcc]);
  const amountRange = AMOUNT_RANGES[mcc];
  const amount = -randomBetween(amountRange.min, amountRange.max); // Negative for debits
  const city = randomChoice(INDIAN_CITIES);
  const ts = randomDate(START_DATE, END_DATE);
  
  return {
    id: `txn_${transactionId.toString().padStart(6, '0')}`,
    customerId: `cust_${customerId.toString().padStart(3, '0')}`,
    cardId: `card_${cardId.toString().padStart(3, '0')}`,
    mcc,
    merchant,
    amount,
    currency: 'INR',
    ts: ts.toISOString(),
    deviceId: `dev_${randomBetween(1, 100)}`,
    geo: {
      lat: city.lat + (Math.random() - 0.5) * 0.1, // Add some randomness
      lon: city.lon + (Math.random() - 0.5) * 0.1,
      country: city.country,
      city: city.name
    },
    status: Math.random() > 0.05 ? 'captured' : 'pending' // 5% pending
  };
}

function generateCustomers() {
  const customers = [];
  for (let i = 1; i <= CUSTOMER_COUNT; i++) {
    const riskFlags = [];
    if (Math.random() < 0.1) riskFlags.push('high_velocity');
    if (Math.random() < 0.05) riskFlags.push('geo_anomaly');
    if (Math.random() < 0.08) riskFlags.push('device_change');
    if (Math.random() < 0.03) riskFlags.push('chargeback_history');
    
    customers.push({
      id: `cust_${i.toString().padStart(3, '0')}`,
      name: `Customer ${i}`,
      email_masked: `c***${i}@e***.com`,
      risk_flags: riskFlags,
      created_at: randomDate(new Date('2023-01-01'), new Date('2024-01-01')).toISOString(),
      status: 'active'
    });
  }
  return customers;
}

function generateCards() {
  const cards = [];
  const networks = ['VISA', 'MASTERCARD', 'AMEX'];
  
  for (let i = 1; i <= CUSTOMER_COUNT; i++) {
    cards.push({
      id: `card_${i.toString().padStart(3, '0')}`,
      customerId: `cust_${i.toString().padStart(3, '0')}`,
      last4: randomBetween(1000, 9999).toString(),
      status: Math.random() > 0.02 ? 'active' : 'frozen', // 2% frozen
      network: randomChoice(networks),
      created_at: randomDate(new Date('2023-01-01'), new Date('2024-01-01')).toISOString()
    });
  }
  return cards;
}

function generateTransactions() {
  const transactions = [];
  let transactionId = 1;
  
  for (let customerId = 1; customerId <= CUSTOMER_COUNT; customerId++) {
    for (let i = 0; i < TRANSACTIONS_PER_CUSTOMER; i++) {
      transactions.push(generateTransaction(transactionId, customerId, customerId));
      transactionId++;
    }
    
    if (customerId % 100 === 0) {
      console.log(`Generated transactions for ${customerId}/${CUSTOMER_COUNT} customers`);
    }
  }
  
  return transactions;
}

function main() {
  console.log('Generating test data...');
  
  const outputDir = path.join(__dirname, '../../fixtures');
  
  // Generate and save customers
  console.log('Generating customers...');
  const customers = generateCustomers();
  fs.writeFileSync(
    path.join(outputDir, 'customers_large.json'),
    JSON.stringify(customers, null, 2)
  );
  
  // Generate and save cards
  console.log('Generating cards...');
  const cards = generateCards();
  fs.writeFileSync(
    path.join(outputDir, 'cards_large.json'),
    JSON.stringify(cards, null, 2)
  );
  
  // Generate and save transactions
  console.log('Generating transactions...');
  const transactions = generateTransactions();
  fs.writeFileSync(
    path.join(outputDir, 'transactions_large.json'),
    JSON.stringify(transactions, null, 2)
  );
  
  console.log(`Generated ${customers.length} customers, ${cards.length} cards, ${transactions.length} transactions`);
  console.log('Data saved to fixtures/ directory');
}

if (require.main === module) {
  main();
}

module.exports = { generateCustomers, generateCards, generateTransactions };
