import { Route, Routes } from 'react-router-dom';
import { MainLayout } from './layouts/MainLayout.jsx';
import { AdminPage } from './pages/AdminPage.jsx';
import { ChatbotPage } from './pages/ChatbotPage.jsx';
import { CommunityWikiPage } from './pages/CommunityWikiPage.jsx';
import { DataVisualizationPage } from './pages/DataVisualizationPage.jsx';
import { HomePage } from './pages/HomePage.jsx';
import { LoginPage } from './pages/LoginPage.jsx';
import { MyPage } from './pages/MyPage.jsx';
import { NotFoundPage } from './pages/NotFoundPage.jsx';
import { QuestionDetailPage } from './pages/QuestionDetailPage.jsx';
import { QuestionFormPage } from './pages/QuestionFormPage.jsx';
import { QuestionListPage } from './pages/QuestionListPage.jsx';
import { SignupPage } from './pages/SignupPage.jsx';
import { WikiDetailPage } from './pages/WikiDetailPage.jsx';
import { WikiFormPage } from './pages/WikiFormPage.jsx';
import { WikiListPage } from './pages/WikiListPage.jsx';
import { AdminRoute } from './routes/AdminRoute.jsx';
import { ProtectedRoute } from './routes/ProtectedRoute.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="wiki" element={<WikiListPage />} />
        <Route path="wiki/community" element={<CommunityWikiPage />} />
        <Route path="wiki/:wikiPostId" element={<WikiDetailPage />} />
        <Route path="questions" element={<QuestionListPage />} />
        <Route path="questions/:questionId" element={<QuestionDetailPage />} />
        <Route path="chatbot" element={<ChatbotPage />} />
        <Route path="visualization" element={<DataVisualizationPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="signup" element={<SignupPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="mypage" element={<MyPage />} />
          <Route path="wiki/new" element={<WikiFormPage />} />
          <Route path="wiki/:wikiPostId/edit" element={<WikiFormPage />} />
          <Route path="questions/new" element={<QuestionFormPage />} />
          <Route path="questions/:questionId/edit" element={<QuestionFormPage />} />
        </Route>
        <Route element={<AdminRoute />}>
          <Route path="admin" element={<AdminPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
