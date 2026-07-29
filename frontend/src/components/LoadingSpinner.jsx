export function LoadingSpinner({ label = '불러오는 중입니다' }) {
  return (
    <div className="common-loading" role="status" aria-live="polite">
      <span className="common-loading-spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
