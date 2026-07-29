import { useEffect, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext.jsx';

export function Header() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, isAdmin, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => { setMenuOpen(false); }, [location.pathname]);

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <header className="site-header">
      <div className="header-inner">
        <NavLink className="brand" to="/" aria-label="UniWiki 홈">
          <span className="brand-mark">U</span>
          <span>UniWiki</span>
        </NavLink>
        <nav className={`main-nav ${menuOpen ? 'main-nav-open' : ''}`} aria-label="주요 메뉴">
          <NavLink to="/wiki">위키</NavLink>
          <NavLink to="/questions">질문 게시판</NavLink>
          <NavLink to="/chatbot">AI 챗봇</NavLink>
          {isAdmin && <NavLink to="/admin">관리자</NavLink>}
        </nav>
        <button className="mobile-menu-button" type="button" aria-label="메뉴 열기" aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}>
          <span /><span /><span />
        </button>
        <div className="user-area">
          {user ? (
            <>
              <NavLink className="user-chip" to="/mypage">
                <span>{user.nickname.slice(0, 1)}</span>{user.nickname}
              </NavLink>
              <button className="text-button" onClick={handleLogout}>로그아웃</button>
            </>
          ) : (
            <NavLink className="button button-small" to="/login">로그인</NavLink>
          )}
        </div>
      </div>
    </header>
  );
}
