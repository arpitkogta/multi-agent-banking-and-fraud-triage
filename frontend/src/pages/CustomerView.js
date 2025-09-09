import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Alert,
  CircularProgress
} from '@mui/material';
import axios from 'axios';

function CustomerView() {
  const { id } = useParams();
  const [customer, setCustomer] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [insights, setInsights] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (id) {
      loadCustomerData();
    }
  }, [id]);

  const loadCustomerData = async () => {
    try {
      setLoading(true);
      
      // Load customer transactions
      const txnResponse = await axios.get(`/api/customer/${id}/transactions?last=90`);
      setTransactions(txnResponse.data.transactions || []);

      // Load customer insights
      const insightsResponse = await axios.get(`/api/customer/${id}/insights/summary`);
      setInsights(insightsResponse.data);

      // Mock customer data
      setCustomer({
        id: id,
        name: `Customer ${id}`,
        emailMasked: `c***@e***.com`,
        status: 'active'
      });

    } catch (err) {
      setError('Failed to load customer data');
      console.error('Customer view error:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatAmount = (amount) => {
    return `₹${(Math.abs(amount) / 100).toLocaleString()}`;
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
        <Typography sx={{ ml: 2 }}>Loading customer data...</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Customer View: {customer?.name || id}
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Customer Info */}
      {customer && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Customer Information
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Typography><strong>ID:</strong> {customer.id}</Typography>
                <Typography><strong>Name:</strong> {customer.name}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography><strong>Email:</strong> {customer.emailMasked}</Typography>
                <Typography><strong>Status:</strong> 
                  <Chip label={customer.status} color="success" size="small" sx={{ ml: 1 }} />
                </Typography>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Insights Summary */}
      {insights && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Insights & Reports
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={3}>
                <Typography color="textSecondary">Total Spend (90d)</Typography>
                <Typography variant="h6">
                  {formatAmount(insights.totalSpend || 0)}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={3}>
                <Typography color="textSecondary">Transactions</Typography>
                <Typography variant="h6">
                  {insights.transactionCount || 0}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={3}>
                <Typography color="textSecondary">Cities Visited</Typography>
                <Typography variant="h6">
                  {insights.riskIndicators?.citiesVisited || 0}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={3}>
                <Typography color="textSecondary">Devices Used</Typography>
                <Typography variant="h6">
                  {insights.riskIndicators?.devicesUsed || 0}
                </Typography>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Transactions Timeline */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Recent Transactions (Last 90 Days)
          </Typography>
          
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Date</TableCell>
                  <TableCell>Merchant</TableCell>
                  <TableCell>Amount</TableCell>
                  <TableCell>MCC</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Location</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {transactions.map((txn) => (
                  <TableRow key={txn.id}>
                    <TableCell>{formatDate(txn.ts)}</TableCell>
                    <TableCell>{txn.merchant}</TableCell>
                    <TableCell>{formatAmount(txn.amount)}</TableCell>
                    <TableCell>{txn.mcc}</TableCell>
                    <TableCell>
                      <Chip 
                        label={txn.status} 
                        color={txn.status === 'captured' ? 'success' : 'warning'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      {txn.geo?.city || 'Unknown'}
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

export default CustomerView;
