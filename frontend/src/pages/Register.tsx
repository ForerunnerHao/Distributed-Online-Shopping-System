import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function Register() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [type, setType] = useState<'USER' | 'ADMIN' | 'STAFF'>('USER');
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api.post('/api/users/signup', { username, email, password, type });
      navigate('/login');
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || 'Register failed');
    }
  }

  return (
    <section>
      <h2>Register</h2>
      <form onSubmit={onSubmit} style={{ display: 'grid', gap: 12, maxWidth: 420 }}>
        <label>
          Username
          <input value={username} onChange={e => setUsername(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required />
        </label>
        <label>
          Role
          <select value={type} onChange={e => setType(e.target.value as any)}>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
            <option value="STAFF">STAFF</option>
          </select>
        </label>
        <button type="submit">Register</button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
        <div>Already have an account? <Link to="/login">Login</Link></div>
      </form>
    </section>
  );
}



