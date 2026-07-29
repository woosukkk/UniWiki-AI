import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { ConfirmDialog } from '../components/ConfirmDialog.jsx';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(value));
}

export function QuestionDetailPage() {
  const { questionId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [question, setQuestion] = useState(null);
  const [answers, setAnswers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showDelete, setShowDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const loadQuestion = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [questionData, answerList] = await Promise.all([
        api.getQuestion(questionId),
        api.getAnswers(questionId),
      ]);
      setQuestion(questionData);
      setAnswers(answerList);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [questionId]);

  useEffect(() => { loadQuestion(); }, [loadQuestion]);

  async function deleteQuestion() {
    setDeleting(true);
    try {
      await api.deleteQuestion(questionId);
      navigate('/questions', { replace: true });
    } catch (requestError) {
      setError(requestError.message);
      setShowDelete(false);
      setDeleting(false);
    }
  }

  if (loading) return <main className="detail-page container"><LoadingSpinner /></main>;
  if (error) return <main className="detail-page container"><ErrorMessage message={error} onRetry={loadQuestion} /></main>;
  if (!question) return null;
  const isAuthor = user?.id === question.authorId;

  return (
    <main className="detail-page container">
      <Link className="back-button" to="/questions">← 질문 목록으로</Link>
      <article className="question-detail">
        <div className="detail-meta">
          <span className={`status status-${question.status.toLowerCase()}`}>{question.status}</span>
          <span>{question.authorNickname}</span>
          <span>{formatDate(question.createdAt)}</span>
        </div>
        <h1>{question.title}</h1>
        <p className="detail-content">{question.content}</p>
        {isAuthor && (
          <div className="detail-actions">
            <span />
            <div className="owner-actions">
              <Link className="text-button" to={`/questions/${question.id}/edit`}>수정</Link>
              <button className="text-button danger" onClick={() => setShowDelete(true)}>삭제</button>
            </div>
          </div>
        )}
      </article>
      <section className="question-answers-section">
        <div className="answers-heading"><h2>답변 <em>{answers.length}</em></h2></div>
        {answers.length === 0 ? <div className="no-answers">아직 등록된 답변이 없습니다.</div> : answers.map((answer) => (
          <article className="answer-card" key={answer.id}>
            <div className="answer-avatar">{answer.authorNickname.slice(0, 1)}</div>
            <div className="answer-body">
              <div className="answer-meta"><strong>{answer.authorNickname}</strong><span>{formatDate(answer.createdAt)}</span>{answer.accepted && <span className="accepted">채택된 답변</span>}</div>
              <p>{answer.content}</p>
            </div>
          </article>
        ))}
      </section>
      {showDelete && <ConfirmDialog title="질문을 삭제할까요?" message="질문과 관련 답변이 함께 삭제될 수 있습니다." confirmLabel="삭제" busy={deleting} onConfirm={deleteQuestion} onCancel={() => setShowDelete(false)} />}
    </main>
  );
}
