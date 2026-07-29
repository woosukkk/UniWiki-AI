import { useEffect, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';

function storedUser() {
  try {
    return JSON.parse(localStorage.getItem('uniwiki-user'));
  } catch {
    return null;
  }
}

export function Header() {
  const navigate = useNavigate();
  const [user, setUser] = useState(storedUser);

  useEffect(() => {
    const syncUser = () => setUser(storedUser());
    window.addEventListener('storage', syncUser);
    window.addEventListener('uniwiki-auth-changed', syncUser);
    return () => {
      window.removeEventListener('storage', syncUser);
      window.removeEventListener('uniwiki-auth-changed', syncUser);
    };
  }, []);

  function logout() {
    localStorage.removeItem('uniwiki-token');
    localStorage.removeItem('uniwiki-user');
    setUser(null);
    navigate('/');
  }

  return (
    <header className="site-header">
      <div className="header-inner">
        <NavLink className="brand" to="/" aria-label="UniWiki 홈">
          <span className="brand-mark">U</span>
          <span>UniWiki</span>
        </NavLink>
        <nav className="main-nav" aria-label="주요 메뉴">
          <NavLink to="/wiki">위키</NavLink>
          <NavLink to="/questions">질문 게시판</NavLink>
          <NavLink to="/chatbot">AI 챗봇</NavLink>
        </nav>
        <div className="user-area">
          {user ? (
            <>
              <NavLink className="user-chip" to="/mypage">
                <span>{user.nickname.slice(0, 1)}</span>{user.nickname}
              </NavLink>
              <button className="text-button" onClick={logout}>로그아웃</button>
            </>
          ) : (
            <NavLink className="button button-small" to="/login">로그인</NavLink>
          )}
        </div>
      </div>
    </header>
  );
}
