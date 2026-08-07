import { Link } from 'react-router-dom';

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}

export function WikiCard({ wikiPost, index = 0 }) {
  return (
    <article className="editorial-wiki-card">
      <Link to={`/wiki/${wikiPost.id}`}>
        <div className="editorial-wiki-card-index">
          {String(index + 1).padStart(2, '0')}
        </div>

        <div className="editorial-wiki-card-main">
          <div className="editorial-wiki-card-meta">
            <span className="editorial-wiki-category">
              {wikiPost.categoryName}
            </span>

            <span>{formatDate(wikiPost.createdAt)}</span>
          </div>

          <h2>{wikiPost.title}</h2>

          <p>
            {wikiPost.summary ||
              '아직 요약이 등록되지 않은 문서입니다.'}
          </p>
        </div>

        <div className="editorial-wiki-card-footer">
          <div>
            <span>AUTHOR</span>
            <strong>{wikiPost.authorNickname}</strong>
          </div>

          <div>
            <span>VIEWS</span>
            <strong>{wikiPost.viewCount.toLocaleString()}</strong>
          </div>
        </div>

        <span
          className="editorial-wiki-card-arrow"
          aria-hidden="true"
        >
          ↗
        </span>
      </Link>
    </article>
  );
}