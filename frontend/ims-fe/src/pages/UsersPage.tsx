import React, { useEffect, useState, useMemo } from 'react';
import { userService } from '../services';
import type { UserAccount, CreateUserRequest, UpdateUserRequest } from '../types';
import { Modal } from '../components/common/Modal';

interface UsersPageProps {
  userRoles: string[];
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
}

const AVAILABLE_ROLES = [
  {
    key: 'USER',
    label: 'Nhân viên Quản lý Kho',
    badgeClass: 'bg-info bg-opacity-10 text-primary border border-info border-opacity-25',
    desc: 'Lập & theo dõi phiếu nhập kho, xuất kho, xem tồn kho & sổ thẻ kho.',
  },
  {
    key: 'CONTROLLER',
    label: 'Kiểm soát viên',
    badgeClass: 'bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25',
    desc: 'Kiểm soát số liệu kế toán và thực hiện khóa / mở khóa chốt sổ kỳ.',
  },
  {
    key: 'ADMIN',
    label: 'Quản trị hệ thống',
    badgeClass: 'bg-warning bg-opacity-25 text-dark border border-warning border-opacity-50 fw-bold',
    desc: 'Toàn quyền cấu hình danh mục hàng hóa, đăng ký và phân quyền nhân viên.',
  },
];

export const UsersPage: React.FC<UsersPageProps> = ({ userRoles, showToast }) => {
  const isAdmin = userRoles.includes('ADMIN');

  const [users, setUsers] = useState<UserAccount[]>([]);
  const [loading, setLoading] = useState(false);

  const [searchTerm, setSearchTerm] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<string>('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [newFullName, setNewFullName] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newPhone, setNewPhone] = useState('');
  const [newRoles, setNewRoles] = useState<string[]>(['USER']);
  const [createSubmitting, setCreateSubmitting] = useState(false);

  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserAccount | null>(null);
  const [editFullName, setEditFullName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editPhone, setEditPhone] = useState('');
  const [editIsActive, setEditIsActive] = useState(true);
  const [editRoles, setEditRoles] = useState<string[]>([]);
  const [editSubmitting, setEditSubmitting] = useState(false);

  const [isResetOpen, setIsResetOpen] = useState(false);
  const [resetTargetUser, setResetTargetUser] = useState<UserAccount | null>(null);
  const [resetNewPassword, setResetNewPassword] = useState('');
  const [showResetPassword, setShowResetPassword] = useState(false);
  const [resetSubmitting, setResetSubmitting] = useState(false);

  const [togglingId, setTogglingId] = useState<number | null>(null);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const isActiveParam = selectedStatus === 'active' ? true : selectedStatus === 'inactive' ? false : undefined;
      const res = await userService.getUsers({
        search: searchTerm,
        isActive: isActiveParam,
        page: currentPage,
        size: 12,
      });
      setUsers(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast('Lỗi tải danh sách người dùng: ' + msg, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage, selectedStatus]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    loadUsers();
  };

  const handleClearFilter = () => {
    setSearchTerm('');
    setSelectedStatus('');
    setCurrentPage(0);
  };

  const generateRandomPassword = () => {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$';
    let pwd = 'Sgb@';
    for (let i = 0; i < 6; i++) {
      pwd += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return pwd;
  };

  const handleOpenCreate = () => {
    setNewUsername('');
    setNewPassword(generateRandomPassword());
    setShowPassword(true);
    setNewFullName('');
    setNewEmail('');
    setNewPhone('');
    setNewRoles(['USER']);
    setIsCreateOpen(true);
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUsername.trim()) {
      showToast('Vui lòng nhập tên đăng nhập', 'warning');
      return;
    }
    if (!newPassword.trim() || newPassword.length < 6) {
      showToast('Mật khẩu phải có ít nhất 6 ký tự', 'warning');
      return;
    }
    if (!newFullName.trim()) {
      showToast('Vui lòng nhập họ tên nhân viên', 'warning');
      return;
    }
    if (newRoles.length === 0) {
      showToast('Vui lòng chọn ít nhất 1 vai trò (role) cho nhân viên', 'warning');
      return;
    }

    try {
      setCreateSubmitting(true);
      const req: CreateUserRequest = {
        username: newUsername.trim(),
        password: newPassword.trim(),
        fullName: newFullName.trim(),
        email: newEmail.trim() || undefined,
        phone: newPhone.trim() || undefined,
        roles: newRoles,
      };
      await userService.createUser(req);
      showToast(`Cấp tài khoản thành công cho "${newFullName}" (${newUsername})!`, 'success');
      setIsCreateOpen(false);
      loadUsers();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast('Lỗi tạo tài khoản: ' + msg, 'error');
    } finally {
      setCreateSubmitting(false);
    }
  };

  const handleOpenEdit = (u: UserAccount) => {
    setEditingUser(u);
    setEditFullName(u.fullName || '');
    setEditEmail(u.email || '');
    setEditPhone(u.phone || '');
    setEditIsActive(Boolean(u.isActive));
    setEditRoles([...u.roles]);
    setIsEditOpen(true);
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;
    if (!editFullName.trim()) {
      showToast('Vui lòng nhập họ tên nhân viên', 'warning');
      return;
    }
    if (editRoles.length === 0) {
      showToast('Vui lòng chọn ít nhất 1 vai trò cho nhân viên', 'warning');
      return;
    }

    try {
      setEditSubmitting(true);
      const req: UpdateUserRequest = {
        fullName: editFullName.trim(),
        email: editEmail.trim() || undefined,
        phone: editPhone.trim() || undefined,
        isActive: editIsActive,
        roles: editRoles,
      };
      await userService.updateUser(editingUser.id, req);
      showToast('Cập nhật thông tin & phân quyền thành công!', 'success');
      setIsEditOpen(false);
      loadUsers();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast('Lỗi cập nhật tài khoản: ' + msg, 'error');
    } finally {
      setEditSubmitting(false);
    }
  };

  const handleToggleStatus = async (u: UserAccount) => {
    if (u.username === 'admin') {
      showToast('Không thể khóa tài khoản quản trị viên chính (admin)', 'warning');
      return;
    }
    const actionName = u.isActive ? 'tạm khóa' : 'kích hoạt';
    if (!window.confirm(`Bạn có chắc chắn muốn ${actionName} tài khoản "${u.username}" (${u.fullName})?`)) {
      return;
    }

    try {
      setTogglingId(u.id);
      const res = await userService.toggleStatus(u.id);
      const msg = res.isActive ? 'Đã kích hoạt tài khoản thành công!' : 'Đã tạm khóa tài khoản thành công!';
      showToast(msg, res.isActive ? 'success' : 'info');
      loadUsers();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast('Lỗi thay đổi trạng thái: ' + msg, 'error');
    } finally {
      setTogglingId(null);
    }
  };

  const handleOpenReset = (u: UserAccount) => {
    setResetTargetUser(u);
    setResetNewPassword(generateRandomPassword());
    setShowResetPassword(true);
    setIsResetOpen(true);
  };

  const handleResetSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetTargetUser) return;
    if (!resetNewPassword.trim() || resetNewPassword.length < 6) {
      showToast('Mật khẩu mới phải có ít nhất 6 ký tự', 'warning');
      return;
    }

    try {
      setResetSubmitting(true);
      await userService.resetPassword(resetTargetUser.id, resetNewPassword.trim());
      showToast(`Đã đặt lại mật khẩu thành công cho tài khoản "${resetTargetUser.username}"!`, 'success');
      setIsResetOpen(false);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast('Lỗi đặt lại mật khẩu: ' + msg, 'error');
    } finally {
      setResetSubmitting(false);
    }
  };

  const stats = useMemo(() => {
    const active = users.filter((u) => u.isActive).length;
    const inactive = users.filter((u) => !u.isActive).length;
    const admins = users.filter((u) => u.roles.includes('ADMIN')).length;
    const controllers = users.filter((u) => u.roles.includes('CONTROLLER')).length;
    const staff = users.filter((u) => u.roles.includes('USER')).length;
    return { active, inactive, admins, controllers, staff };
  }, [users]);

  const toggleRoleInList = (role: string, currentList: string[], setter: (val: string[]) => void) => {
    if (currentList.includes(role)) {
      if (currentList.length === 1) {
        showToast('Tài khoản phải có ít nhất 1 quyền hạn', 'warning');
        return;
      }
      setter(currentList.filter((r) => r !== role));
    } else {
      setter([...currentList, role]);
    }
  };

  if (!isAdmin) {
    return (
      <div className="animate-fade-in text-center py-5">
        <div className="card shadow-sm border-0 mx-auto p-5" style={{ maxWidth: '500px' }}>
          <div className="fs-1 mb-3"><i className="bi bi-shield-lock" aria-hidden="true" /></div>
          <h4 className="fw-bold text-danger mb-2">Quyền Truy Cập Bị Hạn Chế</h4>
          <p className="text-muted mb-0">
            Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang quản lý tài khoản và phân quyền nhân viên.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      {/* Page Hero */}
      <div className="page-hero mb-4">
        <div className="row align-items-center g-3">
          <div className="col">
            <div className="page-hero-badge"><i className="bi bi-person-gear" aria-hidden="true" /> Quản trị người dùng</div>
            <h2 className="mb-1">Quản Lý Tài Khoản &amp; Phân Quyền Nhân Viên</h2>
            <p>
              Đăng ký tài khoản nhân viên mới, phân quyền truy cập chức năng hệ thống (USER, CONTROLLER, ADMIN) — tổng{' '}
              <strong className="text-white">{totalElements}</strong> tài khoản
            </p>
          </div>
          <div className="col-auto">
            <button
              type="button"
              className="btn btn-gold fw-semibold d-flex align-items-center gap-2 rounded-pill px-4"
              style={{ boxShadow: '0 4px 14px rgba(34,197,94,0.4)' }}
              onClick={handleOpenCreate}
            >
              <i className="bi bi-person-plus" aria-hidden="true" /> Cấp Tài Khoản Mới
            </button>
          </div>
        </div>
      </div>

      {/* KPI Stats Overview */}
      <div className="row g-3 mb-4">
        <div className="col-12 col-sm-6 col-xl-3">
          <div className="card border-0 shadow-sm h-100 p-3">
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <span className="text-muted small text-uppercase fw-semibold" style={{ fontSize: '0.72rem' }}>
                  Tổng tài khoản
                </span>
                <h3 className="fw-bold mb-0 mt-1" style={{ color: 'var(--sgb-navy-800)' }}>
                  {totalElements}
                </h3>
              </div>
              <div
                className="rounded-3 d-flex align-items-center justify-content-center"
                style={{ width: 44, height: 44, background: 'var(--sgb-navy-50)', color: 'var(--sgb-navy-600)' }}
              >
                <i className="bi bi-people" aria-hidden="true" />
              </div>
            </div>
            <div className="mt-2 small text-muted" style={{ fontSize: '0.78rem' }}>
              Trang hiện tại: {stats.active} hoạt động / {stats.inactive} tạm khóa
            </div>
          </div>
        </div>

        <div className="col-12 col-sm-6 col-xl-3">
          <div className="card border-0 shadow-sm h-100 p-3">
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <span className="text-muted small text-uppercase fw-semibold" style={{ fontSize: '0.72rem' }}>
                  Nhân viên kho (USER)
                </span>
                <h3 className="fw-bold mb-0 mt-1 text-primary">
                  {stats.staff}
                </h3>
              </div>
              <div
                className="rounded-3 d-flex align-items-center justify-content-center"
                style={{ width: 44, height: 44, background: '#E0F2FE', color: '#0284C7' }}
              >
                <i className="bi bi-box-seam" aria-hidden="true" />
              </div>
            </div>
            <div className="mt-2 small text-muted" style={{ fontSize: '0.78rem' }}>
              Quyền nhập/xuất/sổ thẻ kho
            </div>
          </div>
        </div>

        <div className="col-12 col-sm-6 col-xl-3">
          <div className="card border-0 shadow-sm h-100 p-3">
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <span className="text-muted small text-uppercase fw-semibold" style={{ fontSize: '0.72rem' }}>
                  Kiểm soát viên (CONTROLLER)
                </span>
                <h3 className="fw-bold mb-0 mt-1" style={{ color: '#004080' }}>
                  {stats.controllers}
                </h3>
              </div>
              <div
                className="rounded-3 d-flex align-items-center justify-content-center"
                style={{ width: 44, height: 44, background: '#DBEAFE', color: '#1E40AF' }}
              >
                <i className="bi bi-shield-lock" aria-hidden="true" />
              </div>
            </div>
            <div className="mt-2 small text-muted" style={{ fontSize: '0.78rem' }}>
              Quyền khóa/mở chốt sổ kỳ
            </div>
          </div>
        </div>

        <div className="col-12 col-sm-6 col-xl-3">
          <div className="card border-0 shadow-sm h-100 p-3">
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <span className="text-muted small text-uppercase fw-semibold" style={{ fontSize: '0.72rem' }}>
                  Quản trị viên (ADMIN)
                </span>
                <h3 className="fw-bold mb-0 mt-1" style={{ color: '#D97706' }}>
                  {stats.admins}
                </h3>
              </div>
              <div
                className="rounded-3 d-flex align-items-center justify-content-center"
                style={{ width: 44, height: 44, background: '#FEF3C7', color: '#D97706' }}
              >
                <i className="bi bi-stars" aria-hidden="true" />
              </div>
            </div>
            <div className="mt-2 small text-muted" style={{ fontSize: '0.78rem' }}>
              Toàn quyền hệ thống
            </div>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="card mb-4">
        <div className="card-body">
          <form onSubmit={handleSearchSubmit} className="d-flex gap-3 flex-wrap align-items-end">
            <div style={{ minWidth: 260, flex: '2 1 260px' }}>
              <label className="form-label">Tìm kiếm nhân viên</label>
              <input
                type="text"
                className="form-control"
                placeholder="Nhập tên đăng nhập hoặc họ tên..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>

            <div style={{ minWidth: 160, flex: '1 1 160px' }}>
              <label className="form-label">Trạng thái tài khoản</label>
              <select
                className="form-select"
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
              >
                <option value="">Tất cả trạng thái</option>
                <option value="active">Đang hoạt động</option>
                <option value="inactive">Đang tạm khóa</option>
              </select>
            </div>

            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary fw-medium d-flex align-items-center gap-2">
                <i className="bi bi-search" aria-hidden="true" /> Tìm kiếm
              </button>
              <button type="button" className="btn btn-light" onClick={handleClearFilter}>
                <i className="bi bi-arrow-counterclockwise me-1" aria-hidden="true" />Xóa bộ lọc
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Users Table */}
      <div className="card shadow-sm border-0 mb-4 overflow-hidden">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th style={{ width: '50px' }} className="text-center">STT</th>
                <th>Nhân viên / Tài khoản</th>
                <th>Liên hệ</th>
                <th>Vai trò &amp; Quyền hạn</th>
                <th className="text-center" style={{ width: '140px' }}>Trạng thái</th>
                <th style={{ width: '150px' }}>Ngày tạo</th>
                <th className="text-center" style={{ width: '180px' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="text-center py-5 text-muted">
                    <div className="spinner-border spinner-border-sm text-primary me-2" role="status" />
                    Đang tải danh sách người dùng...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-5 text-muted">
                    <div className="fs-3 mb-2"><i className="bi bi-person" aria-hidden="true" /></div>
                    Không tìm thấy tài khoản nhân viên nào phù hợp.
                  </td>
                </tr>
              ) : (
                users.map((u, idx) => {
                  const initials = u.fullName
                    ? u.fullName.split(' ').map((w) => w[0]).slice(-2).join('').toUpperCase()
                    : u.username.slice(0, 2).toUpperCase();

                  return (
                    <tr key={u.id}>
                      <td className="text-center text-muted fw-medium">
                        {currentPage * 12 + idx + 1}
                      </td>
                      <td>
                        <div className="d-flex align-items-center gap-3">
                          <div
                            className="rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0"
                            style={{
                              width: 38,
                              height: 38,
                              background: u.roles.includes('ADMIN')
                                ? 'linear-gradient(135deg, #F5A623 0%, #D97706 100%)'
                                : u.roles.includes('CONTROLLER')
                                ? 'linear-gradient(135deg, #0056A3 0%, #003366 100%)'
                                : 'linear-gradient(135deg, #475569 0%, #334155 100%)',
                              color: '#fff',
                              fontSize: '0.8rem',
                              boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
                            }}
                          >
                            {initials}
                          </div>
                          <div>
                            <div className="fw-bold" style={{ color: 'var(--text-primary)', fontSize: '0.9rem' }}>
                              {u.fullName || 'Chưa cập nhật tên'}
                            </div>
                            <div className="text-muted small" style={{ fontSize: '0.78rem' }}>
                              @{u.username}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="small">
                          {u.email ? (
                            <div className="text-secondary mb-1">
                              <i className="bi bi-envelope me-1" aria-hidden="true" /> {u.email}
                            </div>
                          ) : (
                            <span className="text-muted fst-italic">Chưa có email</span>
                          )}
                          {u.phone && (
                            <div className="text-muted" style={{ fontSize: '0.76rem' }}>
                              <i className="bi bi-telephone me-1" aria-hidden="true" /> {u.phone}
                            </div>
                          )}
                        </div>
                      </td>
                      <td>
                        <div className="d-flex flex-wrap gap-1">
                          {u.roles.map((role) => {
                            const rObj = AVAILABLE_ROLES.find((r) => r.key === role);
                            return (
                              <span
                                key={role}
                                className={`badge px-2 py-1 ${rObj ? rObj.badgeClass : 'bg-secondary'}`}
                                style={{ fontSize: '0.72rem' }}
                                title={rObj?.desc}
                              >
                                {role === 'ADMIN' ? 'ADMIN' : role === 'CONTROLLER' ? 'CONTROLLER' : 'USER'}
                              </span>
                            );
                          })}
                        </div>
                      </td>
                      <td className="text-center">
                        {u.isActive ? (
                          <span
                            className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-2 py-1 d-inline-flex align-items-center gap-1"
                            style={{ fontSize: '0.75rem' }}
                          >
                            <span className="pulse-dot rounded-circle bg-success" style={{ width: 6, height: 6 }} />
                            Hoạt động
                          </span>
                        ) : (
                          <span
                            className="badge bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25 px-2 py-1"
                            style={{ fontSize: '0.75rem' }}
                          >
                            <i className="bi bi-lock-fill me-1" aria-hidden="true" />Tạm khóa
                          </span>
                        )}
                      </td>
                      <td className="small text-muted">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '—'}
                      </td>
                      <td>
                        <div className="d-flex align-items-center justify-content-center gap-1">
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-primary"
                            style={{ padding: '4px 8px', fontSize: '0.78rem' }}
                            title="Chỉnh sửa quyền & thông tin"
                            onClick={() => handleOpenEdit(u)}
                          >
                            <i className="bi bi-pencil-square me-1" aria-hidden="true" />Sửa
                          </button>
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary"
                            style={{ padding: '4px 8px', fontSize: '0.78rem' }}
                            title="Đặt lại mật khẩu"
                            onClick={() => handleOpenReset(u)}
                          >
                            <i className="bi bi-key me-1" aria-hidden="true" />Đổi MK
                          </button>
                          <button
                            type="button"
                            className={`btn btn-sm ${u.isActive ? 'btn-outline-danger' : 'btn-outline-success'}`}
                            style={{ padding: '4px 8px', fontSize: '0.78rem' }}
                            title={u.isActive ? 'Tạm khóa tài khoản' : 'Mở khóa tài khoản'}
                            disabled={togglingId === u.id || u.username === 'admin'}
                            onClick={() => handleToggleStatus(u)}
                          >
                            {togglingId === u.id ? (
                              <span className="spinner-border spinner-border-sm" />
                            ) : u.isActive ? (
                              <><i className="bi bi-lock me-1" aria-hidden="true" />Khóa</>
                            ) : (
                              <><i className="bi bi-unlock me-1" aria-hidden="true" />Mở</>
                            )}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="card-footer bg-white d-flex justify-content-between align-items-center py-3 px-4 border-top">
            <span className="small text-muted">
              Trang {currentPage + 1} / {totalPages} (Tổng {totalElements} tài khoản)
            </span>
            <div className="d-flex gap-1">
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                disabled={currentPage === 0 || loading}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              >
                <i className="bi bi-arrow-left me-1" aria-hidden="true" />Trước
              </button>
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                disabled={currentPage >= totalPages - 1 || loading}
                onClick={() => setCurrentPage((p) => p + 1)}
              >
                Sau <i className="bi bi-arrow-right ms-1" aria-hidden="true" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* MODAL 1: CẤP TÀI KHOẢN MỚI */}
      <Modal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title="Đăng Ký &amp; Cấp Tài Khoản Nhân Viên Mới"
        maxWidth="680px"
      >
        <form onSubmit={handleCreateSubmit}>
          <div className="alert alert-info border-0 d-flex gap-2 p-3 mb-4" style={{ backgroundColor: '#E0F2FE', fontSize: '0.84rem' }}>
            <span className="fs-5"><i className="bi bi-info-circle" aria-hidden="true" /></span>
            <div>
              <strong>Lưu ý cho Quản trị viên:</strong> Tài khoản sau khi tạo sẽ được kích hoạt ngay lập tức. Hãy cấp ít nhất một vai trò phù hợp với nghiệp vụ của nhân viên.
            </div>
          </div>

          <div className="row g-3 mb-3">
            <div className="col-md-6">
              <label className="form-label required fw-semibold">
                Tên đăng nhập (Username) <span className="text-danger">*</span>
              </label>
              <input
                type="text"
                className="form-control"
                placeholder="VD: nguyenvanan, ketoan01..."
                value={newUsername}
                onChange={(e) => setNewUsername(e.target.value.toLowerCase().trim())}
                required
              />
              <div className="form-text" style={{ fontSize: '0.75rem' }}>Dùng chữ thường, không dấu hoặc số.</div>
            </div>

            <div className="col-md-6">
              <label className="form-label required fw-semibold d-flex justify-content-between">
                <span>Mật khẩu khởi tạo <span className="text-danger">*</span></span>
                <button
                  type="button"
                  className="btn btn-link p-0 text-decoration-none small text-primary"
                  style={{ fontSize: '0.78rem' }}
                  onClick={() => setNewPassword(generateRandomPassword())}
                >
                  <i className="bi bi-shuffle me-1" aria-hidden="true" />Tạo ngẫu nhiên
                </button>
              </label>
              <div className="input-group">
                <input
                  type={showPassword ? 'text' : 'password'}
                  className="form-control"
                  placeholder="Tối thiểu 6 ký tự..."
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                />
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setShowPassword(!showPassword)}
                  title={showPassword ? 'Ẩn' : 'Hiện'}
                >
                  <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`} aria-hidden="true" />
                </button>
              </div>
            </div>
          </div>

          <div className="mb-3">
            <label className="form-label required fw-semibold">
              Họ và tên nhân viên <span className="text-danger">*</span>
            </label>
            <input
              type="text"
              className="form-control"
              placeholder="VD: Nguyễn Văn An"
              value={newFullName}
              onChange={(e) => setNewFullName(e.target.value)}
              required
            />
          </div>

          <div className="row g-3 mb-4">
            <div className="col-md-6">
              <label className="form-label fw-semibold">Email cơ quan / cá nhân</label>
              <input
                type="email"
                className="form-control"
                placeholder="VD: an.nv@saigonbank.com.vn"
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
              />
            </div>
            <div className="col-md-6">
              <label className="form-label fw-semibold">Số điện thoại liên hệ</label>
              <input
                type="tel"
                className="form-control"
                placeholder="VD: 0901234567"
                value={newPhone}
                onChange={(e) => setNewPhone(e.target.value)}
              />
            </div>
          </div>

          {/* Phân quyền Roles Selection */}
          <div className="mb-4">
            <label className="form-label fw-bold d-block mb-2" style={{ color: 'var(--sgb-navy-800)' }}>
              Phân Quyền Vai Trò (Roles) <span className="text-danger">*</span>
            </label>
            <div className="d-flex flex-column gap-2">
              {AVAILABLE_ROLES.map((role) => {
                const isChecked = newRoles.includes(role.key);
                return (
                  <div
                    key={role.key}
                    className={`card p-3 border cursor-pointer transition-all ${
                      isChecked ? 'border-primary bg-primary bg-opacity-10' : 'bg-light'
                    }`}
                    style={{ cursor: 'pointer' }}
                    onClick={() => toggleRoleInList(role.key, newRoles, setNewRoles)}
                  >
                    <div className="d-flex align-items-start gap-3">
                      <input
                        type="checkbox"
                        className="form-check-input mt-1"
                        checked={isChecked}
                        onChange={() => {}} // Handled by card click
                      />
                      <div className="flex-grow-1">
                        <div className="d-flex align-items-center gap-2 mb-1">
                          <span className="fw-bold" style={{ fontSize: '0.9rem' }}>
                            {role.label}
                          </span>
                          <span className={`badge ${role.badgeClass}`} style={{ fontSize: '0.7rem' }}>
                            {role.key}
                          </span>
                        </div>
                        <div className="text-muted small" style={{ fontSize: '0.78rem' }}>
                          {role.desc}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="d-flex justify-content-end gap-2 pt-3 border-top">
            <button
              type="button"
              className="btn btn-light rounded-pill px-4"
              onClick={() => setIsCreateOpen(false)}
              disabled={createSubmitting}
            >
              Hủy
            </button>
            <button
              type="submit"
              className="btn btn-gold rounded-pill px-4 fw-semibold"
              disabled={createSubmitting}
            >
              {createSubmitting ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" />
                  Đang tạo...
                </>
              ) : (
                <><i className="bi bi-check-circle me-1" aria-hidden="true" />Xác Nhận Cấp Tài Khoản</>
              )}
            </button>
          </div>
        </form>
      </Modal>

      {/* MODAL 2: CHỈNH SỬA THÔNG TIN & PHÂN QUYỀN */}
      <Modal
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        title={`Phân Quyền &amp; Cập Nhật: ${editingUser?.fullName || editingUser?.username}`}
        maxWidth="640px"
      >
        <form onSubmit={handleEditSubmit}>
          <div className="mb-3">
            <label className="form-label text-muted small">Tên đăng nhập (không thể sửa)</label>
            <input
              type="text"
              className="form-control bg-light"
              value={editingUser?.username || ''}
              disabled
            />
          </div>

          <div className="mb-3">
            <label className="form-label required fw-semibold">
              Họ và tên nhân viên <span className="text-danger">*</span>
            </label>
            <input
              type="text"
              className="form-control"
              value={editFullName}
              onChange={(e) => setEditFullName(e.target.value)}
              required
            />
          </div>

          <div className="row g-3 mb-3">
            <div className="col-md-6">
              <label className="form-label fw-semibold">Email</label>
              <input
                type="email"
                className="form-control"
                value={editEmail}
                onChange={(e) => setEditEmail(e.target.value)}
              />
            </div>
            <div className="col-md-6">
              <label className="form-label fw-semibold">Số điện thoại</label>
              <input
                type="tel"
                className="form-control"
                value={editPhone}
                onChange={(e) => setEditPhone(e.target.value)}
              />
            </div>
          </div>

          <div className="mb-4">
            <label className="form-label fw-semibold">Trạng thái hoạt động</label>
            <div className="form-check form-switch">
              <input
                type="checkbox"
                className="form-check-input"
                id="editStatusSwitch"
                checked={editIsActive}
                onChange={(e) => setEditIsActive(e.target.checked)}
                disabled={editingUser?.username === 'admin'}
              />
              <label className="form-check-label fw-medium ms-2" htmlFor="editStatusSwitch">
                {editIsActive ? (
                  <span className="text-success fw-bold">Đang hoạt động</span>
                ) : (
                  <span className="text-danger fw-bold">Tạm khóa tài khoản</span>
                )}
              </label>
            </div>
          </div>

          {/* Phân quyền Roles */}
          <div className="mb-4">
            <label className="form-label fw-bold d-block mb-2" style={{ color: 'var(--sgb-navy-800)' }}>
              Cấp / Đổi Vai Trò (Roles) <span className="text-danger">*</span>
            </label>
            <div className="d-flex flex-column gap-2">
              {AVAILABLE_ROLES.map((role) => {
                const isChecked = editRoles.includes(role.key);
                return (
                  <div
                    key={role.key}
                    className={`card p-3 border cursor-pointer transition-all ${
                      isChecked ? 'border-primary bg-primary bg-opacity-10' : 'bg-light'
                    }`}
                    style={{ cursor: 'pointer' }}
                    onClick={() => toggleRoleInList(role.key, editRoles, setEditRoles)}
                  >
                    <div className="d-flex align-items-start gap-3">
                      <input
                        type="checkbox"
                        className="form-check-input mt-1"
                        checked={isChecked}
                        onChange={() => {}}
                      />
                      <div className="flex-grow-1">
                        <div className="d-flex align-items-center gap-2 mb-1">
                          <span className="fw-bold" style={{ fontSize: '0.9rem' }}>
                            {role.label}
                          </span>
                          <span className={`badge ${role.badgeClass}`} style={{ fontSize: '0.7rem' }}>
                            {role.key}
                          </span>
                        </div>
                        <div className="text-muted small" style={{ fontSize: '0.78rem' }}>
                          {role.desc}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="d-flex justify-content-end gap-2 pt-3 border-top">
            <button
              type="button"
              className="btn btn-light rounded-pill px-4"
              onClick={() => setIsEditOpen(false)}
              disabled={editSubmitting}
            >
              Hủy
            </button>
            <button
              type="submit"
              className="btn btn-primary rounded-pill px-4 fw-semibold"
              disabled={editSubmitting}
            >
              {editSubmitting ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" />
                  Đang lưu...
                </>
              ) : (
                <><i className="bi bi-floppy me-1" aria-hidden="true" />Lưu Thay Đổi</>
              )}
            </button>
          </div>
        </form>
      </Modal>

      {/* MODAL 3: ĐẶT LẠI MẬT KHẨU */}
      <Modal
        isOpen={isResetOpen}
        onClose={() => setIsResetOpen(false)}
        title={`Đặt Lại Mật Khẩu: ${resetTargetUser?.fullName || resetTargetUser?.username}`}
        maxWidth="500px"
      >
        <form onSubmit={handleResetSubmit}>
          <p className="text-muted small mb-3">
            Đặt lại mật khẩu mới cho tài khoản <strong>@{resetTargetUser?.username}</strong>. Hãy gửi mật khẩu mới này cho nhân viên sau khi hoàn tất.
          </p>

          <div className="mb-4">
            <div className="d-flex justify-content-between align-items-center mb-1">
              <label className="form-label required fw-semibold mb-0">Mật khẩu mới</label>
              <button
                type="button"
                className="btn btn-link p-0 text-decoration-none small text-primary"
                style={{ fontSize: '0.78rem' }}
                onClick={() => setResetNewPassword(generateRandomPassword())}
              >
                <i className="bi bi-shuffle me-1" aria-hidden="true" />Tạo ngẫu nhiên
              </button>
            </div>
            <div className="input-group">
              <input
                type={showResetPassword ? 'text' : 'password'}
                className="form-control"
                placeholder="Tối thiểu 6 ký tự..."
                value={resetNewPassword}
                onChange={(e) => setResetNewPassword(e.target.value)}
                required
              />
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => setShowResetPassword(!showResetPassword)}
              >
                <i className={`bi ${showResetPassword ? 'bi-eye-slash' : 'bi-eye'}`} aria-hidden="true" />
              </button>
            </div>
          </div>

          <div className="d-flex justify-content-end gap-2 pt-3 border-top">
            <button
              type="button"
              className="btn btn-light rounded-pill px-4"
              onClick={() => setIsResetOpen(false)}
              disabled={resetSubmitting}
            >
              Hủy
            </button>
            <button
              type="submit"
              className="btn btn-gold rounded-pill px-4 fw-semibold"
              disabled={resetSubmitting}
            >
              {resetSubmitting ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" />
                  Đang cập nhật...
                </>
              ) : (
                <><i className="bi bi-key me-1" aria-hidden="true" />Xác Nhận Đổi Mật Khẩu</>
              )}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
