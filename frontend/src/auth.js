export const AUTH_CHANGED_EVENT = 'uniwiki-auth-changed';

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('uniwiki-user'));
  } catch {
    return null;
  }
}

export function saveAuthentication(response) {
  if (!response?.token || !response?.id || !response?.email) {
    throw new Error('로그인 응답에 인증 정보가 없습니다.');
  }
  const user = {
    id: response.id,
    email: response.email,
    nickname: response.nickname,
    role: response.role,
  };
  localStorage.setItem('uniwiki-token', response.token);
  localStorage.setItem('uniwiki-user', JSON.stringify(user));
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
  return user;
}

export function updateStoredUser(response) {
  if (!response?.id || !response?.email) return null;
  const user = {
    id: response.id,
    email: response.email,
    nickname: response.nickname,
    role: response.role,
  };
  localStorage.setItem('uniwiki-user', JSON.stringify(user));
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
  return user;
}

export function clearAuthentication() {
  localStorage.removeItem('uniwiki-token');
  localStorage.removeItem('uniwiki-user');
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}
