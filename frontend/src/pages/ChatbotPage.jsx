import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';

const welcomeMessage = {
  role: 'assistant',
  answer: '대학 생활에 관해 궁금한 내용을 물어보세요. UniWiki 문서를 근거로 답변할게요.',
  grounded: true,
  sources: [],
};

export function ChatbotPage() {
  const [messages, setMessages] = useState([welcomeMessage]);
  const [question, setQuestion] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submitQuestion(event) {
    event.preventDefault();
    const trimmedQuestion = question.trim();
    if (!trimmedQuestion || submitting) return;

    setMessages((current) => [...current, { role: 'user', answer: trimmedQuestion }]);
    setQuestion('');
    setSubmitting(true);
    try {
      const response = await api.askAi({ question: trimmedQuestion });
      setMessages((current) => [...current, { role: 'assistant', ...response }]);
    } catch (requestError) {
      setMessages((current) => [...current, {
        role: 'assistant',
        answer: requestError.status === 503
          ? 'AI 서비스 설정을 확인한 뒤 다시 시도해 주세요.'
          : '답변을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.',
        error: true,
        sources: [],
      }]);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="chatbot-page container">
      <header className="chatbot-heading">
        <span>UNIWIKI AI</span>
        <h1>학교생활 질문, 문서에서 찾아드려요.</h1>
        <p>답변은 UniWiki에 등록된 문서를 바탕으로 생성됩니다.</p>
      </header>
      <section className="chat-panel" aria-label="AI 챗봇 대화">
        <div className="chat-messages" aria-live="polite">
          {messages.map((message, index) => (
            <article className={`chat-message chat-message-${message.role} ${message.error ? 'chat-message-error' : ''}`} key={`${message.role}-${index}`}>
              <strong>{message.role === 'assistant' ? 'UniWiki AI' : '나'}</strong>
              <p>{message.answer}</p>
              {message.role === 'assistant' && message.grounded === false && (
                <small>근거가 충분한 위키 문서를 찾지 못한 답변입니다.</small>
              )}
              {message.sources?.length > 0 && (
                <div className="chat-sources">
                  <span>출처</span>
                  {message.sources.map((source) => (
                    <Link key={source.wikiPostId} to={`/wiki/${source.wikiPostId}`}>{source.title}</Link>
                  ))}
                </div>
              )}
            </article>
          ))}
          {submitting && <div className="chat-thinking"><i /><i /><i /><span>문서를 검색하고 있어요</span></div>}
        </div>
        <form className="chat-input" onSubmit={submitQuestion}>
          <label className="sr-only" htmlFor="ai-question">질문</label>
          <textarea
            id="ai-question"
            maxLength="1000"
            placeholder="예: 수강신청 정정 기간은 언제인가요?"
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                event.currentTarget.form.requestSubmit();
              }
            }}
          />
          <button className="button" disabled={submitting || !question.trim()}>질문하기</button>
        </form>
      </section>
    </main>
  );
}
