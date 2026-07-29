import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <main className="system-page container">
      <section className="not-found-page">
        <span className="not-found-code">404</span>
        <h1>페이지를 찾을 수 없습니다</h1>
        <p>주소가 올바른지 확인하거나 홈으로 돌아가 주세요.</p>
        <Link className="button" to="/">홈으로 돌아가기</Link>
      </section>
    </main>
  );
}
