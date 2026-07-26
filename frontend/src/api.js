const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function request(path, options = {}) {
  const token = localStorage.getItem('uniwiki-token');
  const headers = new Headers(options.headers);

  if (options.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  const data = contentType.includes('application/json')
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const message = typeof data === 'string'
      ? data
      : data?.message || data?.detail || '요청을 처리하지 못했습니다.';
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return data;
}

export const api = {
  login: (payload) => request('/api/users/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  signup: (payload) => request('/api/users/signup', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  getQuestions: () => request('/api/questions'),
  getQuestion: (questionId) => request(`/api/questions/${questionId}`),
  createQuestion: (payload) => request('/api/questions', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  updateQuestion: (questionId, payload) => request(`/api/questions/${questionId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }),
  deleteQuestion: (questionId) => request(`/api/questions/${questionId}`, {
    method: 'DELETE',
  }),
  getQuestionLikes: (questionId) => request(`/api/questions/${questionId}/likes/count`),
  likeQuestion: (questionId) => request(`/api/questions/${questionId}/likes`, { method: 'POST' }),
  unlikeQuestion: (questionId) => request(`/api/questions/${questionId}/likes`, { method: 'DELETE' }),
  getAnswers: (questionId) => request(`/api/questions/${questionId}/answers`),
  createAnswer: (questionId, payload) => request(`/api/answers/questions/${questionId}`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  updateAnswer: (answerId, payload) => request(`/api/answers/${answerId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }),
  deleteAnswer: (answerId) => request(`/api/answers/${answerId}`, { method: 'DELETE' }),
  getAnswerLikes: (answerId) => request(`/api/answers/${answerId}/likes/count`),
  likeAnswer: (answerId) => request(`/api/answers/${answerId}/likes`, { method: 'POST' }),
  unlikeAnswer: (answerId) => request(`/api/answers/${answerId}/likes`, { method: 'DELETE' }),
};
