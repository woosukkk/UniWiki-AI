import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { AnswerItem } from '../components/AnswerItem.jsx';
import { ConfirmDialog } from '../components/ConfirmDialog.jsx';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { LikeButton } from '../components/LikeButton.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(value));
}

export function QuestionDetailPage() {
  const { questionId } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [question, setQuestion] = useState(null);
  const [answers, setAnswers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showDelete, setShowDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [answerContent, setAnswerContent] = useState('');
  const [answerSubmitting, setAnswerSubmitting] = useState(false);
  const [answerToDelete, setAnswerToDelete] = useState(null);
  const [actionError, setActionError] = useState('');
  const [questionLike, setQuestionLike] = useState({ likeCount: 0, liked: false, busy: false });
  const [answerLikes, setAnswerLikes] = useState({});

  const loadQuestion = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [questionData, answerList, questionLikeStatus] = await Promise.all([
        api.getQuestion(questionId),
        api.getAnswers(questionId),
        isAuthenticated ? api.getQuestionLikeStatus(questionId) : api.getQuestionLikes(questionId),
      ]);
      const answerLikeEntries = await Promise.all(answerList.map(async (answer) => {
        const status = isAuthenticated
          ? await api.getAnswerLikeStatus(answer.id)
          : await api.getAnswerLikes(answer.id);
        return [answer.id, { ...status, busy: false }];
      }));
      setQuestion(questionData);
      setAnswers(answerList);
      setQuestionLike({ ...questionLikeStatus, busy: false });
      setAnswerLikes(Object.fromEntries(answerLikeEntries));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [questionId, isAuthenticated]);

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

  async function createAnswer(event) {
    event.preventDefault();
    if (!answerContent.trim()) return;
    setAnswerSubmitting(true);
    setActionError('');
    try {
      const created = await api.createAnswer(questionId, { content: answerContent.trim() });
      setAnswers((current) => [...current, created]);
      setAnswerLikes((current) => ({ ...current, [created.id]: { likeCount: 0, liked: false, busy: false } }));
      setAnswerContent('');
    } catch (requestError) {
      setActionError(requestError.message);
    } finally {
      setAnswerSubmitting(false);
    }
  }

  async function updateAnswer(answerId, content) {
    setActionError('');
    try {
      const updated = await api.updateAnswer(answerId, { content });
      setAnswers((current) => current.map((answer) => answer.id === answerId ? updated : answer));
      return true;
    } catch (requestError) {
      setActionError(requestError.message);
      return false;
    }
  }

  async function deleteAnswer() {
    if (!answerToDelete) return;
    setDeleting(true);
    setActionError('');
    try {
      await api.deleteAnswer(answerToDelete.id);
      setAnswers((current) => current.filter((answer) => answer.id !== answerToDelete.id));
      setAnswerToDelete(null);
    } catch (requestError) {
      setActionError(requestError.message);
    } finally {
      setDeleting(false);
    }
  }

  async function acceptAnswer(answerId) {
    setActionError('');
    try {
      const accepted = await api.acceptAnswer(answerId);
      setAnswers((current) => current.map((answer) => answer.id === answerId ? accepted : answer));
      setQuestion((current) => ({ ...current, status: 'CLOSED' }));
    } catch (requestError) {
      setActionError(requestError.message);
    }
  }

  async function toggleQuestionLike() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/questions/${questionId}` } } });
      return;
    }
    setQuestionLike((current) => ({ ...current, busy: true }));
    try {
      if (questionLike.liked) {
        await api.unlikeQuestion(questionId);
        setQuestionLike((current) => ({ likeCount: Math.max(0, current.likeCount - 1), liked: false, busy: false }));
      } else {
        const response = await api.likeQuestion(questionId);
        setQuestionLike({ ...response, busy: false });
      }
    } catch (requestError) {
      setActionError(requestError.message);
      setQuestionLike((current) => ({ ...current, busy: false }));
    }
  }

  async function toggleAnswerLike(answerId) {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/questions/${questionId}` } } });
      return;
    }
    const currentLike = answerLikes[answerId] || { likeCount: 0, liked: false };
    setAnswerLikes((current) => ({ ...current, [answerId]: { ...currentLike, busy: true } }));
    try {
      if (currentLike.liked) {
        await api.unlikeAnswer(answerId);
        setAnswerLikes((current) => ({ ...current, [answerId]: { likeCount: Math.max(0, currentLike.likeCount - 1), liked: false, busy: false } }));
      } else {
        const response = await api.likeAnswer(answerId);
        setAnswerLikes((current) => ({ ...current, [answerId]: { ...response, busy: false } }));
      }
    } catch (requestError) {
      setActionError(requestError.message);
      setAnswerLikes((current) => ({ ...current, [answerId]: { ...currentLike, busy: false } }));
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
        <div className="detail-actions">
          <LikeButton count={questionLike.likeCount} liked={questionLike.liked} busy={questionLike.busy} onClick={toggleQuestionLike} />
          {isAuthor && (
            <div className="owner-actions">
              <Link className="text-button" to={`/questions/${question.id}/edit`}>수정</Link>
              <button className="text-button danger" onClick={() => setShowDelete(true)}>삭제</button>
            </div>
          )}
        </div>
      </article>
      <section className="question-answers-section">
        <div className="answers-heading"><h2>답변 <em>{answers.length}</em></h2></div>
        {actionError && <div className="auth-error" role="alert">{actionError}</div>}
        {answers.length === 0 ? <div className="no-answers">아직 등록된 답변이 없습니다.</div> : answers.map((answer) => (
          <AnswerItem
            key={answer.id}
            answer={answer}
            like={answerLikes[answer.id]}
            canEdit={user?.id === answer.authorId}
            canAccept={isAuthor && !answer.accepted && !answers.some((item) => item.accepted)}
            onUpdate={updateAnswer}
            onDelete={setAnswerToDelete}
            onAccept={acceptAnswer}
            onLike={toggleAnswerLike}
          />
        ))}
        {isAuthenticated ? (
          <form className="answer-form" onSubmit={createAnswer}>
            <div className="answer-form-heading"><strong>답변 작성</strong><span>{answerContent.length}자</span></div>
            <textarea value={answerContent} onChange={(event) => setAnswerContent(event.target.value)} placeholder="알고 있는 정보와 경험을 구체적으로 알려주세요." />
            <div className="form-footer"><span>정확하고 친절한 답변을 작성해 주세요.</span><button className="button button-small" disabled={answerSubmitting || !answerContent.trim()}>{answerSubmitting ? '등록 중...' : '답변 등록'}</button></div>
          </form>
        ) : (
          <div className="answer-login-guide"><Link to="/login" state={{ from: { pathname: `/questions/${questionId}` } }}>로그인</Link>하면 답변을 작성할 수 있습니다.</div>
        )}
      </section>
      {showDelete && <ConfirmDialog title="질문을 삭제할까요?" message="질문과 관련 답변이 함께 삭제될 수 있습니다." confirmLabel="삭제" busy={deleting} onConfirm={deleteQuestion} onCancel={() => setShowDelete(false)} />}
      {answerToDelete && <ConfirmDialog title="답변을 삭제할까요?" message="삭제한 답변은 복구할 수 없습니다." confirmLabel="삭제" busy={deleting} onConfirm={deleteAnswer} onCancel={() => setAnswerToDelete(null)} />}
    </main>
  );
}
