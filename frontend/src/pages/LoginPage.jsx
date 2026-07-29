import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../contexts/AuthContext.jsx';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const response = await api.login({
        email: form.email.trim(),
        password: form.password,
      });
      login(response);
      navigate(location.state?.from?.pathname || '/questions', { replace: true });
    } catch (requestError) {
      setError(requestError.message || '이메일 또는 비밀번호를 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-page container">
      <section className="auth-card">
        <span className="section-kicker">WELCOME BACK</span>
        <h1>다시 만나 반가워요</h1>
        <p>UniWiki 계정으로 로그인해 질문하고 지식을 나눠보세요.</p>
        {location.state?.signupComplete && (
          <div className="auth-success" role="status">회원가입이 완료되었습니다. 로그인해 주세요.</div>
        )}
        {error && <div className="auth-error" role="alert">{error}</div>}
        <form className="auth-page-form" onSubmit={handleSubmit}>
          <label>
            이메일
            <input
              required
              autoComplete="email"
              type="email"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
              placeholder="name@university.ac.kr"
            />
          </label>
          <label>
            비밀번호
            <input
              required
              autoComplete="current-password"
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              placeholder="비밀번호 입력"
            />
          </label>
          <button className="button auth-submit" disabled={submitting}>
            {submitting ? '로그인 중...' : '로그인'}
          </button>
        </form>
        <p className="auth-switch">아직 계정이 없나요? <Link to="/signup">회원가입</Link></p>
      </section>
    </main>
  );
}
