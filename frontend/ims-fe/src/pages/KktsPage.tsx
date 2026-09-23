import React, { useEffect, useState } from 'react';
import { kktsService } from '../services';
import type { KktsItem } from '../types';

interface KktsPageProps {
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
}

export const KktsPage: React.FC<KktsPageProps> = ({ showToast }) => {
  const currentDate = new Date();
  const [selectedYear, setSelectedYear] = useState<number>(currentDate.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState<number>(currentDate.getMonth() + 1);
  const [kktsList, setKktsList] = useState<KktsItem[]>([]);
  const [loading, setLoading] = useState(false);

  const loadKkts = async () => {
    try {
      setLoading(true);
      const data = await kktsService.getKktsByPeriod(selectedYear, selectedMonth);
      setKktsList(data || []);
    } catch (err: any) {
      showToast('Lỗi tải báo cáo KKTS: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadKkts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedYear, selectedMonth]);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
  };

  const calculateTotals = () => {
    return kktsList.reduce(
      (acc, item) => ({
        openingAmount: acc.openingAmount + Number(item.openingAmount || 0),
        importAmount: acc.importAmount + Number(item.importAmount || 0),
        exportAmount: acc.exportAmount + Number(item.exportAmount || 0),
        endingAmount: acc.endingAmount + Number(item.endingAmount || 0),
      }),
      { openingAmount: 0, importAmount: 0, exportAmount: 0, endingAmount: 0 }
    );
  };

  const totals = calculateTotals();

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="animate-fade-in">

      {/* Page Hero */}
      <div className="page-hero mb-4">
        <div className="row align-items-center g-3">
          <div className="col">
            <div className="page-hero-badge"><i className="bi bi-card-checklist" aria-hidden="true" /> Kiểm kê tài sản</div>
            <h2 className="mb-1">Bảng Kiểm Kê Tài Sản (KKTS) Theo Kỳ</h2>
            <p>Báo cáo tổng hợp số lượng &amp; giá trị nhập xuất tồn ấn phẩm — kỳ <strong className="text-white">Tháng {selectedMonth}/{selectedYear}</strong></p>
          </div>
          <div className="col-auto d-flex gap-2">
            <button
              type="button"
              className="btn btn-light btn-sm rounded-pill fw-semibold px-3 d-flex align-items-center gap-2"
              onClick={loadKkts}
              disabled={loading}
            >
              <i className="bi bi-arrow-clockwise" aria-hidden="true" /> Cập nhật
            </button>
            <button
              type="button"
              className="btn btn-gold fw-semibold d-flex align-items-center gap-2 rounded-pill px-4"
              style={{ boxShadow: '0 4px 14px rgba(34,197,94,0.4)' }}
              onClick={handlePrint}
            >
              <i className="bi bi-printer" aria-hidden="true" /> In Báo Cáo
            </button>
          </div>
        </div>
      </div>

      {/* Period Selector */}
      <div className="card mb-4">
        <div className="card-body">
          <div className="d-flex align-items-center gap-3 flex-wrap">
            <div className="d-flex align-items-center gap-2">
              <label className="form-label mb-0 fw-semibold" style={{ whiteSpace: 'nowrap', fontSize: '0.875rem' }}>Chọn kỳ:</label>
              <select
                className="form-select form-select-sm shadow-none"
                style={{ width: '130px' }}
                value={selectedMonth}
                onChange={e => setSelectedMonth(Number(e.target.value))}
              >
                {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
                  <option key={m} value={m}>Tháng {m}</option>
                ))}
              </select>
            </div>
            <div className="d-flex align-items-center gap-2">
              <select
                className="form-select form-select-sm shadow-none"
                style={{ width: '130px' }}
                value={selectedYear}
                onChange={e => setSelectedYear(Number(e.target.value))}
              >
                {[2024, 2025, 2026, 2027].map(y => (
                  <option key={y} value={y}>Năm {y}</option>
                ))}
              </select>
            </div>
            <button
              type="button"
              className="btn btn-primary fw-medium d-flex align-items-center gap-2"
              onClick={loadKkts}
            >
              <i className="bi bi-arrow-clockwise" aria-hidden="true" /> Xem báo cáo
            </button>
          </div>
        </div>
      </div>

      {/* KKTS Table */}
      <div className="card shadow-sm border-0">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th style={{ width: '50px' }}>STT</th>
                <th>Mã ấn phẩm</th>
                <th>Tên ấn phẩm / VPP</th>
                <th>Nhóm</th>
                <th>ĐVT</th>
                <th className="text-end">Tồn đầu kỳ</th>
                <th className="text-end">Tiền tồn đầu</th>
                <th className="text-end">Nhập trong kỳ</th>
                <th className="text-end">Tiền nhập</th>
                <th className="text-end">Xuất trong kỳ</th>
                <th className="text-end">Tiền xuất</th>
                <th className="text-end" style={{ backgroundColor: '#E6F0FA' }}>TỒN CUỐI KỲ</th>
                <th className="text-end" style={{ backgroundColor: '#E6F0FA' }}>ĐƠN GIÁ BQ</th>
                <th className="text-end" style={{ backgroundColor: '#E6F0FA' }}>THÀNH TIỀN TỒN</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={14} className="text-center p-4 text-muted">
                    Đang tổng hợp báo cáo KKTS...
                  </td>
                </tr>
              ) : kktsList.length === 0 ? (
                <tr>
                  <td colSpan={14} className="text-center p-4 text-muted">
                    Không có dữ liệu kiểm kê cho tháng {selectedMonth}/{selectedYear}
                  </td>
                </tr>
              ) : (
                <>
                  {kktsList.map((item, idx) => (
                    <tr key={item.id}>
                      <td>{idx + 1}</td>
                      <td className="fw-bold text-monospace" style={{ color: 'var(--sgb-navy-800)' }}>
                        {item.maSo}
                      </td>
                      <td className="fw-semibold">{item.tenHang}</td>
                      <td>
                        <span className="badge text-bg-secondary">{item.nhomHang}</span>
                      </td>
                      <td>{item.donViTinh}</td>
                      <td className="text-end">{item.openingQuantity}</td>
                      <td className="text-end">{formatCurrency(item.openingAmount)}</td>
                      <td className="text-end fw-semibold text-success">+{item.importQuantity}</td>
                      <td className="text-end text-success">{formatCurrency(item.importAmount)}</td>
                      <td className="text-end fw-semibold text-danger">-{item.exportQuantity}</td>
                      <td className="text-end text-danger">{formatCurrency(item.exportAmount)}</td>
                      <td className="text-end fw-bold" style={{ backgroundColor: '#F0F6FC' }}>
                        {item.endingQuantity}
                      </td>
                      <td className="text-end" style={{ backgroundColor: '#F0F6FC' }}>
                        {formatCurrency(item.averagePrice)}
                      </td>
                      <td className="text-end fw-bold" style={{ backgroundColor: '#F0F6FC', color: 'var(--sgb-navy-900)' }}>
                        {formatCurrency(item.endingAmount)}
                      </td>
                    </tr>
                  ))}
                  {/* Totals Summary Row */}
                  <tr className="table-light border-top border-2">
                    <td colSpan={5} className="fw-bold fs-6" style={{ color: 'var(--sgb-navy-900)' }}>
                      TỔNG CỘNG TOÀN NGÂN HÀNG:
                    </td>
                    <td></td>
                    <td className="text-end fw-bold">{formatCurrency(totals.openingAmount)}</td>
                    <td></td>
                    <td className="text-end fw-bold text-success">{formatCurrency(totals.importAmount)}</td>
                    <td></td>
                    <td className="text-end fw-bold text-danger">{formatCurrency(totals.exportAmount)}</td>
                    <td></td>
                    <td></td>
                    <td className="text-end fw-bolder fs-6" style={{ color: 'var(--sgb-navy-900)' }}>
                      {formatCurrency(totals.endingAmount)}
                    </td>
                  </tr>
                </>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
