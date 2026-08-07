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
import { useAuth } from '../contexts/AuthContext.jsx';

const PAGE_SIZE = 8;

function formatDate(value) {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}

function getStatusLabel(status) {
  switch (status) {
    case 'CLOSED':
      return '답변 완료';

    case 'OPEN':
      return '답변 대기';

    default:
      return status || '답변 대기';
  }
}

function getStatusClass(status) {
  return status === 'CLOSED'
    ? 'question-status-closed'
    : 'question-status-open';
}

export function QuestionListPage() {
  const { isAuthenticated } = useAuth();
  const [searchParams, setSearchParams] =
    useSearchParams();

  const initialKeyword =
    searchParams.get('keyword') ||
    searchParams.get('search') ||
    '';

  const initialStatus =
    searchParams.get('status') || 'all';

  const initialSort =
    searchParams.get('sort') || 'latest';

  const initialPage = Math.max(
    1,
    Number(searchParams.get('page')) || 1,
  );

  const [questions, setQuestions] = useState([]);
  const [searchInput, setSearchInput] =
    useState(initialKeyword);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadQuestions = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const questionList = await api.getQuestions();

      const enrichedQuestions = await Promise.all(
        questionList.map(async (question) => {
          try {
            const answers = await api.getAnswers(
              question.id,
            );

            return {
              ...question,
              answerCount: answers.length,
            };
          } catch {
            return {
              ...question,
              answerCount: 0,
            };
          }
        }),
      );

      setQuestions(enrichedQuestions);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadQuestions();
  }, [loadQuestions]);

  useEffect(() => {
    setSearchInput(initialKeyword);
  }, [initialKeyword]);

  const filteredQuestions = useMemo(() => {
    const keyword = initialKeyword
      .trim()
      .toLowerCase();

    const filtered = questions.filter((question) => {
      const matchesKeyword =
        !keyword ||
        `${question.title} ${question.content} ${question.authorNickname}`
          .toLowerCase()
          .includes(keyword);

      const matchesStatus =
        initialStatus === 'all' ||
        question.status === initialStatus;

      return matchesKeyword && matchesStatus;
    });

    return [...filtered].sort((left, right) => {
      if (initialSort === 'answers') {
        return (
          right.answerCount - left.answerCount
        );
      }

      return (
        new Date(right.createdAt) -
        new Date(left.createdAt)
      );
    });
  }, [
    questions,
    initialKeyword,
    initialStatus,
    initialSort,
  ]);

  const totalPages = Math.max(
    1,
    Math.ceil(
      filteredQuestions.length / PAGE_SIZE,
    ),
  );

  const currentPage = Math.min(
    initialPage,
    totalPages,
  );

  const visibleQuestions = filteredQuestions.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  const totalAnswerCount = questions.reduce(
    (sum, question) =>
      sum + (question.answerCount || 0),
    0,
  );

  const closedQuestionCount = questions.filter(
    (question) => question.status === 'CLOSED',
  ).length;

  const solvedRate =
    questions.length > 0
      ? Math.round(
          (closedQuestionCount /
            questions.length) *
            100,
        )
      : 0;

  function updateParams(updates) {
    const nextParams = new URLSearchParams(
      searchParams,
    );

    Object.entries(updates).forEach(
      ([key, value]) => {
        if (
          value === undefined ||
          value === null ||
          value === '' ||
          value === 'all' ||
          value === 'latest'
        ) {
          nextParams.delete(key);
        } else {
          nextParams.set(key, String(value));
        }
      },
    );

    nextParams.delete('search');

    setSearchParams(nextParams);
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
    <div className="editorial-question-page">
      <section className="editorial-question-hero">
        <div className="editorial-question-content-width editorial-question-hero-inner">
          <div className="editorial-question-hero-copy">
            <span className="editorial-question-label">
              CAMPUS Q&amp;A
            </span>

            <h1>
              질문하고 답변받는
              <br />
              <em>캠퍼스 Q&amp;A.</em>
            </h1>

            <p>
              학교생활에서 생긴 궁금한 점을 질문하고,
              <br />
              다른 학생들의 경험과 정보를 공유하세요.
            </p>

            <div className="editorial-question-hero-line" />

            <span className="editorial-question-caption">
              ASK · ANSWER · SHARE
            </span>
          </div>

          <div
            className="editorial-question-hero-graphic"
            aria-hidden="true"
          >
            <div className="editorial-question-search-preview">
              <span>FIND A QUESTION</span>

              <div>
                질문을 검색해보세요...
                <strong>⌕</strong>
              </div>

              <small>
                STUDENT KNOWLEDGE NETWORK
              </small>
            </div>

            <div className="editorial-question-intro-card">
              <span>UNIWIKI</span>

              <strong>
                OPEN
                <br />
                DISCUSSION
              </strong>

              <small>
                QUESTIONS BECOME KNOWLEDGE
              </small>
            </div>

            <span className="editorial-question-blue-bar" />
            <span className="editorial-question-red-dot" />
          </div>
        </div>
      </section>

      <section className="editorial-question-summary">
        <div className="editorial-question-content-width editorial-question-summary-grid">
          <article>
            <strong>
              {questions.length.toLocaleString()}
            </strong>
            <span>TOTAL QUESTIONS</span>
          </article>

          <article>
            <strong>
              {totalAnswerCount.toLocaleString()}
            </strong>
            <span>TOTAL ANSWERS</span>
          </article>

          <article>
            <strong>{solvedRate}%</strong>
            <span>SOLVED RATE</span>
          </article>
        </div>
      </section>

      <section className="editorial-question-toolbar-section">
        <div className="editorial-question-content-width">
          <form
            className="editorial-question-toolbar"
            onSubmit={submitSearch}
          >
            <div className="editorial-question-toolbar-heading">
              <div>
                <span className="editorial-question-label">
                  QUESTION ARCHIVE
                </span>

                <h2>질문 찾아보기</h2>
              </div>

              {isAuthenticated ? (
                <Link
                  className="editorial-question-create-button"
                  to="/questions/new"
                >
                  NEW QUESTION
                  <span>＋</span>
                </Link>
              ) : (
                <Link
                  className="editorial-question-create-button"
                  to="/login"
                  state={{
                    from: {
                      pathname: '/questions/new',
                    },
                  }}
                >
                  LOGIN TO ASK
                  <span>→</span>
                </Link>
              )}
            </div>

            <div className="editorial-question-search-control">
              <span aria-hidden="true">⌕</span>

              <input
                type="search"
                value={searchInput}
                onChange={(event) =>
                  setSearchInput(event.target.value)
                }
                placeholder="질문 제목, 내용, 작성자 검색"
                aria-label="질문 검색"
              />

              <button type="submit">
                SEARCH
                <span>→</span>
              </button>
            </div>

            <div className="editorial-question-filter-row">
              <label>
                <span>STATUS</span>

                <select
                  value={initialStatus}
                  onChange={(event) =>
                    updateParams({
                      status: event.target.value,
                      page: '',
                    })
                  }
                >
                  <option value="all">
                    전체 질문
                  </option>
                  <option value="OPEN">
                    답변 대기
                  </option>
                  <option value="CLOSED">
                    답변 완료
                  </option>
                </select>
              </label>

              <label>
                <span>SORT BY</span>

                <select
                  value={initialSort}
                  onChange={(event) =>
                    updateParams({
                      sort: event.target.value,
                      page: '',
                    })
                  }
                >
                  <option value="latest">
                    최신순
                  </option>
                  <option value="answers">
                    답변 많은 순
                  </option>
                </select>
              </label>

              {(initialKeyword ||
                initialStatus !== 'all' ||
                initialSort !== 'latest') && (
                <button
                  className="editorial-question-reset-button"
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

      <section className="editorial-question-list-section">
        <div className="editorial-question-content-width">
          <div className="editorial-question-list-heading">
            <div>
              <span className="editorial-question-label">
                RECENT QUESTIONS
              </span>

              <h2>질문 목록</h2>
            </div>

            <p>
              총{' '}
              <strong>
                {filteredQuestions.length.toLocaleString()}
              </strong>
              개의 질문
            </p>
          </div>

          {loading ? (
            <div className="editorial-question-state">
              <LoadingSpinner label="질문을 불러오는 중입니다" />
            </div>
          ) : error ? (
            <div className="editorial-question-state">
              <ErrorMessage
                message={error}
                onRetry={loadQuestions}
              />
            </div>
          ) : visibleQuestions.length === 0 ? (
            <section className="editorial-question-empty">
              <span>NO QUESTIONS</span>

              <h2>검색 결과가 없습니다.</h2>

              <p>
                검색 조건을 변경하거나 새로운 질문을
                작성해보세요.
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
              <div className="editorial-question-list">
                {visibleQuestions.map(
                  (question, index) => (
                    <Link
                      className={
                        question.status === 'CLOSED'
                          ? 'editorial-question-card editorial-question-card-closed'
                          : 'editorial-question-card'
                      }
                      key={question.id}
                      to={`/questions/${question.id}`}
                    >
                      <div className="editorial-question-card-index">
                        {String(
                          (currentPage - 1) *
                            PAGE_SIZE +
                            index +
                            1,
                        ).padStart(2, '0')}
                      </div>

                      <div className="editorial-question-card-main">
                        <div className="editorial-question-card-meta">
                          <span
                            className={[
                              'editorial-question-status',
                              getStatusClass(
                                question.status,
                              ),
                            ].join(' ')}
                          >
                            {getStatusLabel(
                              question.status,
                            )}
                          </span>

                          <span>
                            {question.authorNickname}
                          </span>

                          <span>
                            {formatDate(
                              question.createdAt,
                            )}
                          </span>
                        </div>

                        <h3>{question.title}</h3>

                        <p>{question.content}</p>
                      </div>

                      <div className="editorial-question-card-count">
                        <strong>
                          {question.answerCount}
                        </strong>
                        <span>ANSWERS</span>
                      </div>

                      <span className="editorial-question-card-arrow">
                        →
                      </span>
                    </Link>
                  ),
                )}
              </div>

              {totalPages > 1 && (
                <nav
                  className="editorial-question-pagination"
                  aria-label="질문 페이지 이동"
                >
                  <button
                    type="button"
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
                        type="button"
                        key={pageNumber}
                        className={
                          pageNumber === currentPage
                            ? 'active'
                            : ''
                        }
                        onClick={() =>
                          updateParams({
                            page:
                              pageNumber === 1
                                ? ''
                                : pageNumber,
                          })
                        }
                      >
                        {String(pageNumber).padStart(
                          2,
                          '0',
                        )}
                      </button>
                    ))}
                  </div>

                  <button
                    type="button"
                    disabled={
                      currentPage === totalPages
                    }
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

      <section className="editorial-question-guide-section">
        <div className="editorial-question-content-width editorial-question-guide-grid">
          <article>
            <span>01</span>
            <strong>먼저 검색하세요</strong>
            <p>
              비슷한 질문이 이미 등록되어 있는지
              확인해주세요.
            </p>
          </article>

          <article>
            <span>02</span>
            <strong>구체적으로 작성하세요</strong>
            <p>
              상황과 궁금한 점을 자세히 작성할수록 좋은
              답변을 받을 수 있습니다.
            </p>
          </article>

          <article>
            <span>03</span>
            <strong>좋은 답변을 채택하세요</strong>
            <p>
              도움이 된 답변을 채택해 다른 학생에게도
              유용한 정보로 남겨주세요.
            </p>
          </article>
        </div>
      </section>
    </div>
  );
}