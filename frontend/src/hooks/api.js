import { useQuery, useMutation, QueryClient } from 'react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30000,
      cacheTime: 3600000,
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
});

export const useCustomerProfile = (customerId) => {
  return useQuery(
    ['customer', customerId], 
    () => fetch(`/api/customer/${customerId}`).then(res => res.json()),
    { enabled: Boolean(customerId) }
  );
};

export const useRecentTransactions = (customerId, days = 90) => {
  return useQuery(
    ['transactions', customerId, days],
    () => fetch(`/api/customer/${customerId}/transactions?days=${days}`).then(res => res.json()),
    { enabled: Boolean(customerId) }
  );
};

export const useTriageWorkflow = () => {
  return useMutation((data) => 
    fetch('/api/triage', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }).then(res => res.json())
  );
};

export const useAlertQueue = (status) => {
  return useQuery(
    ['alerts', status],
    () => fetch(`/api/alerts?status=${status}`).then(res => res.json())
  );
};

export const useKnowledgeBase = (query) => {
  return useQuery(
    ['kb', query],
    () => fetch(`/api/kb/search?q=${encodeURIComponent(query)}`).then(res => res.json()),
    { enabled: Boolean(query) }
  );
};