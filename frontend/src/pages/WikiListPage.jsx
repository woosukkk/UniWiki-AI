import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { WikiCard } from '../components/WikiCard.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

export function WikiListPage() {
  const { isAuthenticated } = useAuth();
  const [wikiPosts, setWikiPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadWikiPosts = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setWikiPosts(await api.getWikiPosts());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadWikiPosts();
  }, [loadWikiPosts]);

  return (
    <main className="wiki-page container">
      <header className="wiki-page-heading">
        <div>
          <span className="section-kicker">CAMPUS WIKI</span>
          <h1>함께 만드는 대학 생활 위키</h1>
          <p>학교 구성원이 직접 정리한 최신 정보를 확인해 보세요.</p>
        </div>
        {isAuthenticated && <Link className="button" to="/wiki/new">위키 작성</Link>}
      </header>

      {loading ? (
        <LoadingSpinner label="위키 문서를 불러오는 중입니다" />
      ) : error ? (
        <ErrorMessage message={error} onRetry={loadWikiPosts} />
      ) : wikiPosts.length === 0 ? (
        <section className="wiki-empty">
          <h2>등록된 위키가 없습니다</h2>
          <p>첫 번째 대학 생활 정보를 작성해 보세요.</p>
        </section>
      ) : (
        <section className="wiki-grid" aria-label="위키 문서 목록">
          {wikiPosts.map((wikiPost) => <WikiCard key={wikiPost.id} wikiPost={wikiPost} />)}
        </section>
      )}
    </main>
  );
}
