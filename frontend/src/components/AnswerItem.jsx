import { useState } from 'react';

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(value));
}

export function AnswerItem({ answer, canEdit, canAccept, onUpdate, onDelete, onAccept }) {
  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(answer.content);
  const [submitting, setSubmitting] = useState(false);

  async function submitEdit(event) {
    event.preventDefault();
    if (!content.trim()) return;
    setSubmitting(true);
    const succeeded = await onUpdate(answer.id, content.trim());
    setSubmitting(false);
    if (succeeded) setEditing(false);
  }

  return (
    <article className={`answer-card ${answer.accepted ? 'answer-card-accepted' : ''}`}>
      <div className="answer-avatar">{answer.authorNickname.slice(0, 1)}</div>
      <div className="answer-body">
        <div className="answer-meta">
          <strong>{answer.authorNickname}</strong>
          <span>{formatDate(answer.createdAt)}</span>
          {answer.accepted && <span className="accepted">채택된 답변</span>}
        </div>
        {editing ? (
          <form className="inline-edit" onSubmit={submitEdit}>
            <textarea value={content} onChange={(event) => setContent(event.target.value)} />
            <div>
              <button className="button button-small" disabled={submitting}>{submitting ? '저장 중...' : '저장'}</button>
              <button className="text-button" type="button" onClick={() => { setContent(answer.content); setEditing(false); }}>취소</button>
            </div>
          </form>
        ) : <p>{answer.content}</p>}
        {!editing && (
          <div className="answer-actions">
            {canAccept && <button className="accept-answer-button" onClick={() => onAccept(answer.id)}>답변 채택</button>}
            {canEdit && <button className="text-button" onClick={() => setEditing(true)}>수정</button>}
            {canEdit && <button className="text-button danger" onClick={() => onDelete(answer)}>삭제</button>}
          </div>
        )}
      </div>
    </article>
  );
}
