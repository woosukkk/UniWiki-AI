import {
  useCallback,
  useEffect,
  useState,
} from 'react';

import {
  Link,
  useNavigate,
  useParams,
} from 'react-router-dom';

import { api } from '../api.js';
import { AnswerItem } from '../components/AnswerItem.jsx';
import { ConfirmDialog } from '../components/ConfirmDialog.jsx';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LikeButton } from '../components/LikeButton.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

function formatDate(value) {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function getStatusLabel(status) {
  return status === 'CLOSED'
    ? '답변 완료'
    : '답변 대기';
}

export function QuestionDetailPage() {
  const { questionId } = useParams();
  const navigate = useNavigate();

  const {
    user,
    isAuthenticated,
    isAdmin,
  } = useAuth();

  const [question, setQuestion] = useState(null);
  const [answers, setAnswers] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showDelete, setShowDelete] =
    useState(false);

  const [deleting, setDeleting] =
    useState(false);

  const [answerContent, setAnswerContent] =
    useState('');

  const [
    answerSubmitting,
    setAnswerSubmitting,
  ] = useState(false);

  const [
    answerToDelete,
    setAnswerToDelete,
  ] = useState(null);

  const [actionError, setActionError] =
    useState('');

  const [questionLike, setQuestionLike] =
    useState({
      likeCount: 0,
      liked: false,
      busy: false,
    });

  const [answerLikes, setAnswerLikes] =
    useState({});
  const [questionPromoted, setQuestionPromoted] = useState(false);
  const [questionPromotionBusy, setQuestionPromotionBusy] = useState(false);

  const loadQuestion = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const questionLikeRequest = (
        isAuthenticated
          ? api.getQuestionLikeStatus(questionId)
          : api.getQuestionLikes(questionId)
      ).catch(async () => {
        try {
          return await api.getQuestionLikes(questionId);
        } catch {
          return { likeCount: 0, liked: false };
        }
      });

      const [
        questionData,
        answerList,
        questionLikeStatus,
        communityEntries,
      ] = await Promise.all([
        api.getQuestion(questionId),
        api.getAnswers(questionId),
        questionLikeRequest,
        isAdmin ? api.getCommunityWiki() : Promise.resolve([]),
      ]);

      const answerLikeEntries = await Promise.all(
        answerList.map(async (answer) => {
          try {
            const status = isAuthenticated
              ? await api.getAnswerLikeStatus(
                  answer.id,
                )
              : await api.getAnswerLikes(
                  answer.id,
                );

            return [
              answer.id,
              {
                ...status,
                busy: false,
              },
            ];
          } catch {
            return [
              answer.id,
              {
                likeCount: 0,
                liked: false,
                busy: false,
              },
            ];
          }
        }),
      );

      setQuestion(questionData);
      setAnswers(answerList);

      setQuestionLike({
        ...questionLikeStatus,
        busy: false,
      });

      setAnswerLikes(
        Object.fromEntries(answerLikeEntries),
      );
      setQuestionPromoted(
        communityEntries.some((entry) => entry.questionId === Number(questionId)),
      );
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [questionId, isAuthenticated, isAdmin]);

  async function promoteQuestion() {
    setActionError('');
    setQuestionPromotionBusy(true);
    try {
      await api.promoteQuestionToWiki(questionId);
      setQuestionPromoted(true);
    } catch (requestError) {
      setActionError(requestError.message);
    } finally {
      setQuestionPromotionBusy(false);
    }
  }

  useEffect(() => {
    loadQuestion();
  }, [loadQuestion]);

  async function deleteQuestion() {
    setDeleting(true);

    try {
      await api.deleteQuestion(questionId);

      navigate('/questions', {
        replace: true,
      });
    } catch (requestError) {
      setError(requestError.message);
      setShowDelete(false);
      setDeleting(false);
    }
  }

  async function createAnswer(event) {
    event.preventDefault();

    const trimmedContent =
      answerContent.trim();

    if (!trimmedContent) {
      return;
    }

    setAnswerSubmitting(true);
    setActionError('');

    try {
      const created = await api.createAnswer(
        questionId,
        {
          content: trimmedContent,
        },
      );

      setAnswers((current) => [
        ...current,
        created,
      ]);

      setAnswerLikes((current) => ({
        ...current,
        [created.id]: {
          likeCount: 0,
          liked: false,
          busy: false,
        },
      }));

      setAnswerContent('');
    } catch (requestError) {
      setActionError(requestError.message);
    } finally {
      setAnswerSubmitting(false);
    }
  }

  async function updateAnswer(
    answerId,
    content,
  ) {
    setActionError('');

    try {
      const updated = await api.updateAnswer(
        answerId,
        {
          content,
        },
      );

      setAnswers((current) =>
        current.map((answer) =>
          answer.id === answerId
            ? updated
            : answer,
        ),
      );

      return true;
    } catch (requestError) {
      setActionError(requestError.message);

      return false;
    }
  }

  async function deleteAnswer() {
    if (!answerToDelete) {
      return;
    }

    setDeleting(true);
    setActionError('');

    try {
      await api.deleteAnswer(
        answerToDelete.id,
      );

      setAnswers((current) =>
        current.filter(
          (answer) =>
            answer.id !== answerToDelete.id,
        ),
      );

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
      const accepted = await api.acceptAnswer(
        answerId,
      );

      setAnswers((current) =>
        current.map((answer) => ({
          ...answer,
          accepted:
            answer.id === answerId
              ? accepted.accepted
              : false,
        })),
      );

      setQuestion((current) => ({
        ...current,
        status: 'CLOSED',
      }));
    } catch (requestError) {
      setActionError(requestError.message);
    }
  }

  async function toggleQuestionLike() {
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: {
            pathname: `/questions/${questionId}`,
          },
        },
      });

      return;
    }

    setQuestionLike((current) => ({
      ...current,
      busy: true,
    }));

    try {
      if (questionLike.liked) {
        await api.unlikeQuestion(questionId);

        setQuestionLike((current) => ({
          likeCount: Math.max(
            0,
            current.likeCount - 1,
          ),
          liked: false,
          busy: false,
        }));
      } else {
        const response =
          await api.likeQuestion(questionId);

        setQuestionLike({
          ...response,
          busy: false,
        });
      }
    } catch (requestError) {
      setActionError(requestError.message);

      setQuestionLike((current) => ({
        ...current,
        busy: false,
      }));
    }
  }

  async function toggleAnswerLike(answerId) {
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: {
            pathname: `/questions/${questionId}`,
          },
        },
      });

      return;
    }

    const currentLike =
      answerLikes[answerId] || {
        likeCount: 0,
        liked: false,
        busy: false,
      };

    setAnswerLikes((current) => ({
      ...current,
      [answerId]: {
        ...currentLike,
        busy: true,
      },
    }));

    try {
      if (currentLike.liked) {
        await api.unlikeAnswer(answerId);

        setAnswerLikes((current) => ({
          ...current,
          [answerId]: {
            likeCount: Math.max(
              0,
              currentLike.likeCount - 1,
            ),
            liked: false,
            busy: false,
          },
        }));
      } else {
        const response =
          await api.likeAnswer(answerId);

        setAnswerLikes((current) => ({
          ...current,
          [answerId]: {
            ...response,
            busy: false,
          },
        }));
      }
    } catch (requestError) {
      setActionError(requestError.message);

      setAnswerLikes((current) => ({
        ...current,
        [answerId]: {
          ...currentLike,
          busy: false,
        },
      }));
    }
  }

  if (loading) {
    return (
      <main className="editorial-question-detail-state">
        <LoadingSpinner />
      </main>
    );
  }

  if (error) {
    return (
      <main className="editorial-question-detail-state">
        <ErrorMessage
          message={error}
          onRetry={loadQuestion}
        />
      </main>
    );
  }

  if (!question) {
    return null;
  }

  const isAuthor =
    user?.id === question.authorId;

  const hasAcceptedAnswer = answers.some(
    (answer) => answer.accepted,
  );

  return (
    <div className="editorial-question-detail-page">
      <section className="editorial-question-detail-hero">
        <div className="editorial-question-content-width">
          <Link
            className="editorial-question-back-link"
            to="/questions"
          >
            ← BACK TO Q&amp;A
          </Link>

          <div className="editorial-question-detail-heading">
            <div>
              <span className="editorial-question-label">
                QUESTION DETAIL
              </span>

              <div className="editorial-question-detail-meta">
                <span
                  className={
                    question.status === 'CLOSED'
                      ? 'question-status-closed'
                      : 'question-status-open'
                  }
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

              <h1>{question.title}</h1>
            </div>

            <aside className="editorial-question-detail-cover">
              <span>UNIWIKI Q&amp;A</span>

              <strong>
                OPEN
                <br />
                QUESTION
              </strong>

              <div>
                <span>
                  ANSWERS {answers.length}
                </span>

                <span>
                  #
                  {String(question.id).padStart(
                    4,
                    '0',
                  )}
                </span>
              </div>
            </aside>
          </div>
        </div>
      </section>

      <section className="editorial-question-detail-main">
        <div className="editorial-question-content-width editorial-question-detail-layout">
          <div>
            <article className="editorial-question-body">
              <div className="editorial-question-body-label">
                <span>QUESTION</span>
                <span>
                  WRITTEN BY{' '}
                  {question.authorNickname}
                </span>
              </div>

              <p>{question.content}</p>


              <div className="editorial-question-body-actions">
                <LikeButton
                  count={
                    questionLike.likeCount
                  }
                  liked={questionLike.liked}
                  busy={questionLike.busy}
                  onClick={toggleQuestionLike}
                />

                {isAdmin && (
                  <button
                    className="editorial-question-promote-button"
                    type="button"
                    disabled={questionPromoted || questionPromotionBusy}
                    onClick={promoteQuestion}
                  >
                    {questionPromoted
                      ? '함께 만든 위키 선정 완료'
                      : questionPromotionBusy
                        ? '선정 중...'
                        : '이 질문을 위키로 선정'}
                  </button>
                )}

                {isAuthor && (
                  <div>
                    <Link
                      to={`/questions/${question.id}/edit`}
                    >
                      EDIT
                    </Link>

                    <button
                      type="button"
                      onClick={() =>
                        setShowDelete(true)
                      }
                    >
                      DELETE
                    </button>
                  </div>
                )}
              </div>
            </article>

            <section className="editorial-question-answers-section">
              <div className="editorial-question-answers-heading">
                <div>
                  <span className="editorial-question-label">
                    ANSWERS
                  </span>

                  <h2>
                    답변{' '}
                    <em>{answers.length}</em>
                  </h2>
                </div>

                <p>
                  정확하고 도움이 되는 답변에는
                  좋아요를 눌러주세요.
                </p>
              </div>

              {actionError && (
                <div
                  className="editorial-question-action-error"
                  role="alert"
                >
                  {actionError}
                </div>
              )}

              {answers.length === 0 ? (
                <div className="editorial-question-no-answers">
                  <span>NO ANSWERS YET</span>
                  <h3>
                    아직 등록된 답변이 없습니다.
                  </h3>
                  <p>
                    이 질문에 대해 알고 있는 정보를
                    공유해주세요.
                  </p>
                </div>
              ) : (
                <div className="editorial-answer-list">
                  {answers.map(
                    (answer, index) => (
                      <AnswerItem
                        key={answer.id}
                        index={index}
                        answer={answer}
                        like={
                          answerLikes[
                            answer.id
                          ]
                        }
                        canEdit={
                          user?.id ===
                          answer.authorId
                        }
                        canAccept={
                          (isAuthor || isAdmin) &&
                          !answer.accepted &&
                          !hasAcceptedAnswer
                        }
                        onUpdate={updateAnswer}
                        onDelete={
                          setAnswerToDelete
                        }
                        onAccept={acceptAnswer}
                        onLike={toggleAnswerLike}
                      />
                    ),
                  )}
                </div>
              )}

              {isAuthenticated ? (
                <form
                  className="editorial-answer-form"
                  onSubmit={createAnswer}
                >
                  <div className="editorial-answer-form-heading">
                    <div>
                      <span className="editorial-question-label">
                        WRITE AN ANSWER
                      </span>

                      <h3>답변 작성</h3>
                    </div>

                    <span>
                      {answerContent.length}자
                    </span>
                  </div>

                  <textarea
                    value={answerContent}
                    onChange={(event) =>
                      setAnswerContent(
                        event.target.value,
                      )
                    }
                    placeholder="알고 있는 정보와 경험을 구체적으로 알려주세요."
                  />

                  <div className="editorial-answer-form-footer">
                    <p>
                      정확하고 친절한 답변을
                      작성해주세요.
                    </p>

                    <button
                      type="submit"
                      disabled={
                        answerSubmitting ||
                        !answerContent.trim()
                      }
                    >
                      {answerSubmitting
                        ? 'SUBMITTING...'
                        : 'SUBMIT ANSWER'}

                      <span>→</span>
                    </button>
                  </div>
                </form>
              ) : (
                <div className="editorial-answer-login-guide">
                  <span>LOGIN REQUIRED</span>

                  <h3>
                    로그인하면 답변을 작성할 수
                    있습니다.
                  </h3>

                  <Link
                    to="/login"
                    state={{
                      from: {
                        pathname: `/questions/${questionId}`,
                      },
                    }}
                  >
                    LOGIN
                    <span>→</span>
                  </Link>
                </div>
              )}
            </section>
          </div>

          <aside className="editorial-question-detail-sidebar">
            <section>
              <span>QUESTION INFO</span>

              <dl>
                <div>
                  <dt>STATUS</dt>
                  <dd>
                    {getStatusLabel(
                      question.status,
                    )}
                  </dd>
                </div>

                <div>
                  <dt>ANSWERS</dt>
                  <dd>{answers.length}</dd>
                </div>

                <div>
                  <dt>LIKES</dt>
                  <dd>
                    {questionLike.likeCount}
                  </dd>
                </div>

                <div>
                  <dt>AUTHOR</dt>
                  <dd>
                    {question.authorNickname}
                  </dd>
                </div>
              </dl>
            </section>

            <section className="editorial-question-guide-card">
              <span>QUESTION GUIDE</span>

              <strong>
                좋은 답변을 받는 방법
              </strong>

              <p>
                필요한 배경과 현재 상황을 구체적으로
                설명하면 더 정확한 답변을 받을 수
                있습니다.
              </p>
            </section>

            <Link
              className="editorial-question-sidebar-link"
              to="/questions"
            >
              MORE QUESTIONS
              <span>↗</span>
            </Link>
          </aside>
        </div>
      </section>

      {showDelete && (
        <ConfirmDialog
          title="질문을 삭제할까요?"
          message="질문과 관련 답변이 함께 삭제될 수 있습니다."
          confirmLabel="삭제"
          busy={deleting}
          onConfirm={deleteQuestion}
          onCancel={() =>
            setShowDelete(false)
          }
        />
      )}

      {answerToDelete && (
        <ConfirmDialog
          title="답변을 삭제할까요?"
          message="삭제한 답변은 복구할 수 없습니다."
          confirmLabel="삭제"
          busy={deleting}
          onConfirm={deleteAnswer}
          onCancel={() =>
            setAnswerToDelete(null)
          }
        />
      )}
    </div>
  );
}
