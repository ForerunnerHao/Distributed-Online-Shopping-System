import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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

export default function DeliveryDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState<Delivery | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get(`/api/deliveries/${id}`);
      setData(res.data);
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || 'Load failed');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [id]);

  return (
    <section>
      <h2>Delivery Detail</h2>
      {loading && <div>Loading...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      {data && (
        <div style={{ display: 'grid', gap: 8 }}>
          <div>Delivery ID: {data.id}</div>
          <div>Order ID: {data.orderId || '-'}</div>

          {Array.isArray(data.packages) && data.packages.length > 0 && (
            <div style={{ marginTop: 8 }}>
              <div style={{ fontWeight: 600, marginBottom: 4 }}>Packages</div>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Delivery ID</th>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Type</th>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Status</th>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>From</th>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>To</th>
                    <th style={{ textAlign: 'left', borderBottom: '1px solid #eee' }}>Status Updated</th>
                  </tr>
                </thead>
                <tbody>
                  {data.packages.map((pkg, idx) => (
                    <tr key={idx}>
                      <td>{pkg.deliveryId}</td>
                      <td>{pkg.type || '-'}</td>
                      <td>{pkg.status || '-'}</td>
                      <td>{pkg.fromAddress || '-'}</td>
                      <td>{pkg.toAddress || '-'}</td>
                      <td>{pkg.statusUpdatedAt || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {data.packages.some(p => p.details) && (
                <div style={{ marginTop: 12 }}>
                  <div style={{ fontWeight: 600, marginBottom: 4 }}>Package Details</div>
                  {data.packages.map((pkg, idx) => (
                    pkg.details && (
                      <div key={idx} style={{ marginBottom: 8, padding: 8, backgroundColor: '#f5f5f5', borderRadius: 4 }}>
                        <div style={{ fontWeight: 600 }}>Package {idx + 1} ({pkg.deliveryId}):</div>
                        <div>{pkg.details}</div>
                      </div>
                    )
                  ))}
                </div>
              )}
            </div>
          )}

          {(!data.packages || data.packages.length === 0) && (
            <div>No packages information available</div>
          )}

          <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
            <button onClick={() => navigate('/deliveries')}>Back to Deliveries</button>
            {data.orderId && (
              <button onClick={() => navigate(`/orders/${data.orderId}`)}>View Order</button>
            )}
          </div>
        </div>
      )}
    </section>
  );
}

