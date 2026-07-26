import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from './api.js';

const initialQuestionForm = { title: '', content: '' };

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('uniwiki-user'));
  } catch {
    return null;
  }
}

function App() {
  const [user, setUser] = useState(getStoredUser);
  const [questions, setQuestions] = useState([]);
  const [questionLikes, setQuestionLikes] = useState({});
  const [selectedQuestion, setSelectedQuestion] = useState(null);
  const [answers, setAnswers] = useState([]);
  const [answerLikes, setAnswerLikes] = useState({});
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [search, setSearch] = useState('');
  const [showAuth, setShowAuth] = useState(false);
  const [showQuestionForm, setShowQuestionForm] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState(false);

  const loadQuestions = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getQuestions();
      setQuestions(data);
      const counts = await Promise.all(
        data.map(async (question) => {
          try {
            const result = await api.getQuestionLikes(question.id);
            return [question.id, { count: result.likeCount, liked: false }];
          } catch {
            return [question.id, { count: 0, liked: false }];
          }
        }),
      );
      setQuestionLikes(Object.fromEntries(counts));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadQuestions();
  }, [loadQuestions]);

  const filteredQuestions = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return questions;
    return questions.filter((question) =>
      `${question.title} ${question.content} ${question.authorNickname}`
        .toLowerCase()
        .includes(keyword),
    );
  }, [questions, search]);

  function requireLogin() {
    if (user) return true;
    setShowAuth(true);
    setNotice('로그인 후 이용할 수 있습니다.');
    return false;
  }

  async function openQuestion(questionId) {
    setDetailLoading(true);
    setError('');
    try {
      const [question, answerList] = await Promise.all([
        api.getQuestion(questionId),
        api.getAnswers(questionId),
      ]);
      setSelectedQuestion(question);
      setAnswers(answerList);
      const counts = await Promise.all(
        answerList.map(async (answer) => {
          try {
            const result = await api.getAnswerLikes(answer.id);
            return [answer.id, { count: result.likeCount, liked: false }];
          } catch {
            return [answer.id, { count: 0, liked: false }];
          }
        }),
      );
      setAnswerLikes(Object.fromEntries(counts));
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setDetailLoading(false);
    }
  }

  function closeQuestion() {
    setSelectedQuestion(null);
    setAnswers([]);
    setEditingQuestion(false);
  }

  async function saveQuestion(form) {
    if (!requireLogin()) return;
    try {
      if (editingQuestion && selectedQuestion) {
        const updated = await api.updateQuestion(selectedQuestion.id, form);
        setSelectedQuestion(updated);
        setQuestions((current) => current.map((item) => item.id === updated.id ? updated : item));
        setEditingQuestion(false);
        setNotice('질문을 수정했습니다.');
      } else {
        const created = await api.createQuestion(form);
        setQuestions((current) => [created, ...current]);
        setQuestionLikes((current) => ({ ...current, [created.id]: { count: 0, liked: false } }));
        setShowQuestionForm(false);
        setNotice('새 질문을 등록했습니다.');
        await openQuestion(created.id);
      }
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function deleteQuestion() {
    if (!selectedQuestion || !window.confirm('이 질문을 삭제할까요?')) return;
    try {
      await api.deleteQuestion(selectedQuestion.id);
      setQuestions((current) => current.filter((item) => item.id !== selectedQuestion.id));
      closeQuestion();
      setNotice('질문을 삭제했습니다.');
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function toggleQuestionLike(questionId) {
    if (!requireLogin()) return;
    const current = questionLikes[questionId] || { count: 0, liked: false };
    try {
      if (current.liked) {
        await api.unlikeQuestion(questionId);
        setQuestionLikes((state) => ({
          ...state,
          [questionId]: { count: Math.max(0, current.count - 1), liked: false },
        }));
      } else {
        const result = await api.likeQuestion(questionId);
        setQuestionLikes((state) => ({
          ...state,
          [questionId]: { count: result.likeCount, liked: true },
        }));
      }
    } catch (requestError) {
      if (requestError.status === 409) {
        setQuestionLikes((state) => ({ ...state, [questionId]: { ...current, liked: true } }));
        setNotice('이미 좋아요를 누른 질문입니다. 다시 누르면 취소할 수 있어요.');
      } else {
        setError(requestError.message);
      }
    }
  }

  async function createAnswer(content) {
    if (!requireLogin() || !selectedQuestion) return;
    try {
      const created = await api.createAnswer(selectedQuestion.id, { content });
      setAnswers((current) => [...current, created]);
      setAnswerLikes((current) => ({ ...current, [created.id]: { count: 0, liked: false } }));
      setNotice('답변을 등록했습니다.');
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function updateAnswer(answerId, content) {
    try {
      const updated = await api.updateAnswer(answerId, { content });
      setAnswers((current) => current.map((answer) => answer.id === answerId ? updated : answer));
      setNotice('답변을 수정했습니다.');
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function deleteAnswer(answerId) {
    if (!window.confirm('이 답변을 삭제할까요?')) return;
    try {
      await api.deleteAnswer(answerId);
      setAnswers((current) => current.filter((answer) => answer.id !== answerId));
      setNotice('답변을 삭제했습니다.');
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function toggleAnswerLike(answerId) {
    if (!requireLogin()) return;
    const current = answerLikes[answerId] || { count: 0, liked: false };
    try {
      if (current.liked) {
        await api.unlikeAnswer(answerId);
        setAnswerLikes((state) => ({
          ...state,
          [answerId]: { count: Math.max(0, current.count - 1), liked: false },
        }));
      } else {
        const result = await api.likeAnswer(answerId);
        setAnswerLikes((state) => ({
          ...state,
          [answerId]: { count: result.likeCount, liked: true },
        }));
      }
    } catch (requestError) {
      if (requestError.status === 409) {
        setAnswerLikes((state) => ({ ...state, [answerId]: { ...current, liked: true } }));
        setNotice('이미 좋아요를 누른 답변입니다. 다시 누르면 취소할 수 있어요.');
      } else {
        setError(requestError.message);
      }
    }
  }

  function handleAuthenticated(response) {
    const authUser = {
      id: response.id,
      email: response.email,
      nickname: response.nickname,
      role: response.role,
    };
    localStorage.setItem('uniwiki-token', response.token);
    localStorage.setItem('uniwiki-user', JSON.stringify(authUser));
    setUser(authUser);
    setShowAuth(false);
    setNotice(`${authUser.nickname}님, 환영합니다.`);
  }

  function logout() {
    localStorage.removeItem('uniwiki-token');
    localStorage.removeItem('uniwiki-user');
    setUser(null);
    setNotice('로그아웃했습니다.');
  }

  return (
    <div className="app-shell">
      <Header user={user} onLogin={() => setShowAuth(true)} onLogout={logout} />

      <main>
        {notice && <Toast message={notice} onClose={() => setNotice('')} />}
        {error && <ErrorBanner message={error} onClose={() => setError('')} />}

        {selectedQuestion ? (
          <QuestionDetail
            question={selectedQuestion}
            answers={answers}
            likes={questionLikes[selectedQuestion.id] || { count: 0, liked: false }}
            answerLikes={answerLikes}
            user={user}
            loading={detailLoading}
            editing={editingQuestion}
            onBack={closeQuestion}
            onLike={() => toggleQuestionLike(selectedQuestion.id)}
            onEdit={() => setEditingQuestion(true)}
            onCancelEdit={() => setEditingQuestion(false)}
            onSaveEdit={saveQuestion}
            onDelete={deleteQuestion}
            onCreateAnswer={createAnswer}
            onUpdateAnswer={updateAnswer}
            onDeleteAnswer={deleteAnswer}
            onLikeAnswer={toggleAnswerLike}
            onRequireLogin={requireLogin}
          />
        ) : (
          <QuestionBoard
            questions={filteredQuestions}
            likes={questionLikes}
            loading={loading}
            search={search}
            onSearch={setSearch}
            onOpen={openQuestion}
            onCreate={() => requireLogin() && setShowQuestionForm(true)}
            onRefresh={loadQuestions}
          />
        )}
      </main>

      {showAuth && (
        <AuthModal
          onClose={() => setShowAuth(false)}
          onAuthenticated={handleAuthenticated}
          onError={setError}
        />
      )}
      {showQuestionForm && (
        <Modal title="새 질문 작성" onClose={() => setShowQuestionForm(false)}>
          <QuestionForm onSubmit={saveQuestion} submitLabel="질문 등록" />
        </Modal>
      )}
    </div>
  );
}

function Header({ user, onLogin, onLogout }) {
  return (
    <header className="site-header">
      <div className="header-inner">
        <a className="brand" href="/" aria-label="UniWiki 홈">
          <span className="brand-mark">U</span>
          <span>UniWiki</span>
        </a>
        <nav className="main-nav" aria-label="주요 메뉴">
          <a href="/">위키</a>
          <a className="active" href="/">질문 게시판</a>
        </nav>
        <div className="user-area">
          {user ? (
            <>
              <span className="user-chip"><span>{user.nickname.slice(0, 1)}</span>{user.nickname}</span>
              <button className="text-button" onClick={onLogout}>로그아웃</button>
            </>
          ) : (
            <button className="button button-small" onClick={onLogin}>로그인</button>
          )}
        </div>
      </div>
    </header>
  );
}

function QuestionBoard({ questions, likes, loading, search, onSearch, onOpen, onCreate, onRefresh }) {
  return (
    <>
      <section className="hero">
        <div className="eyebrow"><span /> CAMPUS Q&amp;A</div>
        <h1>궁금한 건 묻고,<br /><em>함께 답을 만들어가요.</em></h1>
        <p>수강신청부터 학교생활까지, 우리 학교 구성원의 경험을 모읍니다.</p>
        <div className="hero-actions">
          <button className="button" onClick={onCreate}>질문하기 <span>→</span></button>
          <span className="question-total">지금까지 <strong>{questions.length}</strong>개의 질문</span>
        </div>
      </section>

      <section className="board container">
        <div className="board-toolbar">
          <div>
            <span className="section-kicker">RECENT QUESTIONS</span>
            <h2>최근 질문</h2>
          </div>
          <label className="search-box">
            <span>⌕</span>
            <input
              value={search}
              onChange={(event) => onSearch(event.target.value)}
              placeholder="질문, 내용, 작성자 검색"
              aria-label="질문 검색"
            />
          </label>
        </div>

        {loading ? (
          <LoadingState />
        ) : questions.length === 0 ? (
          <EmptyState search={search} onCreate={onCreate} />
        ) : (
          <div className="question-list">
            {questions.map((question, index) => (
              <article
                className="question-card"
                key={question.id}
                onClick={() => onOpen(question.id)}
              >
                <div className="question-index">{String(index + 1).padStart(2, '0')}</div>
                <div className="question-copy">
                  <div className="card-meta">
                    <span className={`status status-${question.status.toLowerCase()}`}>{question.status}</span>
                    <span>{question.authorNickname}</span>
                    <span>{formatDate(question.createdAt)}</span>
                  </div>
                  <h3>{question.title}</h3>
                  <p>{question.content}</p>
                </div>
                <div className="question-stats">
                  <span className="like-symbol">♥</span>
                  <strong>{likes[question.id]?.count || 0}</strong>
                  <small>좋아요</small>
                </div>
                <span className="card-arrow">↗</span>
              </article>
            ))}
          </div>
        )}
        <button className="refresh-button" onClick={onRefresh}>목록 새로고침</button>
      </section>
    </>
  );
}

function QuestionDetail(props) {
  const {
    question, answers, likes, answerLikes, user, loading, editing,
    onBack, onLike, onEdit, onCancelEdit, onSaveEdit, onDelete,
    onCreateAnswer, onUpdateAnswer, onDeleteAnswer, onLikeAnswer, onRequireLogin,
  } = props;
  const isAuthor = user?.id === question.authorId;

  return (
    <section className="detail-page container">
      <button className="back-button" onClick={onBack}>← 질문 목록으로</button>
      {loading ? <LoadingState /> : (
        <>
          <article className="question-detail">
            <div className="detail-meta">
              <span className={`status status-${question.status.toLowerCase()}`}>{question.status}</span>
              <span>{question.authorNickname}</span>
              <span>{formatDate(question.createdAt)}</span>
            </div>
            {editing ? (
              <QuestionForm
                initialValue={{ title: question.title, content: question.content }}
                onSubmit={onSaveEdit}
                onCancel={onCancelEdit}
                submitLabel="수정 완료"
              />
            ) : (
              <>
                <h1>{question.title}</h1>
                <p className="detail-content">{question.content}</p>
              </>
            )}
            {!editing && (
              <div className="detail-actions">
                <button className={`like-button ${likes.liked ? 'liked' : ''}`} onClick={onLike}>
                  ♥ <span>{likes.count}</span> 좋아요
                </button>
                {isAuthor && (
                  <div className="owner-actions">
                    <button className="text-button" onClick={onEdit}>수정</button>
                    <button className="text-button danger" onClick={onDelete}>삭제</button>
                  </div>
                )}
              </div>
            )}
          </article>

          <div className="answers-heading">
            <div>
              <span className="section-kicker">ANSWERS</span>
              <h2>답변 <em>{answers.length}</em></h2>
            </div>
            <p>도움이 된 답변에는 좋아요를 남겨주세요.</p>
          </div>

          <div className="answer-list">
            {answers.length === 0 && (
              <div className="no-answers">아직 답변이 없습니다. 첫 번째 경험을 나눠주세요.</div>
            )}
            {answers.map((answer) => (
              <AnswerCard
                key={answer.id}
                answer={answer}
                likes={answerLikes[answer.id] || { count: 0, liked: false }}
                isAuthor={user?.id === answer.authorId}
                onUpdate={onUpdateAnswer}
                onDelete={onDeleteAnswer}
                onLike={onLikeAnswer}
              />
            ))}
          </div>

          <AnswerForm
            user={user}
            onRequireLogin={onRequireLogin}
            onSubmit={onCreateAnswer}
          />
        </>
      )}
    </section>
  );
}

function AnswerCard({ answer, likes, isAuthor, onUpdate, onDelete, onLike }) {
  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(answer.content);

  async function submitEdit(event) {
    event.preventDefault();
    if (!content.trim()) return;
    await onUpdate(answer.id, content.trim());
    setEditing(false);
  }

  return (
    <article className="answer-card">
      <div className="answer-avatar">{answer.authorNickname.slice(0, 1)}</div>
      <div className="answer-body">
        <div className="answer-meta">
          <strong>{answer.authorNickname}</strong>
          <span>{formatDate(answer.createdAt)}</span>
          {answer.accepted && <span className="accepted">채택된 답변</span>}
        </div>
        {editing ? (
          <form className="inline-edit" onSubmit={submitEdit}>
            <textarea value={content} onChange={(event) => setContent(event.target.value)} />
            <div><button className="button button-small">저장</button><button type="button" className="text-button" onClick={() => setEditing(false)}>취소</button></div>
          </form>
        ) : <p>{answer.content}</p>}
        {!editing && (
          <div className="answer-actions">
            <button className={`mini-like ${likes.liked ? 'liked' : ''}`} onClick={() => onLike(answer.id)}>♥ {likes.count}</button>
            {isAuthor && <><button className="text-button" onClick={() => setEditing(true)}>수정</button><button className="text-button danger" onClick={() => onDelete(answer.id)}>삭제</button></>}
          </div>
        )}
      </div>
    </article>
  );
}

function AnswerForm({ user, onRequireLogin, onSubmit }) {
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    if (!user) return onRequireLogin();
    if (!content.trim()) return;
    setSubmitting(true);
    await onSubmit(content.trim());
    setContent('');
    setSubmitting(false);
  }

  return (
    <form className="answer-form" onSubmit={handleSubmit}>
      <div className="answer-form-heading"><strong>내 답변 작성</strong><span>{content.length}자</span></div>
      <textarea
        value={content}
        onChange={(event) => setContent(event.target.value)}
        onFocus={() => !user && onRequireLogin()}
        placeholder={user ? '알고 있는 경험과 정보를 구체적으로 알려주세요.' : '로그인 후 답변을 작성할 수 있습니다.'}
      />
      <div className="form-footer"><span>정확하고 친절한 답변은 위키 문서가 될 수 있어요.</span><button className="button button-small" disabled={submitting || !content.trim()}>{submitting ? '등록 중…' : '답변 등록'}</button></div>
    </form>
  );
}

function QuestionForm({ initialValue = initialQuestionForm, onSubmit, onCancel, submitLabel }) {
  const [form, setForm] = useState(initialValue);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    if (!form.title.trim() || !form.content.trim()) return;
    setSubmitting(true);
    await onSubmit({ title: form.title.trim(), content: form.content.trim() });
    setSubmitting(false);
  }

  return (
    <form className="question-form" onSubmit={handleSubmit}>
      <label>질문 제목<input autoFocus maxLength="200" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="무엇이 궁금한가요?" /></label>
      <label>질문 내용<textarea value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} placeholder="상황을 자세히 적으면 더 좋은 답변을 받을 수 있어요." /></label>
      <div className="form-actions">{onCancel && <button type="button" className="text-button" onClick={onCancel}>취소</button>}<button className="button" disabled={submitting}>{submitting ? '저장 중…' : submitLabel}</button></div>
    </form>
  );
}

function AuthModal({ onClose, onAuthenticated, onError }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ email: '', password: '', nickname: '' });
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    try {
      const response = mode === 'login'
        ? await api.login({ email: form.email, password: form.password })
        : await api.signup(form);
      onAuthenticated(response);
    } catch (requestError) {
      onError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title={mode === 'login' ? '다시 만나 반가워요' : 'UniWiki 시작하기'} onClose={onClose}>
      <div className="auth-tabs"><button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>로그인</button><button className={mode === 'signup' ? 'active' : ''} onClick={() => setMode('signup')}>회원가입</button></div>
      <form className="auth-form" onSubmit={handleSubmit}>
        {mode === 'signup' && <label>닉네임<input required value={form.nickname} onChange={(event) => setForm({ ...form, nickname: event.target.value })} placeholder="게시판에서 사용할 이름" /></label>}
        <label>이메일<input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="name@university.ac.kr" /></label>
        <label>비밀번호<input required type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="비밀번호 입력" /></label>
        <button className="button auth-submit" disabled={submitting}>{submitting ? '처리 중…' : mode === 'login' ? '로그인' : '회원가입'}</button>
      </form>
    </Modal>
  );
}

function Modal({ title, onClose, children }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <button className="modal-close" onClick={onClose} aria-label="닫기">×</button>
        <span className="section-kicker">UNIWIKI</span><h2>{title}</h2>
        {children}
      </section>
    </div>
  );
}

function LoadingState() {
  return <div className="loading-state"><span /><span /><span /><p>정보를 불러오고 있어요</p></div>;
}

function EmptyState({ search, onCreate }) {
  return <div className="empty-state"><span>?</span><h3>{search ? '검색 결과가 없습니다' : '첫 질문을 기다리고 있어요'}</h3><p>{search ? '다른 검색어를 입력해 보세요.' : '학교생활의 궁금한 점을 가장 먼저 남겨보세요.'}</p>{!search && <button className="button button-small" onClick={onCreate}>질문하기</button>}</div>;
}

function Toast({ message, onClose }) {
  useEffect(() => {
    const timer = setTimeout(onClose, 3500);
    return () => clearTimeout(timer);
  }, [onClose]);
  return <div className="toast">✓ {message}<button onClick={onClose}>×</button></div>;
}

function ErrorBanner({ message, onClose }) {
  return <div className="error-banner"><strong>요청을 완료하지 못했습니다.</strong><span>{message}</span><button onClick={onClose}>×</button></div>;
}

export default App;
