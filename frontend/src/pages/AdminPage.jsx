import {
  useCallback,
  useEffect,
  useState,
} from 'react';

import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';

const cards = [
  {
    key: 'userCount',
    index: '01',
    englishLabel: 'TOTAL USERS',
    koreanLabel: '회원',
  },
  {
    key: 'wikiPostCount',
    index: '02',
    englishLabel: 'TOTAL WIKI',
    koreanLabel: '위키 문서',
  },
  {
    key: 'questionCount',
    index: '03',
    englishLabel: 'QUESTIONS',
    koreanLabel: '질문',
  },
  {
    key: 'answerCount',
    index: '04',
    englishLabel: 'ANSWERS',
    koreanLabel: '답변',
  },
];

function formatDate(value) {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

export function AdminPage() {
  const [dashboard, setDashboard] =
    useState(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState('');

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const response =
        await api.getAdminDashboard();

      setDashboard(response);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  if (loading) {
    return (
      <main className="editorial-admin-state">
        <LoadingSpinner label="운영 현황을 불러오는 중입니다" />
      </main>
    );
  }

  if (error) {
    return (
      <main className="editorial-admin-state">
        <ErrorMessage
          message={error}
          onRetry={loadDashboard}
        />
      </main>
    );
  }

  if (!dashboard) {
    return (
      <main className="editorial-admin-state">
        <p>관리자 데이터를 불러오지 못했습니다.</p>
      </main>
    );
  }

  const recentActivities =
    dashboard.recentActivities ??
    dashboard.activities ??
    [];

  return (
    <div className="editorial-admin-page">
      <section className="editorial-admin-hero">
        <div className="editorial-admin-content-width editorial-admin-hero-inner">
          <div>
            <span className="editorial-admin-label">
              ADMIN CONTROL
            </span>

            <h1>
              UniWiki
              <br />
              관리 대시보드.
            </h1>
          </div>

          <p>
            회원과 위키 문서, 질문과 답변의 현황을
            확인하고 서비스 운영 상태를 관리합니다.
          </p>
        </div>
      </section>

      <main className="editorial-admin-main">
        <div className="editorial-admin-content-width">
          <section
            className="editorial-admin-stat-grid"
            aria-label="서비스 운영 통계"
          >
            {cards.map((card, index) => (
              <article
                key={card.key}
                className={
                  index === cards.length - 1
                    ? 'editorial-admin-stat-card highlight'
                    : 'editorial-admin-stat-card'
                }
              >
                <span>
                  {card.index} / {card.englishLabel}
                </span>

                <strong>
                  {Number(
                    dashboard[card.key] ?? 0,
                  ).toLocaleString()}
                </strong>

                <p>{card.koreanLabel}</p>
              </article>
            ))}
          </section>

          <section className="editorial-admin-section">
            <header className="editorial-admin-section-header">
              <div>
                <span className="editorial-admin-label">
                  SERVICE OVERVIEW
                </span>

                <h2>운영 현황</h2>
              </div>

              <span>REAL-TIME DASHBOARD</span>
            </header>

            <div className="editorial-admin-overview-grid">
              <article>
                <span>USERS</span>

                <strong>
                  {Number(
                    dashboard.userCount ?? 0,
                  ).toLocaleString()}
                </strong>

                <p>
                  현재 UniWiki에 등록된 전체 회원입니다.
                </p>
              </article>

              <article>
                <span>KNOWLEDGE</span>

                <strong>
                  {Number(
                    dashboard.wikiPostCount ?? 0,
                  ).toLocaleString()}
                </strong>

                <p>
                  학생들이 작성한 전체 위키 문서입니다.
                </p>
              </article>

              <article>
                <span>DISCUSSION</span>

                <strong>
                  {Number(
                    dashboard.questionCount ?? 0,
                  ).toLocaleString()}
                </strong>

                <p>
                  질문 게시판에 등록된 전체 질문입니다.
                </p>
              </article>

              <article>
                <span>ANSWERS</span>

                <strong>
                  {Number(
                    dashboard.answerCount ?? 0,
                  ).toLocaleString()}
                </strong>

                <p>
                  학생들이 작성한 전체 답변입니다.
                </p>
              </article>
            </div>
          </section>

          <section className="editorial-admin-section">
            <header className="editorial-admin-section-header">
              <div>
                <span className="editorial-admin-label">
                  RECENT ACTIVITY
                </span>

                <h2>최근 활동</h2>
              </div>

              <span>
                {recentActivities.length} ITEMS
              </span>
            </header>

            {recentActivities.length > 0 ? (
              <div className="editorial-admin-table-wrapper">
                <table className="editorial-admin-table">
                  <thead>
                    <tr>
                      <th>TYPE</th>
                      <th>TITLE</th>
                      <th>AUTHOR</th>
                      <th>DATE</th>
                    </tr>
                  </thead>

                  <tbody>
                    {recentActivities.map(
                      (activity, index) => (
                        <tr
                          key={
                            activity.id ??
                            `${activity.type}-${index}`
                          }
                        >
                          <td>
                            {activity.type ??
                              activity.activityType ??
                              'ACTIVITY'}
                          </td>

                          <td>
                            {activity.title ??
                              activity.description ??
                              '-'}
                          </td>

                          <td>
                            {activity.authorNickname ??
                              activity.nickname ??
                              '-'}
                          </td>

                          <td>
                            {formatDate(
                              activity.createdAt ??
                                activity.date,
                            )}
                          </td>
                        </tr>
                      ),
                    )}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="editorial-admin-empty">
                <div>
                  <strong>
                    최근 활동 데이터가 없습니다.
                  </strong>

                  <p>
                    현재 관리자 API는 통계 정보만
                    제공하고 있습니다.
                  </p>
                </div>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}