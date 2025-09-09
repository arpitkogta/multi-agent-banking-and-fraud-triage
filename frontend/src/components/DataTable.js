import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Typography,
  Box
} from '@mui/material';
import { useVirtual } from 'react-virtual';

export function DataTable({ 
  columns, 
  rows, 
  loading = false,
  error,
  rowHeight = 52,
  containerHeight = 400
}) {
  const parentRef = React.useRef(null);

  const rowVirtualizer = useVirtual({
    size: rows.length,
    parentRef,
    estimateSize: React.useCallback(() => rowHeight, [rowHeight]),
    overscan: 5
  });

  if (error) {
    return (
      <Box sx={{ p: 2 }}>
        <Typography color="error">{error}</Typography>
      </Box>
    );
  }

  return (
    <TableContainer 
      component={Paper} 
      ref={parentRef} 
      sx={{ height: containerHeight, maxHeight: containerHeight }}
    >
      <Table stickyHeader>
        <TableHead>
          <TableRow>
            {columns.map((column) => (
              <TableCell 
                key={String(column.field)}
                style={{ width: column.width }}
              >
                {column.headerName}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          <TableRow style={{ height: rowVirtualizer.totalSize }} />
          {rowVirtualizer.virtualItems.map((virtualRow) => {
            const row = rows[virtualRow.index];
            return (
              <TableRow
                key={virtualRow.index}
                style={{
                  height: rowHeight,
                  transform: `translateY(${virtualRow.start - rowVirtualizer.totalSize}px)`
                }}
              >
                {columns.map((column) => (
                  <TableCell key={String(column.field)}>
                    {column.renderCell 
                      ? column.renderCell(row)
                      : row[column.field]}
                  </TableCell>
                ))}
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
}