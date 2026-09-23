import React, { useEffect, useState } from 'react';
import { productService } from '../services';
import type { Product, ProductGroup, Unit, CreateProductRequest } from '../types';
import { Modal } from '../components/common/Modal';

interface ProductsPageProps {
  userRoles: string[];
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
}

const PRODUCT_PREFIX_MAP: Record<string, string> = {
  ACTT: '32F.9020100.',
  VPP: '32F.7100001.',
  ACQT: '32F.5010000.',
  VLK: '32F.9080117.',
};

const getProductPrefix = (nhomHang: string): string => {
  const upper = (nhomHang || '').trim().toUpperCase();
  if (upper.startsWith('32F.')) {
    return upper.endsWith('.') ? upper : `${upper}.`;
  }
  return PRODUCT_PREFIX_MAP[upper] || `${upper}.`;
};

export const ProductsPage: React.FC<ProductsPageProps> = ({ userRoles, showToast }) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [groups, setGroups] = useState<ProductGroup[]>([]);
  const [units, setUnits] = useState<Unit[]>([]);
  const [loading, setLoading] = useState(false);

  const [searchMaSo, setSearchMaSo] = useState('');
  const [searchTenHang, setSearchTenHang] = useState('');
  const [selectedGroup, setSelectedGroup] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newTenHang, setNewTenHang] = useState('');
  const [newNhomHang, setNewNhomHang] = useState('VPP');
  const [newDonViTinh, setNewDonViTinh] = useState('');
  const [newDonGiaNhap, setNewDonGiaNhap] = useState<number>(0);
  const [submitting, setSubmitting] = useState(false);
  const [nameWarning, setNameWarning] = useState<string | null>(null);
  const [migrating, setMigrating] = useState(false);

  const isAdmin = userRoles.includes('ADMIN') || userRoles.includes('ROLE_ADMIN');
  const hasLegacyCodes = products.some((p) => p.maSo && !p.maSo.startsWith('32F.'));

  const handleMigrateCodes = async () => {
    try {
      setMigrating(true);
      const res = await productService.migrateCodes();
      showToast(res?.message || 'Đã chuẩn hóa toàn bộ mã hàng!', 'success');
      loadProducts();
    } catch (err: any) {
      showToast('Lỗi khi chuẩn hóa mã: ' + err.message, 'error');
    } finally {
      setMigrating(false);
    }
  };

  const handleOpenCreateModal = () => {
    const defaultGroup = newNhomHang || (groups.length > 0 ? groups[0].code : 'VPP');
    setNewNhomHang(defaultGroup);
    setNewTenHang('');
    setNewDonGiaNhap(0);
    setNameWarning(null);
    setIsModalOpen(true);
  };

  const loadProducts = async () => {
    try {
      setLoading(true);
      const res = await productService.getProducts({
        page: currentPage,
        size: 10,
        maSo: searchMaSo || undefined,
        tenHang: searchTenHang || undefined,
        nhomHang: selectedGroup || undefined,
        trangThai: selectedStatus || undefined,
      });
      setProducts(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      showToast('Không thể tải danh sách sản phẩm: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, [currentPage, selectedGroup, selectedStatus]);

  useEffect(() => {
    const loadLookups = async () => {
      try {
        const [gList, uList] = await Promise.all([
          productService.getGroups(),
          productService.getUnits(),
        ]);
        setGroups(gList);
        setUnits(uList);
        if (gList.length > 0) setNewNhomHang(gList[0].code);
        if (uList.length > 0) setNewDonViTinh(uList[0].name);
      } catch (err: any) {
        showToast('Lỗi tải dữ liệu tra cứu: ' + err.message, 'warning');
      }
    };
    loadLookups();
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    loadProducts();
  };

  const handleCreateProduct = async (e: React.FormEvent, forceSave?: boolean) => {
    e.preventDefault();
    if (!newTenHang.trim() || !newNhomHang || !newDonViTinh) {
      showToast('Vui lòng nhập đầy đủ các trường bắt buộc (Tên hàng, Nhóm hàng, Đơn vị tính)', 'warning');
      return;
    }

    try {
      setSubmitting(true);

      // Tự động sinh mã hàng hóa theo quy chuẩn:
      // ACTT: 32F.9020100.STT | VPP: 32F.7100001.STT | ACQT: 32F.5010000.STT | VLK: 32F.9080117.STT
      let generatedMaSo = '';
      try {
        const nextCodeRes = await productService.getNextCode(newNhomHang);
        if (nextCodeRes && nextCodeRes.maSo) {
          generatedMaSo = nextCodeRes.maSo;
        }
      } catch {
        // Fallback nếu backend chưa phản hồi
      }

      if (!generatedMaSo) {
        try {
          const prodsRes = await productService.getProducts({ nhomHang: newNhomHang, size: 500 });
          const items = prodsRes?.content || [];
          const prefix = getProductPrefix(newNhomHang);
          let maxSeq = 0;
          for (const item of items) {
            if (!item.maSo) continue;
            const code = item.maSo.trim().toUpperCase();
            if (code.startsWith(prefix.toUpperCase())) {
              const remainder = code.slice(prefix.length);
              const match = remainder.match(/(\d+)$/);
              if (match) {
                const num = parseInt(match[1], 10);
                if (num > maxSeq) maxSeq = num;
              }
            }
          }
          generatedMaSo = `${prefix}${String(maxSeq + 1).padStart(3, '0')}`;
        } catch {
          generatedMaSo = `${getProductPrefix(newNhomHang)}001`;
        }
      }

      const req: CreateProductRequest = {
        maSo: generatedMaSo,
        tenHang: newTenHang.trim(),
        nhomHang: newNhomHang,
        donViTinh: newDonViTinh,
        donGiaNhap: Number(newDonGiaNhap) || 0,
        forceSaveIfDuplicateName: forceSave || false,
      };
      const created = await productService.createProduct(req);

      if (created.nameWarning && !forceSave) {
        // Cảnh báo tên trùng: hiển thị trong modal, chưa đóng
        setNameWarning(created.warningMessage || 'Cảnh báo: Có sản phẩm có tên tương tự.');
      } else {
        setNameWarning(null);
        showToast(`Thêm hàng hóa mới thành công! Mã hàng tự cấp: ${created.maSo}`, 'success');
        setIsModalOpen(false);
        setNewTenHang('');
        setNewDonGiaNhap(0);
        loadProducts();
      }
    } catch (err: any) {
      showToast('Không thể thêm sản phẩm: ' + err.message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeactivate = async (id: number, maSo: string) => {
    if (!window.confirm(`Bạn có chắc chắn muốn ngừng sử dụng mã hàng [${maSo}]?`)) return;
    try {
      await productService.deactivateProduct(id);
      showToast(`Đã ngừng sử dụng mã hàng ${maSo}`, 'success');
      loadProducts();
    } catch (err: any) {
      showToast('Lỗi: ' + err.message, 'error');
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
            <div className="page-hero-badge"><i className="bi bi-box-seam" aria-hidden="true" /> Danh mục hàng hóa</div>
            <h2 className="mb-1">Danh Mục Ấn Phẩm &amp; Văn Phòng Phẩm</h2>
            <p>Quản lý danh sách mã ấn chỉ, tài sản, VPP &amp; vật liệu khác — tổng <strong className="text-white">{totalElements}</strong> bản ghi</p>
          </div>
          {isAdmin && (
            <div className="col-auto">
              <button
                type="button"
                className="btn btn-gold fw-semibold d-flex align-items-center gap-2 rounded-pill px-4"
                style={{ boxShadow: '0 4px 14px rgba(34,197,94,0.4)' }}
                onClick={handleOpenCreateModal}
              >
                <i className="bi bi-plus-circle" aria-hidden="true" /> Thêm Hàng Hóa Mới
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Banner cảnh báo nếu còn mã hàng cũ */}
      {hasLegacyCodes && (
        <div className="alert alert-warning border-warning shadow-sm d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 py-3 px-4 rounded-3" role="alert">
          <div className="d-flex align-items-center gap-3">
            <i className="bi bi-exclamation-triangle-fill text-warning fs-3" aria-hidden="true" />
            <div>
              <strong className="fs-6 text-dark">Phát hiện mã hàng cũ chưa theo chuẩn mới (32F.xxxxxxx.STT)</strong>
              <div className="text-muted small mt-1">
                Các mã như <code>ACQT-002</code>, <code>VPP-003</code>,... cần được chuyển sang chuẩn tài khoản kế toán Saigonbank. Bạn có thể nhấn nút để hệ thống tự động cập nhật ngay.
              </div>
            </div>
          </div>
          <button
            type="button"
            className="btn btn-warning fw-semibold px-4 py-2 rounded-pill shadow-sm d-flex align-items-center gap-2"
            onClick={handleMigrateCodes}
            disabled={migrating}
          >
            <i className={`bi ${migrating ? 'bi-arrow-repeat spin' : 'bi-magic'}`} aria-hidden="true" />
            {migrating ? 'Đang chuẩn hóa...' : 'Chuẩn hóa toàn bộ mã 32F'}
          </button>
        </div>
      )}

      {/* Filter Bar */}
      <div className="card mb-4">
        <div className="card-body">
          <form onSubmit={handleSearch} className="d-flex gap-3 flex-wrap align-items-end">
            <div style={{ minWidth: 160, flex: '1 1 160px' }}>
              <label className="form-label">Mã số hàng</label>
              <input type="text" className="form-control" placeholder="VD: 32F.7100001.001" value={searchMaSo} onChange={e => setSearchMaSo(e.target.value)} />
            </div>
            <div style={{ minWidth: 240, flex: '2 1 240px' }}>
              <label className="form-label">Tên hàng hóa</label>
              <input type="text" className="form-control" placeholder="Tìm theo tên ấn phẩm..." value={searchTenHang} onChange={e => setSearchTenHang(e.target.value)} />
            </div>
            <div style={{ minWidth: 150, flex: '1 1 150px' }}>
              <label className="form-label">Nhóm hàng</label>
              <select className="form-select" value={selectedGroup} onChange={e => setSelectedGroup(e.target.value)}>
                <option value="">Tất cả nhóm</option>
                {groups.map(g => <option key={g.code} value={g.code}>{g.code} — {g.name}</option>)}
              </select>
            </div>
            <div style={{ minWidth: 140, flex: '1 1 140px' }}>
              <label className="form-label">Trạng thái</label>
              <select className="form-select" value={selectedStatus} onChange={e => setSelectedStatus(e.target.value)}>
                <option value="">Tất cả</option>
                <option value="HOAT_DONG">Đang hoạt động</option>
                <option value="NGUNG_SU_DUNG">Ngưng sử dụng</option>
              </select>
            </div>
            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary fw-medium d-flex align-items-center gap-2">
                <i className="bi bi-search" aria-hidden="true" /> Tìm kiếm
              </button>
              <button type="button" className="btn btn-light" onClick={() => { setSearchMaSo(''); setSearchTenHang(''); setSelectedGroup(''); setSelectedStatus(''); }}>
                <i className="bi bi-arrow-counterclockwise me-1" aria-hidden="true" />Xóa bộ lọc
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Product Data Table */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th style={{ width: '60px' }}>STT</th>
                <th>Mã hàng hóa</th>
                <th>Tên ấn phẩm / VPP</th>
                <th>Nhóm hàng</th>
                <th>ĐVT</th>
                <th className="text-end">Đơn giá nhập chuẩn</th>
                <th>Trạng thái</th>
                {isAdmin && <th className="text-center" style={{ width: '100px' }}>Thao tác</th>}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={isAdmin ? 8 : 7} className="text-center p-4 text-muted">
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : products.length === 0 ? (
                <tr>
                  <td colSpan={isAdmin ? 8 : 7} className="text-center p-4 text-muted">
                    Không tìm thấy hàng hóa nào phù hợp
                  </td>
                </tr>
              ) : (
                products.map((item, idx) => (
                  <tr key={item.id}>
                    <td>{currentPage * 15 + idx + 1}</td>
                    <td className="fw-bold text-monospace" style={{ color: 'var(--sgb-navy-800)' }}>
                      {item.maSo}
                    </td>
                    <td className="fw-semibold">{item.tenHang}</td>
                    <td>
                      <span className="badge text-bg-secondary me-1">{item.nhomHang}</span>
                      {item.nhomHangTen && <span className="small text-muted">({item.nhomHangTen})</span>}
                    </td>
                    <td>{item.donViTinh}</td>
                    <td className="text-end fw-semibold">
                      {formatCurrency(item.donGiaNhap)}
                    </td>
                    <td>
                      <span className={`badge rounded-pill ${item.trangThai === 'HOAT_DONG' ? 'text-bg-success' : 'text-bg-danger'}`}>
                        {item.trangThai === 'HOAT_DONG' ? 'Hoạt động' : 'Ngừng sử dụng'}
                      </span>
                    </td>
                    {isAdmin && (
                      <td className="text-center">
                        {item.trangThai === 'HOAT_DONG' && (
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            title="Ngừng sử dụng mã này"
                            onClick={() => handleDeactivate(item.id, item.maSo)}
                          >
                            Ngừng
                          </button>
                        )}
                      </td>
                    )}
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

      {/* Modal: Thêm Hàng Hóa Mới */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => { setIsModalOpen(false); setNameWarning(null); }}
        title="Thêm Hàng Hóa Mới vào Hệ Thống"
      >
        <form onSubmit={(e) => handleCreateProduct(e)}>
          <div className="row g-3 mb-3">
            <div className="col-md-6">
              <label className="form-label fw-medium small text-secondary">Nhóm hàng (*):</label>
              <select
                className="form-select"
                value={newNhomHang}
                onChange={(e) => setNewNhomHang(e.target.value)}
                required
              >
                {groups.map((g) => (
                  <option key={g.code} value={g.code}>
                    {g.code} - {g.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-md-6">
              <label className="form-label fw-medium small text-secondary">Đơn vị tính (*):</label>
              <select
                className="form-select"
                value={newDonViTinh}
                onChange={(e) => setNewDonViTinh(e.target.value)}
                required
              >
                {units.map((u) => (
                  <option key={u.name} value={u.name}>
                    {u.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="mb-3">
            <label className="form-label fw-medium small text-secondary">Tên hàng hóa / Ấn phẩm (*):</label>
            <input
              type="text"
              className="form-control"
              placeholder="VD: Giấy in A4 Double A 70gsm"
              value={newTenHang}
              onChange={(e) => setNewTenHang(e.target.value)}
              required
            />
          </div>

          <div className="mb-3">
            <label className="form-label fw-medium small text-secondary">Đơn giá nhập dự kiến (VND):</label>
            <input
              type="number"
              className="form-control"
              placeholder="0"
              value={newDonGiaNhap}
              onChange={(e) => setNewDonGiaNhap(Number(e.target.value))}
              min="0"
            />
          </div>

          {/* Cảnh báo tên trùng / gần giống */}
          {nameWarning && (
            <div className="alert alert-warning border-warning py-3 px-3 mb-3" role="alert">
              <div className="d-flex align-items-start gap-2">
                <i className="bi bi-exclamation-triangle-fill text-warning mt-1" aria-hidden="true" />
                <div className="flex-1">
                  <strong>⚠️ Cảnh báo tên trùng:</strong>
                  <div className="mt-1 small">{nameWarning}</div>
                  <div className="d-flex gap-2 mt-2">
                    <button
                      type="button"
                      className="btn btn-warning btn-sm fw-semibold"
                      disabled={submitting}
                      onClick={(e) => handleCreateProduct(e as any, true)}
                    >
                      Lưu vẫn tên này
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm"
                      onClick={() => setNameWarning(null)}
                    >
                      Sửa tên
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className="d-flex justify-content-end gap-2">
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={() => setIsModalOpen(false)}
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              className="btn btn-gold"
              disabled={submitting}
            >
              {submitting ? 'Đang lưu...' : 'Lưu Hàng Hóa'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
