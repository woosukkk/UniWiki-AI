import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import {
  Link,
  useSearchParams,
} from 'react-router-dom';

import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { WikiCard } from '../components/WikiCard.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

const PAGE_SIZE = 9;

export function WikiListPage() {
  const { isAuthenticated } = useAuth();

  const [searchParams, setSearchParams] = useSearchParams();

  const keyword =
    searchParams.get('keyword') ||
    searchParams.get('search') ||
    '';

  const categoryId = searchParams.get('category') || '';
  const sort = searchParams.get('sort') || 'latest';
  const page = Math.max(
    1,
    Number(searchParams.get('page')) || 1,
  );

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
        api.searchWikiPosts(keyword),
        api.getCategories(),
      ]);

      setWikiPosts(posts);
      setCategories(categoryList);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [keyword]);

  useEffect(() => {
    setSearchInput(keyword);
    loadWikiPosts();
  }, [keyword, loadWikiPosts]);

  const filteredPosts = useMemo(() => {
    const filtered = categoryId
      ? wikiPosts.filter(
          (post) =>
            String(post.categoryId) === categoryId,
        )
      : wikiPosts;

    return [...filtered].sort((left, right) =>
      sort === 'views'
        ? right.viewCount - left.viewCount
        : new Date(right.createdAt) -
          new Date(left.createdAt),
    );
  }, [wikiPosts, categoryId, sort]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredPosts.length / PAGE_SIZE),
  );

  const currentPage = Math.min(page, totalPages);

  const visiblePosts = filteredPosts.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  function updateParams(updates) {
    const next = new URLSearchParams(searchParams);

    Object.entries(updates).forEach(([key, value]) => {
      if (value) {
        next.set(key, String(value));
      } else {
        next.delete(key);
      }
    });

    next.delete('search');

    setSearchParams(next);
  }

  function submitSearch(event) {
    event.preventDefault();

    updateParams({
      keyword: searchInput.trim(),
      page: '',
    });
  }

  function resetFilters() {
    setSearchInput('');
    setSearchParams({});
  }

  return (
    <div className="editorial-wiki-page">
      <section className="editorial-wiki-hero">
        <div className="editorial-wiki-content-width editorial-wiki-hero-inner">
          <div className="editorial-wiki-hero-copy">
            <span className="editorial-wiki-label">
              WIKI ARCHIVE
            </span>

            <h1>
              학생들이 함께 정리한
              <br />
              <em>캠퍼스 지식.</em>
            </h1>

            <p>
              학교생활에 필요한 정보를 찾고,
              <br />
              부족한 내용에는 새로운 지식을 더해보세요.
            </p>

            <div className="editorial-wiki-hero-line" />

            <span className="editorial-wiki-hero-caption">
              READ · WRITE · CONNECT
            </span>
          </div>

          <div
            className="editorial-wiki-hero-graphic"
            aria-hidden="true"
          >
            <div className="editorial-wiki-document-card">
              <span>UNIWIKI DOCUMENT</span>

              <strong>
                KNOWLEDGE
                <br />
                ARCHIVE
              </strong>

              <div className="editorial-wiki-document-lines">
                <span />
                <span />
                <span />
                <span />
              </div>

              <small>UPDATED BY STUDENTS</small>
            </div>

            <span className="editorial-wiki-blue-bar" />
            <span className="editorial-wiki-red-dot" />
          </div>
        </div>
      </section>

      <section className="editorial-wiki-search-section">
        <div className="editorial-wiki-content-width">
          <form
            className="editorial-wiki-search-form"
            onSubmit={submitSearch}
          >
            <div className="editorial-wiki-search-heading">
              <div>
                <span className="editorial-wiki-label">
                  FIND KNOWLEDGE
                </span>
                <h2>무엇을 찾고 있나요?</h2>
              </div>

              {isAuthenticated && (
                <Link
                  className="editorial-wiki-create-button"
                  to="/wiki/new"
                >
                  NEW WIKI
                  <span>＋</span>
                </Link>
              )}
            </div>

            <div className="editorial-wiki-search-control">
              <span aria-hidden="true">⌕</span>

              <input
                value={searchInput}
                onChange={(event) =>
                  setSearchInput(event.target.value)
                }
                placeholder="제목과 본문에서 검색하세요"
                aria-label="위키 검색어"
              />

              <button type="submit">
                SEARCH
                <span>→</span>
              </button>
            </div>

            <div className="editorial-wiki-filter-row">
              <label>
                <span>CATEGORY</span>

                <select
                  aria-label="카테고리 필터"
                  value={categoryId}
                  onChange={(event) =>
                    updateParams({
                      category: event.target.value,
                      page: '',
                    })
                  }
                >
                  <option value="">전체 카테고리</option>

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
                <span>SORT BY</span>

                <select
                  aria-label="정렬 기준"
                  value={sort}
                  onChange={(event) =>
                    updateParams({
                      sort:
                        event.target.value === 'latest'
                          ? ''
                          : event.target.value,
                      page: '',
                    })
                  }
                >
                  <option value="latest">최신순</option>
                  <option value="views">조회수순</option>
                </select>
              </label>

              {(keyword || categoryId || sort !== 'latest') && (
                <button
                  className="editorial-wiki-reset-button"
                  type="button"
                  onClick={resetFilters}
                >
                  RESET FILTERS
                </button>
              )}
            </div>
          </form>
        </div>
      </section>

      <section className="editorial-wiki-list-section">
        <div className="editorial-wiki-content-width">
          <div className="editorial-wiki-list-heading">
            <div>
              <span className="editorial-wiki-label">
                WIKI DOCUMENTS
              </span>

              <h2>위키 문서</h2>
            </div>

            <p>
              총{' '}
              <strong>
                {filteredPosts.length.toLocaleString()}
              </strong>
              개의 문서
            </p>
          </div>

          {loading ? (
            <div className="editorial-wiki-state">
              <LoadingSpinner label="위키 문서를 불러오는 중입니다" />
            </div>
          ) : error ? (
            <div className="editorial-wiki-state">
              <ErrorMessage
                message={error}
                onRetry={loadWikiPosts}
              />
            </div>
          ) : visiblePosts.length === 0 ? (
            <section className="editorial-wiki-empty">
              <span>NO RESULT</span>
              <h2>검색 결과가 없습니다.</h2>
              <p>
                검색어나 카테고리를 변경해보세요.
              </p>

              <button
                type="button"
                onClick={resetFilters}
              >
                검색 조건 초기화
                <span>→</span>
              </button>
            </section>
          ) : (
            <>
              <section
                className="editorial-wiki-grid"
                aria-label="위키 문서 목록"
              >
                {visiblePosts.map((wikiPost, index) => (
                  <WikiCard
                    key={wikiPost.id}
                    wikiPost={wikiPost}
                    index={
                      (currentPage - 1) * PAGE_SIZE +
                      index
                    }
                  />
                ))}
              </section>

              {totalPages > 1 && (
                <nav
                  className="editorial-wiki-pagination"
                  aria-label="위키 페이지 이동"
                >
                  <button
                    disabled={currentPage === 1}
                    onClick={() =>
                      updateParams({
                        page: currentPage - 1,
                      })
                    }
                  >
                    ← PREV
                  </button>

                  <div>
                    {Array.from(
                      { length: totalPages },
                      (_, index) => index + 1,
                    ).map((pageNumber) => (
                      <button
                        className={
                          pageNumber === currentPage
                            ? 'active'
                            : ''
                        }
                        key={pageNumber}
                        onClick={() =>
                          updateParams({
                            page:
                              pageNumber === 1
                                ? ''
                                : pageNumber,
                          })
                        }
                      >
                        {String(pageNumber).padStart(2, '0')}
                      </button>
                    ))}
                  </div>

                  <button
                    disabled={currentPage === totalPages}
                    onClick={() =>
                      updateParams({
                        page: currentPage + 1,
                      })
                    }
                  >
                    NEXT →
                  </button>
                </nav>
              )}
            </>
          )}
        </div>
      </section>
    </div>
  );
}