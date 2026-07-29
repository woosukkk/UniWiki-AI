export function ErrorMessage({
  title = '요청을 완료하지 못했습니다',
  message,
  onRetry,
}) {
  return (
    <section className="common-state common-state-error" role="alert">
      <span className="common-state-icon">!</span>
      <h2>{title}</h2>
      <p>{message || '잠시 후 다시 시도해 주세요.'}</p>
      {onRetry && <button className="button button-small" onClick={onRetry}>다시 시도</button>}
    </section>
  );
}
