#!/usr/bin/env node

/**
 * Evaluation Runner for Aegis Support
 * Runs the 12 golden test cases and generates evaluation report
 */

const axios = require('axios');
const fs = require('fs');
const path = require('path');

const API_BASE = 'http://localhost:8080/api';
const FIXTURES_DIR = path.join(__dirname, '..', 'fixtures', 'evals');

class EvaluationRunner {
    constructor() {
        this.results = [];
        this.metrics = {
            totalTests: 0,
            passedTests: 0,
            failedTests: 0,
            fallbackRate: 0,
            avgLatency: 0,
            policyDenials: {},
            confusionMatrix: { low: 0, medium: 0, high: 0 }
        };
    }

    async loadTestCases() {
        console.log('📁 Loading evaluation test cases...');
        
        const testFiles = fs.readdirSync(FIXTURES_DIR)
            .filter(file => file.endsWith('.json'))
            .sort();
        
        const testCases = [];
        for (const file of testFiles) {
            const filePath = path.join(FIXTURES_DIR, file);
            const testCase = JSON.parse(fs.readFileSync(filePath, 'utf8'));
            testCase.id = file.replace('.json', '');
            testCases.push(testCase);
        }
        
        console.log(`✅ Loaded ${testCases.length} test cases`);
        return testCases;
    }

    async runTestCase(testCase) {
        console.log(`\n🧪 Running test case: ${testCase.id}`);
        console.log(`📝 Description: ${testCase.description}`);
        
        const startTime = Date.now();
        let result = {
            id: testCase.id,
            description: testCase.description,
            expected: testCase.expected,
            actual: null,
            passed: false,
            latency: 0,
            error: null,
            fallbackUsed: false,
            policyDenied: false
        };
        
        try {
            // Execute the triage request
            const response = await axios.post(`${API_BASE}/triage`, {
                customerId: testCase.input.customerId,
                suspectTxnId: testCase.input.suspectTxnId,
                alertType: testCase.input.alertType,
                userMessage: testCase.input.userMessage
            }, {
                timeout: 10000,
                headers: {
                    'Content-Type': 'application/json',
                    'X-API-Key': 'test-key'
                }
            });
            
            result.latency = Date.now() - startTime;
            result.actual = {
                riskScore: response.data.riskScore,
                recommendedAction: response.data.recommendedAction,
                reasons: response.data.reasons,
                requiresOTP: response.data.requiresOTP,
                fallbackUsed: response.data.fallbackUsed
            };
            
            // Check if fallback was used
            if (response.data.fallbackUsed) {
                result.fallbackUsed = true;
                this.metrics.fallbackRate++;
            }
            
            // Evaluate the result
            result.passed = this.evaluateResult(testCase.expected, result.actual);
            
            // Update confusion matrix
            if (result.actual.riskScore) {
                this.metrics.confusionMatrix[result.actual.riskScore]++;
            }
            
            console.log(`   ⏱️  Latency: ${result.latency}ms`);
            console.log(`   🎯 Risk Score: ${result.actual.riskScore}`);
            console.log(`   🔧 Action: ${result.actual.recommendedAction}`);
            console.log(`   🔄 Fallback: ${result.fallbackUsed ? 'Yes' : 'No'}`);
            console.log(`   ✅ Result: ${result.passed ? 'PASS' : 'FAIL'}`);
            
        } catch (error) {
            result.latency = Date.now() - startTime;
            result.error = error.message;
            result.passed = false;
            
            console.log(`   ❌ Error: ${error.message}`);
            console.log(`   ✅ Result: FAIL`);
        }
        
        this.results.push(result);
        return result;
    }

    evaluateResult(expected, actual) {
        // Check risk score
        if (expected.riskScore && actual.riskScore !== expected.riskScore) {
            return false;
        }
        
        // Check recommended action
        if (expected.recommendedAction && actual.recommendedAction !== expected.recommendedAction) {
            return false;
        }
        
        // Check if OTP is required
        if (expected.requiresOTP !== undefined && actual.requiresOTP !== expected.requiresOTP) {
            return false;
        }
        
        // Check for specific reasons
        if (expected.reasons) {
            for (const reason of expected.reasons) {
                if (!actual.reasons || !actual.reasons.includes(reason)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    async testRateLimiting() {
        console.log('\n🚦 Testing rate limiting (case_008)...');
        
        const requests = [];
        for (let i = 0; i < 10; i++) {
            requests.push(
                axios.post(`${API_BASE}/triage`, {
                    customerId: 'cust_001',
                    suspectTxnId: 'txn_01001',
                    userMessage: 'Rate limit test'
                }, {
                    timeout: 5000,
                    headers: {
                        'Content-Type': 'application/json',
                        'X-API-Key': 'test-key'
                    }
                }).catch(error => ({ error }))
            );
        }
        
        const responses = await Promise.all(requests);
        const rateLimited = responses.filter(r => r.error?.response?.status === 429);
        
        console.log(`   📊 Rate limited requests: ${rateLimited.length}/10`);
        
        if (rateLimited.length > 0) {
            console.log('   ✅ Rate limiting working correctly');
            return true;
        } else {
            console.log('   ❌ Rate limiting not working');
            return false;
        }
    }

    async testPiiRedaction() {
        console.log('\n🔒 Testing PII redaction (case_010)...');
        
        try {
            const response = await axios.post(`${API_BASE}/triage`, {
                customerId: 'cust_017',
                suspectTxnId: 'txn_01001',
                alertType: 'pii_test',
                userMessage: 'My card number is 4111111111111111, please help me'
            }, {
                timeout: 5000,
                headers: {
                    'Content-Type': 'application/json',
                    'X-API-Key': 'test-key'
                }
            });
            
            // Check all required assertions
            const responseStr = JSON.stringify(response.data);
            const results = {
                piiDetected: response.data.piiDetected === true,
                redacted: !responseStr.includes('4111111111111111'),
                neverEchoed: !responseStr.includes('4111111111111111'),
                redactorApplied: response.data.traceSteps?.includes('redaction_applied') || false,
                maskedLogs: response.data.traceSteps?.includes('pii_detection') || false
            };
            
            const allAssertionsPassed = Object.values(results).every(r => r === true);
            
            if (allAssertionsPassed) {
                console.log('   ✅ PII redaction working correctly');
                console.log('   📋 All assertions passed:');
                Object.entries(results).forEach(([key, value]) => {
                    console.log(`      - ${key}: ${value ? '✅' : '❌'}`);
                });
                return true;
            } else {
                console.log('   ❌ PII not properly redacted');
                console.log('   📋 Failed assertions:');
                Object.entries(results).forEach(([key, value]) => {
                    if (!value) {
                        console.log(`      - ${key}: ❌`);
                    }
                });
                return false;
            }
            
        } catch (error) {
            console.log(`   ❌ PII test failed: ${error.message}`);
            return false;
        }
    }

    generateReport() {
        console.log('\n📊 EVALUATION REPORT');
        console.log('='.repeat(50));
        
        this.metrics.totalTests = this.results.length;
        this.metrics.passedTests = this.results.filter(r => r.passed).length;
        this.metrics.failedTests = this.results.filter(r => !r.passed).length;
        
        const avgLatency = this.results.reduce((sum, r) => sum + r.latency, 0) / this.results.length;
        this.metrics.avgLatency = avgLatency;
        
        console.log(`📈 Task Success Rate: ${((this.metrics.passedTests / this.metrics.totalTests) * 100).toFixed(1)}%`);
        console.log(`🔄 Fallback Rate: ${((this.metrics.fallbackRate / this.metrics.totalTests) * 100).toFixed(1)}%`);
        console.log(`⏱️  Avg Agent Latency: ${avgLatency.toFixed(2)}ms`);
        
        // Calculate percentiles
        const latencies = this.results.map(r => r.latency).sort((a, b) => a - b);
        const p50 = latencies[Math.floor(latencies.length * 0.5)];
        const p95 = latencies[Math.floor(latencies.length * 0.95)];
        
        console.log(`📊 P50 Latency: ${p50}ms`);
        console.log(`📊 P95 Latency: ${p95}ms`);
        
        // Confusion matrix
        console.log('\n🎯 Risk Score Distribution:');
        console.log(`   Low: ${this.metrics.confusionMatrix.low}`);
        console.log(`   Medium: ${this.metrics.confusionMatrix.medium}`);
        console.log(`   High: ${this.metrics.confusionMatrix.high}`);
        
        // Failed tests
        const failedTests = this.results.filter(r => !r.passed);
        if (failedTests.length > 0) {
            console.log('\n❌ Failed Tests:');
            failedTests.forEach(test => {
                console.log(`   ${test.id}: ${test.description}`);
                if (test.error) {
                    console.log(`     Error: ${test.error}`);
                }
            });
        }
        
        // Save detailed report
        this.saveReport();
    }

    saveReport() {
        const reportPath = path.join(__dirname, 'eval-report.json');
        const report = {
            timestamp: new Date().toISOString(),
            metrics: this.metrics,
            results: this.results,
            summary: {
                successRate: (this.metrics.passedTests / this.metrics.totalTests) * 100,
                fallbackRate: (this.metrics.fallbackRate / this.metrics.totalTests) * 100,
                avgLatency: this.metrics.avgLatency
            }
        };
        
        fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
        console.log(`\n💾 Detailed report saved to: ${reportPath}`);
    }

    async run() {
        console.log('🎯 Aegis Support Evaluation Runner');
        console.log('==================================\n');
        
        try {
            const testCases = await this.loadTestCases();
            
            // Run all test cases with delay to avoid rate limiting
            for (const testCase of testCases) {
                await this.runTestCase(testCase);
                // Add delay between requests to avoid rate limiting (5 requests per second)
                console.log('   ⏳ Waiting 2 seconds to avoid rate limiting...');
                await new Promise(resolve => setTimeout(resolve, 2000));
            }
            
            // Run special tests with delay
            console.log('\n⏳ Waiting 3 seconds before special tests...');
            await new Promise(resolve => setTimeout(resolve, 3000));
            await this.testRateLimiting();
            
            console.log('⏳ Waiting 3 seconds before PII test...');
            await new Promise(resolve => setTimeout(resolve, 3000));
            await this.testPiiRedaction();
            
            // Generate report
            this.generateReport();
            
            console.log('\n🎉 Evaluation completed!');
            
        } catch (error) {
            console.error('❌ Evaluation failed:', error.message);
            process.exit(1);
        }
    }
}

// Run the evaluation
if (require.main === module) {
    const runner = new EvaluationRunner();
    runner.run();
}

module.exports = EvaluationRunner;