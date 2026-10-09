import React, { useEffect, useState } from 'react';
import { importService, authService } from '../services';
import type { ImportDoc } from '../types';
import { Modal } from '../components/common/Modal';
import { PrintVoucherModal } from '../components/print/PrintVoucherModal';

interface ImportsPageProps {
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
  onNavigateCreate: () => void;
}

export const ImportsPage: React.FC<ImportsPageProps> = ({ showToast, onNavigateCreate }) => {
  const currentUser = authService.getCurrentUser();
  const userRoles = currentUser?.roles || [];
  const isAdmin = userRoles.includes('ADMIN') || userRoles.includes('ROLE_ADMIN');

  const [imports, setImports] = useState<ImportDoc[]>([]);
  const [loading, setLoading] = useState(false);

  const [searchSoPhieu, setSearchSoPhieu] = useState('');
  const [searchNoiMua, setSearchNoiMua] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [selectedImport, setSelectedImport] = useState<ImportDoc | null>(null);
  const [printTarget, setPrintTarget] = useState<ImportDoc | null>(null);

  const loadImports = async () => {
    try {
      setLoading(true);
      const res = await importService.getImports({
        soPhieu: searchSoPhieu,
        noiMua: searchNoiMua,
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        trangThai: statusFilter,
        page: currentPage,
        size: 15,
      });
      setImports(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      showToast('Lỗi tải danh sách phiếu nhập: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadImports();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage, statusFilter]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    loadImports();
  };

  const handleCancelImport = async (id: number, soPhieu: string) => {
    if (!isAdmin) {
      showToast('Chỉ Quản trị viên (Admin) mới có quyền hủy phiếu nhập!', 'error');
      return;
    }

    if (!window.confirm(`Bạn có chắc muốn HỦY phiếu nhập [${soPhieu}]? Hành động này sẽ hoàn tác tồn kho tương ứng.`)) {
      return;
    }

    try {
      await importService.cancelImport(id);
      showToast(`Đã hủy phiếu nhập ${soPhieu} thành công!`, 'success');
      loadImports();
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
        <div className="row align-items-center g-3">
          <div className="col">
            <div className="page-hero-badge"><i className="bi bi-box-arrow-in-down" aria-hidden="true" /> Quản lý nhập kho</div>
            <h2 className="mb-1">Phiếu Nhập Kho Ấn Phẩm &amp; VPP</h2>
            <p>Theo dõi và lập phiếu nhập ấn chỉ, hóa đơn, ấn phẩm &amp; văn phòng phẩm — tổng <strong className="text-white">{totalElements}</strong> phiếu</p>
          </div>
          <div className="col-auto">
            <button
              type="button"
              className="btn btn-gold fw-semibold d-flex align-items-center gap-2 rounded-pill px-4"
              style={{ boxShadow: '0 4px 14px rgba(34,197,94,0.4)' }}
              onClick={() => onNavigateCreate()}
            >
              <i className="bi bi-plus-circle" aria-hidden="true" /> Lập Phiếu Nhập Mới
            </button>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="card mb-4">
        <div className="card-body">
          <form onSubmit={handleSearch} className="d-flex gap-3 flex-wrap align-items-end">
            <div style={{ minWidth: 140, flex: '1 1 140px' }}>
              <label className="form-label">Số phiếu nhập</label>
              <input type="text" className="form-control" placeholder="VD: 001/ACQT-2026" value={searchSoPhieu} onChange={e => setSearchSoPhieu(e.target.value)} />
            </div>
            <div style={{ minWidth: 200, flex: '2 1 200px' }}>
              <label className="form-label">Nơi mua / Nhà cung cấp</label>
              <input type="text" className="form-control" placeholder="Tìm theo nhà cung cấp..." value={searchNoiMua} onChange={e => setSearchNoiMua(e.target.value)} />
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
              <button type="button" className="btn btn-light" onClick={() => { setSearchSoPhieu(''); setSearchNoiMua(''); setFromDate(''); setToDate(''); setStatusFilter(''); }}>
                <i className="bi bi-arrow-counterclockwise me-1" aria-hidden="true" />Xóa
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Import Table */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>Số phiếu</th>
                <th>Ngày nhập</th>
                <th>Nơi mua / Đơn vị cấp</th>
                <th>Số hóa đơn</th>
                <th>Nhóm ĐD</th>
                <th className="text-end">Tổng tiền</th>
                <th>Trạng thái</th>
                <th>Người tạo</th>
                <th className="text-center" style={{ width: '130px' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={9} className="text-center p-4 text-muted">
                    Đang tải danh sách phiếu nhập...
                  </td>
                </tr>
              ) : imports.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center p-4 text-muted">
                    Không tìm thấy phiếu nhập nào
                  </td>
                </tr>
              ) : (
                imports.map((item) => (
                  <tr key={item.id}>
                    <td className="fw-bold text-monospace" style={{ color: 'var(--sgb-navy-800)' }}>
                      {item.soPhieu}
                    </td>
                    <td>{item.ngayNhap}</td>
                    <td className="fw-semibold">{item.noiMua || '—'}</td>
                    <td>{item.soHoaDon || '—'}</td>
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
                          onClick={() => setSelectedImport(item)}
                        >
                          Chi tiết
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-primary btn-sm"
                          title="In phiếu"
                          onClick={() => setPrintTarget(item)}
                        >
                          <i className="bi bi-printer" aria-hidden="true" />
                        </button>
                        {isAdmin && item.trangThai === 'HOAN_THANH' && (
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            title="Hủy phiếu này"
                            onClick={() => handleCancelImport(item.id, item.soPhieu)}
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

      {/* Modal: Xem Chi Tiết Phiếu Nhập */}
      <Modal
        isOpen={!!selectedImport}
        onClose={() => setSelectedImport(null)}
        title={`Chi Tiết Phiếu Nhập: ${selectedImport?.soPhieu || ''}`}
        maxWidth="1140px"
      >
        {selectedImport && (
          <div>
            <div className="row g-3 mb-4 bg-light p-3 rounded border">
              <div className="col-md-4"><strong>Ngày nhập:</strong> {selectedImport.ngayNhap}</div>
              <div className="col-md-4"><strong>Nơi mua:</strong> {selectedImport.noiMua || '—'}</div>
              <div className="col-md-4"><strong>Nơi nhập:</strong> {selectedImport.noiNhap || '—'}</div>
              <div className="col-md-4"><strong>Số hóa đơn:</strong> {selectedImport.soHoaDon || '—'}</div>
              <div className="col-md-4"><strong>Ngày hóa đơn:</strong> {selectedImport.ngayHoaDon || '—'}</div>
              <div className="col-md-4"><strong>TK Nợ / Có:</strong> {selectedImport.tkNo || '—'} / {selectedImport.tkCo || '—'}</div>
              <div className="col-md-4"><strong>Người lập:</strong> {selectedImport.creatorFullName || selectedImport.createdBy || 'system'}</div>
            </div>

            <h4 className="fs-6 fw-bold mb-2">Danh Sách Hàng Hóa Nhập:</h4>
            <div className="table-responsive border rounded mb-4">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Mã số</th>
                    <th>Tên hàng hóa</th>
                    <th>ĐVT</th>
                    <th className="text-end">Số lượng</th>
                    <th className="text-end">Đơn giá (VND)</th>
                    <th className="text-end">Thành tiền (VND)</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedImport.details?.map((d, i) => (
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
                    <td colSpan={5} className="text-end fw-bolder fs-6">TỔNG CỘNG:</td>
                    <td className="text-end fw-bolder fs-5" style={{ color: 'var(--sgb-navy-900)' }}>
                      {formatCurrency(selectedImport.tongTien)}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div className="d-flex justify-content-end">
              <button
                type="button"
                className="btn btn-primary px-4"
                onClick={() => setSelectedImport(null)}
              >
                Đóng
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal: In Phiếu Nhập */}
      <PrintVoucherModal
        isOpen={!!printTarget}
        onClose={() => setPrintTarget(null)}
        voucher={printTarget}
        type="IMPORT"
      />
    </div>
  );
};

