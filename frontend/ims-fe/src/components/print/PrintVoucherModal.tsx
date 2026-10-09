import React, { useRef } from 'react';
import type { ImportDoc, ExportDoc } from '../../types';
import { numberToVietnamese } from '../../utils/numberToVietnamese';
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

  if (!voucher) return null;

  const imp   = isImport(voucher) ? voucher : null;
  const exp   = !isImport(voucher) ? voucher : null;
  const date  = imp ? imp.ngayNhap : exp!.ngayXuat;
  const title = type === 'IMPORT' ? 'PHIEU NHAP KHO' : 'PHIEU XUAT KHO';
  const fmt   = (v: number) => new Intl.NumberFormat('vi-VN').format(v || 0);

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={"Xem truoc phieu: " + voucher.soPhieu} maxWidth="900px">
      <div ref={printRef} style={{ fontFamily: 'Times New Roman,serif', fontSize: 13, color: '#000' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 16 }}>
          <div style={{ fontSize: 12 }}>NGAN HANG THUONG MAI CO PHAN SAI GON CONG THUONG</div>
          <div style={{ fontSize: 11, color: '#666' }}>Bo phan: Phong Ke toan - Kho vat tu</div>
          <h1 style={{ fontSize: 20, fontWeight: 'bold', margin: '10px 0 4px', textTransform: 'uppercase', letterSpacing: 1 }}>{title}</h1>
          <div style={{ fontWeight: 'bold', fontSize: 14 }}>So phieu: {voucher.soPhieu}</div>
        </div>

        {/* Info */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '3px 20px', border: '1px solid #ccc', padding: 10, marginBottom: 12 }}>
          <div><strong>Ngay {type === 'IMPORT' ? 'nhap' : 'xuat'}:</strong> {date}</div>
          {imp && <><div><strong>Noi mua:</strong> {imp.noiMua || '---'}</div>
          <div><strong>Noi nhap:</strong> {imp.noiNhap || 'Kho vat tu'}</div>
          <div><strong>So hoa don:</strong> {imp.soHoaDon || '---'}</div>
          <div><strong>Ngay hoa don:</strong> {imp.ngayHoaDon || '---'}</div></>}
          {exp && <><div><strong>Noi nhan:</strong> {exp.noiNhan || '---'}</div>
          <div><strong>Ly do xuat:</strong> {exp.lyDoXuat || '---'}</div></>}
          <div><strong>TK No / Co:</strong> {voucher.tkNo || '---'} / {voucher.tkCo || '---'}</div>
          <div><strong>Nguoi lap:</strong> {voucher.creatorFullName || voucher.createdBy || '---'}</div>
        </div>

        {/* Detail Table */}
        <table>
          <thead>
            <tr>
              <th style={{ textAlign: 'center' }}>STT</th>
              <th style={{ textAlign: 'center' }}>Ma so</th>
              <th>Ten hang hoa / an pham</th>
              <th style={{ textAlign: 'center' }}>DVT</th>
              <th style={{ textAlign: 'center' }}>So luong</th>
              <th style={{ textAlign: 'right' }}>Don gia</th>
              <th style={{ textAlign: 'right' }}>Thanh tien</th>
            </tr>
          </thead>
          <tbody>
            {voucher.details?.map((d, i) => (
              <tr key={i}>
                <td style={{ textAlign: 'center' }}>{i + 1}</td>
                <td style={{ fontWeight: 'bold' }}>{d.maSo}</td>
                <td>{d.tenHang}</td>
                <td style={{ textAlign: 'center' }}>{d.donViTinh}</td>
                <td style={{ textAlign: 'right' }}>{d.quantity}</td>
                <td style={{ textAlign: 'right' }}>{fmt(d.unitPrice)}</td>
                <td style={{ textAlign: 'right', fontWeight: 'bold' }}>{fmt(d.amount || d.quantity * d.unitPrice)}</td>
              </tr>
            ))}
            <tr style={{ background: '#f5f5f5' }}>
              <td colSpan={6} style={{ textAlign: 'right', fontWeight: 'bold', padding: '5px 8px' }}>CONG:</td>
              <td style={{ textAlign: 'right', fontWeight: 'bold', fontSize: 14, padding: '5px 8px' }}>{fmt(voucher.tongTien)}</td>
            </tr>
          </tbody>
        </table>

        {/* Amount in words */}
        <div style={{ border: '1px solid #ccc', padding: '6px 10px', marginBottom: 16 }}>
          <strong>So tien bang chu:</strong> <em>{numberToVietnamese(voucher.tongTien)}</em>
        </div>

        {/* Signatures */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginTop: 24, textAlign: 'center' }}>
          {['Nguoi lap phieu', 'Thu kho', 'Ke toan truong'].map((r) => (
            <div key={r}>
              <div style={{ fontWeight: 'bold', marginBottom: 60 }}>{r}</div>
              <div style={{ fontSize: 11 }}>(Ky, ho ten)</div>
            </div>
          ))}
        </div>
      </div>

      {/* Actions */}
      <div className="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
        <button type="button" className="btn btn-outline-secondary" onClick={onClose}>Dong</button>
        <button type="button" className="btn btn-primary d-flex align-items-center gap-2" onClick={handlePrint}>
          <i className="bi bi-printer" aria-hidden="true" /> In phieu
        </button>
      </div>
    </Modal>
  );
};
