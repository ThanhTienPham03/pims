import React, { useMemo, useState } from 'react';
import saigonbankLogo from '../../assets/saigonbank.png';

interface SidebarProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
  userRoles: string[];
  isCollapsed: boolean;
  onToggleCollapse: () => void;
}

const NavIcon = ({ children }: { children: React.ReactNode }) => (
  <span className="sgb-sidebar-nav-icon">
    {children}
  </span>
);

const mainMenuItems = [
  {
    id: 'dashboard',
    label: 'Tổng quan',
    icon: <i className="bi bi-grid-1x2-fill" aria-hidden="true" />,
  },
  {
    id: 'products',
    label: 'Danh mục Ấn phẩm',
    icon: <i className="bi bi-box-seam" aria-hidden="true" />,
  },
  {
    id: 'imports',
    label: 'Nhập kho',
    icon: <i className="bi bi-box-arrow-in-down" aria-hidden="true" />,
  },
  {
    id: 'exports',
    label: 'Xuất kho',
    icon: <i className="bi bi-box-arrow-up-right" aria-hidden="true" />,
  },
  {
    id: 'inventory',
    label: 'Tồn kho & Sổ thẻ',
    icon: <i className="bi bi-journal-text" aria-hidden="true" />,
  },
  {
    id: 'kkts',
    label: 'Báo cáo Kiểm kê',
    icon: <i className="bi bi-clipboard2-data" aria-hidden="true" />,
  },
];

const mainItemIds = mainMenuItems.map((item) => item.id);

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onTabChange,
  userRoles,
  isCollapsed,
  onToggleCollapse,
}) => {
  const isController = userRoles.includes('CONTROLLER') || userRoles.includes('ADMIN');
  const isAdmin = userRoles.includes('ADMIN');
  const [isMainOpen, setIsMainOpen] = useState(true);
  const [isControlOpen, setIsControlOpen] = useState(true);
  const [prevActiveTab, setPrevActiveTab] = useState(activeTab);

  const controlMenuItems = useMemo(() => [
    ...(isController ? [{
      id: 'closings',
      label: 'Chốt sổ kỳ kế toán',
      icon: <i className="bi bi-lock" aria-hidden="true" />,
    }] : []),
    ...(isAdmin ? [{
      id: 'users',
      label: 'Quản lý Tài khoản',
      icon: <i className="bi bi-people" aria-hidden="true" />,
    }, {
      id: 'activity',
      label: 'Lịch sử thao tác',
      icon: <i className="bi bi-clock-history" aria-hidden="true" />,
    }] : []),
  ], [isController, isAdmin]);

  const controlItemIds = useMemo(() => controlMenuItems.map((item) => item.id), [controlMenuItems]);

  if (activeTab !== prevActiveTab) {
    setPrevActiveTab(activeTab);
    if (mainItemIds.includes(activeTab)) {
      setIsMainOpen(true);
    } else if (controlItemIds.includes(activeTab)) {
      setIsControlOpen(true);
    }
  }

  const renderMenuItems = (items: typeof mainMenuItems | typeof controlMenuItems) => (
    <ul className="nav flex-column gap-0 mb-2">
      {items.map((item) => (
        <li key={item.id} className="nav-item">
          <button
            type="button"
            className={`nav-link w-100 ${isCollapsed ? 'justify-content-center' : 'text-start'} border-0 bg-transparent${activeTab === item.id ? ' active' : ''}`}
            onClick={() => onTabChange(item.id)}
            title={isCollapsed ? item.label : undefined}
          >
            <NavIcon>{item.icon}</NavIcon>
            {!isCollapsed && <span>{item.label}</span>}
          </button>
        </li>
      ))}
    </ul>
  );

  return (
    <aside className={`sgb-sidebar flex-shrink-0${isCollapsed ? ' collapsed' : ''}`}>
      {/* Brand logo */}
      <div className={`d-flex align-items-center sgb-sidebar-brand-row ${isCollapsed ? 'justify-content-center' : 'justify-content-between px-4'}`}>
        {!isCollapsed && (
          <img src={saigonbankLogo} alt="Saigonbank" className="sgb-sidebar-logo-img" />
        )}
        <button
          type="button"
          className="sgb-sidebar-collapse-btn"
          onClick={onToggleCollapse}
          title={isCollapsed ? 'Mở rộng sidebar' : 'Thu gọn sidebar'}
          aria-label={isCollapsed ? 'Mở rộng sidebar' : 'Thu gọn sidebar'}
        >
          <i className={`bi ${isCollapsed ? 'bi-layout-sidebar-inset' : 'bi-layout-sidebar'}`} aria-hidden="true" />
        </button>
      </div>

      {/* Navigation */}
      <nav className={`flex-grow-1 overflow-auto py-3 ${isCollapsed ? 'px-2' : 'px-3'}`}>
        {isCollapsed ? (
          <>
            {renderMenuItems(mainMenuItems)}
            {controlMenuItems.length > 0 && (
              <div className="my-2 sgb-sidebar-divider" />
            )}
            {controlMenuItems.length > 0 && renderMenuItems(controlMenuItems)}
          </>
        ) : (
          <>
            {/* Main menu */}
            <button
              type="button"
              className="sgb-section-toggle"
              onClick={() => setIsMainOpen((v) => !v)}
              aria-expanded={isMainOpen}
            >
              <span className="sgb-section-toggle-label">Chức năng chính</span>
              <i className={`bi ${isMainOpen ? 'bi-chevron-up' : 'bi-chevron-down'}`} aria-hidden="true" />
            </button>
            {isMainOpen && renderMenuItems(mainMenuItems)}

            {/* Controller section */}
            {controlMenuItems.length > 0 && (
              <>
                <button
                  type="button"
                  className="sgb-section-toggle"
                  onClick={() => setIsControlOpen((v) => !v)}
                  aria-expanded={isControlOpen}
                >
                  <span className="sgb-section-toggle-label">Kiểm soát</span>
                  <i className={`bi ${isControlOpen ? 'bi-chevron-up' : 'bi-chevron-down'}`} aria-hidden="true" />
                </button>
                {isControlOpen && renderMenuItems(controlMenuItems)}
              </>
            )}
          </>
        )}
      </nav>

      {/* Footer */}
      <div className={`sgb-sidebar-footer-row ${isCollapsed ? 'px-2 py-3 d-flex justify-content-center' : 'px-4 py-3'}`}>
        <div className="d-flex align-items-center gap-2">
          <span className="pulse-dot rounded-circle sgb-sidebar-pulse" />
          {!isCollapsed && (
            <div>
              <div className="fw-semibold sgb-sidebar-version">
                PIMS v2.0
              </div>
              <div className="sgb-sidebar-subtext">
                Phòng CNTT &amp; Quản lý Tài sản
              </div>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};
