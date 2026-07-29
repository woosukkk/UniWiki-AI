import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorMessage } from '../components/ErrorMessage.jsx';
import { LoadingSpinner } from '../components/LoadingSpinner.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';

export function MyPage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(user);
  const [activity, setActivity] = useState({ wiki: [], questions: [], answers: [] });
  const [tab, setTab] = useState('wiki');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const [me, wikiPosts, questions] = await Promise.all([api.getMe(), api.getWikiPosts(), api.getQuestions()]);
        const answerGroups = await Promise.all(questions.map(async (question) => ({
          question,
          answers: await api.getAnswers(question.id),
        })));
        setProfile(me);
        setActivity({
          wiki: wikiPosts.filter((post) => post.authorId === me.id),
          questions: questions.filter((question) => question.authorId === me.id),
          answers: answerGroups.flatMap(({ question, answers }) => answers
            .filter((answer) => answer.authorId === me.id)
            .map((answer) => ({ ...answer, questionTitle: question.title, questionId: question.id }))),
        });
      } catch (requestError) {
        setError(requestError.message);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  if (loading) return <main className="mypage container"><LoadingSpinner label="내 활동을 불러오는 중입니다" /></main>;
  if (error) return <main className="mypage container"><ErrorMessage message={error} /></main>;

  const items = activity[tab];
  return (
    <main className="mypage container">
      <section className="profile-card">
        <div className="profile-avatar">{profile.nickname.slice(0, 1)}</div>
        <div><span>MY UNIWIKI</span><h1>{profile.nickname}</h1><p>{profile.email}</p></div>
        <strong>{profile.role}</strong>
      </section>
      <section className="activity-card">
        <nav className="activity-tabs" aria-label="내 활동 종류">
          {[
            ['wiki', '작성 위키'], ['questions', '작성 질문'], ['answers', '작성 답변'],
          ].map(([key, label]) => <button className={tab === key ? 'active' : ''} key={key} onClick={() => setTab(key)}>{label}<em>{activity[key].length}</em></button>)}
        </nav>
        {items.length === 0 ? <div className="activity-empty">아직 등록한 활동이 없습니다.</div> : (
          <div className="activity-list">
            {items.map((item) => {
              const href = tab === 'wiki' ? `/wiki/${item.id}` : `/questions/${tab === 'answers' ? item.questionId : item.id}`;
              const title = tab === 'answers' ? item.questionTitle : item.title;
              return <Link key={item.id} to={href}><strong>{title}</strong>{tab === 'answers' && <p>{item.content}</p>}<span>자세히 보기 →</span></Link>;
            })}
          </div>
        )}
      </section>
    </main>
  );
}
