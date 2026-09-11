import React from 'react';
import type { JwtResponse } from '../../types';

interface HeaderProps {
  user: JwtResponse | null;
  onLogout: () => void;
}

export const Header: React.FC<HeaderProps> = ({ user, onLogout }) => {
  const now = new Date();
  const currentDate = now.toLocaleDateString('vi-VN', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
  });
  const currentTime = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

  const firstName = user?.fullName?.split(' ').pop() || user?.username || 'Bạn';
  const initials = user?.fullName
    ? user.fullName.split(' ').map(w => w[0]).slice(-2).join('').toUpperCase()
    : (user?.username?.slice(0, 2).toUpperCase() ?? 'U');

  return (
    <header className="sgb-header d-flex align-items-center justify-content-between gap-3">
      {/* Left: greeting + date */}
      <div className="d-flex align-items-center gap-3 min-width-0">
        <div className="d-flex flex-column" style={{ minWidth: 0 }}>
          <div className="fw-bold text-truncate sgb-header-greeting">
            Xin chào, <span className="sgb-header-greeting-name">{firstName}</span>
          </div>
          <div className="d-flex align-items-center gap-2 mt-1">
            <span className="sgb-header-date">{currentDate}</span>
            <span className="fw-semibold px-2 py-0 rounded sgb-header-time-badge">
              {currentTime}
            </span>
          </div>
        </div>
      </div>

      {/* Right: user info + logout */}
      <div className="d-flex align-items-center gap-3 flex-shrink-0">
        {/* Role badges */}
        <div className="d-flex align-items-center gap-2">
          {user?.roles?.map((role) => (
            <span key={role} className={`sgb-role-badge ${role}`}>
              {role}
            </span>
          ))}
        </div>

        {/* User name + avatar */}
        <div className="d-flex align-items-center gap-2">
          <span className="fw-semibold text-truncate d-none d-sm-block sgb-header-name">
            {user?.fullName || user?.username}
          </span>
          {/* Avatar */}
          <div className="rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0 sgb-header-avatar">
            {initials}
          </div>
        </div>

        {/* Divider */}
        <div className="sgb-header-divider" />

        {/* Logout */}
        <button
          type="button"
          className="btn btn-sm d-flex align-items-center gap-2 fw-medium sgb-header-logout-btn"
          onClick={onLogout}
          title="Đăng xuất"
        >
          <i className="bi bi-box-arrow-right" aria-hidden="true" />
          <span className="d-none d-md-inline">Đăng xuất</span>
        </button>
      </div>
    </header>
  );
};
