import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { QueryClientProvider } from 'react-query';
import CssBaseline from '@mui/material/CssBaseline';
import { Box, AppBar, Toolbar, Typography, Button, Container } from '@mui/material';

import { ErrorBoundary } from './components/ErrorBoundary';
import { queryClient } from './hooks/api';
import Dashboard from './pages/Dashboard';
import CustomerView from './pages/CustomerView';
import AlertsQueue from './pages/AlertsQueue';
import EvalsView from './pages/EvalsView';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
        },
      },
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <ErrorBoundary>
          <Router>
            <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
              <AppBar position="static">
                <Toolbar>
                  <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                    Aegis Support
                  </Typography>
                  <Button color="inherit" href="/">Dashboard</Button>
                  <Button color="inherit" href="/alerts">Alerts</Button>
                  <Button color="inherit" href="/evals">Evaluations</Button>
                </Toolbar>
              </AppBar>
              
              <Container maxWidth="xl" sx={{ flexGrow: 1, py: 3 }}>
                <ErrorBoundary>
                  <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/customer/:id" element={<CustomerView />} />
                    <Route path="/alerts" element={<AlertsQueue />} />
                    <Route path="/evals" element={<EvalsView />} />
                  </Routes>
                </ErrorBoundary>
              </Container>
            </Box>
          </Router>
        </ErrorBoundary>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

export default App;
