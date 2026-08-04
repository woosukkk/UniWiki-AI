import { useMemo, useState } from 'react';

const categoryDefinitions = [
  {
    key: 'curriculum',
    label: '교과정보',
    colorClass: 'data-category-green',
  },
  {
    key: 'academic',
    label: '학교정보',
    colorClass: 'data-category-blue',
  },
  {
    key: 'campus',
    label: '학교생활',
    colorClass: 'data-category-orange',
  },
  {
    key: 'support',
    label: '학생지원',
    colorClass: 'data-category-purple',
  },
  {
    key: 'career',
    label: '진로·취업',
    colorClass: 'data-category-yellow',
  },
  {
    key: 'software',
    label: 'SW·인증',
    colorClass: 'data-category-teal',
  },
];

const knowledgeNodes = [
  {
    id: 1,
    label: '시간표',
    value: 206,
    category: 'curriculum',
    size: 'large',
    x: 15,
    y: 24,
    description: '학기별 강의 시간표와 수업 운영 정보',
  },
  {
    id: 2,
    label: '교과과정',
    value: 229,
    category: 'curriculum',
    size: 'xlarge',
    x: 30,
    y: 42,
    description: '전공별 교육과정, 필수 및 선택 과목 정보',
  },
  {
    id: 3,
    label: '학사제도',
    value: 8,
    category: 'academic',
    size: 'small',
    x: 43,
    y: 14,
    description: '휴학, 복학, 전과 등 학사 제도 관련 정보',
  },
  {
    id: 4,
    label: '수강편람',
    value: 6,
    category: 'academic',
    size: 'small',
    x: 56,
    y: 25,
    description: '수강신청 안내와 과목별 수강 정보',
  },
  {
    id: 5,
    label: '수강신청',
    value: 3,
    category: 'academic',
    size: 'small',
    x: 50,
    y: 45,
    description: '수강신청 일정, 절차 및 유의사항',
  },
  {
    id: 6,
    label: '학과공지',
    value: 29,
    category: 'campus',
    size: 'medium',
    x: 70,
    y: 18,
    description: '학과별 공지사항과 주요 안내',
  },
  {
    id: 7,
    label: '장학',
    value: 104,
    category: 'support',
    size: 'large',
    x: 77,
    y: 48,
    description: '교내외 장학금 종류와 신청 정보',
  },
  {
    id: 8,
    label: '현장실습',
    value: 87,
    category: 'support',
    size: 'medium',
    x: 82,
    y: 70,
    description: '현장실습, 인턴십, 산학협력 관련 정보',
  },
  {
    id: 9,
    label: '취업공지',
    value: 510,
    category: 'career',
    size: 'xxlarge',
    x: 40,
    y: 72,
    description: '채용공고, 취업지원, 기업설명회 정보',
  },
  {
    id: 10,
    label: '프로그램',
    value: 49,
    category: 'career',
    size: 'medium',
    x: 59,
    y: 77,
    description: '취업 및 진로 관련 비교과 프로그램',
  },
  {
    id: 11,
    label: 'SW사업',
    value: 24,
    category: 'software',
    size: 'medium',
    x: 17,
    y: 70,
    description: 'SW중심대학사업 및 교육 프로그램',
  },
  {
    id: 12,
    label: 'TOSC',
    value: 18,
    category: 'software',
    size: 'small',
    x: 27,
    y: 83,
    description: 'SW 역량 인증 및 시험 관련 정보',
  },
];

const coverageItems = [
  {
    key: 'curriculum',
    label: '교과정보',
    value: 435,
    status: '많음',
    statusClass: 'coverage-status-high',
  },
  {
    key: 'academic',
    label: '학교정보',
    value: 17,
    status: '부족',
    statusClass: 'coverage-status-low',
  },
  {
    key: 'campus',
    label: '학교생활',
    value: 29,
    status: '부족',
    statusClass: 'coverage-status-low',
  },
  {
    key: 'support',
    label: '학생지원',
    value: 171,
    status: '보통',
    statusClass: 'coverage-status-medium',
  },
  {
    key: 'career',
    label: '진로·취업',
    value: 559,
    status: '많음',
    statusClass: 'coverage-status-high',
  },
  {
    key: 'software',
    label: 'SW·인증',
    value: 42,
    status: '부족',
    statusClass: 'coverage-status-low',
  },
];

const summaryItems = [
  {
    label: 'TOTAL RECORDS',
    value: '1,253',
  },
  {
    label: 'DATA SOURCES',
    value: '12',
  },
  {
    label: 'LOW COVERAGE',
    value: '3',
  },
  {
    label: 'LAST UPDATED',
    value: '2026.07.31',
  },
];

const maxCoverage = Math.max(...coverageItems.map((item) => item.value));

export function DataVisualizationPage() {
  const [activeCategory, setActiveCategory] = useState('all');
  const [selectedNode, setSelectedNode] = useState(knowledgeNodes[0]);

  const filteredNodes = useMemo(() => {
    if (activeCategory === 'all') {
      return knowledgeNodes;
    }

    return knowledgeNodes.filter(
      (node) => node.category === activeCategory,
    );
  }, [activeCategory]);

  function findCategory(categoryKey) {
    return categoryDefinitions.find(
      (category) => category.key === categoryKey,
    );
  }

  return (
    <div className="data-map-page">
      <section className="data-map-hero">
        <div className="data-map-content-width data-map-hero-inner">
          <div className="data-map-hero-copy">
            <span className="data-map-section-label">
              KNOWLEDGE COVERAGE
            </span>

            <h1>세종대 정보 지형도</h1>

            <p>
              색은 정보 영역을 나타내며, 원의 크기는 해당 영역에
              축적된 정보의 양을 의미합니다.
              <br />
              지식을 함께 만들고, 아직 부족한 정보 영역을 확인해보세요.
            </p>
          </div>

          <div className="data-map-hero-graphic" aria-hidden="true">
            <span className="data-map-orbit orbit-one" />
            <span className="data-map-orbit orbit-two" />
            <span className="data-map-orbit orbit-three" />

            <strong>U</strong>

            <span className="data-map-hero-dot dot-one" />
            <span className="data-map-hero-dot dot-two" />
            <span className="data-map-hero-dot dot-three" />
            <span className="data-map-hero-dot dot-four" />

            <span className="data-map-chart-bar bar-one" />
            <span className="data-map-chart-bar bar-two" />
            <span className="data-map-chart-bar bar-three" />
          </div>
        </div>
      </section>

      <section className="data-map-summary">
        <div className="data-map-content-width data-map-summary-grid">
          {summaryItems.map((item) => (
            <article className="data-map-summary-card" key={item.label}>
              <strong>{item.value}</strong>
              <span>{item.label}</span>
            </article>
          ))}
        </div>
      </section>

      <section className="data-map-main-section">
        <div className="data-map-content-width data-map-main-layout">
          <div className="data-map-board">
            <div className="data-map-board-header">
              <div>
                <span className="data-map-section-label">
                  INFORMATION LANDSCAPE
                </span>
                <h2>정보 영역 분포 지도</h2>
              </div>

              <p>
                카테고리를 선택하거나 원을 눌러
                <br />
                상세 정보를 확인할 수 있습니다.
              </p>
            </div>

            <div
              className="data-map-category-tabs"
              aria-label="데이터 카테고리"
            >
              <button
                type="button"
                className={
                  activeCategory === 'all'
                    ? 'data-map-category-button active'
                    : 'data-map-category-button'
                }
                onClick={() => setActiveCategory('all')}
              >
                전체
              </button>

              {categoryDefinitions.map((category) => (
                <button
                  type="button"
                  key={category.key}
                  className={
                    activeCategory === category.key
                      ? 'data-map-category-button active'
                      : 'data-map-category-button'
                  }
                  onClick={() => setActiveCategory(category.key)}
                >
                  {category.label}
                </button>
              ))}
            </div>

            <div className="data-map-visualization">
              <span className="data-map-grid-line line-one" />
              <span className="data-map-grid-line line-two" />
              <span className="data-map-grid-line line-three" />

              {filteredNodes.map((node) => {
                const category = findCategory(node.category);

                return (
                  <button
                    type="button"
                    key={node.id}
                    className={[
                      'data-map-node',
                      `data-map-node-${node.size}`,
                      category?.colorClass,
                      selectedNode?.id === node.id
                        ? 'data-map-node-selected'
                        : '',
                    ]
                      .filter(Boolean)
                      .join(' ')}
                    style={{
                      left: `${node.x}%`,
                      top: `${node.y}%`,
                    }}
                    onClick={() => setSelectedNode(node)}
                    aria-label={`${node.label}, ${node.value}건`}
                  >
                    <strong>{node.label}</strong>
                    <span>{node.value}</span>
                  </button>
                );
              })}
            </div>

            <div className="data-map-legend">
              {categoryDefinitions.map((category) => (
                <div key={category.key}>
                  <span
                    className={[
                      'data-map-legend-dot',
                      category.colorClass,
                    ].join(' ')}
                  />
                  <span>{category.label}</span>
                </div>
              ))}
            </div>
          </div>

          <aside className="data-map-detail-panel">
            <span className="data-map-detail-kicker">
              SELECTED DATA
            </span>

            <div className="data-map-detail-index">
              {String(selectedNode.id).padStart(2, '0')}
            </div>

            <h2>{selectedNode.label}</h2>

            <p>{selectedNode.description}</p>

            <dl>
              <div>
                <dt>RECORDS</dt>
                <dd>{selectedNode.value}</dd>
              </div>

              <div>
                <dt>CATEGORY</dt>
                <dd>
                  {findCategory(selectedNode.category)?.label}
                </dd>
              </div>

              <div>
                <dt>STATUS</dt>
                <dd>
                  {selectedNode.value < 50 ? 'LOW' : 'ACTIVE'}
                </dd>
              </div>
            </dl>

            <div className="data-map-detail-guide">
              <span className="data-map-detail-guide-dot" />

              <div>
                <strong>HOW TO READ</strong>
                <p>
                  원의 크기는 기록 수를, 색상은 정보 카테고리를
                  의미합니다.
                </p>
              </div>
            </div>
          </aside>
        </div>
      </section>

      <section className="data-map-coverage-section">
        <div className="data-map-content-width">
          <div className="data-map-coverage-header">
            <div>
              <span className="data-map-section-label">
                COVERAGE BALANCE
              </span>
              <h2>영역별 데이터 충족도</h2>
            </div>

            <p>
              현재 50건 미만인 영역을
              <br />
              ‘부족’으로 표시합니다.
            </p>
          </div>

          <div className="data-map-coverage-list">
            {coverageItems.map((item) => {
              const category = findCategory(item.key);
              const ratio = (item.value / maxCoverage) * 100;

              return (
                <div
                  className="data-map-coverage-row"
                  key={item.key}
                >
                  <div className="data-map-coverage-label">
                    <strong>{item.label}</strong>
                    <span className={item.statusClass}>
                      {item.status}
                    </span>
                  </div>

                  <div className="data-map-progress">
                    <span
                      className={[
                        'data-map-progress-value',
                        category?.colorClass,
                      ]
                        .filter(Boolean)
                        .join(' ')}
                      style={{
                        width: `${Math.max(ratio, 4)}%`,
                      }}
                    />
                  </div>

                  <strong className="data-map-coverage-value">
                    {item.value}건
                  </strong>
                </div>
              );
            })}
          </div>

          <p className="data-map-coverage-note">
            ※ 현재 50건 미만인 영역은 ‘부족’으로 표시됩니다.
            원천 데이터 간 일부 중복이 있어 실제 문서 수와 다를 수
            있습니다.
          </p>
        </div>
      </section>
    </div>
  );
}