import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../services/api';

type Product = {
  id: number | string;
  name: string;
  price?: number;
  description?: string;
  sku?: string;
  itemWarehouseList?: Array<{
    warehouseName: string;
    warehouseCode: string;
    totalQty: number;
    reservedQty: number;
    deductedQty: number;
  }>;
};

export default function Products() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState(params.get('q') || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [items, setItems] = useState<Product[]>([]);

  async function fetchProducts() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/api/items');
      const list: Product[] = res.data || [];
      const q = keyword.trim().toLowerCase();
      const filtered = q ? list.filter(i => (i.name || '').toLowerCase().includes(q)) : list;

      // 逐个获取详情并合并（@GetMapping("{item_id}")）
      const withDetails = await Promise.all(
        filtered.map(async (p) => {
          try {
            const d = await api.get(`/api/items/${p.id}`);
            return { ...p, ...d.data } as Product;
          } catch {
            return p; // 失败则返回原数据
          }
        })
      );

      setItems(withDetails);
      setParams(q ? { q } : {});
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || '查询失败');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchProducts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <section>
      <h2>Product Catalog</h2>
      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <input
          placeholder="By name/keyword"
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          onKeyDown={e => { if (e.key === 'Enter') fetchProducts(); }}
        />
        <button onClick={fetchProducts} disabled={loading}>Search</button>
      </div>

      {loading && <div>Loading...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}

      <div style={{ display: 'grid', gap: 12 }}>
        {items.map(p => (
          <div key={p.id} style={{ border: '1px solid #ddd', padding: 12, borderRadius: 4 }}>
            <div style={{ fontWeight: 600 }}>{p.name}</div>
            {p.sku && <div>SKU: {p.sku}</div>}
            {p.description && <div style={{ color: '#666' }}>{p.description}</div>}
            {typeof p.price !== 'undefined' && <div>Price: {p.price}</div>}
            {Array.isArray(p.itemWarehouseList) && p.itemWarehouseList.length > 0 && (
              <div style={{ marginTop: 8 }}>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>Inventory</div>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr>
                      <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Warehouse</th>
                      <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Code</th>
                      <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Total</th>
                      <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Reserved</th>
                      <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Deducted</th>
                    </tr>
                  </thead>
                  <tbody>
                    {p.itemWarehouseList.map((w, idx) => (
                      <tr key={idx}>
                        <td>{w.warehouseName}</td>
                        <td>{w.warehouseCode}</td>
                        <td>{w.totalQty}</td>
                        <td>{w.reservedQty}</td>
                        <td>{w.deductedQty}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <button onClick={() => navigate(`/orders/new?productId=${p.id}`)}>Buy</button>
          </div>
        ))}
        {items.length === 0 && !loading && <div>No data</div>}
      </div>
    </section>
  );
}



