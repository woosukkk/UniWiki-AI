export function ConfirmDialog({ title, message, confirmLabel = '확인', busy, onConfirm, onCancel }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onCancel()}>
      <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title">
        <span className="section-kicker">PLEASE CONFIRM</span>
        <h2 id="confirm-title">{title}</h2>
        <p>{message}</p>
        <div className="form-actions">
          <button className="text-button" type="button" onClick={onCancel} disabled={busy}>취소</button>
          <button className="button danger-button" type="button" onClick={onConfirm} disabled={busy}>
            {busy ? '처리 중...' : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
