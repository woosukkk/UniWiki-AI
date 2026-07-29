export const AUTH_CHANGED_EVENT = 'uniwiki-auth-changed';

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('uniwiki-user'));
  } catch {
    return null;
  }
}

export function saveAuthentication(response) {
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

export function clearAuthentication() {
  localStorage.removeItem('uniwiki-token');
  localStorage.removeItem('uniwiki-user');
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}
