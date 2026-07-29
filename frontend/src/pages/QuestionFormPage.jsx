import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';

export function QuestionFormPage() {
  const { questionId } = useParams();
  const navigate = useNavigate();
  const editing = Boolean(questionId);
  const [form, setForm] = useState({ title: '', content: '' });
  const [loading, setLoading] = useState(editing);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!editing) return;
    api.getQuestion(questionId)
      .then((question) => setForm({ title: question.title, content: question.content }))
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  }, [editing, questionId]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    if (!form.title.trim() || !form.content.trim()) {
      setError('제목과 내용을 모두 입력해 주세요.');
      return;
    }
    setSubmitting(true);
    try {
      const payload = { title: form.title.trim(), content: form.content.trim() };
      const saved = editing
        ? await api.updateQuestion(questionId, payload)
        : await api.createQuestion(payload);
      navigate(`/questions/${saved.id}`, { replace: true });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <main className="question-form-page container"><LoadingSpinner /></main>;
  if (error && !form.title) return <main className="question-form-page container"><ErrorMessage message={error} /></main>;

  return (
    <main className="question-form-page container">
      <Link className="back-button" to={editing ? `/questions/${questionId}` : '/questions'}>← 돌아가기</Link>
      <section className="wiki-form-card">
        <span className="section-kicker">{editing ? 'EDIT QUESTION' : 'NEW QUESTION'}</span>
        <h1>{editing ? '질문 수정' : '새 질문 작성'}</h1>
        {error && <div className="auth-error" role="alert">{error}</div>}
        <form className="wiki-form" onSubmit={handleSubmit}>
          <label>제목 <span>{form.title.length}/200</span>
            <input maxLength="200" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} required />
          </label>
          <label>질문 내용
            <textarea className="wiki-content-input" value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} required />
          </label>
          <div className="form-actions">
            <Link className="text-button" to={editing ? `/questions/${questionId}` : '/questions'}>취소</Link>
            <button className="button" disabled={submitting}>{submitting ? '저장 중...' : editing ? '수정 완료' : '질문 등록'}</button>
          </div>
        </form>
      </section>
    </main>
  );
}
