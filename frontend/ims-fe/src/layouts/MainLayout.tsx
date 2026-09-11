import React, { useState } from 'react';
import { Sidebar } from '../components/common/Sidebar';
import { Header } from '../components/common/Header';
import type { JwtResponse } from '../types';

interface MainLayoutProps {
  user: JwtResponse | null;
  activeTab: string;
  onTabChange: (tab: string) => void;
  onLogout: () => void;
  children: React.ReactNode;
}

export const MainLayout: React.FC<MainLayoutProps> = ({
  user, activeTab, onTabChange, onLogout, children,
}) => {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  return (
    <div className="d-flex" style={{ minHeight: '100vh', width: '100vw', overflowX: 'hidden' }}>
      <Sidebar
        activeTab={activeTab}
        onTabChange={onTabChange}
        userRoles={user?.roles || []}
        isCollapsed={isSidebarCollapsed}
        onToggleCollapse={() => setIsSidebarCollapsed((v) => !v)}
      />

      <div className="d-flex flex-column flex-grow-1" style={{ minWidth: 0, backgroundColor: 'var(--bg-main)' }}>
        <Header user={user} onLogout={onLogout} />
        <main className="flex-grow-1 overflow-auto" style={{ padding: '28px 32px 40px' }}>
          {children}
        </main>
      </div>
    </div>
  );
};
