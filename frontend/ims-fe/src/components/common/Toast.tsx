import React, { useEffect } from 'react';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
}

interface ToastProps {
  toasts: ToastMessage[];
  onDismiss: (id: string) => void;
}

const TOAST_CONFIG = {
  success: { bsClass: 'text-bg-success', iconClass: 'bi-check-circle-fill' },
  error:   { bsClass: 'text-bg-danger',  iconClass: 'bi-exclamation-triangle-fill' },
  warning: { bsClass: 'text-bg-warning', iconClass: 'bi-bell-fill' },
  info:    { bsClass: 'text-bg-primary', iconClass: 'bi-info-circle-fill' },
};

export const Toast: React.FC<ToastProps> = ({ toasts, onDismiss }) => {
  useEffect(() => {
    if (toasts.length > 0) {
      const timer = setTimeout(() => onDismiss(toasts[0].id), 4000);
      return () => clearTimeout(timer);
    }
  }, [toasts, onDismiss]);

  if (toasts.length === 0) return null;

  return (
    <div
      className="toast-container position-fixed top-0 end-0 p-3"
      style={{ zIndex: 9999, minWidth: '320px', maxWidth: '420px' }}
    >
      {toasts.map((toast) => {
        const cfg = TOAST_CONFIG[toast.type];
        return (
          <div
            key={toast.id}
            className={`toast show d-flex align-items-center ${cfg.bsClass} border-0 animate-fade-in mb-2`}
            role="alert"
          >
            <div className="toast-body d-flex align-items-center gap-2 fw-medium">
              <i className={`bi ${cfg.iconClass}`} aria-hidden="true" />
              <span>{toast.message}</span>
            </div>
            <button
              type="button"
              className="btn-close btn-close-white me-2 m-auto flex-shrink-0"
              aria-label="Close"
              onClick={() => onDismiss(toast.id)}
            />
          </div>
        );
      })}
    </div>
  );
};
