import { useState } from 'react';
import {
  Link,
  useNavigate,
} from 'react-router-dom';

import { api } from '../api.js';

export function SignupPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    nickname: '',
    email: '',
    password: '',
    passwordConfirm: '',
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

    const nickname = form.nickname.trim();
    const email = form.email.trim();

    if (
      !nickname ||
      !email ||
      !form.password ||
      !form.passwordConfirm
    ) {
      setError(
        '모든 항목을 입력해주세요.',
      );

      return;
    }

    if (
      form.password !==
      form.passwordConfirm
    ) {
      setError(
        '비밀번호와 비밀번호 확인이 일치하지 않습니다.',
      );

      return;
    }

    setSubmitting(true);

    try {
      await api.signup({
        nickname,
        email,
        password: form.password,
      });

      navigate('/login', {
        replace: true,
      });
    } catch (requestError) {
      setError(
        requestError.message ||
          '회원가입에 실패했습니다.',
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="editorial-auth-page">
      <section className="editorial-auth-visual">
        <span className="editorial-auth-visual-label">
          JOIN UNIWIKI
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
          학생이 만드는 지식,
          <br />
          <em>AI가 연결하는 정보.</em>
        </h1>

        <p>
          학교생활에 필요한 정보를 직접 기록하고,
          다른 학생의 질문에 답하며 캠퍼스 지식에
          기여하세요.
        </p>

        <div className="editorial-auth-visual-meta">
          <span>
            CREATE · SHARE · CONNECT
          </span>

          <span>UNIWIKI AI</span>
        </div>
      </section>

      <section className="editorial-auth-form-area">
        <div className="editorial-auth-form-card">
          <span className="editorial-auth-form-label">
            SIGN UP
          </span>

          <h2>새 계정을 만드세요.</h2>

          <p className="editorial-auth-form-description">
            가입 후 위키, 질문, 답변 작성 기능을
            사용할 수 있습니다.
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
              <span>
                NICKNAME
                <small>공개 프로필 이름</small>
              </span>

              <input
                name="nickname"
                value={form.nickname}
                onChange={updateField}
                placeholder="사용할 닉네임"
                autoComplete="nickname"
                required
              />
            </label>

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
                autoComplete="new-password"
                required
              />
            </label>

            <label>
              <span>
                PASSWORD CONFIRM
              </span>

              <input
                name="passwordConfirm"
                type="password"
                value={form.passwordConfirm}
                onChange={updateField}
                placeholder="비밀번호를 다시 입력하세요"
                autoComplete="new-password"
                required
              />
            </label>

            <button
              className="editorial-auth-submit"
              type="submit"
              disabled={submitting}
            >
              {submitting
                ? 'CREATING ACCOUNT...'
                : 'CREATE ACCOUNT'}

              <span>→</span>
            </button>
          </form>

          <p className="editorial-auth-footer">
            이미 계정이 있나요?
            <Link to="/login">
              로그인
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}