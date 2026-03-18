import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Products from './pages/Products';
import Orders from './pages/Orders';
import OrderDetail from './pages/OrderDetail';
import OrderNew from './pages/OrderNew';
import OrderPay from './pages/OrderPay';
import Deliveries from './pages/Deliveries';
import DeliveryDetail from './pages/DeliveryDetail';
import { getToken, clearToken } from './services/api';

function RequireAuth({ children }: { children: JSX.Element }) {
  const token = getToken();
  const location = useLocation();
  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return children;
}

function RequireGuest({ children }: { children: JSX.Element }) {
  const token = getToken();
  if (token) {
    return <Navigate to="/products" replace />;
  }
  return children;
}

function Navbar() {
  const token = getToken();
  // Only show navigation items when user is logged in
  if (!token) {
    return null;
  }
  return (
    <nav style={{ display: 'flex', gap: 12, padding: 12, borderBottom: '1px solid #eee' }}>
      <Link to="/products">Products</Link>
      <Link to="/orders">My Orders</Link>
      <Link to="/deliveries">My Deliveries</Link>
      <button onClick={() => { clearToken(); window.location.href = '/login'; }}>Logout</button>
    </nav>
  );
}

function RootRedirect() {
  const token = getToken();
  if (token) {
    return <Navigate to="/products" replace />;
  }
  return <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <div>
      <Navbar />
      <main style={{ padding: 16, maxWidth: 960, margin: '0 auto' }}>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login" element={<RequireGuest><Login /></RequireGuest>} />
          <Route path="/register" element={<RequireGuest><Register /></RequireGuest>} />

          <Route path="/products" element={<RequireAuth><Products /></RequireAuth>} />

          <Route path="/orders" element={<RequireAuth><Orders /></RequireAuth>} />
          <Route path="/orders/new" element={<RequireAuth><OrderNew /></RequireAuth>} />
          <Route path="/orders/:id" element={<RequireAuth><OrderDetail /></RequireAuth>} />
          <Route path="/orders/:id/pay" element={<RequireAuth><OrderPay /></RequireAuth>} />

          <Route path="/deliveries" element={<RequireAuth><Deliveries /></RequireAuth>} />
          <Route path="/deliveries/:id" element={<RequireAuth><DeliveryDetail /></RequireAuth>} />

          <Route path="*" element={<div>Page not found</div>} />
        </Routes>
      </main>
    </div>
  );
}




