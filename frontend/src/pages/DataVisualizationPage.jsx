import { useEffect, useMemo, useState } from 'react';

const palette = {
  '교과정보': '#315c49',
  '학교정보': '#5579c6',
  '학교생활': '#dc6b45',
  '학생지원': '#a45db5',
  '진로·취업': '#d09a25',
  'SW·인증': '#2b9b91',
};

function levelLabel(level) {
  return { scarce: '부족', balanced: '보통', dense: '많음' }[level] || level;
}

export function DataVisualizationPage() {
  const [data, setData] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState('전체');
  const [selectedNode, setSelectedNode] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    fetch('/data/knowledge-map.json')
      .then((response) => {
        if (!response.ok) throw new Error('시각화 데이터를 불러오지 못했습니다.');
        return response.json();
      })
      .then(setData)
      .catch((requestError) => setError(requestError.message));
  }, []);

  const visibleNodes = useMemo(() => {
    if (!data) return [];
    return selectedCategory === '전체'
      ? data.nodes
      : data.nodes.filter((node) => node.category === selectedCategory);
  }, [data, selectedCategory]);

  if (error) return <main className="coverage-page container"><p>{error}</p></main>;
  if (!data) return <main className="coverage-page container"><p>데이터 지도를 준비하고 있습니다.</p></main>;

  const maxCount = Math.max(...data.categories.map((category) => category.count));

  return (
    <main className="coverage-page container">
      <header className="coverage-heading">
        <div><span>KNOWLEDGE COVERAGE</span><h1>세종대 정보 지형도</h1></div>
        <p>색은 정보 영역, 원의 크기는 현재 확보한 레코드 수를 나타냅니다. 가까운 위치는 같은 분류에 속한다는 뜻이며 아직 실제 임베딩 거리는 아닙니다.</p>
      </header>

      <section className="coverage-summary">
        <article><strong>{data.totalRecords.toLocaleString()}</strong><span>전체 레코드</span></article>
        <article><strong>{data.nodes.length}</strong><span>데이터 소스</span></article>
        <article><strong>{data.categories.filter((item) => item.level === 'scarce').length}</strong><span>보강 필요 영역</span></article>
        <article><strong>{data.generatedAt}</strong><span>집계 기준일</span></article>
      </section>

      <section className="coverage-grid">
        <div className="coverage-map-panel">
          <div className="coverage-toolbar">
            {['전체', ...data.categories.map((item) => item.name)].map((category) => (
              <button key={category} className={selectedCategory === category ? 'active' : ''} onClick={() => setSelectedCategory(category)}>{category}</button>
            ))}
          </div>
          <svg className="coverage-map" viewBox="0 0 900 540" role="img" aria-label="분류별 데이터 분포 지도">
            <defs><filter id="node-shadow"><feDropShadow dx="0" dy="5" stdDeviation="6" floodOpacity=".18" /></filter></defs>
            {visibleNodes.map((node) => {
              const radius = 15 + Math.sqrt(node.count) * 1.45;
              return (
                <g key={node.id} className="coverage-node" onClick={() => setSelectedNode(node)} tabIndex="0">
                  <circle cx={node.x} cy={node.y} r={radius} fill={palette[node.category]} filter="url(#node-shadow)" opacity={selectedNode?.id === node.id ? 1 : .86} />
                  <text x={node.x} y={node.y - 3} textAnchor="middle">{node.shortLabel}</text>
                  <text className="coverage-node-count" x={node.x} y={node.y + 14} textAnchor="middle">{node.count.toLocaleString()}</text>
                </g>
              );
            })}
          </svg>
          <div className="coverage-legend">{Object.entries(palette).map(([name, color]) => <span key={name}><i style={{ background: color }} />{name}</span>)}</div>
        </div>

        <aside className="coverage-detail">
          {selectedNode ? (
            <>
              <span className="coverage-kicker">{selectedNode.category}</span>
              <h2>{selectedNode.label}</h2>
              <strong>{selectedNode.count.toLocaleString()}건</strong>
              <p>{selectedNode.description}</p>
              <small>수집 범위: {selectedNode.range}</small>
              {selectedNode.source && <a href={selectedNode.source} target="_blank" rel="noreferrer">공식 출처 보기 ↗</a>}
            </>
          ) : (
            <><span className="coverage-kicker">HOW TO READ</span><h2>원을 선택해보세요</h2><p>데이터 출처, 수집 범위와 현재 보유량을 확인할 수 있습니다.</p></>
          )}
        </aside>
      </section>

      <section className="coverage-bars">
        <div className="coverage-section-title"><span>COVERAGE BALANCE</span><h2>영역별 데이터 충족도</h2></div>
        {data.categories.map((category) => (
          <article key={category.name}>
            <div><strong>{category.name}</strong><span className={`coverage-level ${category.level}`}>{levelLabel(category.level)}</span></div>
            <div className="coverage-track"><i style={{ width: `${Math.max(4, category.count / maxCount * 100)}%`, background: palette[category.name] }} /></div>
            <b>{category.count.toLocaleString()}건</b>
          </article>
        ))}
        <p className="coverage-note">※ 현재 50건 미만인 영역을 ‘부족’으로 표시합니다. 원천 데이터 간 부분 중복이 있어 절대적인 문서 수와는 다를 수 있습니다.</p>
      </section>
    </main>
  );
}
