import { NavLink } from 'react-router-dom';

export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="editorial-footer">
      <div className="editorial-footer-inner">
        <div className="editorial-footer-brand">
          <NavLink to="/" aria-label="UniWiki 메인으로 이동">
            UNIWIKI
          </NavLink>
          <p>ACADEMIC DATA PORTAL</p>
        </div>

        <nav
          className="editorial-footer-nav"
          aria-label="하단 메뉴"
        >
          <span>TERMS</span>
          <span>PRIVACY</span>
          <span>SOURCES</span>
          <span>CONTACT</span>
        </nav>

        <p className="editorial-footer-copyright">
          © {currentYear} UNIWIKI.
          <br />
          ALL RIGHTS RESERVED.
        </p>
      </div>
    </footer>
  );
}