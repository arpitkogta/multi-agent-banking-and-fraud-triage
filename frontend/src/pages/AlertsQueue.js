import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Chip,
  Alert,
  CircularProgress
} from '@mui/material';
import axios from 'axios';

function AlertsQueue() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [triageDialogOpen, setTriageDialogOpen] = useState(false);
  const [triageResult, setTriageResult] = useState(null);
  const [userMessage, setUserMessage] = useState('');

  useEffect(() => {
    loadAlerts();
  }, []);

  const loadAlerts = async () => {
    try {
      setLoading(true);
      
      setAlerts([
        {
          id: 'alert_001',
          customerId: 'cust_017',
          txnId: 'txn_01001',
          riskScore: 'high',
          merchant: 'ABC Mart',
          amount: 4999,
          timestamp: '2025-01-13T13:02:11Z',
          status: 'pending',
          reasons: ['unauthorized_transaction', 'high_amount']
        },
        {
          id: 'alert_002',
          customerId: 'cust_001',
          txnId: 'txn_01003',
          riskScore: 'medium',
          merchant: 'QuickCab',
          amount: 2500,
          timestamp: '2025-01-12T10:15:00Z',
          status: 'pending',
          reasons: ['duplicate_transaction']
        },
        {
          id: 'alert_003',
          customerId: 'cust_025',
          txnId: 'txn_01005',
          riskScore: 'high',
          merchant: 'Restaurant XYZ',
          amount: 1500,
          timestamp: '2025-01-11T19:45:00Z',
          status: 'pending',
          reasons: ['chargeback_history', 'device_change']
        }
      ]);

    } catch (err) {
      setError('Failed to load alerts');
      console.error('Alerts error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleTriageAlert = (alert) => {
    setSelectedAlert(alert);
    setTriageDialogOpen(true);
    setTriageResult(null);
    setUserMessage('');
  };

  const executeTriage = async () => {
    if (!selectedAlert) return;

    try {
      const response = await axios.post('/api/triage', {
        customerId: selectedAlert.customerId,
        suspectTxnId: selectedAlert.txnId,
        alertType: 'fraud_detection',
        userMessage: userMessage || 'Please review this suspicious transaction'
      });

      setTriageResult(response.data);
      
      setAlerts(prev => prev.map(a => 
        a.id === selectedAlert.id ? { ...a, status: 'completed' } : a
      ));

    } catch (err) {
      console.error('Triage error:', err);
      setTriageResult({
        error: 'Failed to process triage request',
        riskScore: 'medium',
        recommendedAction: 'contact_customer'
      });
    }
  };

  const closeTriageDialog = () => {
    setTriageDialogOpen(false);
    setSelectedAlert(null);
    setTriageResult(null);
    setUserMessage('');
  };

  const getRiskColor = (risk) => {
    switch (risk) {
      case 'high': return 'error';
      case 'medium': return 'warning';
      case 'low': return 'success';
      default: return 'default';
    }
  };

  const formatAmount = (amount) => {
    return `₹${(amount / 100).toLocaleString()}`;
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
        <Typography sx={{ ml: 2 }}>Loading alerts...</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Alerts Queue
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Alerts List */}
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {alerts.map((alert) => (
          <Card key={alert.id}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <Box>
                  <Typography variant="h6" gutterBottom>
                    Alert {alert.id}
                  </Typography>
                  <Typography color="textSecondary">
                    Customer: {alert.customerId} | Transaction: {alert.txnId}
                  </Typography>
                  <Typography color="textSecondary">
                    Merchant: {alert.merchant} | Amount: {formatAmount(alert.amount)}
                  </Typography>
                  <Typography color="textSecondary" variant="body2">
                    {formatDate(alert.timestamp)}
                  </Typography>
                  
                  <Box sx={{ mt: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    <Chip 
                      label={alert.riskScore} 
                      color={getRiskColor(alert.riskScore)}
                      size="small"
                    />
                    <Chip 
                      label={alert.status} 
                      variant="outlined"
                      size="small"
                    />
                    {alert.reasons.map((reason, index) => (
                      <Chip 
                        key={index}
                        label={reason.replace('_', ' ')} 
                        variant="outlined"
                        size="small"
                        color="info"
                      />
                    ))}
                  </Box>
                </Box>
                
                <Button
                  variant="contained"
                  onClick={() => handleTriageAlert(alert)}
                  disabled={alert.status === 'completed'}
                >
                  {alert.status === 'completed' ? 'Completed' : 'Triage'}
                </Button>
              </Box>
            </CardContent>
          </Card>
        ))}
      </Box>

      {/* Triage Dialog */}
      <Dialog 
        open={triageDialogOpen} 
        onClose={closeTriageDialog}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          Triage Alert: {selectedAlert?.id}
        </DialogTitle>
        <DialogContent>
          {selectedAlert && (
            <Box>
              <Typography variant="body1" gutterBottom>
                <strong>Customer:</strong> {selectedAlert.customerId}
              </Typography>
              <Typography variant="body1" gutterBottom>
                <strong>Transaction:</strong> {selectedAlert.txnId}
              </Typography>
              <Typography variant="body1" gutterBottom>
                <strong>Merchant:</strong> {selectedAlert.merchant}
              </Typography>
              <Typography variant="body1" gutterBottom>
                <strong>Amount:</strong> {formatAmount(selectedAlert.amount)}
              </Typography>
              <Typography variant="body1" gutterBottom>
                <strong>Risk Score:</strong> 
                <Chip 
                  label={selectedAlert.riskScore} 
                  color={getRiskColor(selectedAlert.riskScore)}
                  size="small"
                  sx={{ ml: 1 }}
                />
              </Typography>
              
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Additional Message (Optional)"
                value={userMessage}
                onChange={(e) => setUserMessage(e.target.value)}
                sx={{ mt: 2 }}
                placeholder="Add any additional context for the triage process..."
              />

              {triageResult && (
                <Box sx={{ mt: 3 }}>
                  <Typography variant="h6" gutterBottom>
                    Triage Result
                  </Typography>
                  
                  {triageResult.error ? (
                    <Alert severity="error">{triageResult.error}</Alert>
                  ) : (
                    <Box>
                      <Typography variant="body1">
                        <strong>Risk Score:</strong> 
                        <Chip 
                          label={triageResult.riskScore} 
                          color={getRiskColor(triageResult.riskScore)}
                          size="small"
                          sx={{ ml: 1 }}
                        />
                      </Typography>
                      <Typography variant="body1" sx={{ mt: 1 }}>
                        <strong>Recommended Action:</strong> {triageResult.recommendedAction}
                      </Typography>
                      {triageResult.reasons && (
                        <Typography variant="body1" sx={{ mt: 1 }}>
                          <strong>Reasons:</strong> {triageResult.reasons.join(', ')}
                        </Typography>
                      )}
                      {triageResult.requiresOTP && (
                        <Alert severity="warning" sx={{ mt: 1 }}>
                          OTP verification required for this action
                        </Alert>
                      )}
                    </Box>
                  )}
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeTriageDialog}>
            {triageResult ? 'Close' : 'Cancel'}
          </Button>
          {!triageResult && (
            <Button 
              variant="contained" 
              onClick={executeTriage}
            >
              Execute Triage
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default AlertsQueue;
