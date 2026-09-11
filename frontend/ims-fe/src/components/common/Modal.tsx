import React, { useEffect } from 'react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  maxWidth?: string;
}

export const Modal: React.FC<ModalProps> = ({
  isOpen, onClose, title, children, maxWidth = '600px',
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    if (isOpen) {
      document.body.classList.add('overflow-hidden');
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.body.classList.remove('overflow-hidden');
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="modal-backdrop show"
        style={{ backdropFilter: 'blur(3px)', backgroundColor: 'rgba(0,34,68,0.6)' }}
        onClick={onClose}
      />

      {/* Modal wrapper — overflow-y: auto để scroll được khi nội dung dài */}
      <div
        className="modal d-block animate-fade-in"
        tabIndex={-1}
        style={{ zIndex: 1055, overflowY: 'auto' }}
        onClick={onClose}
      >
        <div
          className="modal-dialog modal-dialog-scrollable"
          style={{ maxWidth, margin: '1.5rem auto', maxHeight: 'calc(100vh - 3rem)' }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="modal-content border-0 shadow-lg" style={{ maxHeight: 'calc(100vh - 3rem)', display: 'flex', flexDirection: 'column' }}>
            {/* Header */}
            <div
              className="modal-header border-0 d-flex align-items-center justify-content-between flex-shrink-0"
              style={{ backgroundColor: 'var(--sgb-navy-800)', padding: '16px 24px' }}
            >
              <h5 className="modal-title fw-bold text-white mb-0" style={{ fontSize: '16px' }}>
                {title}
              </h5>
              <button
                type="button"
                className="btn-close btn-close-white"
                aria-label="Đóng"
                onClick={onClose}
              />
            </div>

            {/* Body — cuộn nội dung bên trong */}
            <div className="modal-body p-4" style={{ overflowY: 'auto', flex: '1 1 auto' }}>
              {children}
            </div>
          </div>
        </div>
      </div>
    </>
  );
};
