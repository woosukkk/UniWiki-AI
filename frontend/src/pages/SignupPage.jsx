import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api.js';

export function SignupPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '', nickname: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await api.signup({
        email: form.email.trim(),
        password: form.password,
        nickname: form.nickname.trim(),
      });
      navigate('/login', { replace: true, state: { signupComplete: true } });
    } catch (requestError) {
      setError(requestError.message || '회원가입 정보를 다시 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-page container">
      <section className="auth-card">
        <span className="section-kicker">JOIN UNIWIKI</span>
        <h1>UniWiki 시작하기</h1>
        <p>학교 구성원들과 유용한 정보를 나눌 계정을 만들어보세요.</p>
        {error && <div className="auth-error" role="alert">{error}</div>}
        <form className="auth-page-form" onSubmit={handleSubmit}>
          <label>
            닉네임
            <input
              required
              maxLength="50"
              autoComplete="nickname"
              value={form.nickname}
              onChange={(event) => setForm({ ...form, nickname: event.target.value })}
              placeholder="게시판에서 사용할 이름"
            />
          </label>
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
              autoComplete="new-password"
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              placeholder="비밀번호 입력"
            />
          </label>
          <button className="button auth-submit" disabled={submitting}>
            {submitting ? '가입 중...' : '회원가입'}
          </button>
        </form>
        <p className="auth-switch">이미 계정이 있나요? <Link to="/login">로그인</Link></p>
      </section>
    </main>
  );
}
