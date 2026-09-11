import React from 'react';

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  subtext?: string;
  colorTheme?: 'navy' | 'gold' | 'green' | 'blue';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  icon,
  subtext,
  colorTheme = 'navy',
}) => {
  return (
    <div className={`card h-100 border-0 rounded-4 p-4 d-flex flex-row align-items-center justify-content-between sgb-stat-card theme-${colorTheme}`}>
      <div className="d-flex flex-column position-relative" style={{ zIndex: 1 }}>
        <div className="fw-semibold text-secondary mb-2 text-uppercase sgb-stat-card-title">
          {title}
        </div>
        <div className="fw-bolder mb-1 sgb-stat-card-value">
          {value}
        </div>
        {subtext && (
          <div className="fw-medium text-muted mt-1 sgb-stat-card-subtext">
            {subtext}
          </div>
        )}
      </div>

      <div className={`d-flex align-items-center justify-content-center flex-shrink-0 shadow-sm sgb-stat-card-icon theme-${colorTheme}`}>
        {icon}
      </div>

      {/* Subtle background glow matching theme */}
      <div className={`sgb-stat-card-glow theme-${colorTheme}`} />
    </div>
  );
};
