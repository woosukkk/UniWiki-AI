import { useEffect, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext.jsx';

const navigationItems = [
  { to: '/wiki', label: 'WIKI' },
  { to: '/questions', label: 'Q&A BOARD' },
  { to: '/chatbot', label: 'AI BOT' },
  { to: '/visualization', label: 'DATA MAP' },
  { to: '/mypage', label: 'MY PAGE' },
];

function getNavLinkClass({ isActive }) {
  return isActive
    ? 'editorial-nav-link editorial-nav-link-active'
    : 'editorial-nav-link';
}

export function Header() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, isAdmin, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <header className="editorial-header">
      <div className="editorial-header-inner">
        <NavLink
          className="editorial-brand"
          to="/"
          aria-label="UniWiki 메인으로 이동"
        >
          UNIWIKI
        </NavLink>

        <nav
          className={
            menuOpen
              ? 'editorial-nav editorial-nav-open'
              : 'editorial-nav'
          }
          aria-label="주요 메뉴"
        >
          {navigationItems.map((item) => (
            <NavLink
              key={item.to}
              className={getNavLinkClass}
              to={item.to}
            >
              {item.label}
            </NavLink>
          ))}

          {isAdmin && (
            <NavLink className={getNavLinkClass} to="/admin">
              ADMIN
            </NavLink>
          )}
        </nav>

        <div className="editorial-header-actions">
          {user ? (
            <>
              <NavLink
                className="editorial-user-link"
                to="/mypage"
                title={`${user.nickname}님의 마이페이지`}
              >
                {user.nickname}
              </NavLink>

              <button
                className="editorial-auth-button editorial-auth-button-text"
                type="button"
                onClick={handleLogout}
              >
                LOGOUT
              </button>
            </>
          ) : (
            <NavLink
              className="editorial-auth-button"
              to="/login"
            >
              LOGIN
            </NavLink>
          )}
        </div>

        <button
          className={
            menuOpen
              ? 'editorial-menu-button editorial-menu-button-open'
              : 'editorial-menu-button'
          }
          type="button"
          aria-label={menuOpen ? '메뉴 닫기' : '메뉴 열기'}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((current) => !current)}
        >
          <span />
          <span />
        </button>
      </div>
    </header>
  );
}