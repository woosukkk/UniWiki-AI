import { Outlet } from 'react-router-dom';
import { Header } from '../components/Header.jsx';

export function MainLayout() {
  return (
    <div className="app-shell">
      <Header />
      <Outlet />
    </div>
  );
}
