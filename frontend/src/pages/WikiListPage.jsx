import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { WikiCard } from '../components/WikiCard.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

const PAGE_SIZE = 9;

export function WikiListPage() {
  const { isAuthenticated } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get('keyword') || '';
  const source = 'official';
  const categoryId = searchParams.get('category') || '';
  const sort = searchParams.get('sort') || 'latest';
  const page = Math.max(1, Number(searchParams.get('page')) || 1);
  const [searchInput, setSearchInput] = useState(keyword);
  const [wikiPosts, setWikiPosts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadWikiPosts = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [posts, categoryList] = await Promise.all([
        api.searchWikiPosts(
          keyword,
          source === 'everytime' ? 'EVERYTIME' : 'OFFICIAL',
          source === 'everytime' ? 'LECTURE_REVIEW' : null,
        ),
        api.getCategories(),
      ]);
      setWikiPosts(posts);
      setCategories(categoryList);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [keyword, source]);

  useEffect(() => {
    setSearchInput(keyword);
    loadWikiPosts();
  }, [keyword, loadWikiPosts]);

  const filteredPosts = useMemo(() => {
    const filtered = categoryId
      ? wikiPosts.filter((post) => String(post.categoryId) === categoryId)
      : wikiPosts;
    return [...filtered].sort((left, right) => sort === 'views'
      ? right.viewCount - left.viewCount
      : new Date(right.createdAt) - new Date(left.createdAt));
  }, [wikiPosts, categoryId, sort]);

  const totalPages = Math.max(1, Math.ceil(filteredPosts.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const visiblePosts = filteredPosts.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  function updateParams(updates) {
    const next = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([key, value]) => {
      if (value) next.set(key, String(value));
      else next.delete(key);
    });
    setSearchParams(next);
  }

  function submitSearch(event) {
    event.preventDefault();
    updateParams({ keyword: searchInput.trim(), page: '' });
  }

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

      <nav className="wiki-source-tabs" aria-label="위키 자료 출처">
        <button
          className={source === 'official' ? 'active' : ''}
          onClick={() => updateParams({ source: '', type: '', category: '', page: '' })}
        >공식·학교 위키</button>
      </nav>

      <section className="wiki-toolbar" aria-label="위키 검색 및 필터">
        <form className="wiki-search" onSubmit={submitSearch}>
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="제목과 본문 검색"
            aria-label="위키 검색어"
          />
          <button className="button button-small">검색</button>
        </form>
        <div className="wiki-filters">
          {source === 'official' && (
            <select
              aria-label="카테고리 필터"
              value={categoryId}
              onChange={(event) => updateParams({ category: event.target.value, page: '' })}
            >
              <option value="">전체 카테고리</option>
              {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
          )}
          <select
            aria-label="정렬 기준"
            value={sort}
            onChange={(event) => updateParams({ sort: event.target.value === 'latest' ? '' : event.target.value, page: '' })}
          >
            <option value="latest">최신순</option>
            <option value="views">조회수순</option>
          </select>
        </div>
      </section>

      {loading ? (
        <LoadingSpinner label="위키 문서를 불러오는 중입니다" />
      ) : error ? (
        <ErrorMessage message={error} onRetry={loadWikiPosts} />
      ) : visiblePosts.length === 0 ? (
        <section className="wiki-empty">
          <h2>검색 결과가 없습니다</h2>
          <p>검색어나 카테고리를 변경해 보세요.</p>
          <button className="text-button" onClick={() => setSearchParams({})}>검색 조건 초기화</button>
        </section>
      ) : (
        <>
          <section className="wiki-grid" aria-label="위키 문서 목록">
            {visiblePosts.map((wikiPost, index) => (
              <WikiCard
                key={wikiPost.id}
                wikiPost={wikiPost}
                index={(currentPage - 1) * PAGE_SIZE + index}
              />
            ))}
          </section>
          {totalPages > 1 && (
            <nav className="pagination" aria-label="위키 페이지 이동">
              <button disabled={currentPage === 1} onClick={() => updateParams({ page: currentPage - 1 })}>이전</button>
              {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
                <button
                  className={pageNumber === currentPage ? 'active' : ''}
                  key={pageNumber}
                  onClick={() => updateParams({ page: pageNumber === 1 ? '' : pageNumber })}
                >{pageNumber}</button>
              ))}
              <button disabled={currentPage === totalPages} onClick={() => updateParams({ page: currentPage + 1 })}>다음</button>
            </nav>
          )}
        </>
      )}
    </main>
  );
}
