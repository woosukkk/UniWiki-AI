import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';

import { Link } from 'react-router-dom';

import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

const tabs = [
  {
    key: 'overview',
    index: '01',
    label: 'OVERVIEW',
    title: '전체 활동',
  },
  {
    key: 'wiki',
    index: '02',
    label: 'MY WIKI',
    title: '작성한 위키',
  },
  {
    key: 'questions',
    index: '03',
    label: 'MY QUESTIONS',
    title: '작성한 질문',
  },
  {
    key: 'answers',
    index: '04',
    label: 'MY ANSWERS',
    title: '작성한 답변',
  },
];

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

function normalizeLikeCount(result) {
  return Number(
    result?.likeCount ??
      result?.count ??
      0,
  );
}

function getInitial(value) {
  return (
    value?.trim()?.slice(0, 1)?.toUpperCase() ||
    'U'
  );
}

export function MyPage() {
  const { user } = useAuth();

  const [activeTab, setActiveTab] =
    useState('overview');

  const [wikiPosts, setWikiPosts] =
    useState([]);

  const [questions, setQuestions] =
    useState([]);

  const [answers, setAnswers] =
    useState([]);

  const [likeCounts, setLikeCounts] =
    useState({
      wiki: {},
      questions: {},
      answers: {},
    });

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState('');

  const loadMyPage = useCallback(async () => {
    if (!user) {
      return;
    }

    setLoading(true);
    setError('');

    try {
      const [allWikiPosts, allQuestions] =
        await Promise.all([
          api.searchWikiPosts(''),
          api.getQuestions(),
        ]);

      const myWikiPosts = allWikiPosts.filter(
        (wikiPost) =>
          Number(wikiPost.authorId) ===
          Number(user.id),
      );

      const myQuestions = allQuestions.filter(
        (question) =>
          Number(question.authorId) ===
          Number(user.id),
      );

      const answersByQuestion =
        await Promise.all(
          allQuestions.map(async (question) => {
            try {
              const questionAnswers =
                await api.getAnswers(
                  question.id,
                );

              return questionAnswers.map(
                (answer) => ({
                  ...answer,
                  questionId:
                    answer.questionId ??
                    question.id,
                  questionTitle:
                    answer.questionTitle ??
                    question.title,
                }),
              );
            } catch {
              return [];
            }
          }),
        );

      const myAnswers = answersByQuestion
        .flat()
        .filter(
          (answer) =>
            Number(answer.authorId) ===
            Number(user.id),
        );

      const [
        wikiLikeEntries,
        questionLikeEntries,
        answerLikeEntries,
      ] = await Promise.all([
        Promise.all(
          myWikiPosts.map(async (wikiPost) => {
            try {
              const result =
                await api.getWikiPostLikes(
                  wikiPost.id,
                );

              return [
                wikiPost.id,
                normalizeLikeCount(result),
              ];
            } catch {
              return [wikiPost.id, 0];
            }
          }),
        ),

        Promise.all(
          myQuestions.map(async (question) => {
            try {
              const result =
                await api.getQuestionLikes(
                  question.id,
                );

              return [
                question.id,
                normalizeLikeCount(result),
              ];
            } catch {
              return [question.id, 0];
            }
          }),
        ),

        Promise.all(
          myAnswers.map(async (answer) => {
            try {
              const result =
                await api.getAnswerLikes(
                  answer.id,
                );

              return [
                answer.id,
                normalizeLikeCount(result),
              ];
            } catch {
              return [answer.id, 0];
            }
          }),
        ),
      ]);

      setWikiPosts(myWikiPosts);
      setQuestions(myQuestions);
      setAnswers(myAnswers);

      setLikeCounts({
        wiki: Object.fromEntries(
          wikiLikeEntries,
        ),
        questions: Object.fromEntries(
          questionLikeEntries,
        ),
        answers: Object.fromEntries(
          answerLikeEntries,
        ),
      });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    loadMyPage();
  }, [loadMyPage]);

  const receivedLikeCount = useMemo(() => {
    const wikiLikes = Object.values(
      likeCounts.wiki,
    ).reduce(
      (sum, count) => sum + count,
      0,
    );

    const questionLikes = Object.values(
      likeCounts.questions,
    ).reduce(
      (sum, count) => sum + count,
      0,
    );

    const answerLikes = Object.values(
      likeCounts.answers,
    ).reduce(
      (sum, count) => sum + count,
      0,
    );

    return (
      wikiLikes +
      questionLikes +
      answerLikes
    );
  }, [likeCounts]);

  const recentActivities = useMemo(() => {
    const wikiActivities = wikiPosts.map(
      (wikiPost) => ({
        id: `wiki-${wikiPost.id}`,
        type: 'WIKI',
        title: wikiPost.title,
        description:
          '새로운 위키 문서를 작성했습니다.',
        date: wikiPost.createdAt,
        to: `/wiki/${wikiPost.id}`,
        accent: 'blue',
      }),
    );

    const questionActivities = questions.map(
      (question) => ({
        id: `question-${question.id}`,
        type: 'QUESTION',
        title: question.title,
        description:
          '질문 게시판에 새로운 질문을 작성했습니다.',
        date: question.createdAt,
        to: `/questions/${question.id}`,
        accent: 'black',
      }),
    );

    const answerActivities = answers.map(
      (answer) => ({
        id: `answer-${answer.id}`,
        type: 'ANSWER',
        title:
          answer.questionTitle ||
          '질문에 답변했습니다.',
        description: answer.content,
        date: answer.createdAt,
        to: `/questions/${answer.questionId}`,
        accent: 'red',
      }),
    );

    return [
      ...wikiActivities,
      ...questionActivities,
      ...answerActivities,
    ]
      .sort(
        (left, right) =>
          new Date(right.date) -
          new Date(left.date),
      )
      .slice(0, 8);
  }, [
    wikiPosts,
    questions,
    answers,
  ]);

  const activeTabInfo =
    tabs.find(
      (tab) => tab.key === activeTab,
    ) || tabs[0];

  if (loading) {
    return (
      <main className="editorial-mypage-state">
        <LoadingSpinner label="내 활동을 불러오는 중입니다" />
      </main>
    );
  }

  if (error) {
    return (
      <main className="editorial-mypage-state">
        <ErrorMessage
          message={error}
          onRetry={loadMyPage}
        />
      </main>
    );
  }

  return (
    <div className="editorial-mypage">
      <section className="editorial-mypage-hero">
        <div className="editorial-mypage-content-width editorial-mypage-hero-inner">
          <div>
            <span className="editorial-mypage-label">
              MY KNOWLEDGE
            </span>

            <h1>
              내가 만든 지식과
              <br />
              <em>활동 기록.</em>
            </h1>
          </div>

          <p>
            작성한 위키와 질문, 답변을
            한곳에서 확인하고
            <br />
            UniWiki에 기여한 활동을
            관리할 수 있습니다.
          </p>
        </div>
      </section>

      <section className="editorial-mypage-dashboard">
        <div className="editorial-mypage-content-width editorial-mypage-layout">
          <aside className="editorial-mypage-sidebar">
            <section className="editorial-mypage-profile">
              <div className="editorial-mypage-avatar">
                {getInitial(
                  user?.nickname ||
                    user?.email,
                )}
              </div>

              <span>MEMBER PROFILE</span>

              <h2>
                {user?.nickname || 'UniWiki 사용자'}
              </h2>

              <p>{user?.email}</p>

              <dl>
                <div>
                  <dt>MEMBER ID</dt>
                  <dd>
                    #
                    {String(
                      user?.id || 0,
                    ).padStart(4, '0')}
                  </dd>
                </div>

                <div>
                  <dt>ROLE</dt>
                  <dd>
                    {user?.role || 'USER'}
                  </dd>
                </div>
              </dl>
            </section>

            <nav
              className="editorial-mypage-navigation"
              aria-label="마이페이지 메뉴"
            >
              {tabs.map((tab) => (
                <button
                  type="button"
                  key={tab.key}
                  className={
                    activeTab === tab.key
                      ? 'active'
                      : ''
                  }
                  onClick={() =>
                    setActiveTab(tab.key)
                  }
                >
                  <span>{tab.index}</span>

                  <strong>
                    {tab.label}
                  </strong>

                  <span>→</span>
                </button>
              ))}
            </nav>

            <section className="editorial-mypage-contribution-note">
              <span>CONTRIBUTION</span>

              <strong>
                학생이 만드는 지식,
                <br />
                AI가 연결하는 정보.
              </strong>

              <p>
                작성한 내용은 다른 학생들의
                질문과 AI 답변에 활용될 수
                있습니다.
              </p>
            </section>
          </aside>

          <main className="editorial-mypage-main">
            <section className="editorial-mypage-summary">
              <div className="editorial-mypage-summary-heading">
                <div>
                  <span className="editorial-mypage-label">
                    CONTRIBUTION SUMMARY
                  </span>

                  <h2>
                    {user?.nickname || '사용자'}님의
                    기여 기록
                  </h2>
                </div>

                <span>
                  UNIWIKI MEMBER
                </span>
              </div>

              <div className="editorial-mypage-stat-grid">
                <article>
                  <span>01</span>
                  <strong>
                    {wikiPosts.length}
                  </strong>
                  <p>작성한 위키</p>
                </article>

                <article>
                  <span>02</span>
                  <strong>
                    {questions.length}
                  </strong>
                  <p>작성한 질문</p>
                </article>

                <article>
                  <span>03</span>
                  <strong>
                    {answers.length}
                  </strong>
                  <p>작성한 답변</p>
                </article>

                <article className="editorial-mypage-stat-highlight">
                  <span>04</span>
                  <strong>
                    {receivedLikeCount}
                  </strong>
                  <p>받은 좋아요</p>
                </article>
              </div>
            </section>

            <section className="editorial-mypage-content-panel">
              <header className="editorial-mypage-panel-heading">
                <div>
                  <span className="editorial-mypage-label">
                    {activeTabInfo.label}
                  </span>

                  <h2>
                    {activeTabInfo.title}
                  </h2>
                </div>

                <span>
                  {activeTab === 'overview'
                    ? recentActivities.length
                    : activeTab === 'wiki'
                      ? wikiPosts.length
                      : activeTab ===
                          'questions'
                        ? questions.length
                        : answers.length}{' '}
                  RECORDS
                </span>
              </header>

              {activeTab === 'overview' && (
                <ActivityList
                  activities={
                    recentActivities
                  }
                />
              )}

              {activeTab === 'wiki' && (
                <WikiActivityList
                  wikiPosts={wikiPosts}
                  likeCounts={
                    likeCounts.wiki
                  }
                />
              )}

              {activeTab ===
                'questions' && (
                <QuestionActivityList
                  questions={questions}
                  likeCounts={
                    likeCounts.questions
                  }
                />
              )}

              {activeTab === 'answers' && (
                <AnswerActivityList
                  answers={answers}
                  likeCounts={
                    likeCounts.answers
                  }
                />
              )}
            </section>
          </main>
        </div>
      </section>
    </div>
  );
}

function ActivityList({ activities }) {
  if (activities.length === 0) {
    return (
      <EmptyActivity
        title="아직 활동 기록이 없습니다."
        description="위키를 작성하거나 질문과 답변에 참여해보세요."
      />
    );
  }

  return (
    <div className="editorial-mypage-activity-list">
      {activities.map(
        (activity, index) => (
          <Link
            key={activity.id}
            to={activity.to}
            className={`editorial-mypage-activity-item activity-${activity.accent}`}
          >
            <span className="editorial-mypage-activity-index">
              {String(index + 1).padStart(
                2,
                '0',
              )}
            </span>

            <div>
              <span>{activity.type}</span>
              <h3>{activity.title}</h3>
              <p>
                {activity.description}
              </p>
            </div>

            <time>
              {formatDate(activity.date)}
            </time>

            <span>↗</span>
          </Link>
        ),
      )}
    </div>
  );
}

function WikiActivityList({
  wikiPosts,
  likeCounts,
}) {
  if (wikiPosts.length === 0) {
    return (
      <EmptyActivity
        title="작성한 위키가 없습니다."
        description="학생들에게 필요한 정보를 첫 위키로 작성해보세요."
        link="/wiki/new"
        linkLabel="NEW WIKI"
      />
    );
  }

  return (
    <div className="editorial-mypage-record-list">
      {wikiPosts.map(
        (wikiPost, index) => (
          <Link
            key={wikiPost.id}
            to={`/wiki/${wikiPost.id}`}
          >
            <span>
              {String(index + 1).padStart(
                2,
                '0',
              )}
            </span>

            <div>
              <small>
                {wikiPost.categoryName ||
                  'WIKI'}
              </small>

              <h3>{wikiPost.title}</h3>

              <p>
                {wikiPost.summary ||
                  '등록된 문서 요약이 없습니다.'}
              </p>
            </div>

            <dl>
              <div>
                <dt>LIKES</dt>
                <dd>
                  {likeCounts[
                    wikiPost.id
                  ] || 0}
                </dd>
              </div>

              <div>
                <dt>DATE</dt>
                <dd>
                  {formatDate(
                    wikiPost.createdAt,
                  )}
                </dd>
              </div>
            </dl>

            <span>↗</span>
          </Link>
        ),
      )}
    </div>
  );
}

function QuestionActivityList({
  questions,
  likeCounts,
}) {
  if (questions.length === 0) {
    return (
      <EmptyActivity
        title="작성한 질문이 없습니다."
        description="학교생활에서 궁금한 점을 질문해보세요."
        link="/questions/new"
        linkLabel="NEW QUESTION"
      />
    );
  }

  return (
    <div className="editorial-mypage-record-list">
      {questions.map(
        (question, index) => (
          <Link
            key={question.id}
            to={`/questions/${question.id}`}
          >
            <span>
              {String(index + 1).padStart(
                2,
                '0',
              )}
            </span>

            <div>
              <small>
                {question.status ||
                  'QUESTION'}
              </small>

              <h3>{question.title}</h3>

              <p>{question.content}</p>
            </div>

            <dl>
              <div>
                <dt>LIKES</dt>
                <dd>
                  {likeCounts[
                    question.id
                  ] || 0}
                </dd>
              </div>

              <div>
                <dt>DATE</dt>
                <dd>
                  {formatDate(
                    question.createdAt,
                  )}
                </dd>
              </div>
            </dl>

            <span>↗</span>
          </Link>
        ),
      )}
    </div>
  );
}

function AnswerActivityList({
  answers,
  likeCounts,
}) {
  if (answers.length === 0) {
    return (
      <EmptyActivity
        title="작성한 답변이 없습니다."
        description="다른 학생의 질문에 알고 있는 정보를 공유해보세요."
        link="/questions"
        linkLabel="VIEW QUESTIONS"
      />
    );
  }

  return (
    <div className="editorial-mypage-record-list">
      {answers.map(
        (answer, index) => (
          <Link
            key={answer.id}
            to={`/questions/${answer.questionId}`}
          >
            <span>
              {String(index + 1).padStart(
                2,
                '0',
              )}
            </span>

            <div>
              <small>
                {answer.accepted
                  ? 'ACCEPTED ANSWER'
                  : 'ANSWER'}
              </small>

              <h3>
                {answer.questionTitle ||
                  '답변한 질문'}
              </h3>

              <p>{answer.content}</p>
            </div>

            <dl>
              <div>
                <dt>LIKES</dt>
                <dd>
                  {likeCounts[
                    answer.id
                  ] || 0}
                </dd>
              </div>

              <div>
                <dt>DATE</dt>
                <dd>
                  {formatDate(
                    answer.createdAt,
                  )}
                </dd>
              </div>
            </dl>

            <span>↗</span>
          </Link>
        ),
      )}
    </div>
  );
}

function EmptyActivity({
  title,
  description,
  link,
  linkLabel,
}) {
  return (
    <section className="editorial-mypage-empty">
      <span>NO ACTIVITY</span>

      <h3>{title}</h3>

      <p>{description}</p>

      {link && (
        <Link to={link}>
          {linkLabel}
          <span>→</span>
        </Link>
      )}
    </section>
  );
}