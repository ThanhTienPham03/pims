import React, { useEffect, useState } from 'react';
import { monthlyClosingService } from '../services';
import type { MonthlyClosing } from '../types';

interface MonthlyClosingPageProps {
  userRoles: string[];
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
}

const MONTH_NAMES = [
  '', 'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4',
  'Tháng 5', 'Tháng 6', 'Tháng 7', 'Tháng 8',
  'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12',
];

export const MonthlyClosingPage: React.FC<MonthlyClosingPageProps> = ({ userRoles, showToast }) => {
  const [closings, setClosings] = useState<MonthlyClosing[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState<number>(new Date().getMonth() + 1);
  const [actionLoading, setActionLoading] = useState(false);

  const isController = userRoles.includes('CONTROLLER') || userRoles.includes('ADMIN');

  const loadClosings = React.useCallback(async () => {
    try {
      setLoading(true);
      const data = await monthlyClosingService.getAll();
      setClosings(data || []);
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast('Lỗi tải dữ liệu chốt sổ: ' + msg, 'error');
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    loadClosings();
  }, [loadClosings]);

  const handleLock = async (year: number, month: number) => {
    if (!window.confirm(
      `XÁC NHẬN KHÓA SỔ\n\nKhóa ${MONTH_NAMES[month]}/${year}?\n\nSau khi khóa, không ai có thể tạo hay hủy phiếu nhập/xuất trong kỳ này.`
    )) return;

    try {
      setActionLoading(true);
      await monthlyClosingService.lockMonth(year, month);
      showToast(`Đã KHÓA SỔ ${MONTH_NAMES[month]}/${year} thành công!`, 'success');
      loadClosings();
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast('Lỗi khóa sổ: ' + msg, 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUnlock = async (year: number, month: number) => {
    if (!window.confirm(
      `XÁC NHẬN MỞ KHÓA\n\nMở khóa sổ ${MONTH_NAMES[month]}/${year}?\n\nChỉ thực hiện khi có lý do đặc biệt được phê duyệt bởi Ban Giám đốc.`
    )) return;

    try {
      setActionLoading(true);
      await monthlyClosingService.unlockMonth(year, month);
      showToast(`Đã MỞ KHÓA sổ ${MONTH_NAMES[month]}/${year} thành công!`, 'success');
      loadClosings();
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast('Lỗi mở khóa: ' + msg, 'error');
    } finally {
      setActionLoading(false);
    }
  };

  // Derived stats
  const totalLocked = closings.filter(c => c.isLocked).length;
  const totalOpen = closings.filter(c => !c.isLocked).length;
  const latestClosing = [...closings].sort((a, b) => {
    if (b.year !== a.year) return b.year - a.year;
    return b.month - a.month;
  })[0];

  return (
    <div className="animate-fade-in">
      <div
        className="rounded-4 shadow p-4 p-md-5 mb-4 position-relative overflow-hidden"
        style={{
          background: 'linear-gradient(135deg, #185089 0%, #2673BE 55%, #1d5f9e 100%)',
          borderLeft: '6px solid #F5A623',
        }}
      >
        <div className="position-absolute rounded-circle" style={{
          width: 320, height: 320, top: -100, right: -60,
          background: 'radial-gradient(circle, rgba(245,166,35,0.12) 0%, rgba(255,255,255,0) 70%)',
          pointerEvents: 'none',
        }} />
        <div className="position-absolute rounded-circle" style={{
          width: 200, height: 200, bottom: -80, right: 200,
          background: 'radial-gradient(circle, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0) 70%)',
          pointerEvents: 'none',
        }} />

        <div className="row align-items-center g-4 position-relative" style={{ zIndex: 1 }}>
          <div className="col-lg-8">
            <span
              className="badge fw-semibold px-3 py-2 rounded-pill mb-3 d-inline-flex align-items-center gap-1"
              style={{ background: 'rgba(245,166,35,0.18)', color: '#F5A623', border: '1px solid rgba(245,166,35,0.4)', fontSize: '12px' }}
            >
              <i className="bi bi-shield-lock" aria-hidden="true" /> Kiểm soát viên (CONTROLLER / ADMIN)
            </span>
            <h1 className="fw-bolder text-white mb-2" style={{ fontSize: '1.6rem', letterSpacing: '-0.5px' }}>
              Chốt Sổ Kỳ Kế Toán Hàng Tháng
            </h1>
            <p className="text-white mb-0" style={{ opacity: 0.65, fontSize: '0.95rem', maxWidth: 560 }}>
              Kiểm soát và khóa kỳ nhập xuất ấn phẩm toàn hệ thống Saigonbank. Sau khi khóa, 
              không thể tạo hoặc hủy phiếu nhập/xuất trong kỳ đó.
            </p>
          </div>
          <div className="col-lg-4 d-flex flex-column gap-2">
            <div className="d-flex gap-3">
              <div
                className="flex-fill rounded-3 px-3 py-2 d-flex align-items-center gap-2"
                style={{ background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.12)' }}
              >
                <span style={{ fontSize: '1.4rem' }}><i className="bi bi-lock-fill" aria-hidden="true" /></span>
                <div>
                  <div className="fw-bolder text-white" style={{ fontSize: '1.3rem', lineHeight: 1.1 }}>{totalLocked}</div>
                  <div style={{ fontSize: '0.72rem', color: 'rgba(255,255,255,0.5)', textTransform: 'uppercase' }}>Kỳ đã khóa</div>
                </div>
              </div>
              <div
                className="flex-fill rounded-3 px-3 py-2 d-flex align-items-center gap-2"
                style={{ background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.12)' }}
              >
                <span style={{ fontSize: '1.4rem' }}><i className="bi bi-unlock-fill" aria-hidden="true" /></span>
                <div>
                  <div className="fw-bolder text-white" style={{ fontSize: '1.3rem', lineHeight: 1.1 }}>{totalOpen}</div>
                  <div style={{ fontSize: '0.72rem', color: 'rgba(255,255,255,0.5)', textTransform: 'uppercase' }}>Kỳ đang mở</div>
                </div>
              </div>
            </div>
            {latestClosing && (
              <div
                className="rounded-3 px-3 py-2 d-flex align-items-center gap-2"
                style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)' }}
              >
                <span style={{ fontSize: '1.1rem' }}><i className="bi bi-calendar3" aria-hidden="true" /></span>
                <div style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.6)' }}>
                  Kỳ gần nhất:{' '}
                  <span className="fw-semibold text-white">
                    {MONTH_NAMES[latestClosing.month]}/{latestClosing.year}
                  </span>
                  {' — '}
                  <span className={latestClosing.isLocked ? 'text-danger' : 'text-success'}>
                    {latestClosing.isLocked ? 'Đã khóa' : 'Đang mở'}
                  </span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {isController && (
        <div
          className="rounded-4 mb-4 p-4 border"
          style={{ background: 'linear-gradient(135deg, #fff9f0 0%, #fffdf8 100%)', borderColor: '#F5A62340 !important' }}
        >
          <div className="row align-items-center g-3">
            <div className="col-auto">
              <div
                className="d-flex align-items-center justify-content-center rounded-3"
                style={{ width: 48, height: 48, background: 'rgba(245,166,35,0.12)', border: '1.5px solid rgba(245,166,35,0.3)' }}
              >
                <span style={{ fontSize: '1.4rem' }}><i className="bi bi-lightning-charge-fill" aria-hidden="true" /></span>
              </div>
            </div>
            <div className="col">
              <div className="fw-bold" style={{ color: '#7c4f00', fontSize: '0.9rem' }}>Thao tác nhanh — Khóa / Mở khóa kỳ kế toán</div>
              <div className="text-muted" style={{ fontSize: '0.78rem', marginTop: 2 }}>
                Chọn tháng và năm cần thao tác, sau đó bấm nút tương ứng. Hệ thống sẽ yêu cầu xác nhận trước khi thực hiện.
              </div>
            </div>
            <div className="col-12 col-md-auto">
              <div className="d-flex align-items-center gap-3 flex-wrap">
                {/* Month selector */}
                <div className="d-flex align-items-center gap-2">
                  <label className="fw-semibold text-secondary" style={{ fontSize: '0.82rem', whiteSpace: 'nowrap' }}>Tháng</label>
                  <select
                    className="form-select form-select-sm shadow-none"
                    style={{ width: 130, borderColor: '#dee2e6', borderRadius: 8 }}
                    value={selectedMonth}
                    onChange={e => setSelectedMonth(Number(e.target.value))}
                  >
                    {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
                      <option key={m} value={m}>{MONTH_NAMES[m]}</option>
                    ))}
                  </select>
                </div>
                {/* Year selector */}
                <div className="d-flex align-items-center gap-2">
                  <label className="fw-semibold text-secondary" style={{ fontSize: '0.82rem', whiteSpace: 'nowrap' }}>Năm</label>
                  <select
                    className="form-select form-select-sm shadow-none"
                    style={{ width: 130, borderColor: '#dee2e6', borderRadius: 8 }}
                    value={selectedYear}
                    onChange={e => setSelectedYear(Number(e.target.value))}
                  >
                    {[2024, 2025, 2026, 2027].map(y => (
                      <option key={y} value={y}>Năm {y}</option>
                    ))}
                  </select>
                </div>

                <div className="vr d-none d-md-block" style={{ height: 36 }} />

                {/* Lock button */}
                <button
                  type="button"
                  className="btn fw-semibold d-flex align-items-center gap-2 shadow-sm"
                  disabled={actionLoading}
                  onClick={() => handleLock(selectedYear, selectedMonth)}
                  style={{
                    background: 'linear-gradient(135deg, #dc3545 0%, #b02a37 100%)',
                    border: 'none', color: '#fff', borderRadius: 10, padding: '8px 18px', fontSize: '0.875rem'
                  }}
                >
                  {actionLoading
                    ? <span className="spinner-border spinner-border-sm" role="status" />
                    : <i className="bi bi-lock-fill" aria-hidden="true" />}
                  Khóa {MONTH_NAMES[selectedMonth]}/{selectedYear}
                </button>
                {/* Unlock button */}
                <button
                  type="button"
                  className="btn fw-semibold d-flex align-items-center gap-2 shadow-sm"
                  disabled={actionLoading}
                  onClick={() => handleUnlock(selectedYear, selectedMonth)}
                  style={{
                    background: 'linear-gradient(135deg, #198754 0%, #146c43 100%)',
                    border: 'none', color: '#fff', borderRadius: 10, padding: '8px 18px', fontSize: '0.875rem'
                  }}
                >
                  {actionLoading
                    ? <span className="spinner-border spinner-border-sm" role="status" />
                    : <i className="bi bi-unlock-fill" aria-hidden="true" />}
                  Mở khóa {MONTH_NAMES[selectedMonth]}/{selectedYear}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="card border-0 shadow-sm rounded-4 overflow-hidden">
        <div className="card-header bg-white border-bottom px-4 py-3 d-flex align-items-center justify-content-between">
          <div>
            <h5 className="fw-bold mb-0" style={{ color: 'var(--sgb-navy-900)', fontSize: '1rem' }}>
              <i className="bi bi-card-checklist me-2" aria-hidden="true" />Lịch sử Chốt Sổ Kế Toán
            </h5>
            <p className="mb-0 text-muted" style={{ fontSize: '0.78rem', marginTop: 2 }}>
              Toàn bộ các kỳ đã được ghi nhận trong hệ thống — tổng {closings.length} kỳ
            </p>
          </div>
          <button
            type="button"
            className="btn btn-light btn-sm rounded-pill fw-semibold px-3"
            style={{ fontSize: '0.82rem' }}
            onClick={loadClosings}
            disabled={loading}
          >
            {loading
              ? <><span className="spinner-border spinner-border-sm me-1" role="status" />Đang tải...</>
              : <><i className="bi bi-arrow-clockwise me-1" aria-hidden="true" />Làm mới</>}
          </button>
        </div>

        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead style={{ background: '#f8fafc' }}>
              <tr>
                <th className="px-4 text-uppercase fw-semibold text-secondary" style={{ fontSize: '0.72rem', letterSpacing: '0.5px', borderBottom: '2px solid #e9ecef', paddingTop: 12, paddingBottom: 12 }}>
                  Kỳ kế toán
                </th>
                <th className="text-uppercase fw-semibold text-secondary" style={{ fontSize: '0.72rem', letterSpacing: '0.5px', borderBottom: '2px solid #e9ecef' }}>
                  Trạng thái
                </th>
                <th className="text-uppercase fw-semibold text-secondary" style={{ fontSize: '0.72rem', letterSpacing: '0.5px', borderBottom: '2px solid #e9ecef' }}>
                  Thời gian khóa
                </th>
                <th className="text-uppercase fw-semibold text-secondary" style={{ fontSize: '0.72rem', letterSpacing: '0.5px', borderBottom: '2px solid #e9ecef' }}>
                  Người khóa
                </th>
                <th className="text-uppercase fw-semibold text-secondary" style={{ fontSize: '0.72rem', letterSpacing: '0.5px', borderBottom: '2px solid #e9ecef' }}>
                  Thời gian mở khóa
                </th>
                <th className="text-uppercase fw-semibold text-secondary" style={{ fontSize: '0.72rem', letterSpacing: '0.5px', borderBottom: '2px solid #e9ecef' }}>
                  Người mở khóa
                </th>
                {isController && (
                  <th className="text-center text-uppercase fw-semibold text-secondary px-4" style={{ fontSize: '0.72rem', letterSpacing: '0.5px', borderBottom: '2px solid #e9ecef', width: 140 }}>
                    Thao tác
                  </th>
                )}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={isController ? 7 : 6} className="text-center py-5">
                    <div className="d-flex flex-column align-items-center gap-2">
                      <div className="spinner-border text-primary" role="status" style={{ width: 36, height: 36 }} />
                      <span className="text-muted" style={{ fontSize: '0.875rem' }}>Đang tải lịch sử chốt sổ...</span>
                    </div>
                  </td>
                </tr>
              ) : closings.length === 0 ? (
                <tr>
                  <td colSpan={isController ? 7 : 6} className="text-center py-5">
                    <div className="d-flex flex-column align-items-center gap-2">
                      <span style={{ fontSize: '2.5rem', opacity: 0.3 }}><i className="bi bi-card-checklist" aria-hidden="true" /></span>
                      <div className="fw-semibold text-muted">Chưa có dữ liệu chốt sổ nào</div>
                      <div className="text-muted" style={{ fontSize: '0.82rem' }}>
                        Thực hiện khóa sổ lần đầu để bắt đầu ghi nhận lịch sử.
                      </div>
                    </div>
                  </td>
                </tr>
              ) : (
                closings.map((c) => (
                  <tr
                    key={c.id}
                    style={{
                      borderLeft: c.isLocked ? '3px solid #dc3545' : '3px solid #198754',
                      transition: 'background 0.15s',
                    }}
                  >
                    <td className="px-4">
                      <div className="d-flex align-items-center gap-3">
                        <div
                          className="d-flex align-items-center justify-content-center rounded-3 flex-shrink-0"
                          style={{
                            width: 40, height: 40,
                            background: c.isLocked ? 'rgba(220,53,69,0.08)' : 'rgba(25,135,84,0.08)',
                            border: `1.5px solid ${c.isLocked ? 'rgba(220,53,69,0.2)' : 'rgba(25,135,84,0.2)'}`,
                          }}
                        >
                          <span style={{ fontSize: '1.1rem' }}><i className={`bi ${c.isLocked ? 'bi-lock-fill' : 'bi-unlock-fill'}`} aria-hidden="true" /></span>
                        </div>
                        <div>
                          <div className="fw-bolder" style={{ color: 'var(--sgb-navy-900)', fontSize: '0.95rem' }}>
                            {MONTH_NAMES[c.month]} / {c.year}
                          </div>
                          <div className="text-muted" style={{ fontSize: '0.75rem' }}>Kỳ kế toán</div>
                        </div>
                      </div>
                    </td>

                    <td>
                      {c.isLocked ? (
                        <span
                          className="badge d-inline-flex align-items-center gap-1 px-3 py-2 rounded-pill fw-semibold"
                          style={{ background: 'rgba(220,53,69,0.1)', color: '#b02a37', border: '1px solid rgba(220,53,69,0.25)', fontSize: '0.8rem' }}
                        >
                          <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#dc3545', display: 'inline-block' }} />
                          ĐÃ KHÓA
                        </span>
                      ) : (
                        <span
                          className="badge d-inline-flex align-items-center gap-1 px-3 py-2 rounded-pill fw-semibold"
                          style={{ background: 'rgba(25,135,84,0.1)', color: '#146c43', border: '1px solid rgba(25,135,84,0.25)', fontSize: '0.8rem' }}
                        >
                          <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#198754', display: 'inline-block', animation: 'pulse 1.5s infinite' }} />
                          ĐANG MỞ
                        </span>
                      )}
                    </td>

                    <td>
                      {c.lockedAt ? (
                        <div>
                          <div className="fw-medium" style={{ fontSize: '0.85rem', color: '#343a40' }}>
                            {new Date(c.lockedAt).toLocaleDateString('vi-VN')}
                          </div>
                          <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                            {new Date(c.lockedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        </div>
                      ) : (
                        <span className="text-muted" style={{ fontSize: '0.82rem' }}>—</span>
                      )}
                    </td>

                    <td>
                      {c.lockedBy ? (
                        <div className="d-flex align-items-center gap-2">
                          <div
                            className="d-flex align-items-center justify-content-center rounded-circle fw-bold flex-shrink-0"
                            style={{ width: 28, height: 28, background: 'rgba(13,27,68,0.1)', color: 'var(--sgb-navy-800)', fontSize: '0.7rem' }}
                          >
                            {c.lockedBy.charAt(0).toUpperCase()}
                          </div>
                          <span className="fw-medium" style={{ fontSize: '0.85rem' }}>{c.lockedBy}</span>
                        </div>
                      ) : (
                        <span className="text-muted" style={{ fontSize: '0.82rem' }}>—</span>
                      )}
                    </td>

                    <td>
                      {c.unlockedAt ? (
                        <div>
                          <div className="fw-medium" style={{ fontSize: '0.85rem', color: '#343a40' }}>
                            {new Date(c.unlockedAt).toLocaleDateString('vi-VN')}
                          </div>
                          <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                            {new Date(c.unlockedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        </div>
                      ) : (
                        <span className="text-muted" style={{ fontSize: '0.82rem' }}>—</span>
                      )}
                    </td>

                    <td>
                      {c.unlockedBy ? (
                        <div className="d-flex align-items-center gap-2">
                          <div
                            className="d-flex align-items-center justify-content-center rounded-circle fw-bold flex-shrink-0"
                            style={{ width: 28, height: 28, background: 'rgba(25,135,84,0.1)', color: '#146c43', fontSize: '0.7rem' }}
                          >
                            {c.unlockedBy.charAt(0).toUpperCase()}
                          </div>
                          <span className="fw-medium" style={{ fontSize: '0.85rem' }}>{c.unlockedBy}</span>
                        </div>
                      ) : (
                        <span className="text-muted" style={{ fontSize: '0.82rem' }}>—</span>
                      )}
                    </td>

                    {isController && (
                      <td className="text-center px-4">
                        {c.isLocked ? (
                          <button
                            type="button"
                            className="btn btn-sm fw-semibold d-inline-flex align-items-center gap-1 shadow-sm"
                            style={{
                              background: 'linear-gradient(135deg, #198754 0%, #146c43 100%)',
                              color: '#fff', border: 'none', borderRadius: 8, fontSize: '0.8rem', padding: '6px 14px'
                            }}
                            onClick={() => handleUnlock(c.year, c.month)}
                            disabled={actionLoading}
                          >
                            <i className="bi bi-unlock-fill" aria-hidden="true" /> Mở khóa
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-sm fw-semibold d-inline-flex align-items-center gap-1 shadow-sm"
                            style={{
                              background: 'linear-gradient(135deg, #dc3545 0%, #b02a37 100%)',
                              color: '#fff', border: 'none', borderRadius: 8, fontSize: '0.8rem', padding: '6px 14px'
                            }}
                            onClick={() => handleLock(c.year, c.month)}
                            disabled={actionLoading}
                          >
                            <i className="bi bi-lock-fill" aria-hidden="true" /> Khóa sổ
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {closings.length > 0 && (
          <div className="card-footer bg-white border-top px-4 py-2 d-flex align-items-center justify-content-between">
            <span className="text-muted" style={{ fontSize: '0.8rem' }}>
              Hiển thị <strong>{closings.length}</strong> kỳ kế toán
            </span>
            <div className="d-flex align-items-center gap-3">
              <span className="d-inline-flex align-items-center gap-1" style={{ fontSize: '0.78rem', color: '#6c757d' }}>
                <span style={{ width: 10, height: 10, borderRadius: 2, background: '#dc3545', display: 'inline-block' }} />
                {totalLocked} kỳ đã khóa
              </span>
              <span className="d-inline-flex align-items-center gap-1" style={{ fontSize: '0.78rem', color: '#6c757d' }}>
                <span style={{ width: 10, height: 10, borderRadius: 2, background: '#198754', display: 'inline-block' }} />
                {totalOpen} kỳ đang mở
              </span>
            </div>
          </div>
        )}
      </div>

      <div
        className="mt-4 rounded-3 p-3 d-flex align-items-start gap-3"
        style={{ background: 'rgba(13,110,253,0.04)', border: '1px solid rgba(13,110,253,0.15)' }}
      >
        <span style={{ fontSize: '1.3rem', marginTop: 2 }}><i className="bi bi-info-circle" aria-hidden="true" /></span>
        <div>
          <div className="fw-semibold" style={{ color: '#084298', fontSize: '0.875rem' }}>
            Lưu ý về quy trình Chốt Sổ
          </div>
          <ul className="mb-0 mt-1" style={{ fontSize: '0.82rem', color: '#6c757d', paddingLeft: 18 }}>
            <li>Chỉ tài khoản <strong>CONTROLLER</strong> hoặc <strong>ADMIN</strong> mới được phép khóa/mở khóa kỳ kế toán.</li>
            <li>Sau khi khóa, toàn bộ phiếu nhập/xuất trong kỳ đó sẽ không thể tạo mới hoặc hủy bỏ.</li>
            <li>Việc mở khóa chỉ được thực hiện khi có lý do đặc biệt và cần phê duyệt từ Ban Giám đốc.</li>
            <li>Mọi thao tác khóa/mở khóa đều được ghi nhận đầy đủ: người thực hiện, thời gian, kỳ kế toán.</li>
          </ul>
        </div>
      </div>
    </div>
  );
};
