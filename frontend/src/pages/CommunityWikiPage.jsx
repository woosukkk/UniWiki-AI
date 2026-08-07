import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';

function formatDay(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(new Date(`${value}T00:00:00`));
}

export function CommunityWikiPage() {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadEntries = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setEntries(await api.getCommunityWiki());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadEntries();
  }, [loadEntries]);

  const dailyGroups = useMemo(() => Object.entries(
    entries.reduce((groups, entry) => {
      const day = entry.promotedDate;
      groups[day] = [...(groups[day] || []), entry];
      return groups;
    }, {}),
  ), [entries]);

  return (
    <main className="community-wiki-page container">
      <header className="community-wiki-heading">
        <Link to="/wiki">← 위키 목록</Link>
        <span className="section-kicker">COMMUNITY ARCHIVE</span>
        <h1>함께 만든 위키</h1>
        <p>질문 게시판에서 위키로 선정된 질문과 답변을 선정 날짜별로 모았습니다.</p>
      </header>

      {loading ? (
        <LoadingSpinner label="함께 만든 위키를 불러오는 중입니다." />
      ) : error ? (
        <ErrorMessage message={error} onRetry={loadEntries} />
      ) : dailyGroups.length === 0 ? (
        <section className="community-wiki-empty">
          <h2>아직 선정된 질문이 없습니다.</h2>
          <p>유용한 답변이 선정되면 날짜별로 이곳에 표시됩니다.</p>
        </section>
      ) : (
        <div className="community-wiki-days">
          {dailyGroups.map(([day, dayEntries]) => (
            <section className="community-wiki-day" key={day}>
              <header>
                <time dateTime={day}>{formatDay(day)}</time>
                <span>{dayEntries.length}개 질문</span>
              </header>
              <div className="community-wiki-links">
                {dayEntries.map((entry) => (
                  <Link to={`/questions/${entry.questionId}`} key={entry.selectedAnswerId}>
                    <span className="community-wiki-question-label">SELECTED Q&amp;A</span>
                    <h2>{entry.questionTitle}</h2>
                    <p>{entry.questionContent}</p>
                    <blockquote>{entry.selectedAnswerContent}</blockquote>
                    <span className="community-wiki-open">질문과 전체 답변 보기 →</span>
                  </Link>
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </main>
  );
}
