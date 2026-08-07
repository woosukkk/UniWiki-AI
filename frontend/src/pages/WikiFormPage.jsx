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
  const [submitting, setSubmitting] =
    useState(false);

  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    async function loadForm() {
      setLoading(true);

      try {
        const [categoryList, wikiPost] =
          await Promise.all([
            api.getCategories(),
            editing
              ? api.getWikiPost(wikiPostId)
              : Promise.resolve(null),
          ]);

        if (!active) {
          return;
        }

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
          setForm((current) => ({
            ...current,
            categoryId: String(categoryList[0].id),
          }));
        }
      } catch (requestError) {
        if (active) {
          setError(requestError.message);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadForm();

    return () => {
      active = false;
    };
  }, [editing, wikiPostId]);

  function updateField(event) {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');

    if (
      !form.categoryId ||
      !form.title.trim() ||
      !form.content.trim()
    ) {
      setError(
        '카테고리, 제목과 본문을 모두 입력해주세요.',
      );

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
        ? await api.updateWikiPost(
            wikiPostId,
            payload,
          )
        : await api.createWikiPost(payload);

      navigate(`/wiki/${saved.id}`, {
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
      <main className="editorial-wiki-form-state">
        <LoadingSpinner label="작성 화면을 준비하는 중입니다" />
      </main>
    );
  }

  if (error && categories.length === 0) {
    return (
      <main className="editorial-wiki-form-state">
        <ErrorMessage message={error} />
      </main>
    );
  }

  return (
    <div className="editorial-wiki-form-page">
      <section className="editorial-wiki-form-hero">
        <div className="editorial-wiki-content-width">
          <Link
            className="editorial-wiki-back-link"
            to={
              editing
                ? `/wiki/${wikiPostId}`
                : '/wiki'
            }
          >
            ← BACK
          </Link>

          <span className="editorial-wiki-label">
            {editing ? 'EDIT WIKI' : 'NEW WIKI'}
          </span>

          <h1>
            {editing
              ? '위키 문서 수정'
              : '새로운 지식을 기록하세요.'}
          </h1>

          <p>
            정확한 정보와 출처를 기반으로
            학생들이 함께 사용할 수 있는 문서를 작성합니다.
          </p>
        </div>
      </section>

      <section className="editorial-wiki-form-section">
        <div className="editorial-wiki-content-width editorial-wiki-form-layout">
          <aside className="editorial-wiki-form-guide">
            <span>WRITING GUIDE</span>

            <h2>
              좋은 위키 문서를
              <br />
              만드는 방법
            </h2>

            <ol>
              <li>
                <span>01</span>
                <p>
                  제목만 읽어도 내용을 알 수 있게 작성하세요.
                </p>
              </li>

              <li>
                <span>02</span>
                <p>
                  공식 자료나 직접 확인한 정보를 사용하세요.
                </p>
              </li>

              <li>
                <span>03</span>
                <p>
                  긴 내용은 문단으로 구분해 읽기 쉽게 만드세요.
                </p>
              </li>

              <li>
                <span>04</span>
                <p>
                  개인적인 추측과 확인되지 않은 정보는 제외하세요.
                </p>
              </li>
            </ol>
          </aside>

          <section className="editorial-wiki-form-card">
            {error && (
              <div
                className="editorial-wiki-form-error"
                role="alert"
              >
                {error}
              </div>
            )}

            <form
              className="editorial-wiki-form"
              onSubmit={handleSubmit}
            >
              <div className="editorial-wiki-form-row">
                <label>
                  <span>CATEGORY</span>

                  <select
                    name="categoryId"
                    value={form.categoryId}
                    onChange={updateField}
                    required
                  >
                    {categories.map((category) => (
                      <option
                        key={category.id}
                        value={category.id}
                      >
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  <span>STATUS</span>

                  <select
                    name="status"
                    value={form.status}
                    onChange={updateField}
                  >
                    <option value="APPROVED">
                      공개
                    </option>

                    <option value="DRAFT">
                      임시 저장
                    </option>

                    <option value="PENDING">
                      검토 대기
                    </option>
                  </select>
                </label>
              </div>

              <label>
                <span>TITLE</span>

                <input
                  name="title"
                  maxLength="200"
                  value={form.title}
                  onChange={updateField}
                  placeholder="문서 제목을 입력하세요"
                  required
                />
              </label>

              <label>
                <span>
                  SUMMARY
                  <small>
                    {form.summary.length}/500
                  </small>
                </span>

                <textarea
                  className="editorial-wiki-summary-input"
                  name="summary"
                  maxLength="500"
                  value={form.summary}
                  onChange={updateField}
                  placeholder="문서의 핵심 내용을 짧게 정리해주세요"
                />
              </label>

              <label>
                <span>CONTENT</span>

                <textarea
                  className="editorial-wiki-content-input"
                  name="content"
                  value={form.content}
                  onChange={updateField}
                  placeholder="정확하고 구체적인 정보를 작성해주세요"
                  required
                />
              </label>

              <div className="editorial-wiki-form-actions">
                <Link
                  to={
                    editing
                      ? `/wiki/${wikiPostId}`
                      : '/wiki'
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
                      : 'PUBLISH WIKI'}

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