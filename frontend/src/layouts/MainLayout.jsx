import { Outlet } from 'react-router-dom';
import { Footer } from '../components/Footer.jsx';
import { Header } from '../components/Header.jsx';

export function MainLayout() {
  return (
    <div className="editorial-app-shell">
      <Header />

      <main className="editorial-page-content">
        <Outlet />
      </main>

      <Footer />
    </div>
  );
}