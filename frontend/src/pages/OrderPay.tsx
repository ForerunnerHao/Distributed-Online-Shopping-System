import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';

type Order = {
  id: number | string;
  status?: string;
  amount?: number;
  item_id?: number;
  quantity?: number;
};

export default function OrderPay() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState<Order | null>(null);
  const [itemName, setItemName] = useState<string | null>(null);
  const [unitPrice, setUnitPrice] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [customerAccount, setCustomerAccount] = useState<string>('');
  const [simulateFailure, setSimulateFailure] = useState<boolean>(false);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get(`/api/orders/${id}`);
      const data = res.data as Order;
      setOrder(data);
      const itemId = data?.item_id;
      if (itemId) {
        try {
          const ir = await api.get(`/api/items/${itemId}`);
          const name = ir.data?.name;
          const price = ir.data?.price;
          if (typeof name === 'string') setItemName(name);
          if (typeof price === 'number') setUnitPrice(price);
        } catch {}
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || 'Load failed');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [id]);

  async function onPay() {
    setMsg(null);
    try {
      await api.post(`/api/payments`, {
        orderId: Number(id),
        customerAccount: customerAccount || 'CUST-ACC-001',
        simulateFailure
      });
      setMsg('Payment submitted');
      await load();
    } catch (err: any) {
      setMsg(err?.response?.data?.message || err.message || 'Payment failed');
    }
  }

  return (
    <section>
      <h2>Payment</h2>
      {loading && <div>Loading...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      {order && (
        <div style={{ display: 'grid', gap: 8 }}>
          <div>Order ID: {order.id}</div>
          <div>Product: {itemName || '-'}</div>
          <div>Unit Price: {typeof unitPrice === 'number' ? unitPrice : '-'}</div>
          <div>Quantity: {typeof order.quantity === 'number' ? order.quantity : '-'}</div>
          <div>
            Amount: {
              typeof order.amount === 'number'
                ? order.amount
                : (typeof unitPrice === 'number' && typeof order.quantity === 'number'
                    ? unitPrice * order.quantity
                    : '-')
            }
          </div>
          <div>Status: {order.status || '-'}</div>
          <div>
            <label>
              Payer Account (example)
              <input
                placeholder="e.g. CUST-ACC-001"
                value={customerAccount}
                onChange={e => setCustomerAccount(e.target.value)}
              />
            </label>
          </div>
          <label style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            <input type="checkbox" checked={simulateFailure} onChange={e => setSimulateFailure(e.target.checked)} />
            Simulate payment failure (demo)
          </label>
          <div style={{ display: 'flex', gap: 8 }}>
            <button onClick={onPay}>Pay Now</button>
            <button onClick={() => navigate(`/orders/${id}`)}>Back to Detail</button>
          </div>
          {msg && <div>{msg}</div>}
        </div>
      )}
    </section>
  );
}



