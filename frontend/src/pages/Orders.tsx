import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

type Order = {
  id: number | string;
  status?: string;
  // 来自后端
  item_id?: number;
  quantity?: number;
  // 前端补充
  productName?: string;
  amount?: number;
};

type SortField = 'id' | 'product' | 'amount' | 'status' | null;
type SortDirection = 'asc' | 'desc';

export default function Orders() {
  const [items, setItems] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');

  async function fetchOrders() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/api/orders');
      const base: Order[] = res.data || [];
      // 并发获取每个订单的商品详情，以得到商品名与单价，再计算总金额
      const enriched = await Promise.all(
        base.map(async (o) => {
          const out: Order = { ...o };
          const itemId = (o as any).item_id;
          const qty = (o as any).quantity;
          if (itemId) {
            try {
              const ir = await api.get(`/api/items/${itemId}`);
              const name = ir.data?.name;
              const price = ir.data?.price;
              if (typeof name === 'string') out.productName = name;
              if (typeof price === 'number' && typeof qty === 'number') {
                out.amount = price * qty;
              }
            } catch {}
          }
          return out;
        })
      );
      setItems(enriched);
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || '查询失败');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { fetchOrders(); }, []);

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
      case 'product':
        aValue = (a.productName || '').toLowerCase();
        bValue = (b.productName || '').toLowerCase();
        break;
      case 'amount':
        aValue = typeof a.amount === 'number' ? a.amount : 0;
        bValue = typeof b.amount === 'number' ? b.amount : 0;
        break;
      case 'status':
        aValue = (a.status || '').toLowerCase();
        bValue = (b.status || '').toLowerCase();
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
      <h2>My Orders</h2>
      {loading && <div>Loading...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <SortableHeader field="id">Order ID</SortableHeader>
            <SortableHeader field="product">Product</SortableHeader>
            <SortableHeader field="amount">Amount</SortableHeader>
            <SortableHeader field="status">Status</SortableHeader>
            <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {sortedItems.map(o => (
            <tr key={o.id}>
              <td>{o.id}</td>
              <td>{o.productName || '-'}</td>
              <td>{typeof o.amount === 'number' ? o.amount : '-'}</td>
              <td>{o.status || '-'}</td>
              <td><Link to={`/orders/${o.id}`}>Details</Link></td>
            </tr>
          ))}
        </tbody>
      </table>
      {sortedItems.length === 0 && !loading && <div>No orders</div>}
    </section>
  );
}



