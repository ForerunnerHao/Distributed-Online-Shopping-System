import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

type Delivery = {
  id: number | string;
  orderId?: string;
  packages?: Array<{
    deliveryId: string;
    type: string;
    status: string;
    statusUpdatedAt?: string;
    details?: string;
    fromAddress?: string;
    toAddress?: string;
  }>;
};

type SortField = 'id' | null;
type SortDirection = 'asc' | 'desc';

export default function Deliveries() {
  const [items, setItems] = useState<Delivery[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');

  async function fetchDeliveries() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/api/deliveries');
      setItems(res.data || []);
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || 'Failed to load deliveries');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { fetchDeliveries(); }, []);

  function handleSort(field: SortField) {
    if (sortField === field) {
      // Toggle direction if clicking the same field
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      // New field, default to ascending
      setSortField(field);
      setSortDirection('asc');
    }
  }

  const sortedItems = [...items].sort((a, b) => {
    if (!sortField) return 0;

    let aValue: any;
    let bValue: any;

    switch (sortField) {
      case 'id':
        aValue = typeof a.id === 'number' ? a.id : String(a.id);
        bValue = typeof b.id === 'number' ? b.id : String(b.id);
        break;
      default:
        return 0;
    }

    if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
    if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
    return 0;
  });

  function SortableHeader({ field, children }: { field: SortField; children: JSX.Element | string }) {
    const isActive = sortField === field;
    return (
      <th
        style={{
          textAlign: 'left',
          borderBottom: '1px solid #eee',
          cursor: 'pointer',
          userSelect: 'none',
          paddingRight: 20,
          position: 'relative'
        }}
        onClick={() => handleSort(field)}
      >
        {children}
        {isActive && (
          <span style={{ marginLeft: 4, fontSize: '0.8em' }}>
            {sortDirection === 'asc' ? '↑' : '↓'}
          </span>
        )}
        {!isActive && (
          <span style={{ marginLeft: 4, fontSize: '0.8em', color: '#ccc' }}>⇅</span>
        )}
      </th>
    );
  }

  return (
    <section>
      <h2>My Deliveries</h2>
      {loading && <div>Loading...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <SortableHeader field="id">Delivery ID</SortableHeader>
            <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Order ID</th>
            <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Packages</th>
            <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {sortedItems.map(d => (
            <tr key={d.id}>
              <td>{d.id}</td>
              <td>{d.orderId || '-'}</td>
              <td>{d.packages?.length || 0}</td>
              <td><Link to={`/deliveries/${d.id}`}>Details</Link></td>
            </tr>
          ))}
        </tbody>
      </table>
      {sortedItems.length === 0 && !loading && <div>No deliveries</div>}
    </section>
  );
}

