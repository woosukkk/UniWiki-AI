import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', { month: 'short', day: 'numeric' }).format(new Date(value));
}

export function QuestionListPage() {
  const { isAuthenticated } = useAuth();
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadQuestions = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const questionList = await api.getQuestions();
      const enriched = await Promise.all(questionList.map(async (question) => {
        try {
          const answers = await api.getAnswers(question.id);
          return { ...question, answerCount: answers.length };
        } catch {
          return { ...question, answerCount: 0 };
        }
      }));
      setQuestions(enriched);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadQuestions(); }, [loadQuestions]);

  return (
    <main className="question-list-page container">
      <header className="wiki-page-heading">
        <div>
          <span className="section-kicker">CAMPUS Q&amp;A</span>
          <h1>궁금한 것을 묻고 함께 답해요</h1>
          <p>학교생활에 관한 경험과 정보를 나눠보세요.</p>
        </div>
        {isAuthenticated && <Link className="button" to="/questions/new">질문 작성</Link>}
      </header>
      {loading ? <LoadingSpinner label="질문을 불러오는 중입니다" /> : error ? (
        <ErrorMessage message={error} onRetry={loadQuestions} />
      ) : questions.length === 0 ? (
        <section className="wiki-empty"><h2>등록된 질문이 없습니다</h2><p>첫 질문을 작성해 보세요.</p></section>
      ) : (
        <section className="routed-question-list">
          {questions.map((question, index) => (
            <Link className="routed-question-card" key={question.id} to={`/questions/${question.id}`}>
              <span className="question-index">{String(index + 1).padStart(2, '0')}</span>
              <div>
                <div className="card-meta">
                  <span className={`status status-${question.status.toLowerCase()}`}>{question.status}</span>
                  <span>{question.authorNickname}</span>
                  <span>{formatDate(question.createdAt)}</span>
                </div>
                <h2>{question.title}</h2>
                <p>{question.content}</p>
              </div>
              <div className="question-answer-count"><strong>{question.answerCount}</strong><span>답변</span></div>
            </Link>
          ))}
        </section>
      )}
    </main>
  );
}
