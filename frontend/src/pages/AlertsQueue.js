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
  CircularProgress,
  Link
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
  const [otpValue, setOtpValue] = useState('');
  const [otpRequired, setOtpRequired] = useState(false);
  const [actionDisabled, setActionDisabled] = useState(false);
  const [actionTimer, setActionTimer] = useState(null);
  const [kbCitation, setKbCitation] = useState(null);

  useEffect(() => {
    loadAlerts();
    return () => {
      if (actionTimer) clearTimeout(actionTimer);
    };
  }, []);

  const loadAlerts = async () => {
    try {
      setLoading(true);
      const response = await axios.get('/api/alerts');
      setAlerts(response.data || []);
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
    setOtpValue('');
    setOtpRequired(false);
    setKbCitation(null);
  };

  const maskPII = (text) => {
    // Mask PAN numbers and other PII
    return text.replace(/\b\d{16}\b/g, '****************');
  };

  const executeTriage = async () => {
    if (!selectedAlert) return;

    try {
      // Rate limiting check
      if (actionDisabled) {
        setError('Please wait before trying again');
        return;
      }

      const maskedMessage = maskPII(userMessage);
      
      const response = await axios.post('/api/triage', {
        customerId: selectedAlert.customerId,
        suspectTxnId: selectedAlert.txnId,
        alertType: selectedAlert.alertType || 'fraud_detection',
        userMessage: maskedMessage
      });

      if (response.status === 429) {
        const retryAfter = parseInt(response.headers['retry-after'] || '2000');
        setActionDisabled(true);
        setError(`Too many requests. Please wait ${retryAfter/1000} seconds.`);
        
        const timer = setTimeout(() => {
          setActionDisabled(false);
          setError(null);
        }, retryAfter);
        
        setActionTimer(timer);
        return;
      }

      const result = response.data;

      // Handle OTP requirement
      if (result.requiresOTP && !otpValue) {
        setOtpRequired(true);
        setTriageResult(result);
        return;
      }

      // Execute freeze card with OTP
      if (result.recommendedAction === 'freeze_card') {
        const freezeResponse = await axios.post('/api/action/freeze-card', {
          cardId: selectedAlert.cardId,
          otp: otpValue,
          idempotencyKey: `freeze-${selectedAlert.cardId}-${Date.now()}`
        }, {
          headers: {
            'X-API-Key': 'test-key'
          }
        });

        if (freezeResponse.data.status === 'FROZEN') {
          result.status = 'FROZEN';
          result.message = 'Card frozen successfully';
          // Track metric
          await axios.post('/api/metrics/increment', {
            name: 'action_blocked_total',
            labels: { policy: 'otp_required' }
          });
        } else if (freezeResponse.data.status === 'INVALID_OTP') {
          setError('Invalid OTP provided. Please try again.');
          return;
        }
      }

      if (result.recommendedAction === 'open_dispute') {
        // Set the default dispute reason code for unauthorized transactions
        result.reasonCode = '10.4';

        const disputeResponse = await axios.post('/api/action/open-dispute', {
          txnId: selectedAlert.txnId,
          reasonCode: result.reasonCode,
          amount: selectedAlert.amount,
          merchant: selectedAlert.merchant,
          idempotencyKey: `dispute-${selectedAlert.txnId}-${Date.now()}`
        }, {
          headers: {
            'X-API-Key': 'test-key'
          }
        });
        
        if (disputeResponse.data.caseId) {
          result.caseId = disputeResponse.data.caseId;
          result.message = 'Dispute case opened successfully';
          
          // Add KB citation for dispute process
          result.kbArticle = {
            title: 'How Disputes Work',
            anchor: 'kb_disputes',
            content: 'Use reason code 10.4 for unauthorized transactions'
          };
        }
      } else if (result.recommendedAction === 'explain_duplicate') {
        // Handle duplicate transaction explanation
        result.kbArticle = {
          title: 'Understanding Preauthorization Holds',
          anchor: 'kb_preauth',
          content: 'Pending charges are temporary authorizations that will be replaced by final charges'
        };
        result.riskScore = 'low';
        result.message = 'This appears to be a preauthorization hold that will be resolved automatically';
      }

      if (result.kbArticle) {
        setKbCitation({
          title: result.kbArticle.title,
          anchor: result.kbArticle.anchor
        });
      }

      setTriageResult(result);
      
      setAlerts(prev => prev.map(a => 
        a.id === selectedAlert.id ? { ...a, status: 'completed' } : a
      ));

    } catch (err) {
      console.error('Triage error:', err);
      const fallbackResult = {
        error: err.response?.data?.error || 'Failed to process triage request',
        riskScore: 'medium',  // Downgrade risk score on fallback
        recommendedAction: 'contact_customer',
        fallbackUsed: true,
        reason: 'risk_unavailable'
      };
      setTriageResult(fallbackResult);

      // Track fallback usage
      await axios.post('/api/metrics/increment', {
        name: 'risk_fallback_used_total',
        labels: { error_type: 'timeout' }
      });
    }
  };

  const closeTriageDialog = () => {
    setTriageDialogOpen(false);
    setSelectedAlert(null);
    setTriageResult(null);
    setUserMessage('');
    setOtpValue('');
    setOtpRequired(false);
    setKbCitation(null);
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
                    {alert.reasons?.map((reason, index) => (
                      <Chip 
                        key={index}
                        label={reason.replace(/_/g, ' ')} 
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
                  disabled={alert.status === 'completed' || actionDisabled}
                >
                  {alert.status === 'completed' ? 'Completed' : 'Triage'}
                </Button>
              </Box>
            </CardContent>
          </Card>
        ))}
      </Box>

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

              {otpRequired && (
                <TextField
                  fullWidth
                  label="Enter OTP"
                  value={otpValue}
                  onChange={(e) => setOtpValue(e.target.value)}
                  sx={{ mt: 2 }}
                  placeholder="Enter the 6-digit OTP"
                />
              )}

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
                      {triageResult.requiresOTP && !otpValue && (
                        <Alert severity="warning" sx={{ mt: 1 }}>
                          OTP verification required for this action
                        </Alert>
                      )}
                      {triageResult.fallbackUsed && (
                        <Alert severity="info" sx={{ mt: 1 }}>
                          Using fallback risk assessment due to service unavailability
                        </Alert>
                      )}
                      {triageResult.status === 'FROZEN' && (
                        <Alert severity="success" sx={{ mt: 1 }}>
                          Card frozen successfully
                        </Alert>
                      )}
                      {triageResult.caseId && (
                        <Alert severity="success" sx={{ mt: 1 }}>
                          Dispute case {triageResult.caseId} opened successfully
                        </Alert>
                      )}
                      {kbCitation && (
                        <Alert severity="info" sx={{ mt: 1 }}>
                          See also: <Link href={`#${kbCitation.anchor}`}>{kbCitation.title}</Link>
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
          {(!triageResult || (triageResult.requiresOTP && !triageResult.status)) && (
            <Button 
              variant="contained" 
              onClick={executeTriage}
              disabled={actionDisabled || (otpRequired && !otpValue)}
            >
              {otpRequired ? 'Submit OTP' : 'Execute Triage'}
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default AlertsQueue;
