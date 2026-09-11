import { useState, useEffect, useCallback } from 'react';
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
  const [currentUser, setCurrentUser] = useState<JwtResponse | null>(() => {
    // Kiểm tra token hết hạn ngay khi load
    if (authService.isTokenExpired()) {
      authService.logout();
      localStorage.removeItem('pims_user');
      return null;
    }
    const savedUser = authService.getCurrentUser();
    return savedUser && savedUser.token ? savedUser : null;
  });
  const [activeTab, setActiveTab] = useState<string>('dashboard');
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const showToast = useCallback((message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') => {
    const id = Date.now().toString() + Math.random().toString();
    setToasts((prev) => [...prev, { id, message, type }]);
  }, []);

  // WebSocket Realtime for Admin
  useEffect(() => {
    if (!currentUser || !currentUser.roles?.includes('ADMIN')) {
      activitySocket.disconnect();
      return;
    }

    activitySocket.connect();

    const unsubscribe = activitySocket.subscribe((log) => {
      // Don't show toast for own login
      if (log.action === 'LOGIN' && log.username === currentUser.username) {
        return;
      }
      const prefix = log.action === 'CREATE' ? '🟢 Tạo mới: '
                   : log.action === 'CANCEL' ? '🔴 Hủy bỏ: '
                   : log.action === 'DELETE' ? '❌ Xóa: '
                   : log.action === 'UPDATE' ? '✏️ Cập nhật: '
                   : log.action === 'LOCK_MONTH' ? '🔒 Khóa sổ: '
                   : log.action === 'UNLOCK_MONTH' ? '🔓 Mở khóa: '
                   : '🔔 ';
      showToast(`${prefix}${log.description || `${log.username} đã thực hiện ${log.action}`}`, 'info');
    });

    return () => {
      unsubscribe();
    };
  }, [currentUser, showToast]);

  // Kiểm tra token hết hạn định kỳ (mỗi 60 giây)
  useEffect(() => {
    if (!currentUser) return;
    const interval = setInterval(() => {
      if (authService.isTokenExpired()) {
        clearInterval(interval);
        activitySocket.disconnect();
        authService.logout();
        localStorage.removeItem('pims_user');
        setCurrentUser(null);
        showToast('Phiên làm việc đã hết hạn (3 giờ). Vui lòng đăng nhập lại.', 'warning');
      }
    }, 60_000);
    return () => clearInterval(interval);
  }, [currentUser, showToast]);

  useEffect(() => {
    const handleUnauthorized = () => {
      activitySocket.disconnect();
      setCurrentUser(null);
      showToast('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 'warning');
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [showToast]);

  const handleDismissToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const handleLogout = () => {
    activitySocket.disconnect();
    authService.logout();
    setCurrentUser(null);
    setActiveTab('dashboard');
    showToast('Đã đăng xuất khỏi hệ thống', 'info');
  };

  if (!currentUser) {
    return (
      <>
        <Toast toasts={toasts} onDismiss={handleDismissToast} />
        <LoginPage onLoginSuccess={(u) => setCurrentUser(u)} showToast={showToast} />
      </>
    );
  }

  const renderActiveTab = () => {
    switch (activeTab) {
      case 'dashboard':
        return <DashboardPage onNavigate={setActiveTab} showToast={showToast} />;
      case 'products':
        return <ProductsPage userRoles={currentUser.roles || []} showToast={showToast} />;
      case 'imports':
        return <ImportsPage showToast={showToast} onNavigateCreate={() => setActiveTab('imports-create')} />;
      case 'imports-create':
        return <CreateImportPage showToast={showToast} onNavigateBack={() => setActiveTab('imports')} />;
      case 'exports':
        return <ExportsPage showToast={showToast} onNavigateCreate={() => setActiveTab('exports-create')} />;
      case 'exports-create':
        return <CreateExportPage showToast={showToast} onNavigateBack={() => setActiveTab('exports')} />;
      case 'inventory':
        return <InventoryPage showToast={showToast} />;
      case 'kkts':
        return <KktsPage showToast={showToast} />;
      case 'closings':
        return <MonthlyClosingPage userRoles={currentUser.roles || []} showToast={showToast} />;
      case 'users':
        return <UsersPage userRoles={currentUser.roles || []} showToast={showToast} />;
      case 'activity':
        return <ActivityPage userRoles={currentUser.roles || []} showToast={showToast} />;
      default:
        return <DashboardPage onNavigate={setActiveTab} showToast={showToast} />;
    }
  };

  return (
    <>
      <Toast toasts={toasts} onDismiss={handleDismissToast} />
      <MainLayout
        user={currentUser}
        activeTab={activeTab}
        onTabChange={setActiveTab}
        onLogout={handleLogout}
      >
        {renderActiveTab()}
      </MainLayout>
    </>
  );
}

export default App;
