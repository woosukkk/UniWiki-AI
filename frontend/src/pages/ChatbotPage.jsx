import {
  useMemo,
  useRef,
  useState,
} from 'react';

import { Link } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { api } from '../api.js';

const welcomeMessage = {
  id: 'welcome',
  role: 'assistant',
  answer:
    '안녕하세요. UniWiki에 등록된 학교 정보를 바탕으로 답변해드릴게요. 궁금한 내용을 입력해보세요.',
  grounded: true,
  sources: [],
};

const suggestedQuestions = [
  '소프트웨어학과 졸업요건을 알려줘',
  '수강신청 정정 기간은 언제야?',
  '교내 장학금 종류를 알려줘',
  '복수전공 신청 방법을 알려줘',
];

function createMessageId(role) {
  return `${role}-${Date.now()}-${Math.random()
    .toString(36)
    .slice(2, 8)}`;
}

export function ChatbotPage() {
  const [messages, setMessages] = useState([
    welcomeMessage,
  ]);

  const [question, setQuestion] = useState('');
  const [submitting, setSubmitting] =
    useState(false);

  const [selectedMessageId, setSelectedMessageId] =
    useState(welcomeMessage.id);

  const textareaRef = useRef(null);

  const selectedAssistantMessage = useMemo(() => {
    const selected = messages.find(
      (message) =>
        message.id === selectedMessageId &&
        message.role === 'assistant',
    );

    if (selected) {
      return selected;
    }

    return [...messages]
      .reverse()
      .find(
        (message) =>
          message.role === 'assistant',
      );
  }, [messages, selectedMessageId]);

  const selectedSources =
    selectedAssistantMessage?.sources || [];

  function applySuggestedQuestion(value) {
    setQuestion(value);

    requestAnimationFrame(() => {
      textareaRef.current?.focus();
    });
  }

  function resetConversation() {
    setMessages([welcomeMessage]);
    setQuestion('');
    setSelectedMessageId(welcomeMessage.id);
  }

  async function submitQuestion(event) {
    event.preventDefault();

    const trimmedQuestion = question.trim();

    if (!trimmedQuestion || submitting) {
      return;
    }

    const userMessage = {
      id: createMessageId('user'),
      role: 'user',
      answer: trimmedQuestion,
    };

    setMessages((current) => [
      ...current,
      userMessage,
    ]);

    setQuestion('');
    setSubmitting(true);

    try {
      const response = await api.askAi({
        question: trimmedQuestion,
      });

      const assistantMessage = {
        id: createMessageId('assistant'),
        role: 'assistant',
        answer: response.answer,
        grounded: response.grounded,
        sources: response.sources || [],
      };

      setMessages((current) => [
        ...current,
        assistantMessage,
      ]);

      setSelectedMessageId(
        assistantMessage.id,
      );
    } catch (requestError) {
      const errorMessage = {
        id: createMessageId('assistant-error'),
        role: 'assistant',
        answer:
          requestError.status === 503
            ? 'AI 서비스 설정을 확인한 뒤 다시 시도해주세요.'
            : '답변을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.',
        grounded: false,
        error: true,
        sources: [],
      };

      setMessages((current) => [
        ...current,
        errorMessage,
      ]);

      setSelectedMessageId(errorMessage.id);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="editorial-chatbot-page">
      <section className="editorial-chatbot-hero">
        <div className="editorial-chatbot-content-width editorial-chatbot-hero-inner">
          <div>
            <span className="editorial-chatbot-label">
              AI CAMPUS GUIDE
            </span>

            <h1>
              학교 정보에 근거해
              <br />
              <em>답변하는 AI.</em>
            </h1>
          </div>

          <p>
            UniWiki에 등록된 문서를 검색해
            관련 정보를 찾고,
            <br />
            답변에 사용된 출처를 함께
            제공합니다.
          </p>
        </div>
      </section>

      <section className="editorial-chatbot-workspace">
        <div className="editorial-chatbot-content-width editorial-chatbot-layout">
          <aside className="editorial-chatbot-sidebar">
            <div className="editorial-chatbot-sidebar-heading">
              <span>UNIWIKI AI</span>

              <strong>
                ASK
                <br />
                ANYTHING
              </strong>

              <p>
                학교생활에 필요한 정보를
                UniWiki 문서에서 찾아드립니다.
              </p>
            </div>

            <button
              className="editorial-chatbot-new-button"
              type="button"
              onClick={resetConversation}
            >
              NEW CHAT
              <span>＋</span>
            </button>

            <section className="editorial-chatbot-suggestions">
              <span>SUGGESTED QUESTIONS</span>

              <div>
                {suggestedQuestions.map(
                  (suggestedQuestion, index) => (
                    <button
                      key={suggestedQuestion}
                      type="button"
                      onClick={() =>
                        applySuggestedQuestion(
                          suggestedQuestion,
                        )
                      }
                    >
                      <span>
                        {String(index + 1).padStart(
                          2,
                          '0',
                        )}
                      </span>

                      <strong>
                        {suggestedQuestion}
                      </strong>

                      <span>→</span>
                    </button>
                  ),
                )}
              </div>
            </section>

            <section className="editorial-chatbot-principles">
              <span>HOW AI ANSWERS</span>

              <ul>
                <li>
                  UniWiki 문서를 먼저 검색합니다.
                </li>
                <li>
                  검색된 문서를 바탕으로
                  답변합니다.
                </li>
                <li>
                  근거 문서가 부족하면 이를
                  표시합니다.
                </li>
              </ul>
            </section>
          </aside>

          <main className="editorial-chatbot-main">
            <header className="editorial-chatbot-main-header">
              <div>
                <span>ACTIVE CONVERSATION</span>

                <strong>UNI AI ASSISTANT</strong>
              </div>

              <div className="editorial-chatbot-status">
                <span />
                ONLINE
              </div>
            </header>

            <section
              className="editorial-chatbot-messages"
              aria-label="AI 챗봇 대화"
              aria-live="polite"
            >
              {messages.map((message, index) => (
                <article
                  key={message.id}
                  className={[
                    'editorial-chat-message',
                    `editorial-chat-message-${message.role}`,
                    message.error
                      ? 'editorial-chat-message-error'
                      : '',
                    selectedMessageId ===
                    message.id
                      ? 'editorial-chat-message-selected'
                      : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => {
                    if (
                      message.role === 'assistant'
                    ) {
                      setSelectedMessageId(
                        message.id,
                      );
                    }
                  }}
                >
                  <div className="editorial-chat-message-profile">
                    <span>
                      {message.role === 'assistant'
                        ? 'AI'
                        : 'ME'}
                    </span>
                  </div>

                  <div className="editorial-chat-message-body">
                    <div className="editorial-chat-message-meta">
                      <strong>
                        {message.role === 'assistant'
                          ? 'UNIWIKI AI'
                          : 'YOU'}
                      </strong>

                      <span>
                        {String(index + 1).padStart(
                          2,
                          '0',
                        )}
                      </span>
                    </div>

                    {message.role === 'assistant' ? (
                      <div className="editorial-chat-message-answer">
                        <ReactMarkdown skipHtml>
                          {message.answer}
                        </ReactMarkdown>
                      </div>
                    ) : (
                      <p>{message.answer}</p>
                    )}

                    {message.role ===
                      'assistant' && (
                      <div className="editorial-chat-message-grounding">
                        {message.error ? (
                          <span className="editorial-chat-grounding-error">
                            RESPONSE ERROR
                          </span>
                        ) : message.grounded ===
                          false ? (
                          <span className="editorial-chat-grounding-warning">
                            LIMITED SOURCES
                          </span>
                        ) : (
                          <span className="editorial-chat-grounding-success">
                            GROUNDED ANSWER
                          </span>
                        )}

                        {message.sources?.length >
                          0 && (
                          <span>
                            {
                              message.sources
                                .length
                            }{' '}
                            SOURCES
                          </span>
                        )}
                      </div>
                    )}

                    {message.role ===
                      'assistant' &&
                      message.grounded ===
                        false &&
                      !message.error && (
                        <small className="editorial-chat-warning-text">
                          관련된 위키 문서를 충분히
                          찾지 못했습니다. 답변을
                          참고용으로만 확인해주세요.
                        </small>
                      )}

                    {message.sources?.length >
                      0 && (
                      <div className="editorial-chat-inline-sources">
                        {message.sources.map(
                          (source, sourceIndex) => (
                            <Link
                              key={`${source.wikiPostId}-${sourceIndex}`}
                              to={`/wiki/${source.wikiPostId}`}
                            >
                              <span>
                                SOURCE{' '}
                                {String(
                                  sourceIndex + 1,
                                ).padStart(
                                  2,
                                  '0',
                                )}
                              </span>

                              <strong>
                                {source.title}
                              </strong>

                              <span>↗</span>
                            </Link>
                          ),
                        )}
                      </div>
                    )}
                  </div>
                </article>
              ))}

              {submitting && (
                <div className="editorial-chat-thinking">
                  <div>
                    <span />
                    <span />
                    <span />
                  </div>

                  <p>
                    UniWiki 문서를 검색하고
                    답변을 생성하고 있습니다.
                  </p>
                </div>
              )}
            </section>

            <form
              className="editorial-chatbot-input"
              onSubmit={submitQuestion}
            >
              <div className="editorial-chatbot-input-heading">
                <label htmlFor="ai-question">
                  ASK A QUESTION
                </label>

                <span>
                  {question.length}/1000
                </span>
              </div>

              <div className="editorial-chatbot-input-control">
                <textarea
                  ref={textareaRef}
                  id="ai-question"
                  maxLength="1000"
                  placeholder="예: 소프트웨어학과 졸업요건을 알려줘"
                  value={question}
                  onChange={(event) =>
                    setQuestion(
                      event.target.value,
                    )
                  }
                  onKeyDown={(event) => {
                    if (
                      event.key === 'Enter' &&
                      !event.shiftKey
                    ) {
                      event.preventDefault();

                      event.currentTarget.form.requestSubmit();
                    }
                  }}
                />

                <button
                  type="submit"
                  disabled={
                    submitting ||
                    !question.trim()
                  }
                >
                  {submitting
                    ? 'THINKING...'
                    : 'ASK AI'}

                  <span>→</span>
                </button>
              </div>

              <p>
                Enter로 전송 · Shift + Enter로
                줄바꿈
              </p>
            </form>
          </main>

          <aside className="editorial-chatbot-sources-panel">
            <header>
              <span>REFERENCE DOCUMENTS</span>

              <strong>
                답변에 사용된
                <br />
                출처
              </strong>
            </header>

            {selectedSources.length > 0 ? (
              <div className="editorial-chatbot-source-list">
                {selectedSources.map(
                  (source, index) => (
                    <Link
                      key={`${source.wikiPostId}-${index}`}
                      to={`/wiki/${source.wikiPostId}`}
                      className="editorial-chatbot-source-card"
                    >
                      <span>
                        SOURCE{' '}
                        {String(
                          index + 1,
                        ).padStart(2, '0')}
                      </span>

                      <strong>
                        {source.title}
                      </strong>

                      <div>
                        <span>
                          WIKI #
                          {String(
                            source.wikiPostId,
                          ).padStart(4, '0')}
                        </span>

                        <span>OPEN ↗</span>
                      </div>
                    </Link>
                  ),
                )}
              </div>
            ) : (
              <div className="editorial-chatbot-no-sources">
                <span>NO SOURCES YET</span>

                <strong>
                  질문을 입력하면
                  <br />
                  참고 문서가 표시됩니다.
                </strong>

                <p>
                  AI가 위키 문서를 찾으면
                  출처 제목과 문서 링크를
                  이곳에서 확인할 수 있습니다.
                </p>
              </div>
            )}

            <section className="editorial-chatbot-source-status">
              <span>GROUNDING STATUS</span>

              <div>
                <strong>
                  {selectedAssistantMessage?.error
                    ? 'ERROR'
                    : selectedAssistantMessage
                          ?.grounded === false
                      ? 'LIMITED'
                      : 'VERIFIED'}
                </strong>

                <span
                  className={
                    selectedAssistantMessage?.error
                      ? 'source-status-error'
                      : selectedAssistantMessage
                            ?.grounded === false
                        ? 'source-status-warning'
                        : 'source-status-success'
                  }
                />
              </div>

              <p>
                {selectedAssistantMessage?.error
                  ? 'AI 답변 생성 중 오류가 발생했습니다.'
                  : selectedAssistantMessage
                        ?.grounded === false
                    ? '충분한 근거 문서를 찾지 못한 답변입니다.'
                    : selectedSources.length >
                        0
                      ? `${selectedSources.length}개의 위키 문서를 바탕으로 생성된 답변입니다.`
                      : '새로운 질문을 입력해 출처 문서를 확인하세요.'}
              </p>
            </section>

            <section className="editorial-chatbot-disclaimer">
              <span>NOTICE</span>

              <p>
                AI 답변은 UniWiki에 등록된 문서를
                기반으로 하며, 중요한 정보는
                반드시 원문을 함께 확인해주세요.
              </p>
            </section>
          </aside>
        </div>
      </section>
    </div>
  );
}
