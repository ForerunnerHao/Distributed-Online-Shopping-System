import { FormEvent, useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import api, { setToken } from '../services/api';

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation() as any;
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      const res = await api.post('/api/users/login', { email, password });
      const token = res.data?.jwtToken;
      if (!token) throw new Error('Login succeeded but no token returned');
      setToken(token);
      const redirectTo = location.state?.from?.pathname || '/products';
      navigate(redirectTo, { replace: true });
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || 'Login failed');
    }
  }

  return (
    <section>
      <h2>Login</h2>
      <form onSubmit={onSubmit} style={{ display: 'grid', gap: 12, maxWidth: 360 }}>
        <label>
          Email
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required />
        </label>
        <button type="submit">Login</button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
        <div>No account? <Link to="/register">Register</Link></div>
      </form>
    </section>
  );
}



