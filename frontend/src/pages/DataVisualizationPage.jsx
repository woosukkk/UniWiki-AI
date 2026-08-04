import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';

const colors = ['#315c49', '#5579c6', '#dc6b45', '#a45db5', '#d09a25', '#2b9b91', '#7b6d57', '#437c90'];

const contentTypeLabels = {
  LECTURE_REVIEW: '강의평',
  ACADEMIC: '학사',
  SCHOLARSHIP: '장학',
  FACILITIES: '시설',
  CAREER: '진로·취업',
  CLUB_EVENT: '동아리·행사',
  SCHOOL_LIFE: '학교생활',
};

function levelLabel(level) {
  return { scarce: '부족', balanced: '보통', dense: '많음' }[level] || level;
}

function formatDate(value) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

export function DataVisualizationPage() {
  const [data, setData] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState('전체');
  const [selectedNode, setSelectedNode] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getWikiCoverage()
      .then(setData)
      .catch((requestError) => setError(requestError.message));
  }, []);

  const nodes = useMemo(() => {
    if (!data) return [];
    const centerX = 450;
    const centerY = 270;
    const radiusX = 310;
    const radiusY = 190;
    return data.categories.map((category, index) => {
      const angle = (Math.PI * 2 * index) / Math.max(1, data.categories.length) - Math.PI / 2;
      return {
        ...category,
        x: centerX + Math.cos(angle) * radiusX,
        y: centerY + Math.sin(angle) * radiusY,
        color: colors[index % colors.length],
      };
    });
  }, [data]);

  const visibleNodes = selectedCategory === '전체'
    ? nodes
    : nodes.filter((node) => node.name === selectedCategory);

  if (error) return <main className="coverage-page container"><p>{error}</p></main>;
  if (!data) return <main className="coverage-page container"><p>운영 DB의 데이터 지도를 불러오고 있습니다.</p></main>;

  const maxCount = Math.max(1, ...data.categories.map((category) => category.count));

  return (
    <main className="coverage-page container">
      <header className="coverage-heading">
        <div><span>DATABASE COVERAGE</span><h1>세종대 정보 지형도</h1></div>
        <p>운영 DB에 승인된 위키 문서를 실시간 집계합니다. 원의 위치는 화면 배치를 위한 것이며 임베딩 거리를 의미하지 않습니다.</p>
      </header>

      <section className="coverage-summary">
        <article><strong>{data.totalRecords.toLocaleString()}</strong><span>승인 위키</span></article>
        <article><strong>{data.wikiRecords.toLocaleString()}</strong><span>일반·공식 위키</span></article>
        <article><strong>{data.everytimeRecords.toLocaleString()}</strong><span>에브리타임 자료</span></article>
        <article><strong>{formatDate(data.latestUpdatedAt)}</strong><span>최근 갱신</span></article>
      </section>

      <section className="coverage-grid">
        <div className="coverage-map-panel">
          <div className="coverage-toolbar">
            {['전체', ...data.categories.map((item) => item.name)].map((category) => (
              <button key={category} className={selectedCategory === category ? 'active' : ''} onClick={() => setSelectedCategory(category)}>{category}</button>
            ))}
          </div>
          <svg className="coverage-map" viewBox="0 0 900 540" role="img" aria-label="운영 DB 카테고리별 위키 문서 분포">
            <defs><filter id="node-shadow"><feDropShadow dx="0" dy="5" stdDeviation="6" floodOpacity=".18" /></filter></defs>
            {visibleNodes.map((node) => {
              const radius = Math.min(72, 24 + Math.sqrt(node.count) * 4);
              return (
                <g key={node.id} className="coverage-node" onClick={() => setSelectedNode(node)} tabIndex="0">
                  <circle cx={node.x} cy={node.y} r={radius} fill={node.color} filter="url(#node-shadow)" opacity={selectedNode?.id === node.id ? 1 : .86} />
                  <text x={node.x} y={node.y - 3} textAnchor="middle">{node.name}</text>
                  <text className="coverage-node-count" x={node.x} y={node.y + 14} textAnchor="middle">{node.count.toLocaleString()}</text>
                </g>
              );
            })}
          </svg>
          <div className="coverage-legend">{nodes.map((node) => <span key={node.id}><i style={{ background: node.color }} />{node.name}</span>)}</div>
        </div>

        <aside className="coverage-detail">
          {selectedNode ? (
            <>
              <span className="coverage-kicker">DB CATEGORY</span>
              <h2>{selectedNode.name}</h2>
              <strong>{selectedNode.count.toLocaleString()}건</strong>
              <p>{selectedNode.description || '카테고리 설명이 등록되지 않았습니다.'}</p>
              <small>승인된 wiki_posts를 현재 시점에 집계한 결과입니다.</small>
            </>
          ) : (
            <><span className="coverage-kicker">HOW TO READ</span><h2>원을 선택해보세요</h2><p>카테고리별 승인 문서 수와 DB에 등록된 설명을 확인할 수 있습니다.</p></>
          )}
        </aside>
      </section>

      <section className="coverage-bars">
        <div className="coverage-section-title"><span>CATEGORY BALANCE</span><h2>카테고리별 위키 분포</h2></div>
        {data.categories.map((category, index) => (
          <article key={category.id}>
            <div><strong>{category.name}</strong><span className={`coverage-level ${category.level}`}>{levelLabel(category.level)}</span></div>
            <div className="coverage-track"><i style={{ width: `${Math.max(4, category.count / maxCount * 100)}%`, background: colors[index % colors.length] }} /></div>
            <b>{category.count.toLocaleString()}건</b>
          </article>
        ))}
        <p className="coverage-note">※ 승인된 위키 10건 미만은 ‘부족’, 30건 미만은 ‘보통’으로 표시합니다.</p>
      </section>

      {data.everytimeContentTypes.length > 0 && (
        <section className="coverage-bars">
          <div className="coverage-section-title"><span>EVERYTIME BREAKDOWN</span><h2>에브리타임 자료 구성</h2></div>
          {data.everytimeContentTypes.map((item) => (
            <article key={item.type}>
              <div><strong>{contentTypeLabels[item.type] || item.type}</strong></div>
              <div className="coverage-track"><i style={{ width: `${Math.max(4, item.count / Math.max(1, data.everytimeRecords) * 100)}%`, background: '#5579c6' }} /></div>
              <b>{item.count.toLocaleString()}건</b>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
