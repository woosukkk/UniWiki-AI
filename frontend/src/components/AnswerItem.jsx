import { useState } from 'react';
import { LikeButton } from './LikeButton.jsx';

function formatDate(value) {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function AnswerItem({
  answer,
  like,
  canEdit,
  canAccept,
  onUpdate,
  onDelete,
  onAccept,
  onLike,
  index = 0,
}) {
  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(
    answer.content,
  );

  const [submitting, setSubmitting] =
    useState(false);

  async function submitEdit(event) {
    event.preventDefault();

    const trimmedContent = content.trim();

    if (!trimmedContent) {
      return;
    }

    setSubmitting(true);

    const succeeded = await onUpdate(
      answer.id,
      trimmedContent,
    );

    setSubmitting(false);

    if (succeeded) {
      setEditing(false);
    }
  }

  function cancelEdit() {
    setContent(answer.content);
    setEditing(false);
  }

  return (
    <article
      className={
        answer.accepted
          ? 'editorial-answer-card editorial-answer-card-accepted'
          : 'editorial-answer-card'
      }
    >
      <div className="editorial-answer-index">
        {String(index + 1).padStart(2, '0')}
      </div>

      <div className="editorial-answer-main">
        <div className="editorial-answer-meta">
          <div className="editorial-answer-author">
            <span>
              {answer.authorNickname
                ?.slice(0, 1)
                .toUpperCase() || 'U'}
            </span>

            <div>
              <strong>
                {answer.authorNickname}
              </strong>
              <small>
                {formatDate(answer.createdAt)}
              </small>
            </div>
          </div>

          {answer.accepted && (
            <span className="editorial-answer-accepted-label">
              ACCEPTED ANSWER
            </span>
          )}
        </div>

        {editing ? (
          <form
            className="editorial-answer-edit-form"
            onSubmit={submitEdit}
          >
            <textarea
              value={content}
              onChange={(event) =>
                setContent(event.target.value)
              }
              aria-label="답변 수정 내용"
            />

            <div>
              <button
                type="button"
                onClick={cancelEdit}
              >
                CANCEL
              </button>

              <button
                type="submit"
                disabled={
                  submitting || !content.trim()
                }
              >
                {submitting
                  ? 'SAVING...'
                  : 'SAVE ANSWER'}
              </button>
            </div>
          </form>
        ) : (
          <p className="editorial-answer-content">
            {answer.content}
          </p>
        )}

        {!editing && (
          <div className="editorial-answer-actions">
            <LikeButton
              count={like?.likeCount || 0}
              liked={Boolean(like?.liked)}
              busy={Boolean(like?.busy)}
              onClick={() => onLike(answer.id)}
            />

            <div>
              {canAccept && (
                <button
                  className="editorial-answer-accept-button"
                  type="button"
                  onClick={() =>
                    onAccept(answer.id)
                  }
                >
                  ACCEPT ANSWER
                  <span>✓</span>
                </button>
              )}

              {canEdit && (
                <button
                  type="button"
                  onClick={() =>
                    setEditing(true)
                  }
                >
                  EDIT
                </button>
              )}

              {canEdit && (
                <button
                  className="danger"
                  type="button"
                  onClick={() => onDelete(answer)}
                >
                  DELETE
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </article>
  );
}
