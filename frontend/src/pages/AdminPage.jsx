import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';

const cards = [
  ['userCount', '회원'], ['wikiPostCount', '위키 문서'], ['questionCount', '질문'], ['answerCount', '답변'],
];

function formatDate(value) {
  return value ? new Intl.DateTimeFormat('ko-KR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '-';
}

export function AdminPage() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError('');
    try { setDashboard(await api.getAdminDashboard()); }
    catch (requestError) { setError(requestError.message); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadDashboard(); }, [loadDashboard]);

  if (loading) return <main className="admin-page container"><LoadingSpinner label="운영 현황을 불러오는 중입니다" /></main>;
  if (error) return <main className="admin-page container"><ErrorMessage message={error} onRetry={loadDashboard} /></main>;

  return (
    <main className="admin-page container">
      <header className="admin-heading"><span>ADMIN CONSOLE</span><h1>서비스 운영 현황</h1><button className="text-button" onClick={loadDashboard}>새로고침</button></header>
      <section className="admin-metrics">
        {cards.map(([key, label]) => <article key={key}><span>{label}</span><strong>{dashboard[key].toLocaleString()}</strong></article>)}
        <article className={dashboard.failedSyncCount ? 'metric-warning' : ''}><span>동기화 실패</span><strong>{dashboard.failedSyncCount}</strong></article>
        <article><span>동기화 대기</span><strong>{dashboard.pendingSyncCount}</strong></article>
      </section>
      <section className="sync-monitor">
        <div><h2>최근 벡터 동기화</h2><p>최근 작업 20건을 기준으로 표시합니다.</p></div>
        {dashboard.recentSyncJobs.length === 0 ? <div className="activity-empty">동기화 작업이 없습니다.</div> : (
          <div className="sync-table-wrap"><table><thead><tr><th>문서</th><th>작업</th><th>상태</th><th>시도</th><th>생성 시각</th><th>오류</th></tr></thead><tbody>
            {dashboard.recentSyncJobs.map((job) => <tr key={job.id}><td>#{job.wikiPostId}</td><td>{job.operation}</td><td><span className={`sync-status sync-${job.status.toLowerCase()}`}>{job.status}</span></td><td>{job.attemptCount}</td><td>{formatDate(job.createdAt)}</td><td className="sync-error" title={job.lastError || ''}>{job.lastError || '-'}</td></tr>)}
          </tbody></table></div>
        )}
      </section>
    </main>
  );
}
