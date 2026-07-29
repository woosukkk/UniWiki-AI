import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { ConfirmDialog } from '../components/ConfirmDialog.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(value));
}

export function WikiDetailPage() {
  const { wikiPostId } = useParams();
  const navigate = useNavigate();
  const { user, isAdmin } = useAuth();
  const [wikiPost, setWikiPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const loadWikiPost = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setWikiPost(await api.getWikiPost(wikiPostId));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [wikiPostId]);

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
          {canManage && (
            <div className="wiki-owner-actions">
              <Link className="text-button" to={`/wiki/${wikiPost.id}/edit`}>수정</Link>
              <button className="text-button danger" type="button" onClick={() => setShowDeleteConfirm(true)}>삭제</button>
            </div>
          )}
        </header>
        {wikiPost.summary && <aside className="wiki-summary"><strong>한눈에 보기</strong><p>{wikiPost.summary}</p></aside>}
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
