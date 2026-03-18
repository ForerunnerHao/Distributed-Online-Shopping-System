import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api from '../services/api';

export default function OrderNew() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [productId, setProductId] = useState<string>('');
  const [quantity, setQuantity] = useState<number>(1);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const pid = params.get('productId') || '';
    setProductId(pid);
  }, [params]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      const res = await api.post('/api/orders', {
        item_id: Number(productId),
        quantity
      });
      const id = res.data?.id || res.data?.orderId;
      if (!id) throw new Error('创建成功但未返回订单ID');
      navigate(`/orders/${id}`);
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || '下单失败');
    }
  }

  return (
    <section>
      <h2>Create Order</h2>
      <form onSubmit={onSubmit} style={{ display: 'grid', gap: 12, maxWidth: 420 }}>
        <label>
          Product ID
          <input value={productId} onChange={e => setProductId(e.target.value)} readOnly />
        </label>
        <label>
          Quantity
          <input type="number" min={1} value={quantity} onChange={e => setQuantity(parseInt(e.target.value || '1', 10))} />
        </label>
        <button type="submit">Submit</button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
      </form>
    </section>
  );
}



