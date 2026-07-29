import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';

const emptyForm = {
  categoryId: '',
  title: '',
  content: '',
  summary: '',
  status: 'APPROVED',
};

export function WikiFormPage() {
  const { wikiPostId } = useParams();
  const navigate = useNavigate();
  const editing = Boolean(wikiPostId);
  const [form, setForm] = useState(emptyForm);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    async function loadForm() {
      setLoading(true);
      try {
        const [categoryList, wikiPost] = await Promise.all([
          api.getCategories(),
          editing ? api.getWikiPost(wikiPostId) : Promise.resolve(null),
        ]);
        if (!active) return;
        setCategories(categoryList);
        if (wikiPost) {
          setForm({
            categoryId: String(wikiPost.categoryId),
            title: wikiPost.title,
            content: wikiPost.content,
            summary: wikiPost.summary || '',
            status: wikiPost.status,
          });
        } else if (categoryList.length > 0) {
          setForm((current) => ({ ...current, categoryId: String(categoryList[0].id) }));
        }
      } catch (requestError) {
        if (active) setError(requestError.message);
      } finally {
        if (active) setLoading(false);
      }
    }
    loadForm();
    return () => { active = false; };
  }, [editing, wikiPostId]);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    if (!form.categoryId || !form.title.trim() || !form.content.trim()) {
      setError('카테고리, 제목과 본문을 모두 입력해 주세요.');
      return;
    }
    const payload = {
      categoryId: Number(form.categoryId),
      title: form.title.trim(),
      content: form.content.trim(),
      summary: form.summary.trim() || null,
      status: form.status,
    };
    setSubmitting(true);
    try {
      const saved = editing
        ? await api.updateWikiPost(wikiPostId, payload)
        : await api.createWikiPost(payload);
      navigate(`/wiki/${saved.id}`, { replace: true });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <main className="wiki-page container"><LoadingSpinner label="작성 화면을 준비하는 중입니다" /></main>;
  if (error && categories.length === 0) return <main className="wiki-page container"><ErrorMessage message={error} /></main>;

  return (
    <main className="wiki-form-page container">
      <Link className="back-button" to={editing ? `/wiki/${wikiPostId}` : '/wiki'}>← 돌아가기</Link>
      <section className="wiki-form-card">
        <span className="section-kicker">{editing ? 'EDIT WIKI' : 'NEW WIKI'}</span>
        <h1>{editing ? '위키 문서 수정' : '새 위키 문서 작성'}</h1>
        {error && <div className="auth-error" role="alert">{error}</div>}
        <form className="wiki-form" onSubmit={handleSubmit}>
          <div className="wiki-form-row">
            <label>카테고리
              <select name="categoryId" value={form.categoryId} onChange={updateField} required>
                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
            </label>
            <label>공개 상태
              <select name="status" value={form.status} onChange={updateField}>
                <option value="APPROVED">공개</option>
                <option value="DRAFT">임시 저장</option>
                <option value="PENDING">검토 대기</option>
              </select>
            </label>
          </div>
          <label>제목
            <input name="title" maxLength="200" value={form.title} onChange={updateField} required />
          </label>
          <label>요약 <span>{form.summary.length}/500</span>
            <textarea className="wiki-summary-input" name="summary" maxLength="500" value={form.summary} onChange={updateField} />
          </label>
          <label>본문
            <textarea className="wiki-content-input" name="content" value={form.content} onChange={updateField} required />
          </label>
          <div className="form-actions">
            <Link className="text-button" to={editing ? `/wiki/${wikiPostId}` : '/wiki'}>취소</Link>
            <button className="button" disabled={submitting}>{submitting ? '저장 중...' : editing ? '수정 완료' : '위키 등록'}</button>
          </div>
        </form>
      </section>
    </main>
  );
}
