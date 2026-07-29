import { Link } from 'react-router-dom';

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}

export function WikiCard({ wikiPost }) {
  return (
    <article className="wiki-card">
      <Link to={`/wiki/${wikiPost.id}`}>
        <div className="wiki-card-meta">
          <span className="wiki-category">{wikiPost.categoryName}</span>
          <span>{formatDate(wikiPost.createdAt)}</span>
        </div>
        <h2>{wikiPost.title}</h2>
        <p>{wikiPost.summary || '요약이 아직 등록되지 않은 문서입니다.'}</p>
        <footer>
          <span>{wikiPost.authorNickname}</span>
          <span>조회 {wikiPost.viewCount.toLocaleString()}</span>
        </footer>
      </Link>
    </article>
  );
}
