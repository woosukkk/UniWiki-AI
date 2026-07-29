import axios from 'axios';
import { clearAuthentication } from './auth.js';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const httpClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

httpClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('uniwiki-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (requestError) => {
    if (requestError.response?.status === 401) {
      clearAuthentication();
    }
    const data = requestError.response?.data;
    const message = typeof data === 'string'
      ? data
      : data?.message || data?.detail || '요청을 처리하지 못했습니다.';
    const error = new Error(message);
    error.status = requestError.response?.status;
    error.cause = requestError;
    return Promise.reject(error);
  },
);

async function request(config) {
  const response = await httpClient.request(config);
  return response.status === 204 ? null : response.data;
}

export const api = {
  login: (payload) => request({ url: '/api/users/login', method: 'POST', data: payload }),
  signup: (payload) => request({ url: '/api/users/signup', method: 'POST', data: payload }),
  getWikiPosts: () => request({ url: '/api/wiki-posts' }),
  searchWikiPosts: (keyword) => request({ url: '/api/wiki-posts/search', params: { keyword } }),
  getWikiPost: (wikiPostId) => request({ url: `/api/wiki-posts/${wikiPostId}` }),
  getWikiPostLikes: (wikiPostId, authenticated = false) => request({ url: `/api/wiki-posts/${wikiPostId}/likes${authenticated ? '' : '/count'}` }),
  likeWikiPost: (wikiPostId) => request({ url: `/api/wiki-posts/${wikiPostId}/likes`, method: 'POST' }),
  unlikeWikiPost: (wikiPostId) => request({ url: `/api/wiki-posts/${wikiPostId}/likes`, method: 'DELETE' }),
  getCategories: () => request({ url: '/api/categories' }),
  createWikiPost: (payload) => request({ url: '/api/wiki-posts', method: 'POST', data: payload }),
  updateWikiPost: (wikiPostId, payload) => request({ url: `/api/wiki-posts/${wikiPostId}`, method: 'PUT', data: payload }),
  deleteWikiPost: (wikiPostId) => request({ url: `/api/wiki-posts/${wikiPostId}`, method: 'DELETE' }),
  getQuestions: () => request({ url: '/api/questions' }),
  getQuestion: (questionId) => request({ url: `/api/questions/${questionId}` }),
  createQuestion: (payload) => request({ url: '/api/questions', method: 'POST', data: payload }),
  updateQuestion: (questionId, payload) => request({ url: `/api/questions/${questionId}`, method: 'PUT', data: payload }),
  deleteQuestion: (questionId) => request({ url: `/api/questions/${questionId}`, method: 'DELETE' }),
  getQuestionLikes: (questionId) => request({ url: `/api/questions/${questionId}/likes/count` }),
  getQuestionLikeStatus: (questionId) => request({ url: `/api/questions/${questionId}/likes` }),
  likeQuestion: (questionId) => request({ url: `/api/questions/${questionId}/likes`, method: 'POST' }),
  unlikeQuestion: (questionId) => request({ url: `/api/questions/${questionId}/likes`, method: 'DELETE' }),
  getAnswers: (questionId) => request({ url: `/api/questions/${questionId}/answers` }),
  createAnswer: (questionId, payload) => request({ url: `/api/answers/questions/${questionId}`, method: 'POST', data: payload }),
  updateAnswer: (answerId, payload) => request({ url: `/api/answers/${answerId}`, method: 'PUT', data: payload }),
  deleteAnswer: (answerId) => request({ url: `/api/answers/${answerId}`, method: 'DELETE' }),
  acceptAnswer: (answerId) => request({ url: `/api/answers/${answerId}/accept`, method: 'PATCH' }),
  getAnswerLikes: (answerId) => request({ url: `/api/answers/${answerId}/likes/count` }),
  getAnswerLikeStatus: (answerId) => request({ url: `/api/answers/${answerId}/likes` }),
  likeAnswer: (answerId) => request({ url: `/api/answers/${answerId}/likes`, method: 'POST' }),
  unlikeAnswer: (answerId) => request({ url: `/api/answers/${answerId}/likes`, method: 'DELETE' }),
  askAi: (payload) => request({ url: '/api/ai/answers', method: 'POST', data: payload }),
  summarizeWikiPost: (wikiPostId, maxChars = 500) => request({ url: `/api/ai/wiki-posts/${wikiPostId}/summary`, method: 'POST', params: { maxChars } }),
};
