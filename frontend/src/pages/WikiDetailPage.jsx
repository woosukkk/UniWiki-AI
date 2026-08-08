import {
  useCallback,
  useEffect,
  useState,
} from 'react';

import {
  Link,
  useNavigate,
  useParams,
} from 'react-router-dom';
import ReactMarkdown from 'react-markdown';

import { api } from '../api.js';
import { ConfirmDialog } from '../components/ConfirmDialog.jsx';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LikeButton } from '../components/LikeButton.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function WikiDetailPage() {
  const { wikiPostId } = useParams();
  const navigate = useNavigate();

  const {
    user,
    isAdmin,
    isAuthenticated,
  } = useAuth();

  const [wikiPost, setWikiPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showDeleteConfirm, setShowDeleteConfirm] =
    useState(false);

  const [deleting, setDeleting] = useState(false);

  const [like, setLike] = useState({
    likeCount: 0,
    liked: false,
    busy: false,
  });

  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] =
    useState(false);

  const [summaryError, setSummaryError] =
    useState('');

  const loadWikiPost = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const [post, likeStatus] = await Promise.all([
        api.getWikiPost(wikiPostId),
        api.getWikiPostLikes(
          wikiPostId,
          isAuthenticated,
        ),
      ]);

      setWikiPost(post);
      setLike({
        ...likeStatus,
        busy: false,
      });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [wikiPostId, isAuthenticated]);

  useEffect(() => {
    loadWikiPost();
  }, [loadWikiPost]);

  if (loading) {
    return (
      <main className="editorial-wiki-detail-state">
        <LoadingSpinner />
      </main>
    );
  }

  if (error) {
    return (
      <main className="editorial-wiki-detail-state">
        <ErrorMessage
          message={error}
          onRetry={loadWikiPost}
        />
      </main>
    );
  }

  if (!wikiPost) {
    return null;
  }

  const canManage =
    user?.id === wikiPost.authorId || isAdmin;

  async function deleteWikiPost() {
    setDeleting(true);
    setError('');

    try {
      await api.deleteWikiPost(wikiPost.id);

      navigate('/wiki', {
        replace: true,
      });
    } catch (requestError) {
      setError(requestError.message);
      setShowDeleteConfirm(false);
      setDeleting(false);
    }
  }

  async function toggleLike() {
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: {
            pathname: `/wiki/${wikiPostId}`,
          },
        },
      });

      return;
    }

    setLike((current) => ({
      ...current,
      busy: true,
    }));

    try {
      if (like.liked) {
        await api.unlikeWikiPost(wikiPostId);

        setLike((current) => ({
          likeCount: Math.max(
            0,
            current.likeCount - 1,
          ),
          liked: false,
          busy: false,
        }));
      } else {
        const response =
          await api.likeWikiPost(wikiPostId);

        setLike({
          ...response,
          busy: false,
        });
      }
    } catch (requestError) {
      setError(requestError.message);

      setLike((current) => ({
        ...current,
        busy: false,
      }));
    }
  }

  async function createSummary() {
    setSummaryLoading(true);
    setSummaryError('');

    try {
      setSummary(
        await api.summarizeWikiPost(wikiPostId),
      );
    } catch (requestError) {
      setSummaryError(
        requestError.status === 404
          ? '아직 AI 검색 저장소에 반영되지 않은 문서입니다.'
          : 'AI 요약을 생성하지 못했습니다. 잠시 후 다시 시도해주세요.',
      );
    } finally {
      setSummaryLoading(false);
    }
  }

  return (
    <div className="editorial-wiki-detail-page">
      <section className="editorial-wiki-detail-hero">
        <div className="editorial-wiki-content-width">
          <Link
            className="editorial-wiki-back-link"
            to="/wiki"
          >
            ← BACK TO WIKI
          </Link>

          <div className="editorial-wiki-detail-heading">
            <div className="editorial-wiki-detail-heading-main">
              <span className="editorial-wiki-label">
                WIKI DOCUMENT
              </span>

              <div className="editorial-wiki-detail-meta">
                <span>{wikiPost.categoryName}</span>
                <span>{wikiPost.status}</span>
                <span>
                  VIEW{' '}
                  {wikiPost.viewCount.toLocaleString()}
                </span>
              </div>

              <h1>{wikiPost.title}</h1>

              <div className="editorial-wiki-detail-byline">
                <div>
                  <span>AUTHOR</span>
                  <strong>
                    {wikiPost.authorNickname}
                  </strong>
                </div>

                <div>
                  <span>LAST UPDATED</span>
                  <strong>
                    {formatDate(wikiPost.createdAt)}
                  </strong>
                </div>
              </div>
            </div>

            <aside className="editorial-wiki-detail-cover">
              <span>UNIWIKI</span>

              <strong>
                VERIFIED
                <br />
                KNOWLEDGE
              </strong>

              <div>
                <span>{wikiPost.categoryName}</span>
                <span>
                  #{String(wikiPost.id).padStart(4, '0')}
                </span>
              </div>
            </aside>
          </div>
        </div>
      </section>

      <section className="editorial-wiki-detail-main">
        <div className="editorial-wiki-content-width editorial-wiki-detail-layout">
          <article className="editorial-wiki-article">
            {wikiPost.summary && (
              <aside className="editorial-wiki-overview">
                <span>QUICK OVERVIEW</span>
                <p>{wikiPost.summary}</p>
              </aside>
            )}

            <section className="editorial-wiki-ai-summary">
              <div className="editorial-wiki-ai-summary-heading">
                <div>
                  <span className="editorial-wiki-label">
                    AI SUMMARY
                  </span>

                  <h2>이 문서의 핵심 내용</h2>
                </div>

                {!summary && (
                  <button
                    type="button"
                    disabled={summaryLoading}
                    onClick={createSummary}
                  >
                    {summaryLoading
                      ? 'SUMMARIZING...'
                      : 'GENERATE SUMMARY'}
                  </button>
                )}
              </div>

              {summary && (
                <div className="editorial-wiki-ai-result">
                  <p>{summary.summary}</p>

                  <small>
                    위키 본문의{' '}
                    {summary.sourceChunkCount}개 구간을
                    바탕으로 생성했습니다.
                  </small>

                  <button
                    type="button"
                    disabled={summaryLoading}
                    onClick={createSummary}
                  >
                    REGENERATE
                  </button>
                </div>
              )}

              {summaryError && (
                <p
                  className="editorial-wiki-summary-error"
                  role="alert"
                >
                  {summaryError}
                </p>
              )}
            </section>

            <div className="editorial-wiki-content">
              <ReactMarkdown>{wikiPost.content || ''}</ReactMarkdown>
            </div>
          </article>

          <aside className="editorial-wiki-detail-sidebar">
            <section className="editorial-wiki-detail-actions">
              <span>ACTIONS</span>

              <div className="editorial-wiki-like-wrapper">
                <LikeButton
                  count={like.likeCount}
                  liked={like.liked}
                  busy={like.busy}
                  onClick={toggleLike}
                />
              </div>

              {canManage && (
                <div className="editorial-wiki-owner-actions">
                  <Link
                    to={`/wiki/${wikiPost.id}/edit`}
                  >
                    EDIT DOCUMENT
                    <span>→</span>
                  </Link>

                  <button
                    type="button"
                    onClick={() =>
                      setShowDeleteConfirm(true)
                    }
                  >
                    DELETE DOCUMENT
                    <span>×</span>
                  </button>
                </div>
              )}
            </section>

            <section className="editorial-wiki-detail-info">
              <span>DOCUMENT INFO</span>

              <dl>
                <div>
                  <dt>CATEGORY</dt>
                  <dd>{wikiPost.categoryName}</dd>
                </div>

                <div>
                  <dt>STATUS</dt>
                  <dd>{wikiPost.status}</dd>
                </div>

                <div>
                  <dt>VIEWS</dt>
                  <dd>
                    {wikiPost.viewCount.toLocaleString()}
                  </dd>
                </div>

                <div>
                  <dt>LIKES</dt>
                  <dd>{like.likeCount}</dd>
                </div>
              </dl>
            </section>

            <Link
              className="editorial-wiki-sidebar-link"
              to="/wiki"
            >
              MORE WIKI DOCUMENTS
              <span>↗</span>
            </Link>
          </aside>
        </div>
      </section>

      {showDeleteConfirm && (
        <ConfirmDialog
          title="위키 문서를 삭제할까요?"
          message="삭제한 문서는 복구할 수 없습니다."
          confirmLabel="삭제"
          busy={deleting}
          onConfirm={deleteWikiPost}
          onCancel={() =>
            setShowDeleteConfirm(false)
          }
        />
      )}
    </div>
  );
}
