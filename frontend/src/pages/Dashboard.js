import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  TextField,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Alert
} from '@mui/material';
import axios from 'axios';

function Dashboard() {
  const [kpis, setKpis] = useState({
    totalSpend: 0,
    highRiskAlerts: 0,
    disputesOpened: 0,
    avgTriageTime: 0
  });
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      
      // Mock data for now - in real implementation would call APIs
      setKpis({
        totalSpend: 1250000, // ₹12,500
        highRiskAlerts: 23,
        disputesOpened: 8,
        avgTriageTime: 3.2
      });

      setAlerts([
        {
          id: 'alert_001',
          customerId: 'cust_017',
          txnId: 'txn_01001',
          riskScore: 'high',
          merchant: 'ABC Mart',
          amount: 4999,
          timestamp: '2025-01-13T13:02:11Z',
          status: 'pending'
        },
        {
          id: 'alert_002',
          customerId: 'cust_001',
          txnId: 'txn_01003',
          riskScore: 'medium',
          merchant: 'QuickCab',
          amount: 2500,
          timestamp: '2025-01-12T10:15:00Z',
          status: 'in_progress'
        }
      ]);

    } catch (err) {
      setError('Failed to load dashboard data');
      console.error('Dashboard error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleTriageAlert = async (alertId) => {
    try {
      const alert = alerts.find(a => a.id === alertId);
      if (!alert) return;

      const response = await axios.post('/api/triage', {
        customerId: alert.customerId,
        suspectTxnId: alert.txnId,
        alertType: 'fraud_detection',
        userMessage: 'Please review this suspicious transaction'
      });

      console.log('Triage response:', response.data);
      
      // Update alert status
      setAlerts(prev => prev.map(a => 
        a.id === alertId ? { ...a, status: 'completed' } : a
      ));

    } catch (err) {
      console.error('Triage error:', err);
      setError('Failed to process triage request');
    }
  };

  const getRiskColor = (risk) => {
    switch (risk) {
      case 'high': return 'error';
      case 'medium': return 'warning';
      case 'low': return 'success';
      default: return 'default';
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <Typography>Loading dashboard...</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* KPIs */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Spend (90d)
              </Typography>
              <Typography variant="h5">
                ₹{(kpis.totalSpend / 100).toLocaleString()}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                High Risk Alerts
              </Typography>
              <Typography variant="h5" color="error">
                {kpis.highRiskAlerts}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Disputes Opened
              </Typography>
              <Typography variant="h5" color="warning">
                {kpis.disputesOpened}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Avg Triage Time
              </Typography>
              <Typography variant="h5">
                {kpis.avgTriageTime}s
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Fraud Triage Table */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Fraud Triage Queue
          </Typography>
          
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Customer</TableCell>
                  <TableCell>Transaction</TableCell>
                  <TableCell>Merchant</TableCell>
                  <TableCell>Amount</TableCell>
                  <TableCell>Risk Score</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {alerts.map((alert) => (
                  <TableRow key={alert.id}>
                    <TableCell>{alert.customerId}</TableCell>
                    <TableCell>{alert.txnId}</TableCell>
                    <TableCell>{alert.merchant}</TableCell>
                    <TableCell>₹{(alert.amount / 100).toLocaleString()}</TableCell>
                    <TableCell>
                      <Chip 
                        label={alert.riskScore} 
                        color={getRiskColor(alert.riskScore)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={alert.status} 
                        variant="outlined"
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="contained"
                        size="small"
                        onClick={() => handleTriageAlert(alert.id)}
                        disabled={alert.status === 'completed'}
                      >
                        Triage
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
}

export default Dashboard;
