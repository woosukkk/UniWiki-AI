import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { api } from '../api.js';

const serviceItems = [
  {
    number: '01',
    label: 'WIKI',
    title: '함께 만드는 위키',
    description: '학생들이 직접 작성하고 수정하며 학교 정보를 축적합니다.',
    to: '/wiki',
  },
  {
    number: '02',
    label: 'Q&A BOARD',
    title: '질문하고 답변하기',
    description: '학교생활의 궁금한 점을 묻고 학생들의 경험을 공유합니다.',
    to: '/questions',
  },
  {
    number: '03',
    label: 'AI BOT',
    title: 'AI에게 바로 질문',
    description: '축적된 위키 문서를 검색해 출처와 함께 답변합니다.',
    to: '/chatbot',
  },
  {
    number: '04',
    label: 'DATA MAP',
    title: '정보 지형 확인',
    description: '현재 축적된 학교 정보의 분포와 부족한 영역을 확인합니다.',
    to: '/visualization',
  },
];

const popularWikiItems = [
  {
    category: '학사정보',
    title: '컴퓨터공학과 졸업요건',
    description: '졸업 학점, 필수 과목, 졸업 작품 요건을 한눈에 확인하세요.',
    views: '1.2K',
    to: '/wiki',
  },
  {
    category: '수강신청',
    title: '수강신청 완벽 가이드',
    description: '장바구니부터 수강 정정까지 필요한 절차를 정리했습니다.',
    views: '984',
    to: '/wiki',
  },
  {
    category: '장학정보',
    title: '장학금 종류와 신청 방법',
    description: '교내외 장학금의 지원 자격과 신청 방법을 확인하세요.',
    views: '821',
    to: '/wiki',
  },
];

const recentQuestions = [
  {
    id: 1,
    category: '수강신청',
    title: '복수전공 신청은 언제부터 가능한가요?',
    meta: '답변 2개 · 20분 전',
  },
  {
    id: 2,
    category: '학교생활',
    title: '기숙사 추가 신청 기간이 따로 있나요?',
    meta: '답변 1개 · 1시간 전',
  },
  {
    id: 3,
    category: '졸업요건',
    title: '캡스톤디자인도 전공 학점에 포함되나요?',
    meta: '답변 4개 · 3시간 전',
  },
];

export function HomePage() {
  const navigate = useNavigate();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [popularWikiPosts, setPopularWikiPosts] = useState([]);
  const [latestQuestions, setLatestQuestions] = useState([]);

  useEffect(() => {
    let active = true;

    Promise.all([
      api.searchWikiPosts('', 'OFFICIAL'),
      api.getQuestions(),
    ])
      .then(([wikiPosts, questions]) => {
        if (!active) return;

        setPopularWikiPosts(
          [...wikiPosts]
            .sort((left, right) => right.viewCount - left.viewCount)
            .slice(0, 3),
        );
        setLatestQuestions(
          [...questions]
            .sort(
              (left, right) =>
                new Date(right.createdAt) - new Date(left.createdAt),
            )
            .slice(0, 3),
        );
      })
      .catch(() => {
        if (!active) return;
        setPopularWikiPosts([]);
        setLatestQuestions([]);
      });

    return () => {
      active = false;
    };
  }, []);

  function handleSearch(event) {
    event.preventDefault();

    const keyword = searchKeyword.trim();

    if (!keyword) {
      return;
    }

    navigate(`/wiki?search=${encodeURIComponent(keyword)}`);
  }

  return (
    <div className="home-page">
      <section className="home-hero">
        <div className="home-hero-inner">
          <div className="home-hero-copy">
            <span className="home-section-label">
              ACADEMIC KNOWLEDGE PLATFORM
            </span>

            <h1>
              학생이 만드는 지식,
              <br />
              <em>AI가 연결하는 정보.</em>
            </h1>

            <p className="home-hero-description">
              학생들이 직접 작성한 학교 정보를 위키로 축적하고,
              <br />
              질문과 답변을 AI가 신뢰할 수 있는 지식으로 연결합니다.
            </p>

            <div className="home-hero-line" />

            <p className="home-hero-caption">
              YES, WE CAN BUILD KNOWLEDGE.
            </p>
          </div>

          <div className="home-hero-visual" aria-hidden="true">
            <div className="home-search-preview">
              <span>FIND YOUR ANSWERS</span>

              <div className="home-search-preview-input">
                <span>검색어 입력...</span>
                <strong>⌕</strong>
              </div>

              <div className="home-search-preview-tags">
                <span>졸업요건</span>
                <span>장학금</span>
              </div>
            </div>

            <div className="home-intro-card">
              <span className="home-intro-index">01 / UNIWIKI</span>
              <span className="home-intro-dot" />

              <strong>
                KNOWLEDGE
                <br />
                CONNECTED
              </strong>

              <span className="home-intro-side">
                STUDENT KNOWLEDGE NETWORK
              </span>
            </div>

            <span className="home-blue-decoration" />
          </div>
        </div>
      </section>

      <section className="home-search-section">
        <div className="home-content-width">
          <form className="home-search-form" onSubmit={handleSearch}>
            <label htmlFor="home-search">
              학교 정보를 검색해보세요
            </label>

            <div className="home-search-control">
              <span className="home-search-icon" aria-hidden="true">
                ⌕
              </span>

              <input
                id="home-search"
                type="search"
                value={searchKeyword}
                onChange={(event) => setSearchKeyword(event.target.value)}
                placeholder="졸업요건, 수강신청, 장학금 등을 검색하세요"
              />

              <button type="submit">
                SEARCH
                <span>→</span>
              </button>
            </div>

            <div className="home-popular-keywords">
              <strong>POPULAR</strong>
              <button
                type="button"
                onClick={() => setSearchKeyword('수강신청')}
              >
                수강신청
              </button>
              <button
                type="button"
                onClick={() => setSearchKeyword('졸업요건')}
              >
                졸업요건
              </button>
              <button
                type="button"
                onClick={() => setSearchKeyword('장학금')}
              >
                장학금
              </button>
              <button
                type="button"
                onClick={() => setSearchKeyword('복수전공')}
              >
                복수전공
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="home-services">
        <div className="home-content-width">
          <div className="home-section-heading">
            <div>
              <span className="home-section-label">EXPLORE UNIWIKI</span>
              <h2>필요한 정보를 찾는 네 가지 방법</h2>
            </div>

            <p>
              찾고, 질문하고, 답변하고,
              <br />
              다시 학교의 지식으로 축적합니다.
            </p>
          </div>

          <div className="home-service-grid">
            {serviceItems.map((item) => (
              <Link
                className="home-service-card"
                key={item.label}
                to={item.to}
              >
                <div className="home-service-card-top">
                  <span>{item.number}</span>
                  <strong>{item.label}</strong>
                </div>

                <div>
                  <h3>{item.title}</h3>
                  <p>{item.description}</p>
                </div>

                <span className="home-service-arrow">↗</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="home-knowledge-section">
        <div className="home-content-width home-knowledge-layout">
          <aside className="home-knowledge-aside">
            <span className="home-blue-sticker">WIKI ARCHIVE</span>

            <h2>
              Knowledge
              <br />
              Archive
            </h2>

            <p>
              학생들이 직접 정리한 학교 정보를 확인하고,
              부족한 내용에는 새로운 지식을 더해보세요.
            </p>

            <Link to="/wiki">
              전체 위키 보기
              <span>→</span>
            </Link>
          </aside>

          <div className="home-wiki-list">
            {popularWikiPosts.map((item, index) => (
              <Link
                className={
                  index === 1
                    ? 'home-wiki-item home-wiki-item-featured'
                    : 'home-wiki-item'
                }
                key={item.title}
                to={`/wiki/${item.id}`}
              >
                <span className="home-wiki-number">
                  {String(index + 1).padStart(2, '0')}
                </span>

                <div className="home-wiki-copy">
                  <span>{item.categoryName}</span>
                  <h3>{item.title}</h3>
                  <p>{item.summary || '요약이 등록되지 않은 문서입니다.'}</p>
                </div>

                <div className="home-wiki-meta">
                  <span>VIEW {item.viewCount.toLocaleString()}</span>
                  <strong>↗</strong>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="home-bottom-section">
        <div className="home-content-width home-bottom-grid">
          <div className="home-question-panel">
            <div className="home-panel-heading">
              <div>
                <span className="home-section-label">RECENT QUESTIONS</span>
                <h2>최근 질문</h2>
              </div>

              <Link to="/questions">
                VIEW ALL
                <span>→</span>
              </Link>
            </div>

            <div className="home-question-list">
              {latestQuestions.map((question) => (
                <Link
                  key={question.id}
                  to={`/questions/${question.id}`}
                  className="home-question-item"
                >
                  <span className="home-question-category">
                    {question.status === 'CLOSED' ? '답변 완료' : '답변 대기'}
                  </span>

                  <div>
                    <h3>{question.title}</h3>
                    <p>{question.authorNickname}</p>
                  </div>

                  <span>→</span>
                </Link>
              ))}
            </div>
          </div>

          <aside className="home-ai-panel">
            <span className="home-section-label">AI BOT</span>

            <h2>
              질문을 입력하면
              <br />
              AI가 지식을 연결합니다.
            </h2>

            <p>
              관련 위키 문서와 학생들의 답변을 찾아
              출처와 함께 정리해드립니다.
            </p>

            <Link to="/chatbot">
              ASK AI
              <span>→</span>
            </Link>

            <div className="home-ai-orbit" aria-hidden="true">
              <span />
              <span />
              <span />
            </div>
          </aside>
        </div>
      </section>

      <section className="home-data-preview">
        <div className="home-content-width home-data-preview-inner">
          <div>
            <span className="home-section-label">DATA MAP</span>

            <h2>
              현재 쌓인 학교 정보를
              <br />
              한눈에 확인하세요.
            </h2>

            <p>
              정보가 많은 영역과 아직 부족한 영역을 확인하고,
              새로운 위키 작성 주제를 발견할 수 있습니다.
            </p>
          </div>

          <div className="home-data-bubbles" aria-hidden="true">
            <span className="home-data-bubble bubble-large">
              학사정보
              <small>435</small>
            </span>

            <span className="home-data-bubble bubble-blue">
              진로·취업
              <small>559</small>
            </span>

            <span className="home-data-bubble bubble-outline">
              학생지원
              <small>171</small>
            </span>

            <span className="home-data-bubble bubble-small">
              SW·인증
              <small>42</small>
            </span>
          </div>

          <Link className="home-data-link" to="/visualization">
            OPEN DATA MAP
            <span>→</span>
          </Link>
        </div>
      </section>
    </div>
  );
}
