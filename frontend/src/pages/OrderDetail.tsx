import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';

type OrderDetail = {
  id: number | string;
  user_id?: number;
  item_id?: number;
  quantity?: number;
  warehouse_reservations?: Array<{
    reservationId: string;
    warehouseCode: string;
    reservedQty: number;
    status: string;
  }>;
  status?: string;
  amount?: number; // 若后端无该字段则显示为 '-'
  productId?: string | number; // 兼容旧字段
  productName?: string; // 兼容旧字段
  expires_at?: string;
  updated_at?: string;
};

export default function OrderDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState<OrderDetail | null>(null);
  const [itemPrice, setItemPrice] = useState<number | null>(null);
  const [itemName, setItemName] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionMsg, setActionMsg] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get(`/api/orders/${id}`);
      setData(res.data);
      // 顺带拉取商品详情以获取价格/名称
      const itemId = res.data?.item_id || res.data?.productId;
      if (itemId) {
        try {
          const itemRes = await api.get(`/api/items/${itemId}`);
          const price = itemRes.data?.price;
          const name = itemRes.data?.name;
          if (typeof price === 'number') setItemPrice(price);
          if (typeof name === 'string') setItemName(name);
        } catch {}
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || 'Load failed');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [id]);

  async function onRefund() {
    setActionMsg(null);
    try {
      await api.post(`/api/payments/refund`, { orderId: Number(id) });
      setActionMsg('Refund request submitted successfully');
      await load();
    } catch (err: any) {
      setActionMsg(err?.response?.data?.message || err.message || 'Refund failed');
    }
  }

  async function onCancel() {
    setActionMsg(null);
    try {
      await api.post(`/api/orders/cancel`, { orderId: id });
      setActionMsg('Order cancellation requested successfully');
      await load();
    } catch (err: any) {
      setActionMsg(err?.response?.data?.message || err.message || 'Cancel failed');
    }
  }

  return (
    <section>
      <h2>Order Detail</h2>
      {loading && <div>Loading...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      {data && (
        <div style={{ display: 'grid', gap: 8 }}>
          <div>Order ID: {data.id}</div>
          <div>User ID: {typeof data.user_id === 'number' ? data.user_id : '-'}</div>
          <div>Product: {itemName || data.productName || data.productId || data.item_id}</div>
          <div>Quantity: {typeof data.quantity === 'number' ? data.quantity : '-'}</div>
          {typeof itemPrice === 'number' && <div>Unit Price: {itemPrice}</div>}
          <div>Amount: {
            typeof data.amount === 'number'
              ? data.amount
              : (typeof itemPrice === 'number' && typeof data.quantity === 'number'
                  ? (itemPrice * data.quantity)
                  : '-')
          }</div>
          <div>Status: {data.status || '-'}</div>
          <div>Expires At: {data.expires_at || '-'}</div>
          <div>Updated At: {data.updated_at || '-'}</div>

          {Array.isArray(data.warehouse_reservations) && data.warehouse_reservations.length > 0 && (
            <div style={{ marginTop: 8 }}>
              <div style={{ fontWeight: 600, marginBottom: 4 }}>Warehouse Reservations</div>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Reservation ID</th>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Warehouse Code</th>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Reserved Qty</th>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {data.warehouse_reservations.map((w, idx) => (
                    <tr key={idx}>
                      <td>{w.reservationId}</td>
                      <td>{w.warehouseCode}</td>
                      <td>{w.reservedQty}</td>
                      <td>{w.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div style={{ display: 'flex', gap: 8 }}>
            <Link to={`/orders/${id}/pay`}><button>Pay Now</button></Link>
            <button onClick={onRefund}>Refund</button>
            <button onClick={onCancel}>Cancel</button>
            <button onClick={() => navigate('/orders')}>Back to Orders</button>
          </div>
          {actionMsg && <div>{actionMsg}</div>}
        </div>
      )}
    </section>
  );
}



