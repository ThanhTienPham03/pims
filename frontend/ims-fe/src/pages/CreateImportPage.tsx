import React, { useEffect, useState } from 'react';
import { importService, productService } from '../services';
import type { Product, CreateImportRequest } from '../types';

interface CreateImportPageProps {
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
  onNavigateBack: () => void;
}

export const CreateImportPage: React.FC<CreateImportPageProps> = ({ showToast, onNavigateBack }) => {
  const [activeProducts, setActiveProducts] = useState<Product[]>([]);

  const [ngayNhap, setNgayNhap] = useState(new Date().toISOString().split('T')[0]);
  const [noiNhap, setNoiNhap] = useState('Kho Trụ sở chính Saigonbank');
  const [noiMua, setNoiMua] = useState('');
  const [soHoaDon, setSoHoaDon] = useState('');
  const [ngayHoaDon, setNgayHoaDon] = useState('');
  const [tkNo, setTkNo] = useState('152');
  const [tkCo, setTkCo] = useState('111');

  const [importRows, setImportRows] = useState<{
    productId: number;
    quantity: number;
    unitPrice: number;
    nhomHang: string;
    donViTinh: string;
  }[]>([{ productId: 0, quantity: 1, unitPrice: 0, nhomHang: '', donViTinh: '' }]);

  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    productService.getActiveProducts()
      .then(setActiveProducts)
      .catch(console.error);
  }, []);

  const handleAddRow = () => {
    setImportRows([...importRows, { productId: 0, quantity: 1, unitPrice: 0, nhomHang: '', donViTinh: '' }]);
  };

  const handleRemoveRow = (idx: number) => {
    if (importRows.length <= 1) return;
    setImportRows(importRows.filter((_, i) => i !== idx));
  };

  const handleRowChange = (idx: number, field: string, val: any) => {
    const updated = [...importRows];
    if (field === 'productId') {
      const pid = Number(val);
      const prod = activeProducts.find((p) => p.id === pid);
      updated[idx] = {
        ...updated[idx],
        productId: pid,
        unitPrice: prod?.donGiaNhap || 0,
        nhomHang: prod?.nhomHang || '',
        donViTinh: prod?.donViTinh || '',
      };
    } else {
      updated[idx] = { ...updated[idx], [field]: val };
    }
    setImportRows(updated);
  };

  const calculateTotal = () =>
    importRows.reduce((sum, r) => sum + (Number(r.quantity) || 0) * (Number(r.unitPrice) || 0), 0);

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validRows = importRows.filter((r) => r.productId > 0 && r.quantity > 0 && r.unitPrice > 0);
    if (validRows.length === 0) {
      showToast('Vui lòng chọn ít nhất 1 mặt hàng với số lượng và đơn giá hợp lệ (>0)', 'warning');
      return;
    }

    // Quy tắc nghiệp vụ: ACQT phải lập phiếu riêng, không được nhập chung với VPP / ACTT / VLK
    const groups = new Set(validRows.map((r) => r.nhomHang).filter(Boolean));
    if (groups.has('ACQT') && groups.size > 1) {
      showToast(
        'QUY TẮC NGHIỆP VỤ: Ấn chỉ quan trọng (ACQT) phải lập phiếu nhập riêng, không được nhập chung với VPP / ACTT / VLK!',
        'error',
      );
      return;
    }

    // Kiểm tra trùng lặp hàng hóa trên các dòng
    const productIds = validRows.map((r) => r.productId);
    if (new Set(productIds).size !== productIds.length) {
      showToast('Có mặt hàng bị chọn trùng nhau trên nhiều dòng. Vui lòng gộp số lượng lại thành 1 dòng!', 'warning');
      return;
    }

    try {
      setSubmitting(true);
      const req: CreateImportRequest = {
        ngayNhap,
        noiNhap,
        noiMua,
        soHoaDon,
        ngayHoaDon: ngayHoaDon || undefined,
        tkNo,
        tkCo,
        details: validRows.map((r) => ({
          productId: r.productId,
          quantity: Number(r.quantity),
          unitPrice: Number(r.unitPrice),
        })),
      };
      const res = await importService.createImport(req);
      showToast(`Lập phiếu nhập thành công! Số phiếu: ${res.soPhieu}`, 'success');
      onNavigateBack();
    } catch (err: any) {
      showToast('Lỗi lập phiếu nhập: ' + err.message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="animate-fade-in">

      {/* Page Hero */}
      <div className="page-hero mb-4">
        <div className="row align-items-center g-3">
          <div className="col">
            <div className="page-hero-badge">
              <i className="bi bi-box-arrow-in-down me-1" aria-hidden="true" />
              Nhập kho
            </div>
            <h2 className="mb-1">Lập Phiếu Nhập Kho Mới</h2>
            <p>Điền đầy đủ thông tin phiếu nhập — số phiếu sẽ được hệ thống sinh tự động.</p>
          </div>
          <div className="col-auto">
            <button
              type="button"
              className="btn btn-outline-light rounded-pill d-flex align-items-center gap-2 px-4"
              onClick={onNavigateBack}
            >
              <i className="bi bi-arrow-left" aria-hidden="true" />
              Quay lại danh sách
            </button>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit}>

        {/* ─── THÔNG TIN PHIẾU ─── */}
        <div className="card shadow-sm border-0 mb-4">
          <div
            className="card-header border-0 fw-bold text-white d-flex align-items-center gap-2"
            style={{ backgroundColor: 'var(--sgb-navy-700)', borderRadius: '12px 12px 0 0', padding: '14px 20px' }}
          >
            <i className="bi bi-file-text" aria-hidden="true" />
            Thông tin phiếu nhập
          </div>
          <div className="card-body p-4">
            <div className="row g-3">

              {/* Ngày nhập */}
              <div className="col-md-4">
                <label className="form-label fw-medium">
                  Ngày nhập <span className="text-danger">*</span>
                </label>
                <input
                  type="date"
                  className="form-control"
                  value={ngayNhap}
                  onChange={(e) => setNgayNhap(e.target.value)}
                  required
                />
              </div>

              {/* Nơi mua */}
              <div className="col-md-4">
                <label className="form-label fw-medium">
                  Nơi mua / Nhà cung cấp <span className="text-danger">*</span>
                </label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="VD: Cty CP In Tài Chính"
                  value={noiMua}
                  onChange={(e) => setNoiMua(e.target.value)}
                  required
                />
              </div>

              {/* Nơi nhập */}
              <div className="col-md-4">
                <label className="form-label fw-medium">Nơi nhập / Kho nhận</label>
                <input
                  type="text"
                  className="form-control"
                  value={noiNhap}
                  onChange={(e) => setNoiNhap(e.target.value)}
                />
              </div>

              {/* Số hóa đơn */}
              <div className="col-md-4">
                <label className="form-label fw-medium">Số hóa đơn</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="VD: HD-001248"
                  value={soHoaDon}
                  onChange={(e) => setSoHoaDon(e.target.value)}
                />
              </div>

              {/* Ngày hóa đơn */}
              <div className="col-md-4">
                <label className="form-label fw-medium">Ngày hóa đơn</label>
                <input
                  type="date"
                  className="form-control"
                  value={ngayHoaDon}
                  onChange={(e) => setNgayHoaDon(e.target.value)}
                />
              </div>

              {/* TK Nợ / Có */}
              <div className="col-md-2">
                <label className="form-label fw-medium">TK Nợ</label>
                <input
                  type="text"
                  className="form-control"
                  value={tkNo}
                  onChange={(e) => setTkNo(e.target.value)}
                />
              </div>
              <div className="col-md-2">
                <label className="form-label fw-medium">TK Có</label>
                <input
                  type="text"
                  className="form-control"
                  value={tkCo}
                  onChange={(e) => setTkCo(e.target.value)}
                />
              </div>

            </div>
          </div>
        </div>

        {/* ─── DANH SÁCH HÀNG NHẬP ─── */}
        <div className="card shadow-sm border-0 mb-4">
          <div
            className="card-header border-0 d-flex align-items-center justify-content-between text-white"
            style={{ backgroundColor: 'var(--sgb-navy-700)', borderRadius: '12px 12px 0 0', padding: '14px 20px' }}
          >
            <span className="fw-bold d-flex align-items-center gap-2">
              <i className="bi bi-list-ul" aria-hidden="true" />
              Danh sách hàng nhập
            </span>
            <button
              type="button"
              className="btn btn-sm btn-gold d-flex align-items-center gap-1"
              onClick={handleAddRow}
            >
              <i className="bi bi-plus-circle" aria-hidden="true" />
              Thêm dòng hàng
            </button>
          </div>

          <div className="card-body p-0">
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0" style={{ fontSize: '0.9rem' }}>
                <thead className="table-light">
                  <tr>
                    <th style={{ width: '32px' }} className="text-center text-muted">#</th>
                    <th style={{ minWidth: '260px' }}>Hàng hóa / Ấn phẩm <span className="text-danger">*</span></th>
                    <th style={{ minWidth: '80px' }}>Nhóm</th>
                    <th style={{ minWidth: '70px' }}>ĐVT</th>
                    <th style={{ minWidth: '110px' }}>Số lượng <span className="text-danger">*</span></th>
                    <th style={{ minWidth: '150px' }}>Đơn giá (VND) <span className="text-danger">*</span></th>
                    <th className="text-end" style={{ minWidth: '140px' }}>Thành tiền</th>
                    <th style={{ width: '56px' }} className="text-center">Xóa</th>
                  </tr>
                </thead>
                <tbody>
                  {importRows.map((row, idx) => (
                    <tr key={idx}>
                      <td className="text-center text-muted small">{idx + 1}</td>
                      <td>
                        <select
                          className="form-select form-select-sm"
                          value={row.productId}
                          onChange={(e) => handleRowChange(idx, 'productId', e.target.value)}
                          required
                        >
                          <option value={0}>-- Chọn hàng hóa --</option>
                          {activeProducts.map((p) => (
                            <option key={p.id} value={p.id}>
                              [{p.nhomHang || '—'}] {p.maSo} – {p.tenHang}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td>
                        <span className="badge text-bg-secondary">
                          {row.nhomHang || '—'}
                        </span>
                      </td>
                      <td className="text-muted">{row.donViTinh || '—'}</td>
                      <td>
                        <input
                          type="number"
                          className="form-control form-control-sm"
                          min="0.001"
                          step="any"
                          placeholder="0"
                          value={row.quantity}
                          onChange={(e) => handleRowChange(idx, 'quantity', e.target.value)}
                          required
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          className="form-control form-control-sm"
                          min="0"
                          placeholder="0"
                          value={row.unitPrice}
                          onChange={(e) => handleRowChange(idx, 'unitPrice', e.target.value)}
                          required
                        />
                      </td>
                      <td className="text-end fw-semibold" style={{ color: 'var(--sgb-navy-800)' }}>
                        {formatCurrency((Number(row.quantity) || 0) * (Number(row.unitPrice) || 0))}
                      </td>
                      <td className="text-center">
                        {importRows.length > 1 && (
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            title="Xóa dòng này"
                            onClick={() => handleRemoveRow(idx)}
                          >
                            <i className="bi bi-x-lg" aria-hidden="true" />
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="table-light">
                  <tr>
                    <td colSpan={6} className="text-end fw-bolder fs-6 py-3 pe-3">TỔNG CỘNG:</td>
                    <td className="text-end fw-bolder fs-5 py-3" style={{ color: 'var(--sgb-navy-900)' }}>
                      {formatCurrency(calculateTotal())}
                    </td>
                    <td />
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>

        {/* ─── GHI CHÚ & NÚT LƯU ─── */}
        <div className="d-flex align-items-center justify-content-between gap-3 flex-wrap">
          <p className="mb-0 small text-muted">
            <i className="bi bi-info-circle me-1" aria-hidden="true" />
            Số phiếu sẽ được hệ thống sinh tự động theo quy tắc <code>STT/NHOM-YYYY</code>.
          </p>
          <div className="d-flex gap-2">
            <button
              type="button"
              className="btn btn-outline-secondary px-4"
              onClick={onNavigateBack}
              disabled={submitting}
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              className="btn btn-gold px-5 fw-semibold d-flex align-items-center gap-2"
              style={{ boxShadow: '0 4px 14px rgba(34,197,94,0.4)' }}
              disabled={submitting}
            >
              {submitting
                ? <><span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" /> Đang lập phiếu...</>
                : <><i className="bi bi-check-circle" aria-hidden="true" /> Xác Nhận & Lưu Phiếu Nhập</>
              }
            </button>
          </div>
        </div>

      </form>
    </div>
  );
};
