#!/usr/bin/env node

/**
 * Performance Testing Script for Aegis Support
 * Tests the system with 1M+ transactions and measures p95 latency
 */

const axios = require('axios');
const fs = require('fs');
const path = require('path');

const API_BASE = 'http://localhost:8080/api';
const TEST_CUSTOMER_ID = 'cust_001';
const NUM_REQUESTS = 1000; // Number of requests to test
const CONCURRENT_REQUESTS = 10; // Concurrent requests

class PerformanceTester {
    constructor() {
        this.results = [];
        this.errors = [];
    }

    async generateTestData() {
        console.log('📊 Generating test data...');
        
        // Load existing fixtures
        const fixturesPath = path.join(__dirname, '..', 'fixtures');
        const customers = JSON.parse(fs.readFileSync(path.join(fixturesPath, 'customers.json'), 'utf8'));
        const transactions = JSON.parse(fs.readFileSync(path.join(fixturesPath, 'transactions.json'), 'utf8'));
        
        console.log(`✅ Loaded ${customers.length} customers and ${transactions.length} transactions`);
        return { customers, transactions };
    }

    async testCustomerTransactionsEndpoint() {
        console.log(`🚀 Testing /customer/${TEST_CUSTOMER_ID}/transactions endpoint...`);
        console.log(`📈 Running ${NUM_REQUESTS} requests with ${CONCURRENT_REQUESTS} concurrent connections`);
        
        const startTime = Date.now();
        const promises = [];
        
        for (let i = 0; i < NUM_REQUESTS; i++) {
            if (promises.length >= CONCURRENT_REQUESTS) {
                await Promise.all(promises);
                promises.length = 0;
            }
            
            promises.push(this.makeRequest(i));
        }
        
        // Wait for remaining requests
        if (promises.length > 0) {
            await Promise.all(promises);
        }
        
        const totalTime = Date.now() - startTime;
        this.analyzeResults(totalTime);
    }

    async makeRequest(requestId) {
        const startTime = Date.now();
        
        try {
            const response = await axios.get(`${API_BASE}/customer/${TEST_CUSTOMER_ID}/transactions?last=90`, {
                timeout: 5000,
                headers: {
                    'X-API-Key': 'test-key' // Add API key if required
                }
            });
            
            const duration = Date.now() - startTime;
            this.results.push({
                requestId,
                duration,
                status: response.status,
                success: true
            });
            
        } catch (error) {
            const duration = Date.now() - startTime;
            this.results.push({
                requestId,
                duration,
                status: error.response?.status || 0,
                success: false,
                error: error.message
            });
            this.errors.push(error);
        }
    }

    analyzeResults(totalTime) {
        console.log('\n📊 PERFORMANCE TEST RESULTS');
        console.log('='.repeat(50));
        
        const successfulRequests = this.results.filter(r => r.success);
        const failedRequests = this.results.filter(r => !r.success);
        
        console.log(`✅ Successful requests: ${successfulRequests.length}`);
        console.log(`❌ Failed requests: ${failedRequests.length}`);
        console.log(`⏱️  Total time: ${totalTime}ms`);
        console.log(`📈 Requests per second: ${(this.results.length / (totalTime / 1000)).toFixed(2)}`);
        
        if (successfulRequests.length > 0) {
            const durations = successfulRequests.map(r => r.duration).sort((a, b) => a - b);
            const p50 = durations[Math.floor(durations.length * 0.5)];
            const p95 = durations[Math.floor(durations.length * 0.95)];
            const p99 = durations[Math.floor(durations.length * 0.99)];
            const avg = durations.reduce((a, b) => a + b, 0) / durations.length;
            const min = Math.min(...durations);
            const max = Math.max(...durations);
            
            console.log('\n📈 LATENCY STATISTICS');
            console.log('-'.repeat(30));
            console.log(`Average: ${avg.toFixed(2)}ms`);
            console.log(`Min: ${min}ms`);
            console.log(`Max: ${max}ms`);
            console.log(`P50: ${p50}ms`);
            console.log(`P95: ${p95}ms`);
            console.log(`P99: ${p99}ms`);
            
            // Check if p95 meets requirement (≤ 100ms)
            if (p95 <= 100) {
                console.log(`\n✅ P95 latency requirement MET: ${p95}ms ≤ 100ms`);
            } else {
                console.log(`\n❌ P95 latency requirement FAILED: ${p95}ms > 100ms`);
            }
        }
        
        if (failedRequests.length > 0) {
            console.log('\n❌ ERROR SUMMARY');
            console.log('-'.repeat(20));
            const errorCounts = {};
            failedRequests.forEach(r => {
                const error = r.error || `HTTP ${r.status}`;
                errorCounts[error] = (errorCounts[error] || 0) + 1;
            });
            
            Object.entries(errorCounts).forEach(([error, count]) => {
                console.log(`${error}: ${count} occurrences`);
            });
        }
        
        // Save detailed results
        this.saveResults();
    }

    saveResults() {
        const resultsPath = path.join(__dirname, 'performance-results.json');
        const report = {
            timestamp: new Date().toISOString(),
            testConfig: {
                numRequests: NUM_REQUESTS,
                concurrentRequests: CONCURRENT_REQUESTS,
                customerId: TEST_CUSTOMER_ID
            },
            summary: {
                totalRequests: this.results.length,
                successfulRequests: this.results.filter(r => r.success).length,
                failedRequests: this.results.filter(r => !r.success).length
            },
            results: this.results
        };
        
        fs.writeFileSync(resultsPath, JSON.stringify(report, null, 2));
        console.log(`\n💾 Detailed results saved to: ${resultsPath}`);
    }

    async testTriageEndpoint() {
        console.log('\n🤖 Testing triage endpoint performance...');
        
        const triageRequests = [
            {
                customerId: 'cust_001',
                suspectTxnId: 'txn_01001',
                userMessage: 'Card lost, need to freeze'
            },
            {
                customerId: 'cust_017',
                suspectTxnId: 'txn_01002',
                userMessage: 'Unauthorized charge detected'
            }
        ];
        
        for (const request of triageRequests) {
            const startTime = Date.now();
            try {
                const response = await axios.post(`${API_BASE}/triage`, request, {
                    timeout: 10000,
                    headers: {
                        'Content-Type': 'application/json',
                        'X-API-Key': 'test-key'
                    }
                });
                
                const duration = Date.now() - startTime;
                console.log(`✅ Triage request completed in ${duration}ms`);
                console.log(`   Risk Score: ${response.data.riskScore}`);
                console.log(`   Action: ${response.data.recommendedAction}`);
                
                if (duration <= 5000) {
                    console.log(`   ✅ E2E requirement MET: ${duration}ms ≤ 5000ms`);
                } else {
                    console.log(`   ❌ E2E requirement FAILED: ${duration}ms > 5000ms`);
                }
                
            } catch (error) {
                console.log(`❌ Triage request failed: ${error.message}`);
            }
        }
    }

    async run() {
        console.log('🎯 Aegis Support Performance Test');
        console.log('================================\n');
        
        try {
            await this.generateTestData();
            await this.testCustomerTransactionsEndpoint();
            await this.testTriageEndpoint();
            
            console.log('\n🎉 Performance testing completed!');
            
        } catch (error) {
            console.error('❌ Performance test failed:', error.message);
            process.exit(1);
        }
    }
}

// Run the performance test
if (require.main === module) {
    const tester = new PerformanceTester();
    tester.run();
}

module.exports = PerformanceTester;
