import React, { useEffect, useRef, useState } from 'react';
import { kktsService } from '../services';
import type { KktsItem } from '../types';
import { exportElementToPdf } from '../utils/exportPdf';

interface KktsPageProps {
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
}

export const KktsPage: React.FC<KktsPageProps> = ({ showToast }) => {
  const currentDate = new Date();
  const [selectedYear, setSelectedYear] = useState<number>(currentDate.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState<number>(currentDate.getMonth() + 1);
  const [kktsList, setKktsList] = useState<KktsItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const reportRef = useRef<HTMLDivElement>(null);

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

  const handleExportPdf = async () => {
    if (!reportRef.current) return;
    setExporting(true);
    try {
      const filename = `Bao_cao_KKTS_Thang_${selectedMonth}_${selectedYear}.pdf`;
      await exportElementToPdf(reportRef.current, {
        filename,
        orientation: 'landscape',
        marginMm: 6,
      });
      showToast('Xuất file PDF báo cáo kiểm kê thành công!', 'success');
    } catch (err: any) {
      console.error('Lỗi xuất PDF:', err);
      showToast('Lỗi xuất file PDF: ' + (err?.message || 'Không thể tạo file'), 'error');
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="animate-fade-in">
      {/* Page Hero */}
      <div className="page-hero mb-4">
        <div className="page-hero-badge">
          <i className="bi bi-card-checklist" aria-hidden="true" /> Kiểm kê tài sản
        </div>
        <h2 className="mb-1">Bảng Kiểm Kê Tài Sản (KKTS) Theo Kỳ</h2>
        <p>
          Báo cáo tổng hợp số lượng &amp; giá trị nhập xuất tồn ấn phẩm — kỳ{' '}
          <strong className="text-white">Tháng {selectedMonth}/{selectedYear}</strong>
        </p>
      </div>

      {/* Period Selector */}
      <div className="card mb-4">
        <div className="card-body">
          <div className="d-flex align-items-center justify-content-between flex-wrap gap-3">
            <div className="d-flex align-items-center gap-3 flex-wrap">
              <div className="d-flex align-items-center gap-2">
                <label className="form-label mb-0 fw-semibold" style={{ whiteSpace: 'nowrap', fontSize: '0.875rem' }}>
                  Chọn kỳ:
                </label>
                <select
                  className="form-select form-select-sm shadow-none"
                  style={{ width: '130px' }}
                  value={selectedMonth}
                  onChange={(e) => setSelectedMonth(Number(e.target.value))}
                >
                  {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                    <option key={m} value={m}>
                      Tháng {m}
                    </option>
                  ))}
                </select>
              </div>
              <div className="d-flex align-items-center gap-2">
                <select
                  className="form-select form-select-sm shadow-none"
                  style={{ width: '130px' }}
                  value={selectedYear}
                  onChange={(e) => setSelectedYear(Number(e.target.value))}
                >
                  {[2024, 2025, 2026, 2027].map((y) => (
                    <option key={y} value={y}>
                      Năm {y}
                    </option>
                  ))}
                </select>
              </div>
              <button
                type="button"
                className="btn btn-primary fw-medium d-flex align-items-center gap-2"
                onClick={loadKkts}
                disabled={loading}
              >
                <i className={`bi bi-arrow-clockwise ${loading ? 'spin' : ''}`} aria-hidden="true" /> Xem báo cáo
              </button>
            </div>

            <div className="d-flex align-items-center gap-2">
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm rounded-pill fw-medium px-3 d-flex align-items-center gap-2"
                onClick={handlePrint}
                disabled={loading || kktsList.length === 0}
                title="In trực tiếp bằng lệnh in của trình duyệt"
              >
                <i className="bi bi-printer" aria-hidden="true" /> In trang
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* KKTS Table Card */}
      <div className="card shadow-sm border-0 mb-4">
        {/* Card Header with Primary Export PDF Action */}
        <div className="card-header bg-white border-bottom px-4 py-3 d-flex align-items-center justify-content-between flex-wrap gap-2">
          <div className="d-flex align-items-center gap-2">
            <h5 className="fw-bold mb-0" style={{ color: 'var(--sgb-navy-900)', fontSize: '1.05rem' }}>
              <i className="bi bi-table me-2 text-primary" aria-hidden="true" />
              Báo Cáo Kiểm Kê Tháng {selectedMonth}/{selectedYear}
            </h5>
            <span className="badge bg-light text-secondary border fw-medium px-2 py-1" style={{ fontSize: '0.78rem' }}>
              {kktsList.length} mặt hàng
            </span>
          </div>
          <button
            type="button"
            className="btn btn-gold fw-semibold d-flex align-items-center gap-2 rounded-pill px-4"
            onClick={handleExportPdf}
            disabled={exporting || loading || kktsList.length === 0}
          >
            <i className={`bi ${exporting ? 'bi-hourglass-split' : 'bi-file-earmark-pdf'}`} aria-hidden="true" />
            {exporting ? 'Đang xuất PDF...' : 'Xuất File PDF'}
          </button>
        </div>

        {/* Printable & Exportable Document Area */}
        <div ref={reportRef} className="bg-white p-3">
          {/* Official Saigonbank Report Header (Visible in PDF / Print) */}
          <div className="p-3 mb-2 border-bottom">
            <div className="d-flex justify-content-between align-items-start mb-2">
              <div>
                <div className="fw-bold" style={{ fontSize: '13px', color: '#185089' }}>
                  NGÂN HÀNG THƯƠNG MẠI CỔ PHẦN SÀI GÒN CÔNG THƯƠNG (SAIGONBANK)
                </div>
                <div className="text-muted" style={{ fontSize: '11px' }}>
                  Phòng Kế toán - Kho vật tư &amp; Ấn chỉ
                </div>
              </div>
              <div className="text-end text-muted" style={{ fontSize: '11px' }}>
                <div className="fw-semibold">Mẫu số: B01-KKTS</div>
                <div>Ban hành theo QĐ Saigonbank</div>
              </div>
            </div>
            <div className="text-center my-2">
              <h4 className="fw-bold text-uppercase mb-1" style={{ color: '#185089', letterSpacing: '0.5px' }}>
                BẢNG BÁO CÁO KIỂM KÊ TÀI SẢN &amp; ẤN PHẨM
              </h4>
              <div className="text-muted" style={{ fontSize: '12px' }}>
                Kỳ kiểm kê: Tháng {selectedMonth} năm {selectedYear} &bull; Ngày lập biểu:{' '}
                {new Date().toLocaleDateString('vi-VN')}
              </div>
            </div>
          </div>

          <div className="table-responsive">
            <table className="table table-hover table-bordered align-middle mb-0" style={{ fontSize: '12px' }}>
              <thead className="table-light text-center">
                <tr>
                  <th style={{ width: '40px' }}>STT</th>
                  <th style={{ width: '130px' }}>Mã ấn phẩm</th>
                  <th style={{ minWidth: '180px' }}>Tên ấn phẩm / VPP</th>
                  <th style={{ width: '65px' }}>Nhóm</th>
                  <th style={{ width: '55px' }}>ĐVT</th>
                  <th className="text-end" style={{ width: '70px' }}>Tồn đầu</th>
                  <th className="text-end" style={{ width: '100px' }}>Tiền tồn đầu</th>
                  <th className="text-end" style={{ width: '70px' }}>Nhập kỳ</th>
                  <th className="text-end" style={{ width: '100px' }}>Tiền nhập</th>
                  <th className="text-end" style={{ width: '70px' }}>Xuất kỳ</th>
                  <th className="text-end" style={{ width: '100px' }}>Tiền xuất</th>
                  <th className="text-end" style={{ width: '75px', backgroundColor: '#E6F0FA' }}>TỒN CUỐI</th>
                  <th className="text-end" style={{ width: '95px', backgroundColor: '#E6F0FA' }}>ĐƠN GIÁ BQ</th>
                  <th className="text-end" style={{ width: '110px', backgroundColor: '#E6F0FA' }}>THÀNH TIỀN</th>
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
                        <td className="text-center">{idx + 1}</td>
                        <td className="fw-bold font-monospace" style={{ color: 'var(--sgb-navy-800)' }}>
                          {item.maSo}
                        </td>
                        <td className="fw-semibold">{item.tenHang}</td>
                        <td className="text-center">
                          <span className="badge text-bg-secondary">{item.nhomHang}</span>
                        </td>
                        <td className="text-center">{item.donViTinh}</td>
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
                        <td
                          className="text-end fw-bold"
                          style={{ backgroundColor: '#F0F6FC', color: 'var(--sgb-navy-900)' }}
                        >
                          {formatCurrency(item.endingAmount)}
                        </td>
                      </tr>
                    ))}
                    {/* Totals Summary Row */}
                    <tr className="table-light border-top border-2">
                      <td colSpan={5} className="fw-bold" style={{ color: 'var(--sgb-navy-900)' }}>
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

          {/* Official Signatures Row */}
          {kktsList.length > 0 && (
            <div className="pt-4 mt-3 border-top" style={{ fontSize: '12px' }}>
              <div className="row text-center">
                <div className="col-3">
                  <div className="fw-bold mb-1">Người Lập Biểu</div>
                  <div className="text-muted" style={{ fontSize: '11px' }}>(Ký, họ tên)</div>
                  <div style={{ height: '55px' }} />
                </div>
                <div className="col-3">
                  <div className="fw-bold mb-1">Thủ Kho</div>
                  <div className="text-muted" style={{ fontSize: '11px' }}>(Ký, họ tên)</div>
                  <div style={{ height: '55px' }} />
                </div>
                <div className="col-3">
                  <div className="fw-bold mb-1">Kế Toán Trưởng</div>
                  <div className="text-muted" style={{ fontSize: '11px' }}>(Ký, họ tên)</div>
                  <div style={{ height: '55px' }} />
                </div>
                <div className="col-3">
                  <div className="fw-bold mb-1">Ban Giám Đốc</div>
                  <div className="text-muted" style={{ fontSize: '11px' }}>(Ký, đóng dấu)</div>
                  <div style={{ height: '55px' }} />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
