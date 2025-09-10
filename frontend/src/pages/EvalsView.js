import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
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
  LinearProgress
} from '@mui/material';
import axios from 'axios';

function EvalsView() {
  const [evalResults, setEvalResults] = useState(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState(null);

  const runEvaluations = async () => {
    try {
      setRunning(true);
      setError(null);
      
      const response = await axios.post('http://localhost:8080/api/evals/run', {}, {
        headers: {
          'X-API-Key': 'test-key',
          'Content-Type': 'application/json'
        }
      });
      
      setEvalResults(response.data);
      setRunning(false);

    } catch (err) {
      setError('Failed to run evaluations: ' + (err.response?.data?.error || err.message));
      setRunning(false);
      console.error('Evaluation error:', err);
    }
  };

  const loadEvaluationResults = async () => {
    try {
      setError(null);
      
      const response = await axios.get('http://localhost:8080/api/evals/results', {
        headers: {
          'X-API-Key': 'test-key'
        }
      });
      
      setEvalResults(response.data);

    } catch (err) {
      setError('Failed to load evaluation results: ' + (err.response?.data?.error || err.message));
      console.error('Load evaluation error:', err);
    }
  };

  useEffect(() => {
    loadEvaluationResults();
  }, []);

  const getStatusColor = (status) => {
    switch (status) {
      case 'PASS': return 'success';
      case 'FAIL': return 'error';
      case 'SKIP': return 'warning';
      default: return 'default';
    }
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Evaluation Results
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Run Evaluations Button */}
      <Box sx={{ mb: 3 }}>
        <Button
          variant="contained"
          onClick={runEvaluations}
          disabled={running}
          size="large"
        >
          {running ? 'Running Evaluations...' : 'Run Evaluations'}
        </Button>
      </Box>

      {running && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="body1" gutterBottom>
            Running evaluation test suite...
          </Typography>
          <LinearProgress />
        </Box>
      )}

      {/* Evaluation Results */}
      {evalResults && (
        <Box>
          {/* Summary Metrics */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography color="textSecondary" gutterBottom>
                    Task Success Rate
                  </Typography>
                  <Typography variant="h4" color="success">
                    {evalResults.taskSuccessRate}%
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography color="textSecondary" gutterBottom>
                    Fallback Rate
                  </Typography>
                  <Typography variant="h4" color="warning">
                    {evalResults.fallbackRate}%
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography color="textSecondary" gutterBottom>
                    Avg Latency (P50)
                  </Typography>
                  <Typography variant="h4">
                    {evalResults.avgLatencyP50}s
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography color="textSecondary" gutterBottom>
                    Avg Latency (P95)
                  </Typography>
                  <Typography variant="h4">
                    {evalResults.avgLatencyP95}s
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Policy Denials */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Policy Denials by Rule
              </Typography>
              <Grid container spacing={2}>
                {Object.entries(evalResults.policyDenials).map(([rule, count]) => (
                  <Grid item xs={12} sm={4} key={rule}>
                    <Typography variant="body1">
                      <strong>{rule.replace('_', ' ')}:</strong> {count}
                    </Typography>
                  </Grid>
                ))}
              </Grid>
            </CardContent>
          </Card>

          {/* Confusion Matrix */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Risk Classification Confusion Matrix
              </Typography>
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell></TableCell>
                      <TableCell align="center">Low</TableCell>
                      <TableCell align="center">Medium</TableCell>
                      <TableCell align="center">High</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {Object.entries(evalResults.confusionMatrix).map(([actual, predictions]) => (
                      <TableRow key={actual}>
                        <TableCell component="th" scope="row">
                          <strong>{actual}</strong>
                        </TableCell>
                        <TableCell align="center">{predictions.low}</TableCell>
                        <TableCell align="center">{predictions.medium}</TableCell>
                        <TableCell align="center">{predictions.high}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>

          {/* Test Cases */}
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Test Case Results
              </Typography>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Test Case</TableCell>
                      <TableCell>Name</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Duration (s)</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {evalResults.testCases.map((testCase) => (
                      <TableRow key={testCase.id}>
                        <TableCell>{testCase.id}</TableCell>
                        <TableCell>{testCase.name}</TableCell>
                        <TableCell>
                          <Chip 
                            label={testCase.status} 
                            color={getStatusColor(testCase.status)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{testCase.duration}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Box>
      )}
    </Box>
  );
}

export default EvalsView;
