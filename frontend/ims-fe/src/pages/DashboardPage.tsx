import React, { useEffect, useState } from 'react';
import { StatCard } from '../components/common/StatCard';
import { dashboardService, importService, exportService } from '../services';
import type { DashboardData, ImportDoc, ExportDoc } from '../types';

interface DashboardPageProps {
  onNavigate: (tab: string) => void;
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
}

const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const ok = status === 'HOAN_THANH';
  return (
    <span className={`badge rounded-pill ${ok ? 'text-bg-success' : 'text-bg-danger'}`}>
      {ok ? 'Hoàn thành' : 'Đã hủy'}
    </span>
  );
};

export const DashboardPage: React.FC<DashboardPageProps> = ({ onNavigate, showToast }) => {
  const [data, setData] = useState<DashboardData | null>(null);
  const [recentImports, setRecentImports] = useState<ImportDoc[]>([]);
  const [recentExports, setRecentExports] = useState<ExportDoc[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [dashRes, impRes, expRes] = await Promise.all([
          dashboardService.getDashboard().catch(() => null),
          importService.getImports({ size: 5 }).catch(() => ({ content: [] })),
          exportService.getExports({ size: 5 }).catch(() => ({ content: [] })),
        ]);
        if (dashRes) setData(dashRes);
        setRecentImports(impRes.content || []);
        setRecentExports(expRes.content || []);
      } catch (err: any) {
        showToast('Không thể tải số liệu dashboard: ' + err.message, 'error');
      }
    };
    fetchData();
  }, [showToast]);

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

  return (
    <div className="animate-fade-in">
      {/* Welcome Banner */}
      <div className="rounded-4 shadow-sm p-4 p-md-5 mb-5 d-flex align-items-center justify-content-between position-relative overflow-hidden sgb-dashboard-banner">
        <div className="position-relative" style={{ zIndex: 1 }}>
          <span className="badge bg-white text-dark mb-2 fw-semibold px-3 py-2 rounded-pill shadow-sm">
            <i className="bi bi-rocket-takeoff-fill me-2" aria-hidden="true" />Bảng Điều Khiển
          </span>
          <h2 className="fw-bolder text-white mb-2 sgb-dashboard-banner-title">
            HỆ THỐNG QUẢN LÝ TÀI SẢN &amp; ẤN PHẨM
          </h2>
          <p className="mb-0 text-white-50 fs-6 sgb-dashboard-banner-desc">
            Ngân hàng TMCP Sài Gòn Công Thương — Theo dõi trực quan tồn kho, biến động luân chuyển và giao dịch trong tháng.
          </p>
        </div>
        
        <div className="d-flex flex-column flex-sm-row gap-3 flex-shrink-0 ms-4 position-relative" style={{ zIndex: 1 }}>
          <button 
            type="button" 
            className="btn btn-light btn-lg rounded-pill shadow-sm fw-bold px-4 d-flex align-items-center gap-2 sgb-btn-banner-light" 
            onClick={() => onNavigate('imports')}
          >
            <i className="bi bi-box-arrow-in-down fs-5" aria-hidden="true" /> Lập Phiếu Nhập
          </button>
          <button
            type="button" 
            className="btn btn-outline-light btn-lg rounded-pill fw-bold px-4 d-flex align-items-center gap-2 sgb-btn-banner-glass"
            onClick={() => onNavigate('exports')}
          >
            <i className="bi bi-box-arrow-up-right fs-5" aria-hidden="true" /> Lập Phiếu Xuất
          </button>
        </div>

        {/* Decorative background circle */}
        <div className="position-absolute rounded-circle sgb-dashboard-banner-glow" />
      </div>

      {/* KPI Cards */}
      <div className="row g-3 mb-4">
        <div className="col-12 col-sm-6 col-xl-3">
          <StatCard title="Tổng mặt hàng hoạt động" value={data ? `${data.totalActiveProducts} mã` : '...'} icon={<i className="bi bi-box-seam" aria-hidden="true" />} subtext="Ấn phẩm, ACTT, ACQT, VPP, VLK" colorTheme="navy" />
        </div>
        <div className="col-12 col-sm-6 col-xl-3">
          <StatCard title="Mặt hàng có tồn kho" value={data ? `${data.productsWithStock} mã` : '...'} icon={<i className="bi bi-bar-chart-line" aria-hidden="true" />} subtext="Số lượng tồn > 0 hiện tại" colorTheme="green" />
        </div>
        <div className="col-12 col-sm-6 col-xl-3">
          <StatCard title="Tổng giá trị tồn kho" value={data ? formatCurrency(data.totalInventoryAmount) : '...'} icon={<i className="bi bi-cash-coin" aria-hidden="true" />} subtext="Theo giá bình quân thực tế" colorTheme="gold" />
        </div>
        <div className="col-12 col-sm-6 col-xl-3">
          <StatCard title={`Giao dịch tháng ${data?.currentMonth || new Date().getMonth() + 1}`} value={data ? `${data.importsThisMonth} Nhập / ${data.exportsThisMonth} Xuất` : '...'} icon={<i className="bi bi-journal-text" aria-hidden="true" />} subtext="Tổng số phiếu đã hoàn thành" colorTheme="blue" />
        </div>
      </div>

      {/* Recent Transactions */}
      <div className="row g-4 mb-4">
        {/* Recent Imports */}
        <div className="col-12 col-lg-6">
          <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden">
            <div className="card-header bg-white border-bottom-0 pt-4 pb-2 px-4 d-flex align-items-center justify-content-between">
              <h5 className="fw-bold mb-0 text-dark">
                <i className="bi bi-box-arrow-in-down fs-5 me-2" aria-hidden="true" />Phiếu nhập gần đây
              </h5>
              <button 
                type="button" 
                className="btn btn-light btn-sm rounded-pill fw-semibold px-3 text-secondary" 
                onClick={() => onNavigate('imports')}
              >
                Xem tất cả <i className="bi bi-arrow-right-short" aria-hidden="true" />
              </button>
            </div>
            <div className="card-body p-0">
              <div className="table-responsive">
                <table className="table table-borderless table-hover mb-0 align-middle">
                  <thead className="table-light">
                    <tr>
                      <th className="px-4 text-secondary small fw-semibold text-uppercase">Số phiếu</th>
                      <th className="text-secondary small fw-semibold text-uppercase">Ngày nhập</th>
                      <th className="text-secondary small fw-semibold text-uppercase">Nơi mua</th>
                      <th className="text-end text-secondary small fw-semibold text-uppercase">Tổng tiền</th>
                      <th className="px-4 text-secondary small fw-semibold text-uppercase">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentImports.length === 0 ? (
                      <tr><td colSpan={5} className="text-center text-muted py-5">Chưa có phiếu nhập nào</td></tr>
                    ) : recentImports.map((item) => (
                      <tr key={item.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                        <td className="px-4 fw-bold text-monospace" style={{ color: 'var(--sgb-navy-800)' }}>{item.soPhieu}</td>
                        <td className="text-muted">{item.ngayNhap}</td>
                        <td className="fw-medium text-dark">{item.noiMua || '—'}</td>
                        <td className="text-end fw-bolder text-success">{formatCurrency(item.tongTien)}</td>
                        <td className="px-4"><StatusBadge status={item.trangThai} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        {/* Recent Exports */}
        <div className="col-12 col-lg-6">
          <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden">
            <div className="card-header bg-white border-bottom-0 pt-4 pb-2 px-4 d-flex align-items-center justify-content-between">
              <h5 className="fw-bold mb-0 text-dark">
                <i className="bi bi-box-arrow-up-right fs-5 me-2" aria-hidden="true" />Phiếu xuất gần đây
              </h5>
              <button 
                type="button" 
                className="btn btn-light btn-sm rounded-pill fw-semibold px-3 text-secondary" 
                onClick={() => onNavigate('exports')}
              >
                Xem tất cả <i className="bi bi-arrow-right-short" aria-hidden="true" />
              </button>
            </div>
            <div className="card-body p-0">
              <div className="table-responsive">
                <table className="table table-borderless table-hover mb-0 align-middle">
                  <thead className="table-light">
                    <tr>
                      <th className="px-4 text-secondary small fw-semibold text-uppercase">Số phiếu</th>
                      <th className="text-secondary small fw-semibold text-uppercase">Ngày xuất</th>
                      <th className="text-secondary small fw-semibold text-uppercase">Nơi nhận</th>
                      <th className="text-end text-secondary small fw-semibold text-uppercase">Tổng tiền</th>
                      <th className="px-4 text-secondary small fw-semibold text-uppercase">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentExports.length === 0 ? (
                      <tr><td colSpan={5} className="text-center text-muted py-5">Chưa có phiếu xuất nào</td></tr>
                    ) : recentExports.map((item) => (
                      <tr key={item.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                        <td className="px-4 fw-bold text-monospace" style={{ color: 'var(--sgb-navy-800)' }}>{item.soPhieu}</td>
                        <td className="text-muted">{item.ngayXuat}</td>
                        <td className="fw-medium text-dark">{item.noiNhan || '—'}</td>
                        <td className="text-end fw-bolder text-danger">{formatCurrency(item.tongTien)}</td>
                        <td className="px-4"><StatusBadge status={item.trangThai} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
