import { useState } from 'react';
import {
  Link,
  useLocation,
  useNavigate,
} from 'react-router-dom';

import { useAuth } from '../contexts/AuthContext.jsx';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [form, setForm] = useState({
    email: '',
    password: '',
  });

  const [submitting, setSubmitting] =
    useState(false);

  const [error, setError] = useState('');

  function updateField(event) {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');

    if (
      !form.email.trim() ||
      !form.password.trim()
    ) {
      setError(
        '이메일과 비밀번호를 모두 입력해주세요.',
      );

      return;
    }

    setSubmitting(true);

    try {
      await login({
        email: form.email.trim(),
        password: form.password,
      });

      const destination =
        location.state?.from?.pathname || '/';

      navigate(destination, {
        replace: true,
      });
    } catch (requestError) {
      setError(
        requestError.message ||
          '로그인에 실패했습니다.',
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="editorial-auth-page">
      <section className="editorial-auth-visual">
        <span className="editorial-auth-visual-label">
          UNIWIKI MEMBER ACCESS
        </span>

        <span
          className="editorial-auth-visual-circle circle-one"
          aria-hidden="true"
        />

        <span
          className="editorial-auth-visual-circle circle-two"
          aria-hidden="true"
        />

        <span
          className="editorial-auth-visual-circle circle-three"
          aria-hidden="true"
        />

        <h1>
          지식을 만들고,
          <br />
          <em>함께 연결하세요.</em>
        </h1>

        <p>
          로그인하면 위키 문서를 작성하고 질문과
          답변에 참여하며, 나의 모든 기여 활동을
          한곳에서 관리할 수 있습니다.
        </p>

        <div className="editorial-auth-visual-meta">
          <span>
            STUDENT KNOWLEDGE NETWORK
          </span>

          <span>SEJONG UNIVERSITY</span>
        </div>
      </section>

      <section className="editorial-auth-form-area">
        <div className="editorial-auth-form-card">
          <span className="editorial-auth-form-label">
            LOGIN
          </span>

          <h2>
            다시 만나서
            <br />
            반가워요.
          </h2>

          <p className="editorial-auth-form-description">
            UniWiki 계정으로 로그인하세요.
          </p>

          {error && (
            <div
              className="editorial-auth-error"
              role="alert"
            >
              {error}
            </div>
          )}

          <form
            className="editorial-auth-form"
            onSubmit={handleSubmit}
          >
            <label>
              <span>EMAIL</span>

              <input
                name="email"
                type="email"
                value={form.email}
                onChange={updateField}
                placeholder="student@example.com"
                autoComplete="email"
                required
              />
            </label>

            <label>
              <span>PASSWORD</span>

              <input
                name="password"
                type="password"
                value={form.password}
                onChange={updateField}
                placeholder="비밀번호를 입력하세요"
                autoComplete="current-password"
                required
              />
            </label>

            <button
              className="editorial-auth-submit"
              type="submit"
              disabled={submitting}
            >
              {submitting
                ? 'SIGNING IN...'
                : 'LOGIN'}

              <span>→</span>
            </button>
          </form>

          <p className="editorial-auth-footer">
            아직 계정이 없나요?
            <Link to="/signup">
              회원가입
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}