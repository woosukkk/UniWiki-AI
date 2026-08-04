import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';

const trustColors = {
  high: '#315c49',
  medium: '#c08a20',
  low: '#b4513f',
};

const contentTypeLabels = {
  LECTURE_REVIEW: '강의평',
  ACADEMIC: '학사',
  SCHOLARSHIP: '장학',
  FACILITIES: '시설',
  CAREER: '진로·취업',
  CLUB_EVENT: '동아리·행사',
  SCHOOL_LIFE: '학교생활',
};

function trustLabel(level) {
  return { low: '확인 필요', medium: '보통', high: '높음' }[level] || level;
}

function volumeLabel(level) {
  return { scarce: '부족', balanced: '보통', dense: '충분' }[level] || level;
}

function formatDate(value) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

function MetricBar({ label, value }) {
  return (
    <div className="trust-metric">
      <span>{label}</span>
      <div className="coverage-track"><i style={{ width: `${value}%` }} /></div>
      <b>{value}점</b>
    </div>
  );
}

export function DataVisualizationPage() {
  const [data, setData] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState('전체');
  const [selectedNode, setSelectedNode] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getWikiCoverage().then(setData).catch((requestError) => setError(requestError.message));
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
        color: trustColors[category.trustLevel] || trustColors.low,
      };
    });
  }, [data]);

  const visibleNodes = selectedCategory === '전체'
    ? nodes
    : nodes.filter((node) => node.name === selectedCategory);

  if (error) return <main className="coverage-page container"><p>{error}</p></main>;
  if (!data) return <main className="coverage-page container"><p>신뢰도 지도를 계산하고 있습니다.</p></main>;

  const maxCount = Math.max(1, ...data.categories.map((category) => category.count));
  const vectorRate = data.totalRecords > 0
    ? Math.round(data.vectorSyncedRecords / data.totalRecords * 100)
    : 0;
  const attachmentRate = data.attachmentRecords > 0
    ? Math.round(data.extractedAttachmentRecords / data.attachmentRecords * 100)
    : 100;

  return (
    <main className="coverage-page container">
      <header className="coverage-heading">
        <div><span>KNOWLEDGE TRUST MAP</span><h1>세종대 정보 신뢰도 지도</h1></div>
        <p>원의 크기는 수집된 위키 수, 색상과 점수는 출처·최신성·첨부 처리·벡터 반영 상태를 나타냅니다. 문서가 많아도 근거가 약하면 높은 점수를 받지 않습니다.</p>
      </header>

      <section className="coverage-summary">
        <article><strong>{data.trustScore}점</strong><span>전체 신뢰도</span></article>
        <article><strong>{data.totalRecords.toLocaleString()}</strong><span>수집·게시 위키</span></article>
        <article><strong>{data.officialRecords.toLocaleString()}</strong><span>공식 출처 문서</span></article>
        <article><strong>{vectorRate}%</strong><span>벡터 DB 반영</span></article>
      </section>

      <section className="trust-evidence-summary">
        <span>첨부 추출 <b>{data.extractedAttachmentRecords}/{data.attachmentRecords}</b> ({attachmentRate}%)</span>
        <span>에브리타임 자료 <b>{data.everytimeRecords.toLocaleString()}</b></span>
        <span>최근 갱신 <b>{formatDate(data.latestUpdatedAt)}</b></span>
      </section>

      <section className="coverage-grid">
        <div className="coverage-map-panel">
          <div className="coverage-toolbar">
            {['전체', ...data.categories.map((item) => item.name)].map((category) => (
              <button key={category} className={selectedCategory === category ? 'active' : ''} onClick={() => setSelectedCategory(category)}>{category}</button>
            ))}
          </div>
          <svg className="coverage-map" viewBox="0 0 900 540" role="img" aria-label="카테고리별 위키 신뢰도 지도">
            <defs><filter id="node-shadow"><feDropShadow dx="0" dy="5" stdDeviation="6" floodOpacity=".18" /></filter></defs>
            {visibleNodes.map((node) => {
              const radius = Math.min(74, 25 + Math.sqrt(node.count) * 4);
              return (
                <g key={node.id} className="coverage-node" onClick={() => setSelectedNode(node)} tabIndex="0">
                  <circle cx={node.x} cy={node.y} r={radius} fill={node.color} filter="url(#node-shadow)" opacity={selectedNode?.id === node.id ? 1 : .88} />
                  <text x={node.x} y={node.y - 8} textAnchor="middle">{node.name}</text>
                  <text className="coverage-node-score" x={node.x} y={node.y + 11} textAnchor="middle">{node.trustScore}점</text>
                  <text className="coverage-node-count" x={node.x} y={node.y + 27} textAnchor="middle">{node.count.toLocaleString()}건</text>
                </g>
              );
            })}
          </svg>
          <div className="trust-map-legend">
            <span><i className="high" />높음 75점 이상</span>
            <span><i className="medium" />보통 55~74점</span>
            <span><i className="low" />확인 필요 55점 미만</span>
          </div>
        </div>

        <aside className="coverage-detail">
          {selectedNode ? (
            <>
              <span className="coverage-kicker">TRUST EVIDENCE</span>
              <h2>{selectedNode.name}</h2>
              <strong>{selectedNode.trustScore}점</strong>
              <em className={`trust-badge ${selectedNode.trustLevel}`}>{trustLabel(selectedNode.trustLevel)}</em>
              <p>{selectedNode.description || '카테고리 설명이 등록되지 않았습니다.'}</p>
              <div className="trust-source-counts">
                <span>공식 {selectedNode.officialCount}건</span>
                <span>일반 위키 {selectedNode.otherCount}건</span>
                <span>에타 {selectedNode.communityCount}건</span>
              </div>
              <MetricBar label="출처 품질" value={selectedNode.sourceScore} />
              <MetricBar label="최신성" value={selectedNode.freshnessScore} />
              <MetricBar label="첨부 처리" value={selectedNode.attachmentScore} />
              <MetricBar label="벡터 반영" value={selectedNode.vectorScore} />
              <small>최근 갱신: {formatDate(selectedNode.latestUpdatedAt)}</small>
            </>
          ) : (
            <><span className="coverage-kicker">HOW TO READ</span><h2>원을 선택해보세요</h2><p>카테고리의 신뢰도 근거와 수집 문서 구성을 확인할 수 있습니다.</p></>
          )}
        </aside>
      </section>

      <section className="coverage-bars">
        <div className="coverage-section-title"><span>TRUST RANKING</span><h2>카테고리별 신뢰도</h2></div>
        {[...data.categories].sort((a, b) => b.trustScore - a.trustScore).map((category) => (
          <article key={category.id}>
            <div><strong>{category.name}</strong><span className={`trust-badge ${category.trustLevel}`}>{trustLabel(category.trustLevel)}</span></div>
            <div className="coverage-track"><i style={{ width: `${category.trustScore}%`, background: trustColors[category.trustLevel] }} /></div>
            <b>{category.trustScore}점</b>
          </article>
        ))}
        <p className="coverage-note">출처 품질 45%, 최신성 25%, 첨부파일 처리 15%, 벡터 DB 반영 15%를 합산합니다.</p>
      </section>

      <section className="coverage-bars">
        <div className="coverage-section-title"><span>COLLECTION VOLUME</span><h2>수집 위키 수</h2></div>
        {data.categories.map((category) => (
          <article key={category.id}>
            <div><strong>{category.name}</strong><span className={`coverage-level ${category.level}`}>{volumeLabel(category.level)}</span></div>
            <div className="coverage-track"><i style={{ width: `${Math.max(4, category.count / maxCount * 100)}%` }} /></div>
            <b>{category.count.toLocaleString()}건</b>
          </article>
        ))}
        <p className="coverage-note">문서 수는 신뢰도와 별도로 유지합니다. 10건 미만은 부족, 30건 미만은 보통으로 표시합니다.</p>
      </section>

      {data.everytimeContentTypes.length > 0 && (
        <section className="coverage-bars">
          <div className="coverage-section-title"><span>COMMUNITY EVIDENCE</span><h2>에브리타임 자료 구성</h2></div>
          {data.everytimeContentTypes.map((item) => (
            <article key={item.type}>
              <div><strong>{contentTypeLabels[item.type] || item.type}</strong></div>
              <div className="coverage-track"><i style={{ width: `${Math.max(4, item.count / Math.max(1, data.everytimeRecords) * 100)}%` }} /></div>
              <b>{item.count.toLocaleString()}건</b>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
