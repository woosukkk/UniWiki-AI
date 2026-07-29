import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { ConfirmDialog } from '../components/ConfirmDialog.jsx';
import { LikeButton } from '../components/LikeButton.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(value));
}

export function WikiDetailPage() {
  const { wikiPostId } = useParams();
  const navigate = useNavigate();
  const { user, isAdmin, isAuthenticated } = useAuth();
  const [wikiPost, setWikiPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [like, setLike] = useState({ likeCount: 0, liked: false, busy: false });
  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summaryError, setSummaryError] = useState('');

  const loadWikiPost = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [post, likeStatus] = await Promise.all([
        api.getWikiPost(wikiPostId),
        api.getWikiPostLikes(wikiPostId, isAuthenticated),
      ]);
      setWikiPost(post);
      setLike({ ...likeStatus, busy: false });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [wikiPostId, isAuthenticated]);

  useEffect(() => {
    loadWikiPost();
  }, [loadWikiPost]);

  if (loading) return <main className="wiki-page container"><LoadingSpinner /></main>;
  if (error) return <main className="wiki-page container"><ErrorMessage message={error} onRetry={loadWikiPost} /></main>;
  if (!wikiPost) return null;

  const canManage = user?.id === wikiPost.authorId || isAdmin;

  async function deleteWikiPost() {
    setDeleting(true);
    setError('');
    try {
      await api.deleteWikiPost(wikiPost.id);
      navigate('/wiki', { replace: true });
    } catch (requestError) {
      setError(requestError.message);
      setShowDeleteConfirm(false);
      setDeleting(false);
    }
  }

  async function toggleLike() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/wiki/${wikiPostId}` } } });
      return;
    }
    setLike((current) => ({ ...current, busy: true }));
    try {
      if (like.liked) {
        await api.unlikeWikiPost(wikiPostId);
        setLike((current) => ({ likeCount: Math.max(0, current.likeCount - 1), liked: false, busy: false }));
      } else {
        const response = await api.likeWikiPost(wikiPostId);
        setLike({ ...response, busy: false });
      }
    } catch (requestError) {
      setError(requestError.message);
      setLike((current) => ({ ...current, busy: false }));
    }
  }

  async function createSummary() {
    setSummaryLoading(true);
    setSummaryError('');
    try {
      setSummary(await api.summarizeWikiPost(wikiPostId));
    } catch (requestError) {
      setSummaryError(requestError.status === 404
        ? '아직 AI 검색 저장소에 반영되지 않은 문서입니다.'
        : 'AI 요약을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      setSummaryLoading(false);
    }
  }
  return (
    <main className="wiki-detail-page container">
      <Link className="back-button" to="/wiki">← 위키 목록으로</Link>
      <article className="wiki-detail">
        <header>
          <div className="wiki-detail-meta">
            <span className="wiki-category">{wikiPost.categoryName}</span>
            <span>{wikiPost.status}</span>
            <span>조회 {wikiPost.viewCount.toLocaleString()}</span>
          </div>
          <h1>{wikiPost.title}</h1>
          <div className="wiki-byline">
            <strong>{wikiPost.authorNickname}</strong>
            <span>{formatDate(wikiPost.createdAt)}</span>
          </div>
          <div className="wiki-like"><LikeButton count={like.likeCount} liked={like.liked} busy={like.busy} onClick={toggleLike} /></div>
          {canManage && (
            <div className="wiki-owner-actions">
              <Link className="text-button" to={`/wiki/${wikiPost.id}/edit`}>수정</Link>
              <button className="text-button danger" type="button" onClick={() => setShowDeleteConfirm(true)}>삭제</button>
            </div>
          )}
        </header>
        {wikiPost.summary && <aside className="wiki-summary"><strong>한눈에 보기</strong><p>{wikiPost.summary}</p></aside>}
        <section className="wiki-summary-panel">
          <div><span>AI SUMMARY</span><h2>이 문서 핵심 요약</h2></div>
          {!summary && <button className="button button-small" disabled={summaryLoading} onClick={createSummary}>{summaryLoading ? '요약 중…' : 'AI 요약하기'}</button>}
          {summary && (
            <div className="wiki-ai-summary">
              <p>{summary.summary}</p>
              <small>위키 본문의 {summary.sourceChunkCount}개 구간을 바탕으로 생성했습니다.</small>
              <button className="text-button" disabled={summaryLoading} onClick={createSummary}>다시 요약</button>
            </div>
          )}
          {summaryError && <p className="wiki-summary-error" role="alert">{summaryError}</p>}
        </section>
        <div className="wiki-content">{wikiPost.content}</div>
      </article>
      {showDeleteConfirm && (
        <ConfirmDialog
          title="위키 문서를 삭제할까요?"
          message="삭제한 문서는 복구할 수 없습니다."
          confirmLabel="삭제"
          busy={deleting}
          onConfirm={deleteWikiPost}
          onCancel={() => setShowDeleteConfirm(false)}
        />
      )}
    </main>
  );
}
