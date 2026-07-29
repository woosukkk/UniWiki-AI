export function LikeButton({ count, liked, busy, onClick }) {
  return (
    <button
      className={`common-like-button ${liked ? 'liked' : ''}`}
      type="button"
      disabled={busy}
      onClick={onClick}
      aria-pressed={liked}
    >
      <span aria-hidden="true">♥</span>
      <strong>{count}</strong>
      <small>{liked ? '좋아요 취소' : '좋아요'}</small>
    </button>
  );
}
