import React, { useEffect, useState } from 'react';
import { exportService, authService } from '../services';
import type { ExportDoc } from '../types';
import { Modal } from '../components/common/Modal';
import { PrintVoucherModal } from '../components/print/PrintVoucherModal';

interface ExportsPageProps {
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
  onNavigateCreate: () => void;
}

export const ExportsPage: React.FC<ExportsPageProps> = ({ showToast, onNavigateCreate }) => {
  const currentUser = authService.getCurrentUser();
  const userRoles = currentUser?.roles || [];
  const isAdmin = userRoles.includes('ADMIN') || userRoles.includes('ROLE_ADMIN');

  const [exports, setExports] = useState<ExportDoc[]>([]);
  const [loading, setLoading] = useState(false);

  const [searchSoPhieu, setSearchSoPhieu] = useState('');
  const [searchNoiNhan, setSearchNoiNhan] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [selectedExport, setSelectedExport] = useState<ExportDoc | null>(null);
  const [printTarget, setPrintTarget] = useState<ExportDoc | null>(null);

  const loadExports = async () => {
    try {
      setLoading(true);
      const res = await exportService.getExports({
        soPhieu: searchSoPhieu,
        noiNhan: searchNoiNhan,
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        trangThai: statusFilter,
        page: currentPage,
        size: 15,
      });
      setExports(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      showToast('Lỗi tải danh sách phiếu xuất: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadExports();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage, statusFilter]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    loadExports();
  };

  const handleCancelExport = async (id: number, soPhieu: string) => {
    if (!isAdmin) {
      showToast('Chỉ Quản trị viên (Admin) mới có quyền hủy phiếu xuất!', 'error');
      return;
    }

    if (!window.confirm(`Bạn có chắc muốn HỦY phiếu xuất [${soPhieu}]? Hành động này sẽ hoàn trả tồn kho.`)) {
      return;
    }

    try {
      await exportService.cancelExport(id);
      showToast(`Đã hủy phiếu xuất ${soPhieu} thành công!`, 'success');
      loadExports();
    } catch (err: any) {
      showToast('Lỗi hủy phiếu: ' + err.message, 'error');
    }
  };


  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
  };

  return (
    <div className="animate-fade-in">

      {/* Page Hero */}
      <div className="page-hero mb-4">
        <div className="page-hero-badge"><i className="bi bi-box-arrow-up-right" aria-hidden="true" /> Quản lý xuất kho</div>
        <h2 className="mb-1">Phiếu Xuất Kho &amp; Cấp Phát Ấn Phẩm</h2>
        <p>Xuất cấp ấn chỉ, hóa đơn, VPP cho các Chi nhánh &amp; Phòng Giao dịch — tổng <strong className="text-white">{totalElements}</strong> phiếu</p>
      </div>

      {/* Filter Bar */}
      <div className="card mb-4">
        <div className="card-body">
          <form onSubmit={handleSearch} className="d-flex gap-3 flex-wrap align-items-end">
            <div style={{ minWidth: 140, flex: '1 1 140px' }}>
              <label className="form-label">Số phiếu xuất</label>
              <input type="text" className="form-control" placeholder="VD: 001/ACQT-2026" value={searchSoPhieu} onChange={e => setSearchSoPhieu(e.target.value)} />
            </div>
            <div style={{ minWidth: 200, flex: '2 1 200px' }}>
              <label className="form-label">Nơi nhận / Chi nhánh</label>
              <input type="text" className="form-control" placeholder="Tìm theo nơi nhận..." value={searchNoiNhan} onChange={e => setSearchNoiNhan(e.target.value)} />
            </div>
            <div style={{ minWidth: 130, flex: '1 1 130px' }}>
              <label className="form-label">Từ ngày</label>
              <input type="date" className="form-control" value={fromDate} onChange={e => setFromDate(e.target.value)} />
            </div>
            <div style={{ minWidth: 130, flex: '1 1 130px' }}>
              <label className="form-label">Đến ngày</label>
              <input type="date" className="form-control" value={toDate} onChange={e => setToDate(e.target.value)} />
            </div>
            <div style={{ minWidth: 140, flex: '1 1 140px' }}>
              <label className="form-label">Trạng thái</label>
              <select className="form-select" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                <option value="">Tất cả</option>
                <option value="HOAN_THANH">Hoàn thành</option>
                <option value="HUY">Đã hủy</option>
              </select>
            </div>
            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary fw-medium d-flex align-items-center gap-2">
                <i className="bi bi-search" aria-hidden="true" /> Lọc
              </button>
              <button type="button" className="btn btn-light" onClick={() => { setSearchSoPhieu(''); setSearchNoiNhan(''); setFromDate(''); setToDate(''); setStatusFilter(''); }}>
                <i className="bi bi-arrow-counterclockwise me-1" aria-hidden="true" />Xóa
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Export Table */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="card-header bg-white border-bottom px-4 py-3 d-flex align-items-center justify-content-between flex-wrap gap-2">
          <div className="d-flex align-items-center gap-2">
            <h5 className="fw-bold mb-0" style={{ color: 'var(--sgb-navy-900)', fontSize: '1.05rem' }}>
              <i className="bi bi-box-arrow-up-right me-2 text-primary" aria-hidden="true" />Danh Sách Phiếu Xuất Kho
            </h5>
            <span className="badge bg-light text-secondary border fw-medium px-2 py-1" style={{ fontSize: '0.78rem' }}>
              {totalElements} phiếu
            </span>
          </div>
          <button
            type="button"
            className="btn btn-gold fw-semibold d-flex align-items-center gap-2 rounded-pill px-4"
            onClick={() => onNavigateCreate()}
          >
            <i className="bi bi-plus-circle" aria-hidden="true" /> Lập Phiếu Xuất Mới
          </button>
        </div>
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>Số phiếu</th>
                <th>Ngày xuất</th>
                <th>Nơi nhận / Đơn vị yêu cầu</th>
                <th>Lý do xuất</th>
                <th>Nhóm ĐD</th>
                <th className="text-end">Tổng tiền xuất</th>
                <th>Trạng thái</th>
                <th>Người lập</th>
                <th className="text-center" style={{ width: '130px' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={9} className="text-center p-4 text-muted">
                    Đang tải danh sách phiếu xuất...
                  </td>
                </tr>
              ) : exports.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center p-4 text-muted">
                    Không tìm thấy phiếu xuất nào
                  </td>
                </tr>
              ) : (
                exports.map((item) => (
                  <tr key={item.id}>
                    <td className="fw-bold text-monospace" style={{ color: 'var(--sgb-navy-800)' }}>
                      {item.soPhieu}
                    </td>
                    <td>{item.ngayXuat}</td>
                    <td className="fw-semibold">{item.noiNhan || '—'}</td>
                    <td>{item.lyDoXuat || '—'}</td>
                    <td>
                      <span className="badge text-bg-secondary">{item.nhomDaiDien || '—'}</span>
                    </td>
                    <td className="text-end fw-bold" style={{ color: 'var(--sgb-navy-900)' }}>
                      {formatCurrency(item.tongTien)}
                    </td>
                    <td>
                      <span className={`badge rounded-pill ${item.trangThai === 'HOAN_THANH' ? 'text-bg-success' : 'text-bg-danger'}`}>
                        {item.trangThai === 'HOAN_THANH' ? 'Hoàn thành' : 'Đã hủy'}
                      </span>
                    </td>
                    <td className="small text-secondary">{item.creatorFullName || item.createdBy || 'system'}</td>
                    <td className="text-center">
                      <div className="d-flex gap-2 justify-content-center">
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          onClick={() => setSelectedExport(item)}
                        >
                          Chi tiết
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-primary btn-sm"
                          title="Xuất PDF / In phiếu"
                          onClick={() => setPrintTarget(item)}
                        >
                          <i className="bi bi-file-earmark-pdf" aria-hidden="true" />
                        </button>
                        {isAdmin && item.trangThai === 'HOAN_THANH' && (
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            title="Hủy phiếu này"
                            onClick={() => handleCancelExport(item.id, item.soPhieu)}
                          >
                            Hủy
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="d-flex justify-content-end gap-2 mb-4">
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            disabled={currentPage === 0}
            onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
          >
            <i className="bi bi-arrow-left me-1" aria-hidden="true" />Trang trước
          </button>
          <span className="d-flex align-items-center px-2 small fw-semibold">
            Trang {currentPage + 1} / {totalPages}
          </span>
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            disabled={currentPage >= totalPages - 1}
            onClick={() => setCurrentPage((p) => p + 1)}
          >
            Trang sau <i className="bi bi-arrow-right ms-1" aria-hidden="true" />
          </button>
        </div>
      )}

      {/* Modal: Xem Chi Tiết Phiếu Xuất */}
      <Modal
        isOpen={!!selectedExport}
        onClose={() => setSelectedExport(null)}
        title={`Chi Tiết Phiếu Xuất: ${selectedExport?.soPhieu || ''}`}
        maxWidth="1140px"
      >
        {selectedExport && (
          <div>
            <div className="row g-3 mb-4 bg-light p-3 rounded border">
              <div className="col-md-4"><strong>Ngày xuất:</strong> {selectedExport.ngayXuat}</div>
              <div className="col-md-4"><strong>Nơi nhận:</strong> {selectedExport.noiNhan || '—'}</div>
              <div className="col-md-4"><strong>Lý do:</strong> {selectedExport.lyDoXuat || '—'}</div>
              <div className="col-md-4"><strong>TK Nợ / Có:</strong> {selectedExport.tkNo || '—'} / {selectedExport.tkCo || '—'}</div>
              <div className="col-md-4"><strong>Trạng thái:</strong> {selectedExport.trangThai}</div>
              <div className="col-md-4"><strong>Người lập:</strong> {selectedExport.creatorFullName || selectedExport.createdBy || 'system'}</div>
            </div>

            <h4 className="fs-6 fw-bold mb-2">Danh Sách Hàng Xuất:</h4>
            <div className="table-responsive border rounded mb-4">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Mã số</th>
                    <th>Tên hàng hóa</th>
                    <th>ĐVT</th>
                    <th className="text-end">Số lượng xuất</th>
                    <th className="text-end">Đơn giá bình quân</th>
                    <th className="text-end">Thành tiền (VND)</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedExport.details?.map((d, i) => (
                    <tr key={i}>
                      <td className="fw-bold">{d.maSo}</td>
                      <td>{d.tenHang}</td>
                      <td>{d.donViTinh}</td>
                      <td className="text-end fw-semibold">{d.quantity}</td>
                      <td className="text-end">{formatCurrency(d.unitPrice)}</td>
                      <td className="text-end fw-bold">{formatCurrency(d.amount || d.quantity * d.unitPrice)}</td>
                    </tr>
                  ))}
                  <tr className="table-light">
                    <td colSpan={5} className="text-end fw-bolder fs-6">TỔNG CỘNG TIỀN XUẤT:</td>
                    <td className="text-end fw-bolder fs-5" style={{ color: 'var(--sgb-navy-900)' }}>
                      {formatCurrency(selectedExport.tongTien)}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div className="d-flex justify-content-end">
              <button
                type="button"
                className="btn btn-primary px-4"
                onClick={() => setSelectedExport(null)}
              >
                Đóng
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal: In Phiếu Xuất */}
      <PrintVoucherModal
        isOpen={!!printTarget}
        onClose={() => setPrintTarget(null)}
        voucher={printTarget}
        type="EXPORT"
      />
    </div>
  );
};

