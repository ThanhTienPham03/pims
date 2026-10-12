import React, { useRef, useState } from 'react';
import type { ImportDoc, ExportDoc } from '../../types';
import { numberToVietnamese } from '../../utils/numberToVietnamese';
import { exportElementToPdf } from '../../utils/exportPdf';
import { Modal } from '../common/Modal';

interface PrintVoucherModalProps {
  isOpen: boolean;
  onClose: () => void;
  voucher: ImportDoc | ExportDoc | null;
  type: 'IMPORT' | 'EXPORT';
}

function isImport(v: ImportDoc | ExportDoc): v is ImportDoc {
  return 'ngayNhap' in v;
}

export const PrintVoucherModal: React.FC<PrintVoucherModalProps> = ({ isOpen, onClose, voucher, type }) => {
  const printRef = useRef<HTMLDivElement>(null);
  const [exporting, setExporting] = useState(false);

  const handlePrint = () => {
    if (!printRef.current) return;
    const printContents = printRef.current.innerHTML;
    const win = window.open('', '_blank', 'width=900,height=700');
    if (!win) return;
    win.document.write(
      '<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"/><style>' +
      '* { box-sizing: border-box; margin: 0; padding: 0; } ' +
      'body { font-family: Times New Roman,Times,serif; font-size: 13px; color: #000; padding: 24px; } ' +
      'table { width: 100%; border-collapse: collapse; margin-bottom: 10px; } ' +
      'th, td { border: 1px solid #333; padding: 5px 8px; font-size: 12px; } ' +
      'thead th { background: #f0f0f0; font-weight: bold; text-align: center; } ' +
      '.tr { text-align: right; } .tc { text-align: center; } .fb { font-weight: bold; } ' +
      '@media print { body { padding: 10mm; } @page { margin: 10mm; size: A4; } } ' +
      '</style></head><body>' + printContents + '</body></html>'
    );
    win.document.close();
    win.focus();
    setTimeout(() => { win.print(); win.close(); }, 400);
  };

  const handleExportPdf = async () => {
    if (!printRef.current || !voucher) return;
    setExporting(true);
    try {
      const sanitizedSoPhieu = (voucher.soPhieu || 'voucher').replace(/[\/\\?%*:|"<>]/g, '_');
      const filename = `Phieu_${type === 'IMPORT' ? 'Nhap' : 'Xuat'}_${sanitizedSoPhieu}.pdf`;
      await exportElementToPdf(printRef.current, {
        filename,
        orientation: 'portrait',
        marginMm: 8,
      });
    } catch (err) {
      console.error('Lỗi xuất file PDF:', err);
      alert('Không thể tạo file PDF. Vui lòng thử lại!');
    } finally {
      setExporting(false);
    }
  };

  if (!voucher) return null;

  const imp   = isImport(voucher) ? voucher : null;
  const exp   = !isImport(voucher) ? voucher : null;
  const date  = imp ? imp.ngayNhap : exp!.ngayXuat;
  const title = type === 'IMPORT' ? 'PHIẾU NHẬP KHO' : 'PHIẾU XUẤT KHO';
  const fmt   = (v: number) => new Intl.NumberFormat('vi-VN').format(v || 0);

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Xem trước & Xuất phiếu: ${voucher.soPhieu}`} maxWidth="900px">
      <div
        ref={printRef}
        style={{
          fontFamily: 'Times New Roman, serif',
          fontSize: 13,
          color: '#000',
          backgroundColor: '#ffffff',
          padding: '24px',
          lineHeight: 1.4,
        }}
      >
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 'bold' }}>NGÂN HÀNG THƯƠNG MẠI CỔ PHẦN SÀI GÒN CÔNG THƯƠNG</div>
          <div style={{ fontSize: 12, color: '#555' }}>Bộ phận: Phòng Kế toán - Kho vật tư &amp; Ấn chỉ</div>
          <h1 style={{ fontSize: 20, fontWeight: 'bold', margin: '12px 0 4px', textTransform: 'uppercase', letterSpacing: 1 }}>{title}</h1>
          <div style={{ fontWeight: 'bold', fontSize: 14 }}>Số phiếu: {voucher.soPhieu}</div>
        </div>

        {/* Info */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4px 20px', border: '1px solid #ccc', padding: 12, marginBottom: 14, fontSize: 13 }}>
          <div><strong>Ngày {type === 'IMPORT' ? 'nhập' : 'xuất'}:</strong> {date}</div>
          {imp && <>
            <div><strong>Nơi mua:</strong> {imp.noiMua || '---'}</div>
            <div><strong>Nơi nhập:</strong> {imp.noiNhap || 'Kho vật tư'}</div>
            <div><strong>Số hóa đơn:</strong> {imp.soHoaDon || '---'}</div>
            <div><strong>Ngày hóa đơn:</strong> {imp.ngayHoaDon || '---'}</div>
          </>}
          {exp && <>
            <div><strong>Nơi nhận:</strong> {exp.noiNhan || '---'}</div>
            <div><strong>Lý do xuất:</strong> {exp.lyDoXuat || '---'}</div>
          </>}
          <div><strong>TK Nợ / Có:</strong> {voucher.tkNo || '---'} / {voucher.tkCo || '---'}</div>
          <div><strong>Người lập:</strong> {voucher.creatorFullName || voucher.createdBy || '---'}</div>
        </div>

        {/* Detail Table */}
        <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 12 }}>
          <thead>
            <tr style={{ backgroundColor: '#f0f0f0' }}>
              <th style={{ border: '1px solid #333', padding: '6px 8px', textAlign: 'center', width: 45 }}>STT</th>
              <th style={{ border: '1px solid #333', padding: '6px 8px', textAlign: 'center', width: 140 }}>Mã số</th>
              <th style={{ border: '1px solid #333', padding: '6px 8px', textAlign: 'left' }}>Tên hàng hóa / ấn phẩm</th>
              <th style={{ border: '1px solid #333', padding: '6px 8px', textAlign: 'center', width: 65 }}>ĐVT</th>
              <th style={{ border: '1px solid #333', padding: '6px 8px', textAlign: 'right', width: 80 }}>Số lượng</th>
              <th style={{ border: '1px solid #333', padding: '6px 8px', textAlign: 'right', width: 110 }}>Đơn giá (đ)</th>
              <th style={{ border: '1px solid #333', padding: '6px 8px', textAlign: 'right', width: 120 }}>Thành tiền (đ)</th>
            </tr>
          </thead>
          <tbody>
            {voucher.details?.map((d, i) => (
              <tr key={i}>
                <td style={{ border: '1px solid #333', padding: '5px 8px', textAlign: 'center' }}>{i + 1}</td>
                <td style={{ border: '1px solid #333', padding: '5px 8px', fontWeight: 'bold' }}>{d.maSo}</td>
                <td style={{ border: '1px solid #333', padding: '5px 8px' }}>{d.tenHang}</td>
                <td style={{ border: '1px solid #333', padding: '5px 8px', textAlign: 'center' }}>{d.donViTinh}</td>
                <td style={{ border: '1px solid #333', padding: '5px 8px', textAlign: 'right' }}>{fmt(d.quantity)}</td>
                <td style={{ border: '1px solid #333', padding: '5px 8px', textAlign: 'right' }}>{fmt(d.unitPrice)}</td>
                <td style={{ border: '1px solid #333', padding: '5px 8px', textAlign: 'right', fontWeight: 'bold' }}>
                  {fmt(d.amount || d.quantity * d.unitPrice)}
                </td>
              </tr>
            ))}
            <tr style={{ background: '#f5f5f5' }}>
              <td colSpan={6} style={{ border: '1px solid #333', textAlign: 'right', fontWeight: 'bold', padding: '6px 8px' }}>CỘNG:</td>
              <td style={{ border: '1px solid #333', textAlign: 'right', fontWeight: 'bold', fontSize: 14, padding: '6px 8px' }}>
                {fmt(voucher.tongTien)}
              </td>
            </tr>
          </tbody>
        </table>

        {/* Amount in words */}
        <div style={{ border: '1px solid #ccc', padding: '8px 12px', marginBottom: 18, fontSize: 13 }}>
          <strong>Số tiền bằng chữ:</strong> <em>{numberToVietnamese(voucher.tongTien)}</em>
        </div>

        {/* Signatures */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12, marginTop: 24, textAlign: 'center' }}>
          {['Người lập phiếu', 'Thủ kho', 'Kế toán trưởng'].map((r) => (
            <div key={r}>
              <div style={{ fontWeight: 'bold', marginBottom: 55 }}>{r}</div>
              <div style={{ fontSize: 11 }}>(Ký, ghi rõ họ tên)</div>
            </div>
          ))}
        </div>
      </div>

      {/* Actions */}
      <div className="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
        <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={exporting}>
          Đóng
        </button>
        <button
          type="button"
          className="btn btn-outline-primary d-flex align-items-center gap-2"
          onClick={handlePrint}
          disabled={exporting}
          title="In trực tiếp bằng máy in"
        >
          <i className="bi bi-printer" aria-hidden="true" /> In phiếu
        </button>
        <button
          type="button"
          className="btn btn-primary d-flex align-items-center gap-2"
          onClick={handleExportPdf}
          disabled={exporting}
        >
          <i className={`bi ${exporting ? 'bi-hourglass-split' : 'bi-file-earmark-pdf'}`} aria-hidden="true" />
          {exporting ? 'Đang xuất PDF...' : 'Xuất File PDF'}
        </button>
      </div>
    </Modal>
  );
};
