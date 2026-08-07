import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { AppErrorBoundary } from './components/AppErrorBoundary.jsx';
import { AuthProvider } from './contexts/AuthContext.jsx';

import './styles/tokens.css';
import './styles.css';
import './styles/layout.css';
import './styles/home.css';
import './styles/data-map.css';
import './styles/wiki.css';
import './styles/questions.css';
import './styles/chatbot.css';
import './styles/mypage.css';
import './styles/auth.css';
import './styles/admin.css';
import './styles/responsive.css';
import './styles/integration.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AppErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </AppErrorBoundary>
  </StrictMode>,
);
