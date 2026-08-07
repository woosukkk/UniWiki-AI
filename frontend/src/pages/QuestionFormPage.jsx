import {
  useEffect,
  useState,
} from 'react';

import {
  Link,
  useNavigate,
  useParams,
} from 'react-router-dom';

import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';

const initialForm = {
  title: '',
  content: '',
};

export function QuestionFormPage() {
  const { questionId } = useParams();
  const navigate = useNavigate();

  const editing = Boolean(questionId);

  const [form, setForm] =
    useState(initialForm);

  const [loading, setLoading] =
    useState(editing);

  const [submitting, setSubmitting] =
    useState(false);

  const [error, setError] = useState('');

  useEffect(() => {
    if (!editing) {
      return;
    }

    let active = true;

    api
      .getQuestion(questionId)
      .then((question) => {
        if (!active) {
          return;
        }

        setForm({
          title: question.title,
          content: question.content,
        });
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError.message);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [editing, questionId]);

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

    const title = form.title.trim();
    const content = form.content.trim();

    if (!title || !content) {
      setError(
        '제목과 내용을 모두 입력해 주세요.',
      );

      return;
    }

    setSubmitting(true);

    try {
      const payload = {
        title,
        content,
      };

      const saved = editing
        ? await api.updateQuestion(
            questionId,
            payload,
          )
        : await api.createQuestion(payload);

      navigate(`/questions/${saved.id}`, {
        replace: true,
      });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <main className="editorial-question-form-state">
        <LoadingSpinner label="질문을 불러오는 중입니다" />
      </main>
    );
  }

  if (error && editing && !form.title) {
    return (
      <main className="editorial-question-form-state">
        <ErrorMessage message={error} />
      </main>
    );
  }

  return (
    <div className="editorial-question-form-page">
      <section className="editorial-question-form-hero">
        <div className="editorial-question-content-width">
          <Link
            className="editorial-question-back-link"
            to={
              editing
                ? `/questions/${questionId}`
                : '/questions'
            }
          >
            ← BACK
          </Link>

          <span className="editorial-question-label">
            {editing
              ? 'EDIT QUESTION'
              : 'NEW QUESTION'}
          </span>

          <h1>
            {editing
              ? '질문을 수정합니다.'
              : '궁금한 점을 질문하세요.'}
          </h1>

          <p>
            상황과 궁금한 점을 구체적으로 작성하면
            더 정확하고 도움이 되는 답변을 받을 수
            있습니다.
          </p>
        </div>
      </section>

      <section className="editorial-question-form-main">
        <div className="editorial-question-content-width editorial-question-form-layout">
          <aside className="editorial-question-form-guide">
            <span>WRITING GUIDE</span>

            <h2>
              좋은 질문을
              <br />
              만드는 방법
            </h2>

            <ol>
              <li>
                <span>01</span>
                <p>
                  제목에 핵심 질문을 명확하게
                  적어주세요.
                </p>
              </li>

              <li>
                <span>02</span>
                <p>
                  현재 상황과 이미 확인한 내용을
                  설명해주세요.
                </p>
              </li>

              <li>
                <span>03</span>
                <p>
                  개인정보나 민감한 정보는 포함하지
                  마세요.
                </p>
              </li>

              <li>
                <span>04</span>
                <p>
                  답변을 받으면 도움이 된 답변을
                  채택해주세요.
                </p>
              </li>
            </ol>
          </aside>

          <section className="editorial-question-form-card">
            {error && (
              <div
                className="editorial-question-form-error"
                role="alert"
              >
                {error}
              </div>
            )}

            <form
              className="editorial-question-form"
              onSubmit={handleSubmit}
            >
              <label>
                <span>
                  TITLE
                  <small>
                    {form.title.length}/200
                  </small>
                </span>

                <input
                  name="title"
                  maxLength="200"
                  value={form.title}
                  onChange={updateField}
                  placeholder="질문의 핵심 내용을 입력하세요"
                  required
                />
              </label>

              <label>
                <span>
                  QUESTION
                  <small>
                    {form.content.length}자
                  </small>
                </span>

                <textarea
                  name="content"
                  value={form.content}
                  onChange={updateField}
                  placeholder="궁금한 점과 현재 상황을 구체적으로 작성해주세요"
                  required
                />
              </label>

              <div className="editorial-question-form-actions">
                <Link
                  to={
                    editing
                      ? `/questions/${questionId}`
                      : '/questions'
                  }
                >
                  CANCEL
                </Link>

                <button
                  type="submit"
                  disabled={submitting}
                >
                  {submitting
                    ? 'SAVING...'
                    : editing
                      ? 'SAVE CHANGES'
                      : 'PUBLISH QUESTION'}

                  <span>→</span>
                </button>
              </div>
            </form>
          </section>
        </div>
      </section>
    </div>
  );
}