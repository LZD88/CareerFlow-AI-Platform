import { createBrowserRouter, Navigate } from 'react-router-dom';
import NoPermission from '../components/NoPermission';
import RequireAuth from '../components/RequireAuth';
import { menuItems } from '../constants/menu';
import AdminLayout from '../layouts/AdminLayout';
import LoginPage from '../pages/login';
import UserPage from '../pages/users';
import RolePage from '../pages/roles';
import PermissionPage from '../pages/permissions';
import StudentPermissionTemplatePage from '../pages/student-permission-templates';
import ConfigPage from '../pages/configs';
import LogAnalysisPage from '../pages/log-analysis';
import DataAnalysisPage from '../pages/data-analysis';
import MediaAgentPage from '../pages/media-agent';
import InterviewReviewPage from '../pages/interview-reviews';
import InterviewQuestionPage from '../pages/interview-questions';
import CampusPage from '../pages/campuses';
import CoursePage from '../pages/courses';
import ClassPage from '../pages/classes';
import ClassSchedulePage from '../pages/class-schedules';
import TeacherSchedulePage from '../pages/teacher-schedules';
import DutyManagementPage from '../pages/duties';
import HomeworkPage from '../pages/homeworks';
import ProductPage from '../pages/products';
import OrderPage from '../pages/orders';
import PaymentOrderPage from '../pages/payment-orders';
import StudentPage from '../pages/students';
import StudentResumeAdminPage from '../pages/student-resumes';
import StudentDailyMomentPage from '../pages/student-daily-moments';
import FeishuCallbackPage from '../pages/feishu-callback';
import KnowledgeDocumentPage from '../pages/knowledge-documents';
import { getFirstAccessibleMenuPath } from '../utils/permission';
import WorkbenchPage from '../pages/workbench';
import PaymentReturnPage from '../pages/payment-return';

function HomeRedirect() {
  const targetPath = getFirstAccessibleMenuPath(menuItems);
  if (targetPath === '/') {
    return <NoPermission />;
  }
  return <Navigate to={targetPath} replace />;
}

export const router = createBrowserRouter([
  { path: '/payment/return', element: <PaymentReturnPage /> },
  {
    path: '/login',
    element: <LoginPage />
  },
  {
    path: '/feishu/callback',
    element: <FeishuCallbackPage />
  },
  {
    path: '/',
    element: <RequireAuth />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { index: true, element: <HomeRedirect /> },
          { path: 'workbench', element: <WorkbenchPage /> },
          { path: 'campuses', element: <CampusPage /> },
          { path: 'courses', element: <CoursePage /> },
          { path: 'classes', element: <ClassPage /> },
          { path: 'schedules', element: <ClassSchedulePage /> },
          { path: 'teacher-schedules', element: <TeacherSchedulePage /> },
          { path: 'duties', element: <DutyManagementPage /> },
          { path: 'homeworks', element: <HomeworkPage /> },
          { path: 'products', element: <ProductPage /> },
          { path: 'orders', element: <OrderPage /> },
          { path: 'payment-orders', element: <PaymentOrderPage /> },
          { path: 'students', element: <StudentPage /> },
          { path: 'student-resumes', element: <StudentResumeAdminPage /> },
          { path: 'student-daily-moments', element: <StudentDailyMomentPage /> },
          { path: 'users', element: <UserPage /> },
          { path: 'roles', element: <RolePage /> },
          { path: 'permissions', element: <PermissionPage /> },
          { path: 'student-permission-templates', element: <StudentPermissionTemplatePage /> },
          { path: 'configs', element: <ConfigPage /> },
          { path: 'knowledge-documents', element: <KnowledgeDocumentPage /> },
          { path: 'code-knowledge', element: <KnowledgeDocumentPage mode="code" /> },
          { path: 'log-analysis', element: <LogAnalysisPage /> },
          { path: 'data-analysis', element: <DataAnalysisPage /> },
          { path: 'interview-reviews', element: <InterviewReviewPage /> },
          { path: 'interview-questions', element: <InterviewQuestionPage /> },
          { path: 'media-agent', element: <MediaAgentPage /> }
        ]
      }
    ]
  }
]);
