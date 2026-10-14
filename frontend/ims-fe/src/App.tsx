import { useState, useEffect, useCallback, useMemo } from 'react';
import { Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { MainLayout } from './layouts/MainLayout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { ProductsPage } from './pages/ProductsPage';
import { ImportsPage } from './pages/ImportsPage';
import { CreateImportPage } from './pages/CreateImportPage';
import { ExportsPage } from './pages/ExportsPage';
import { CreateExportPage } from './pages/CreateExportPage';
import { InventoryPage } from './pages/InventoryPage';
import { KktsPage } from './pages/KktsPage';
import { MonthlyClosingPage } from './pages/MonthlyClosingPage';
import { UsersPage } from './pages/UsersPage';
import { ActivityPage } from './pages/ActivityPage';
import { Toast } from './components/common/Toast';
import type { ToastMessage } from './components/common/Toast';
import { authService, activitySocket } from './services';
import type { JwtResponse } from './types';
import './App.css';

export function App() {
  const navigate = useNavigate();
  const location = useLocation();

  const [currentUser, setCurrentUser] = useState<JwtResponse | null>(() => {
    // Kiểm tra phiên hết hạn ngay khi load
    if (authService.isTokenExpired()) {
      authService.logout();
      return null;
    }
    return authService.getCurrentUser();
  });
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const showToast = useCallback((message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') => {
    const id = Date.now().toString() + Math.random().toString();
    setToasts((prev) => [...prev, { id, message, type }]);
  }, []);

  // Xác định activeTab trên Sidebar dựa trên URL hiện tại
  const activeTab = useMemo(() => {
    const p = location.pathname;
    if (p.startsWith('/products')) return 'products';
    if (p.startsWith('/imports')) return 'imports';
    if (p.startsWith('/exports')) return 'exports';
    if (p.startsWith('/inventory')) return 'inventory';
    if (p.startsWith('/kkts')) return 'kkts';
    if (p.startsWith('/closings')) return 'closings';
    if (p.startsWith('/users')) return 'users';
    if (p.startsWith('/activity')) return 'activity';
    return 'dashboard';
  }, [location.pathname]);

  // WebSocket Realtime for Admin
  useEffect(() => {
    if (!currentUser || !currentUser.roles?.includes('ADMIN')) {
      activitySocket.disconnect();
      return;
    }

    activitySocket.connect();

    const unsubscribe = activitySocket.subscribe((log) => {
      // Không hiển thị trên giao diện cho kết quả/thông báo hệ thống Camunda
      if (
        log.action?.startsWith('CAMUNDA') ||
        log.action?.startsWith('INIT_') ||
        log.description?.toLowerCase().includes('camunda')
      ) {
        return;
      }

      // Don't show toast for own login
      if (log.action === 'LOGIN' && log.username === currentUser.username) {
        return;
      }
      const prefix = log.action === 'CREATE' ? ' Tạo mới: '
        : log.action === 'CANCEL' ? ' Hủy bỏ: '
          : log.action === 'DELETE' ? ' Xóa: '
            : log.action === 'UPDATE' ? ' Cập nhật: '
              : log.action === 'LOCK_MONTH' ? ' Khóa sổ: '
                : log.action === 'UNLOCK_MONTH' ? ' Mở khóa: '
                  : ' ';
      showToast(`${prefix}${log.description || `${log.username} đã thực hiện ${log.action}`}`, 'info');
    });

    return () => {
      unsubscribe();
    };
  }, [currentUser, showToast]);

  // Xác thực phiên làm việc thực tế với backend qua HttpOnly Cookie khi có thông tin người dùng
  useEffect(() => {
    const savedUser = authService.getCurrentUser();
    if (!savedUser) {
      // Người dùng chưa đăng nhập hoặc đã đăng xuất, không tự động gọi getMe
      return;
    }

    authService.getMe()
      .then((user) => {
        setCurrentUser(user);
      })
      .catch(() => {
        // Nếu cookie không còn hợp lệ hoặc đã hết hạn trên server
        authService.logout();
        setCurrentUser(null);
        navigate('/login');
      });
  }, [navigate]);

  // Kiểm tra phiên hết hạn định kỳ (mỗi 60 giây)
  useEffect(() => {
    if (!currentUser) return;
    const interval = setInterval(() => {
      if (authService.isTokenExpired()) {
        clearInterval(interval);
        activitySocket.disconnect();
        authService.logout();
        setCurrentUser(null);
        navigate('/login');
        showToast('Phiên làm việc đã hết hạn (3 giờ). Vui lòng đăng nhập lại.', 'warning');
      }
    }, 60_000);
    return () => clearInterval(interval);
  }, [currentUser, showToast, navigate]);

  useEffect(() => {
    const handleUnauthorized = () => {
      activitySocket.disconnect();
      authService.logout();
      setCurrentUser(null);
      navigate('/login');
      showToast('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 'warning');
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [showToast, navigate]);

  const handleDismissToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const handleLogout = async () => {
    activitySocket.disconnect();
    await authService.logout();
    setCurrentUser(null);
    navigate('/login');
    showToast('Đã đăng xuất khỏi hệ thống', 'info');
  };

  const handleDashboardNavigate = (target: string) => {
    if (target === 'imports-create') {
      navigate('/imports/create');
    } else if (target === 'exports-create') {
      navigate('/exports/create');
    } else if (target.startsWith('/')) {
      navigate(target);
    } else {
      navigate(`/${target}`);
    }
  };

  if (!currentUser) {
    return (
      <>
        <Toast toasts={toasts} onDismiss={handleDismissToast} />
        <Routes>
          <Route
            path="/login"
            element={
              <LoginPage
                onLoginSuccess={(u) => {
                  setCurrentUser(u);
                  navigate('/dashboard');
                }}
                showToast={showToast}
              />
            }
          />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </>
    );
  }

  const isController = currentUser.roles?.includes('CONTROLLER') || currentUser.roles?.includes('ADMIN');
  const isAdmin = currentUser.roles?.includes('ADMIN');

  return (
    <>
      <Toast toasts={toasts} onDismiss={handleDismissToast} />
      <MainLayout
        user={currentUser}
        activeTab={activeTab}
        onTabChange={(tab) => navigate(`/${tab}`)}
        onLogout={handleLogout}
      >
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/login" element={<Navigate to="/dashboard" replace />} />
          <Route
            path="/dashboard"
            element={<DashboardPage onNavigate={handleDashboardNavigate} showToast={showToast} />}
          />
          <Route
            path="/products"
            element={<ProductsPage userRoles={currentUser.roles || []} showToast={showToast} />}
          />
          <Route
            path="/imports"
            element={<ImportsPage showToast={showToast} onNavigateCreate={() => navigate('/imports/create')} />}
          />
          <Route
            path="/imports/create"
            element={<CreateImportPage showToast={showToast} onNavigateBack={() => navigate('/imports')} />}
          />
          <Route
            path="/exports"
            element={<ExportsPage showToast={showToast} onNavigateCreate={() => navigate('/exports/create')} />}
          />
          <Route
            path="/exports/create"
            element={<CreateExportPage showToast={showToast} onNavigateBack={() => navigate('/exports')} />}
          />
          <Route
            path="/inventory"
            element={<InventoryPage showToast={showToast} />}
          />
          <Route
            path="/kkts"
            element={<KktsPage showToast={showToast} />}
          />
          <Route
            path="/closings"
            element={isController ? <MonthlyClosingPage userRoles={currentUser.roles || []} showToast={showToast} /> : <Navigate to="/dashboard" replace />}
          />
          <Route
            path="/users"
            element={isAdmin ? <UsersPage userRoles={currentUser.roles || []} showToast={showToast} /> : <Navigate to="/dashboard" replace />}
          />
          <Route
            path="/activity"
            element={isAdmin ? <ActivityPage userRoles={currentUser.roles || []} showToast={showToast} /> : <Navigate to="/dashboard" replace />}
          />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </MainLayout>
    </>
  );
}

export default App;
