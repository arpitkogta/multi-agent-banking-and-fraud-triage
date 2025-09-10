import React from 'react';
import { useParams } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Chip,
  Alert
} from '@mui/material';
import { useCustomerProfile, useRecentTransactions } from '../hooks/api';
import { LoadingState } from '../components/LoadingState';
import { DataTable } from '../components/DataTable';

const ROW_HEIGHT = 52;
const TABLE_HEIGHT = 600;

function CustomerView() {
  const { id } = useParams();
  
  // Use React Query for data fetching with caching
  const { 
    data: customer,
    error: customerError,
    isLoading: customerLoading 
  } = useCustomerProfile(id);
  
  const {
    data: transactionData,
    error: transactionError,
    isLoading: transactionLoading
  } = useRecentTransactions(id, 90);

  const formatAmount = (amount) => {
    return `₹${(Math.abs(amount) / 100).toLocaleString()}`;
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString();
  };

  // Transaction table columns configuration
  const columns = [
    { field: 'ts', headerName: 'Date', width: 120, 
      renderCell: (row) => formatDate(row.ts) },
    { field: 'merchant', headerName: 'Merchant', width: 200 },
    { field: 'amount', headerName: 'Amount', width: 120,
      renderCell: (row) => formatAmount(row.amount) },
    { field: 'mcc', headerName: 'MCC', width: 100 },
    { field: 'status', headerName: 'Status', width: 120,
      renderCell: (row) => (
        <Chip 
          label={row.status} 
          color={row.status === 'captured' ? 'success' : 'warning'}
          size="small"
        />
      )
    },
    { field: 'location', headerName: 'Location', width: 150,
      renderCell: (row) => row.geo?.city || 'Unknown' }
  ];

  if (customerLoading || transactionLoading) {
    return <LoadingState message="Loading customer data..." />;
  }

  const error = customerError || transactionError;
  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {error.message || 'Failed to load customer data'}
      </Alert>
    );
  }

  const transactions = transactionData?.transactions || [];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Customer View: {customer?.name || id}
      </Typography>

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
      {transactionData?.insights && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Insights & Reports
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={3}>
                <Typography color="textSecondary">Total Spend (90d)</Typography>
                <Typography variant="h6">
                  {formatAmount(transactionData.insights.totalSpend || 0)}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={3}>
                <Typography color="textSecondary">Transactions</Typography>
                <Typography variant="h6">
                  {transactions.length || 0}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={3}>
                <Typography color="textSecondary">Cities Visited</Typography>
                <Typography variant="h6">
                  {transactionData.insights?.citiesVisited || 0}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={3}>
                <Typography color="textSecondary">Devices Used</Typography>
                <Typography variant="h6">
                  {transactionData.insights?.devicesUsed || 0}
                </Typography>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Transactions Timeline with virtualization */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Recent Transactions (Last 90 Days)
          </Typography>
          
          <DataTable
            columns={columns}
            rows={transactions}
            loading={transactionLoading}
            error={transactionError?.message}
            rowHeight={ROW_HEIGHT}
            containerHeight={TABLE_HEIGHT}
          />
        </CardContent>
      </Card>
    </Box>
  );
}

export default CustomerView;
