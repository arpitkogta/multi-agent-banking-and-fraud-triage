import { useQuery, useMutation, QueryClient } from 'react-query';

// Create query client with optimized settings
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60000, // Data stays fresh for 1 minute
      cacheTime: 3600000, // Cache persists for 1 hour
      retry: 2,
      refetchOnWindowFocus: false,
      refetchOnMount: true,
      keepPreviousData: true, // Keep showing old data while fetching
      suspense: false, // Don't use React Suspense
    },
  },
});

export const useCustomerProfile = (customerId) => {
  return useQuery(
    ['customer', customerId],
    () => fetch(`/api/customer/${customerId}`).then(res => res.json()),
    {
      enabled: Boolean(customerId),
      staleTime: 300000, // Profile data stays fresh for 5 minutes
    }
  );
};

export const useRecentTransactions = (customerId, days = 90) => {
  return useQuery(
    ['transactions', customerId, days],
    () => fetch(`/api/customer/${customerId}/transactions?last=${days}`).then(res => res.json()),
    {
      enabled: Boolean(customerId),
      keepPreviousData: true,
      // Use optimized settings for transaction list
      staleTime: 30000, // Transaction data stays fresh for 30 seconds
      refetchInterval: 60000, // Poll for updates every minute
      select: (data) => ({
        ...data,
        // Pre-compute derived data to avoid repeated calculations
        transactions: data.transactions.map(txn => ({
          ...txn,
          formattedAmount: `₹${(Math.abs(txn.amount) / 100).toLocaleString()}`,
          formattedDate: new Date(txn.ts).toLocaleDateString()
        }))
      })
    }
  );
};

export const useTriageWorkflow = () => {
  return useMutation(
    (data) => fetch('/api/triage', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-API-Key': 'test-key'
      },
      body: JSON.stringify(data)
    }).then(res => res.json()),
    {
      // Optimistic updates configuration
      onMutate: async (data) => {
        // Cancel outgoing refetches to avoid race conditions
        await queryClient.cancelQueries(['transactions', data.customerId]);
        
        // Return rollback data
        return { 
          previousTransactions: queryClient.getQueryData(['transactions', data.customerId])
        };
      },
      onError: (err, data, context) => {
        // Rollback on error
        if (context?.previousTransactions) {
          queryClient.setQueryData(
            ['transactions', data.customerId],
            context.previousTransactions
          );
        }
      },
      onSettled: (data) => {
        // Refetch after mutation completes
        queryClient.invalidateQueries(['transactions', data.customerId]);
      }
    }
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