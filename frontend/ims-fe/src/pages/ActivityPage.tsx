import React, { useEffect, useState, useCallback, useRef } from 'react';
import { auditLogService, activitySocket } from '../services';
import type { AuditLogItem } from '../types';

interface ActivityPageProps {
  userRoles: string[];
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
}

const ACTION_CONFIG: Record<string, { label: string; icon: string; badgeClass: string }> = {
  CREATE:       { label: 'Tạo mới',     icon: 'bi-plus-circle-fill',    badgeClass: 'badge-action-create'    },
  CANCEL:       { label: 'Hủy bỏ',      icon: 'bi-x-circle-fill',        badgeClass: 'badge-action-cancel'    },
  UPDATE:       { label: 'Cập nhật',    icon: 'bi-pencil-fill',          badgeClass: 'badge-action-update'    },
  DELETE:       { label: 'Xóa',         icon: 'bi-trash-fill',           badgeClass: 'badge-action-delete'    },
  DEACTIVATE:   { label: 'Ngưng SD',    icon: 'bi-slash-circle-fill',    badgeClass: 'badge-action-deactivate'},
  LOGIN:        { label: 'Đăng nhập',   icon: 'bi-box-arrow-in-right',   badgeClass: 'badge-action-login'     },
  LOCK_MONTH:   { label: 'Khóa sổ',    icon: 'bi-lock-fill',            badgeClass: 'badge-action-lock'      },
  UNLOCK_MONTH: { label: 'Mở khóa',    icon: 'bi-unlock-fill',          badgeClass: 'badge-action-unlock'    },
};

const ENTITY_LABEL: Record<string, string> = {
  IMPORT:          'Phiếu nhập',
  EXPORT:          'Phiếu xuất',
  PRODUCT:         'Mã hàng',
  USER:            'Tài khoản',
  MONTHLY_CLOSING: 'Kỳ kế toán',
};

const ACTION_OPTIONS = [
  { value: '', label: 'Tất cả hành động' },
  { value: 'CREATE',       label: 'Tạo mới' },
  { value: 'CANCEL',       label: 'Hủy bỏ' },
  { value: 'UPDATE',       label: 'Cập nhật' },
  { value: 'DELETE',       label: 'Xóa' },
  { value: 'DEACTIVATE',   label: 'Ngưng sử dụng' },
  { value: 'LOGIN',        label: 'Đăng nhập' },
  { value: 'LOCK_MONTH',   label: 'Khóa sổ' },
  { value: 'UNLOCK_MONTH', label: 'Mở khóa sổ' },
];

const ENTITY_OPTIONS = [
  { value: '', label: 'Tất cả đối tượng' },
  { value: 'IMPORT',          label: 'Phiếu nhập' },
  { value: 'EXPORT',          label: 'Phiếu xuất' },
  { value: 'PRODUCT',         label: 'Mã hàng' },
  { value: 'USER',            label: 'Tài khoản' },
  { value: 'MONTHLY_CLOSING', label: 'Kỳ kế toán' },
];

const formatDateTime = (iso: string) => {
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    });
  } catch {
    return iso;
  }
};

const AccessDenied = () => (
  <div className="animate-fade-in text-center py-5">
    <div className="card shadow-sm border-0 mx-auto p-5" style={{ maxWidth: 480 }}>
      <div className="fs-1 mb-3 text-danger"><i className="bi bi-shield-lock" /></div>
      <h4 className="fw-bold mb-2">Quyền truy cập bị hạn chế</h4>
      <p className="text-muted mb-0">
        Chức năng Lịch sử thao tác chỉ dành cho <strong>Quản trị viên (ADMIN)</strong>.
      </p>
    </div>
  </div>
);

export const ActivityPage: React.FC<ActivityPageProps> = ({ userRoles, showToast }) => {
  const isAdmin = userRoles.includes('ADMIN');

  const [logs, setLogs] = useState<AuditLogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [recentLogIds, setRecentLogIds] = useState<number[]>([]);

  const [filterUsername, setFilterUsername] = useState('');
  const [filterAction, setFilterAction] = useState('');
  const [filterEntity, setFilterEntity] = useState('');
  const [filterFrom, setFilterFrom] = useState('');
  const [filterTo, setFilterTo] = useState('');

  const filterRef = useRef({ filterUsername, filterAction, filterEntity, filterFrom, filterTo, currentPage });
  useEffect(() => {
    filterRef.current = { filterUsername, filterAction, filterEntity, filterFrom, filterTo, currentPage };
  });

  const loadLogs = useCallback(async (page = 0) => {
    try {
      setLoading(true);
      const res = await auditLogService.getLogs({
        username: filterUsername || undefined,
        action: filterAction || undefined,
        entityName: filterEntity || undefined,
        fromDate: filterFrom || undefined,
        toDate: filterTo || undefined,
        page,
        size: 20,
      });
      setLogs(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
      setCurrentPage(page);
    } catch (err: any) {
      showToast('Lỗi tải lịch sử thao tác: ' + (err?.message || 'Lỗi không xác định'), 'error');
    } finally {
      setLoading(false);
    }
  }, [filterUsername, filterAction, filterEntity, filterFrom, filterTo, showToast]);

  useEffect(() => {
    if (isAdmin) loadLogs(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin]);

  useEffect(() => {
    if (!isAdmin) return;

    activitySocket.connect();

    const unsubMsg = activitySocket.subscribe((newLog) => {
      const { filterUsername: fUser, filterAction: fAct, filterEntity: fEnt, currentPage: cPage } = filterRef.current;

      if (cPage === 0) {
        if (fUser && !newLog.username.toLowerCase().includes(fUser.toLowerCase())) return;
        if (fAct && newLog.action !== fAct) return;
        if (fEnt && newLog.entityName !== fEnt) return;

        setLogs((prev) => {
          if (prev.some((item) => item.id === newLog.id)) return prev;
          return [newLog, ...prev.slice(0, 19)];
        });
        setTotalElements((prev) => prev + 1);

        if (newLog.id) {
          setRecentLogIds((prev) => [...prev, newLog.id]);
          setTimeout(() => {
            setRecentLogIds((prev) => prev.filter((id) => id !== newLog.id));
          }, 3000);
        }
      }
    });

    return () => {
      unsubMsg();
    };
  }, [isAdmin]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadLogs(0);
  };

  const handleReset = () => {
    setFilterUsername('');
    setFilterAction('');
    setFilterEntity('');
    setFilterFrom('');
    setFilterTo('');
    setTimeout(() => loadLogs(0), 0);
  };

  if (!isAdmin) return <AccessDenied />;

  return (
    <div className="animate-fade-in">
      <div className="page-hero mb-4">
        <div className="row align-items-center g-3">
          <div className="col">
            <div className="page-hero-badge">
              <i className="bi bi-clock-history" aria-hidden="true" /> Nhật ký hệ thống
            </div>
            <h1 className="mb-1" style={{ fontSize: '1.6rem' }}>Lịch Sử Thao Tác</h1>
            <p className="mb-0">
              Theo dõi toàn bộ hoạt động của nhân viên trên hệ thống theo thời gian thực —&nbsp;
              <strong className="text-white">{totalElements}</strong> bản ghi
            </p>
          </div>
        </div>
      </div>

      <div className="card shadow-sm mb-4">
        <div className="card-body py-3">
          <form onSubmit={handleSearch}>
            <div className="row g-2 align-items-end">
              <div className="col-12 col-sm-6 col-md-3">
                <label className="form-label">Người dùng</label>
                <input
                  id="filter-username"
                  type="text"
                  className="form-control form-control-sm"
                  placeholder="Nhập username..."
                  value={filterUsername}
                  onChange={e => setFilterUsername(e.target.value)}
                />
              </div>
              <div className="col-12 col-sm-6 col-md-2">
                <label className="form-label">Hành động</label>
                <select
                  id="filter-action"
                  className="form-select form-select-sm"
                  value={filterAction}
                  onChange={e => setFilterAction(e.target.value)}
                >
                  {ACTION_OPTIONS.map(o => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </div>
              <div className="col-12 col-sm-6 col-md-2">
                <label className="form-label">Đối tượng</label>
                <select
                  id="filter-entity"
                  className="form-select form-select-sm"
                  value={filterEntity}
                  onChange={e => setFilterEntity(e.target.value)}
                >
                  {ENTITY_OPTIONS.map(o => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </div>
              <div className="col-12 col-sm-6 col-md-2">
                <label className="form-label">Từ ngày</label>
                <input
                  id="filter-from"
                  type="date"
                  className="form-control form-control-sm"
                  value={filterFrom}
                  onChange={e => setFilterFrom(e.target.value)}
                />
              </div>
              <div className="col-12 col-sm-6 col-md-2">
                <label className="form-label">Đến ngày</label>
                <input
                  id="filter-to"
                  type="date"
                  className="form-control form-control-sm"
                  value={filterTo}
                  onChange={e => setFilterTo(e.target.value)}
                />
              </div>
              <div className="col-12 col-md-1 d-flex gap-2">
                <button id="btn-search-log" type="submit" className="btn btn-primary btn-sm flex-fill">
                  <i className="bi bi-search" aria-hidden="true" />
                </button>
                <button id="btn-reset-log" type="button" className="btn btn-outline-secondary btn-sm flex-fill" onClick={handleReset}>
                  <i className="bi bi-x-lg" aria-hidden="true" />
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>

      <div className="card shadow-sm">
        <div className="card-body p-0">
          {loading ? (
            <div className="text-center py-5 text-muted">
              <div className="spinner-border spinner-border-sm me-2" role="status" />
              Đang tải lịch sử...
            </div>
          ) : logs.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-inbox fs-1 d-block mb-2 opacity-25" />
              Không có dữ liệu lịch sử
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle">
                <thead>
                  <tr>
                    <th style={{ width: 160 }}>Thời gian</th>
                    <th style={{ width: 120 }}>Người dùng</th>
                    <th style={{ width: 120 }}>Hành động</th>
                    <th style={{ width: 120 }}>Đối tượng</th>
                    <th style={{ width: 130 }}>Mã / Số phiếu</th>
                    <th>Mô tả chi tiết</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map(log => {
                    const cfg = ACTION_CONFIG[log.action] ?? {
                      label: log.action, icon: 'bi-circle', badgeClass: 'badge-action-default',
                    };
                    const isRecent = recentLogIds.includes(log.id);
                    return (
                      <tr key={log.id} className={isRecent ? 'row-highlight-new' : undefined}>
                        <td>
                          <span className="text-secondary text-nowrap" style={{ fontSize: '0.8rem' }}>
                            {log.createdAt ? formatDateTime(log.createdAt) : '—'}
                          </span>
                        </td>
                        <td>
                          <span className="fw-semibold" style={{ fontSize: '0.875rem' }}>
                            <i className="bi bi-person-circle me-1 text-muted" />
                            {log.username}
                          </span>
                        </td>
                        <td>
                          <span className={`badge d-inline-flex align-items-center gap-1 py-1 px-2 ${cfg.badgeClass}`}>
                            <i className={`bi ${cfg.icon}`} />
                            {cfg.label}
                          </span>
                        </td>
                        <td>
                          <span className="badge badge-group">
                            {ENTITY_LABEL[log.entityName ?? ''] ?? log.entityName ?? '—'}
                          </span>
                        </td>
                        <td>
                          <code style={{ fontSize: '0.8rem' }}>{log.entityId ?? '—'}</code>
                        </td>
                        <td className="text-secondary" style={{ fontSize: '0.875rem' }}>
                          {log.description ?? '—'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {totalPages > 1 && (
          <div className="card-footer d-flex justify-content-between align-items-center py-2">
            <span className="text-muted" style={{ fontSize: '0.8rem' }}>
              Trang {currentPage + 1} / {totalPages} — {totalElements} bản ghi
            </span>
            <div className="d-flex gap-1">
              <button
                id="btn-page-prev"
                className="btn btn-sm btn-outline-secondary"
                disabled={currentPage === 0}
                onClick={() => loadLogs(currentPage - 1)}
              >
                <i className="bi bi-chevron-left" />
              </button>
              {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                const pageIndex = Math.max(0, Math.min(currentPage - 2, totalPages - 5)) + i;
                return (
                  <button
                    key={pageIndex}
                    className={`btn btn-sm ${pageIndex === currentPage ? 'btn-primary' : 'btn-outline-secondary'}`}
                    onClick={() => loadLogs(pageIndex)}
                  >
                    {pageIndex + 1}
                  </button>
                );
              })}
              <button
                id="btn-page-next"
                className="btn btn-sm btn-outline-secondary"
                disabled={currentPage >= totalPages - 1}
                onClick={() => loadLogs(currentPage + 1)}
              >
                <i className="bi bi-chevron-right" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
