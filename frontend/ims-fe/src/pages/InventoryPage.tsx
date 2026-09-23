import React, { useEffect, useState } from 'react';
import { inventoryService, productService } from '../services';
import type { InventoryItem, StockCard, ProductGroup } from '../types';
import { Modal } from '../components/common/Modal';

interface InventoryPageProps {
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
}

export const InventoryPage: React.FC<InventoryPageProps> = ({ showToast }) => {
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [groups, setGroups] = useState<ProductGroup[]>([]);
  const [loading, setLoading] = useState(false);

  const [searchMaSo, setSearchMaSo] = useState('');
  const [searchTenHang, setSearchTenHang] = useState('');
  const [selectedGroup, setSelectedGroup] = useState('');

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [stockCard, setStockCard] = useState<StockCard | null>(null);

  const loadInventory = async () => {
    try {
      setLoading(true);
      const res = await inventoryService.getInventory({
        maSo: searchMaSo,
        tenHang: searchTenHang,
        nhomHang: selectedGroup,
        page: currentPage,
        size: 15,
      });
      setInventory(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      showToast('Lỗi tải dữ liệu tồn kho: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    productService.getGroups().then(setGroups).catch(console.error);
  }, []);

  useEffect(() => {
    loadInventory();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage, selectedGroup]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    loadInventory();
  };

  const handleViewStockCard = async (productId: number) => {
    try {
      const card = await inventoryService.getStockCard(productId);
      setStockCard(card);
    } catch (err: any) {
      showToast('Lỗi tải sổ thẻ kho: ' + err.message, 'error');
    }
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
  };

  return (
    <div className="animate-fade-in">

      {/* Page Hero */}
      <div className="page-hero mb-4">
        <div className="d-flex align-items-center justify-content-between gap-3 flex-wrap">
          <div>
            <div className="page-hero-badge"><i className="bi bi-journal-text" aria-hidden="true" /> Tồn kho tức thời</div>
            <h2 className="mb-1">Tồn Kho &amp; Sổ Thẻ Kho</h2>
            <p>Theo dõi số lượng tồn, giá trị luân chuyển và chi tiết sổ thẻ kho từng mặt hàng — tổng <strong className="text-white">{totalElements}</strong> sản phẩm</p>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="card mb-4">
        <div className="card-body">
          <form onSubmit={handleSearch} className="d-flex gap-3 flex-wrap align-items-end">
            <div style={{ minWidth: 160, flex: '1 1 160px' }}>
              <label className="form-label">Mã hàng hóa</label>
              <input type="text" className="form-control" placeholder="VD: 32F.7100001.001" value={searchMaSo} onChange={e => setSearchMaSo(e.target.value)} />
            </div>
            <div style={{ minWidth: 240, flex: '2 1 240px' }}>
              <label className="form-label">Tên ấn phẩm</label>
              <input type="text" className="form-control" placeholder="Tìm theo tên..." value={searchTenHang} onChange={e => setSearchTenHang(e.target.value)} />
            </div>
            <div style={{ minWidth: 160, flex: '1 1 160px' }}>
              <label className="form-label">Nhóm hàng</label>
              <select className="form-select" value={selectedGroup} onChange={e => setSelectedGroup(e.target.value)}>
                <option value="">Tất cả nhóm</option>
                {groups.map(g => <option key={g.code} value={g.code}>{g.code} — {g.name}</option>)}
              </select>
            </div>
            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary fw-medium d-flex align-items-center gap-2">
                <i className="bi bi-search" aria-hidden="true" /> Lọc
              </button>
              <button type="button" className="btn btn-light" onClick={() => { setSearchMaSo(''); setSearchTenHang(''); setSelectedGroup(''); }}>
                <i className="bi bi-arrow-counterclockwise me-1" aria-hidden="true" />Xóa
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Inventory Table */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th style={{ width: '50px' }}>STT</th>
                <th>Mã hàng</th>
                <th>Tên hàng hóa</th>
                <th>Nhóm</th>
                <th>ĐVT</th>
                <th className="text-end">Tồn đầu</th>
                <th className="text-end">Nhập kỳ</th>
                <th className="text-end">Xuất kỳ</th>
                <th className="text-end" style={{ backgroundColor: '#E6F0FA', color: '#003366' }}>TỒN CUỐI</th>
                <th className="text-end">Đơn giá BQ</th>
                <th className="text-end">Thành tiền tồn</th>
                <th className="text-center" style={{ width: '100px' }}>Thẻ kho</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={12} className="text-center p-4 text-muted">
                    Đang tải bảng tồn kho...
                  </td>
                </tr>
              ) : inventory.length === 0 ? (
                <tr>
                  <td colSpan={12} className="text-center p-4 text-muted">
                    Không tìm thấy dữ liệu tồn kho nào
                  </td>
                </tr>
              ) : (
                inventory.map((item, idx) => (
                  <tr key={item.inventoryId}>
                    <td>{currentPage * 15 + idx + 1}</td>
                    <td className="fw-bold text-monospace" style={{ color: 'var(--sgb-navy-800)' }}>
                      {item.maSo}
                    </td>
                    <td className="fw-semibold">{item.tenHang}</td>
                    <td>
                      <span className="badge text-bg-secondary">{item.nhomHang}</span>
                    </td>
                    <td>{item.donViTinh}</td>
                    <td className="text-end">{item.openingQuantity}</td>
                    <td className="text-end fw-semibold text-success">+{item.importQuantity}</td>
                    <td className="text-end fw-semibold text-danger">-{item.exportQuantity}</td>
                    <td className="text-end fw-bold" style={{ backgroundColor: '#F0F6FC', color: 'var(--sgb-navy-900)' }}>
                      {item.endingQuantity}
                    </td>
                    <td className="text-end">{formatCurrency(item.averagePrice)}</td>
                    <td className="text-end fw-bold" style={{ color: 'var(--sgb-navy-900)' }}>
                      {formatCurrency(item.endingAmount)}
                    </td>
                    <td className="text-center">
                      <button
                        type="button"
                        className="btn btn-outline-info btn-sm fw-medium"
                        onClick={() => handleViewStockCard(item.productId)}
                      >
                        <i className="bi bi-book me-1" aria-hidden="true" />Sổ thẻ
                      </button>
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

      {/* Modal: Sổ Thẻ Kho */}
      <Modal
        isOpen={!!stockCard}
        onClose={() => setStockCard(null)}
        title={`Sổ Thẻ Kho: [${stockCard?.maSo || ''}] ${stockCard?.tenHang || ''}`}
        maxWidth="950px"
      >
        {stockCard && (
          <div>
            <div className="d-flex justify-content-between align-items-center bg-light p-3 rounded mb-3">
              <div>
                <strong>Nhóm:</strong> {stockCard.nhomHang} | <strong>Đơn vị tính:</strong> {stockCard.donViTinh}
              </div>
              <div className="small text-muted">
                Ngân hàng TMCP Sài Gòn Công Thương — Sổ theo dõi chi tiết
              </div>
            </div>

            <div className="table-responsive border rounded mb-4">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Ngày</th>
                    <th>Số phiếu</th>
                    <th>Loại giao dịch</th>
                    <th className="text-end">SL Nhập</th>
                    <th className="text-end">Tiền Nhập</th>
                    <th className="text-end">SL Xuất</th>
                    <th className="text-end">Tiền Xuất</th>
                    <th className="text-end" style={{ backgroundColor: '#E6F0FA' }}>TỒN SL</th>
                    <th className="text-end" style={{ backgroundColor: '#E6F0FA' }}>TỒN TIỀN</th>
                    <th>Ghi chú</th>
                  </tr>
                </thead>
                <tbody>
                  {stockCard.entries?.length === 0 ? (
                    <tr>
                      <td colSpan={10} className="text-center p-4 text-muted">
                        Chưa có lịch sử giao dịch
                      </td>
                    </tr>
                  ) : (
                    stockCard.entries?.map((e) => (
                      <tr key={e.id}>
                        <td>{e.entryDate}</td>
                        <td className="fw-bold text-monospace">{e.soPhieu || '—'}</td>
                        <td>
                          <span className={`badge rounded-pill ${
                            e.tranType === 'OPEN' ? 'text-bg-info' : e.tranType === 'IMPORT' ? 'text-bg-success' : 'text-bg-warning'
                          }`}>
                            {e.tranType === 'OPEN' ? 'Mở sổ' : e.tranType === 'IMPORT' ? 'Nhập kho' : 'Xuất kho'}
                          </span>
                        </td>
                        <td className="text-end fw-semibold text-success">
                          {e.importQuantity > 0 ? `+${e.importQuantity}` : '—'}
                        </td>
                        <td className="text-end">
                          {e.importAmount > 0 ? formatCurrency(e.importAmount) : '—'}
                        </td>
                        <td className="text-end fw-semibold text-danger">
                          {e.exportQuantity > 0 ? `-${e.exportQuantity}` : '—'}
                        </td>
                        <td className="text-end">
                          {e.exportAmount > 0 ? formatCurrency(e.exportAmount) : '—'}
                        </td>
                        <td className="text-end fw-bold" style={{ backgroundColor: '#F0F6FC' }}>
                          {e.balanceQuantity}
                        </td>
                        <td className="text-end fw-bold" style={{ backgroundColor: '#F0F6FC', color: 'var(--sgb-navy-900)' }}>
                          {formatCurrency(e.balanceAmount)}
                        </td>
                        <td className="small text-secondary">{e.note || '—'}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="d-flex justify-content-end">
              <button
                type="button"
                className="btn btn-primary px-4"
                onClick={() => setStockCard(null)}
              >
                Đóng
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
