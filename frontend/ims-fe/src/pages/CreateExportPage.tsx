import React, { useEffect, useState } from 'react';
import { exportService, productService, inventoryService } from '../services';
import type { Product, CreateExportRequest } from '../types';

interface CreateExportPageProps {
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
  onNavigateBack: () => void;
}

interface ExportRow {
  productId: number;
  quantity: number;
  nhomHang: string;
  donViTinh: string;
  currentStock?: number;
  averagePrice?: number;
  loadingStock?: boolean;
}

export const CreateExportPage: React.FC<CreateExportPageProps> = ({ showToast, onNavigateBack }) => {
  const [activeProducts, setActiveProducts] = useState<Product[]>([]);

  const [ngayXuat, setNgayXuat] = useState(new Date().toISOString().split('T')[0]);
  const [noiNhan, setNoiNhan] = useState('');
  const [lyDoXuat, setLyDoXuat] = useState('Cấp phát cho đơn vị');
  const [tkNo, setTkNo] = useState('642');
  const [tkCo, setTkCo] = useState('152');

  const [exportRows, setExportRows] = useState<ExportRow[]>([
    { productId: 0, quantity: 1, nhomHang: '', donViTinh: '' },
  ]);

  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    productService.getActiveProducts()
      .then(setActiveProducts)
      .catch(console.error);
  }, []);

  const handleAddRow = () => {
    setExportRows([...exportRows, { productId: 0, quantity: 1, nhomHang: '', donViTinh: '' }]);
  };

  const handleRemoveRow = (idx: number) => {
    if (exportRows.length <= 1) return;
    setExportRows(exportRows.filter((_, i) => i !== idx));
  };

  const handleRowChange = async (idx: number, field: string, val: any) => {
    const updated = [...exportRows];

    if (field === 'productId') {
      const pid = Number(val);
      const prod = activeProducts.find((p) => p.id === pid);
      updated[idx] = {
        ...updated[idx],
        productId: pid,
        nhomHang: prod?.nhomHang || '',
        donViTinh: prod?.donViTinh || '',
        currentStock: undefined,
        averagePrice: undefined,
        loadingStock: pid > 0,
      };
      setExportRows([...updated]);

      if (pid > 0) {
        try {
          const inv = await inventoryService.getInventoryByProduct(pid);
          setExportRows((prev) => {
            const next = [...prev];
            if (next[idx]?.productId === pid) {
              next[idx] = {
                ...next[idx],
                currentStock: inv.endingQuantity,
                averagePrice: inv.averagePrice,
                loadingStock: false,
              };
            }
            return next;
          });
        } catch {
          setExportRows((prev) => {
            const next = [...prev];
            if (next[idx]?.productId === pid) {
              next[idx] = { ...next[idx], loadingStock: false };
            }
            return next;
          });
        }
      }
    } else {
      updated[idx] = { ...updated[idx], [field]: val };
      setExportRows(updated);
    }
  };

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validRows = exportRows.filter((r) => r.productId > 0 && r.quantity > 0);
    if (validRows.length === 0) {
      showToast('Vui lòng chọn ít nhất 1 mặt hàng với số lượng xuất hợp lệ (>0)', 'warning');
      return;
    }

    // ACQT không được xuất chung nhóm khác
    const groups = new Set(validRows.map((r) => r.nhomHang).filter(Boolean));
    if (groups.has('ACQT') && groups.size > 1) {
      showToast(
        'QUY TẮC NGHIỆP VỤ: Ấn chỉ quan trọng (ACQT) phải lập phiếu riêng, không được xuất chung với VPP / ACTT / VLK!',
        'error',
      );
      return;
    }

    const overStock = validRows.find(
      (r) => r.currentStock !== undefined && Number(r.quantity) > r.currentStock,
    );
    if (overStock) {
      const prod = activeProducts.find((p) => p.id === overStock.productId);
      showToast(
        `Số lượng xuất của "${prod?.tenHang}" (${overStock.quantity}) vượt tồn khả dụng (${overStock.currentStock})!`,
        'error',
      );
      return;
    }

    try {
      setSubmitting(true);
      const req: CreateExportRequest = {
        ngayXuat,
        noiNhan,
        lyDoXuat,
        tkNo,
        tkCo,
        details: validRows.map((r) => ({
          productId: r.productId,
          quantity: Number(r.quantity),
        })),
      };
      const res = await exportService.createExport(req);
      showToast(`Lập phiếu xuất thành công! Số phiếu: ${res.soPhieu}`, 'success');
      onNavigateBack();
    } catch (err: any) {
      showToast('Lỗi lập phiếu xuất: ' + err.message, 'error');
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
              <i className="bi bi-box-arrow-up-right me-1" aria-hidden="true" />
              Xuất kho
            </div>
            <h2 className="mb-1">Lập Phiếu Xuất Kho Mới</h2>
            <p>Điền đầy đủ thông tin — đơn giá xuất được hệ thống tự tính theo giá bình quân gia quyền.</p>
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
            Thông tin phiếu xuất
          </div>
          <div className="card-body p-4">
            <div className="row g-3">

              {/* Ngày xuất */}
              <div className="col-md-3">
                <label className="form-label fw-medium">
                  Ngày xuất <span className="text-danger">*</span>
                </label>
                <input
                  type="date"
                  className="form-control"
                  value={ngayXuat}
                  onChange={(e) => setNgayXuat(e.target.value)}
                  required
                />
              </div>

              {/* Nơi nhận */}
              <div className="col-md-9">
                <label className="form-label fw-medium">
                  Nơi nhận / Đơn vị sử dụng <span className="text-danger">*</span>
                </label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="VD: CN Chợ Lớn, PGD Bến Thành, Phòng Kế toán..."
                  value={noiNhan}
                  onChange={(e) => setNoiNhan(e.target.value)}
                  required
                />
              </div>

              {/* Lý do xuất */}
              <div className="col-md-6">
                <label className="form-label fw-medium">
                  Lý do xuất <span className="text-danger">*</span>
                </label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="VD: Cấp phát cho đơn vị, Sử dụng nội bộ..."
                  value={lyDoXuat}
                  onChange={(e) => setLyDoXuat(e.target.value)}
                  required
                />
              </div>

              {/* TK Nợ */}
              <div className="col-md-3">
                <label className="form-label fw-medium">TK Nợ</label>
                <input
                  type="text"
                  className="form-control"
                  value={tkNo}
                  onChange={(e) => setTkNo(e.target.value)}
                />
              </div>

              {/* TK Có */}
              <div className="col-md-3">
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

        {/* ─── CẢNH BÁO QUY TẮC ─── */}
        <div
          className="alert d-flex align-items-start gap-2 mb-4 border-0"
          role="alert"
          style={{ backgroundColor: '#FFFBEB', color: '#92400E', borderLeft: '4px solid #F59E0B', borderRadius: '8px' }}
        >
          <i className="bi bi-exclamation-triangle-fill mt-1 flex-shrink-0" aria-hidden="true" />
          <div>
            <strong>Quy tắc nghiệp vụ:</strong> Đơn giá xuất được hệ thống tự động tính theo{' '}
            <em>Giá bình quân gia quyền</em> của kỳ trước. Nhóm{' '}
            <strong>Ấn chỉ quan trọng (ACQT)</strong> không được lập chung với các nhóm hàng khác.
          </div>
        </div>

        {/* ─── DANH SÁCH HÀNG XUẤT ─── */}
        <div className="card shadow-sm border-0 mb-4">
          <div
            className="card-header border-0 d-flex align-items-center justify-content-between text-white"
            style={{ backgroundColor: 'var(--sgb-navy-700)', borderRadius: '12px 12px 0 0', padding: '14px 20px' }}
          >
            <span className="fw-bold d-flex align-items-center gap-2">
              <i className="bi bi-list-ul" aria-hidden="true" />
              Danh sách hàng xuất
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
                    <th className="text-end" style={{ minWidth: '120px' }}>Tồn khả dụng</th>
                    <th className="text-end" style={{ minWidth: '150px' }}>Đơn giá bình quân</th>
                    <th style={{ minWidth: '120px' }}>Số lượng xuất <span className="text-danger">*</span></th>
                    <th className="text-end" style={{ minWidth: '140px' }}>Thành tiền (ước tính)</th>
                    <th style={{ width: '56px' }} className="text-center">Xóa</th>
                  </tr>
                </thead>
                <tbody>
                  {exportRows.map((row, idx) => {
                    const estAmount = (Number(row.quantity) || 0) * (row.averagePrice || 0);
                    const isOverStock = row.currentStock !== undefined && Number(row.quantity) > row.currentStock;

                    return (
                      <tr key={idx} className={isOverStock ? 'table-danger' : ''}>
                        <td className="text-center text-muted small">{idx + 1}</td>

                        {/* Chọn hàng hóa */}
                        <td>
                          <select
                            className="form-select form-select-sm"
                            value={row.productId}
                            onChange={(e) => handleRowChange(idx, 'productId', e.target.value)}
                            required
                          >
                            <option value={0}>-- Chọn hàng hóa để xuất --</option>
                            {activeProducts.map((p) => (
                              <option key={p.id} value={p.id}>
                                [{p.nhomHang}] {p.maSo} – {p.tenHang}
                              </option>
                            ))}
                          </select>
                        </td>

                        {/* Nhóm */}
                        <td>
                          <span className="badge text-bg-secondary">{row.nhomHang || '—'}</span>
                        </td>

                        {/* ĐVT */}
                        <td className="text-muted">{row.donViTinh || '—'}</td>

                        {/* Tồn kho */}
                        <td className="text-end">
                          {row.loadingStock ? (
                            <span className="spinner-border spinner-border-sm text-secondary" role="status" />
                          ) : row.currentStock !== undefined ? (
                            <span className={`fw-bold ${row.currentStock > 0 ? 'text-success' : 'text-danger'}`}>
                              {row.currentStock.toLocaleString('vi-VN')}
                            </span>
                          ) : '—'}
                        </td>

                        {/* Đơn giá bình quân */}
                        <td className="text-end text-muted small">
                          {row.averagePrice !== undefined ? formatCurrency(row.averagePrice) : '—'}
                        </td>

                        {/* Số lượng xuất */}
                        <td>
                          <input
                            type="number"
                            className={`form-control form-control-sm ${isOverStock ? 'is-invalid border-danger' : ''}`}
                            min="0.001"
                            max={row.currentStock || undefined}
                            step="any"
                            placeholder="0"
                            value={row.quantity}
                            onChange={(e) => handleRowChange(idx, 'quantity', e.target.value)}
                            required
                          />
                          {isOverStock && (
                            <div className="invalid-feedback d-block" style={{ fontSize: '0.75rem' }}>
                              Vượt tồn ({row.currentStock})
                            </div>
                          )}
                        </td>

                        {/* Thành tiền ước tính */}
                        <td className="text-end fw-semibold" style={{ color: estAmount > 0 ? 'var(--sgb-navy-800)' : '#aaa' }}>
                          {estAmount > 0 ? formatCurrency(estAmount) : '—'}
                        </td>

                        {/* Nút xóa */}
                        <td className="text-center">
                          {exportRows.length > 1 && (
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
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* ─── NÚT HÀNH ĐỘNG ─── */}
        <div className="d-flex align-items-center justify-content-between gap-3 flex-wrap">
          <p className="mb-0 small text-muted">
            <i className="bi bi-info-circle me-1" aria-hidden="true" />
            Số phiếu sẽ được hệ thống sinh tự động. Thành tiền hiển thị là ước tính theo giá bình quân hiện tại.
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
                : <><i className="bi bi-check-circle" aria-hidden="true" /> Xác Nhận & Lưu Phiếu Xuất</>
              }
            </button>
          </div>
        </div>

      </form>
    </div>
  );
};
