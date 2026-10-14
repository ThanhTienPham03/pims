import { api } from './api';
import type {
  UserAccount,
  CreateUserRequest,
  UpdateUserRequest,
  JwtResponse,
  PageResponse,
  Product,
  CreateProductRequest,
  UpdateProductRequest,
  ProductGroup,
  Unit,
  ImportDoc,
  CreateImportRequest,
  ExportDoc,
  CreateExportRequest,
  InventoryItem,
  StockCard,
  KktsItem,
  MonthlyClosing,
  DashboardData,
  AuditLogItem,
} from '../types';

export const authService = {
  initiateLogin: async (
    email: string,
    password: string,
    forceResend: boolean = false
  ): Promise<{ maskedEmail: string; message: string; isNewOtp?: boolean; expiresInSeconds?: number }> => {
    const data = await api.post<{ maskedEmail: string; message: string; isNewOtp?: boolean; expiresInSeconds?: number }>(
      '/auth/login',
      { email, password, forceResend }
    );
    return data;
  },

  verifyOtp: async (email: string, otpCode: string): Promise<JwtResponse> => {
    const data = await api.post<JwtResponse>('/auth/verify-otp', { email, otpCode });
    if (data) {
      // Backend đã tự động đính kèm HttpOnly Cookie "pims_token"
      // Lưu thông tin người dùng (không chứa token) để hiển thị giao diện tức thời
      const { token: _token, ...userProfile } = data;
      const expiryTime = Date.now() + 3 * 60 * 60 * 1000;
      localStorage.setItem('pims_session_expiry', String(expiryTime));
      localStorage.setItem('pims_user', JSON.stringify(userProfile));
    }
    return data;
  },

  getMe: async (): Promise<JwtResponse> => {
    const data = await api.get<JwtResponse>('/auth/me');
    if (data) {
      const { token: _token, ...userProfile } = data;
      localStorage.setItem('pims_user', JSON.stringify(userProfile));
    }
    return data;
  },

  getCurrentUser: (): JwtResponse | null => {
    const userStr = localStorage.getItem('pims_user');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  },

  isTokenExpired: (): boolean => {
    const expiry = localStorage.getItem('pims_session_expiry');
    if (!expiry) return false;
    return Date.now() > parseInt(expiry, 10);
  },

  logout: async () => {
    // 1. Xóa ngay lập tức dữ liệu phiên ở client để tránh race condition khi refresh
    api.removeToken();
    try {
      // 2. Gửi yêu cầu backend xóa HttpOnly Cookie pims_token
      await api.post('/auth/logout');
    } catch {
      // Bỏ qua lỗi nếu mạng rớt
    }
  },
};


export const productService = {
  getProducts: (params?: {
    maSo?: string;
    tenHang?: string;
    nhomHang?: string;
    trangThai?: string;
    page?: number;
    size?: number;
  }) => api.get<PageResponse<Product>>('/products', params),

  getActiveProducts: () => api.get<Product[]>('/products/active'),

  getProductById: (id: number) => api.get<Product>(`/products/${id}`),

  createProduct: (data: CreateProductRequest) => api.post<Product>('/products', data),

  updateProduct: (id: number, data: UpdateProductRequest) => api.put<Product>(`/products/${id}`, data),

  deactivateProduct: (id: number) => api.delete<void>(`/products/${id}`),

  getGroups: () => api.get<ProductGroup[]>('/products/groups'),

  getUnits: () => api.get<Unit[]>('/products/units'),

  getNextCode: (nhomHang: string) => api.get<{ maSo: string }>(`/products/next-code?nhomHang=${encodeURIComponent(nhomHang)}`),

  migrateCodes: () => api.post<{ migratedCount: number; message: string }>('/products/migrate-codes'),
};

export const importService = {
  getImports: (params?: {
    soPhieu?: string;
    noiMua?: string;
    fromDate?: string;
    toDate?: string;
    trangThai?: string;
    page?: number;
    size?: number;
  }) => api.get<PageResponse<ImportDoc>>('/imports', params),

  getImportById: (id: number) => api.get<ImportDoc>(`/imports/${id}`),

  createImport: (data: CreateImportRequest) => api.post<ImportDoc>('/imports', data),

  cancelImport: (id: number) => api.post<ImportDoc>(`/imports/${id}/cancel`),
};

export const exportService = {
  getExports: (params?: {
    soPhieu?: string;
    noiNhan?: string;
    fromDate?: string;
    toDate?: string;
    trangThai?: string;
    page?: number;
    size?: number;
  }) => api.get<PageResponse<ExportDoc>>('/exports', params),

  getExportById: (id: number) => api.get<ExportDoc>(`/exports/${id}`),

  createExport: (data: CreateExportRequest) => api.post<ExportDoc>('/exports', data),

  cancelExport: (id: number) => api.post<ExportDoc>(`/exports/${id}/cancel`),
};

export const inventoryService = {
  getInventory: (params?: {
    maSo?: string;
    tenHang?: string;
    nhomHang?: string;
    page?: number;
    size?: number;
  }) => api.get<PageResponse<InventoryItem>>('/inventory', params),

  getInventoryByProduct: (productId: number) => api.get<InventoryItem>(`/inventory/${productId}`),

  getStockCard: (productId: number) => api.get<StockCard>(`/inventory/${productId}/stock-card`),
};

export const kktsService = {
  getKktsByPeriod: (year?: number, month?: number) =>
    api.get<KktsItem[]>('/kkts', { year, month }),
};

export const monthlyClosingService = {
  getAll: () => api.get<MonthlyClosing[]>('/monthly-closings'),

  lockMonth: (year: number, month: number) =>
    api.post<MonthlyClosing>(`/monthly-closings/lock?year=${year}&month=${month}`),

  unlockMonth: (year: number, month: number) =>
    api.post<MonthlyClosing>(`/monthly-closings/unlock?year=${year}&month=${month}`),
};

export const dashboardService = {
  getDashboard: () => api.get<DashboardData>('/dashboard'),
};

export const userService = {
  getUsers: (params?: {
    search?: string;
    isActive?: boolean;
    page?: number;
    size?: number;
  }) => api.get<PageResponse<UserAccount>>('/users', params),

  getUserById: (id: number) => api.get<UserAccount>(`/users/${id}`),

  createUser: (data: CreateUserRequest) => api.post<UserAccount>('/users', data),

  updateUser: (id: number, data: UpdateUserRequest) => api.put<UserAccount>(`/users/${id}`, data),

  toggleStatus: (id: number) => api.patch<UserAccount>(`/users/${id}/status`),

  resetPassword: (id: number, newPassword: string) =>
    api.post<void>(`/users/${id}/reset-password`, { newPassword }),
};

export const auditLogService = {
  getLogs: (params?: {
    username?: string;
    action?: string;
    entityName?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
    size?: number;
  }) => api.get<PageResponse<AuditLogItem>>('/audit-logs', params),
};

export { activitySocket } from './websocket';
