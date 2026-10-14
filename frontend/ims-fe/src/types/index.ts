export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isLast: boolean;
}

export interface User {
  id: number;
  username: string;
  fullName: string;
  email?: string;
  phone?: string;
  isActive?: boolean;
  roles: string[];
  createdAt?: string;
}

export interface UserAccount {
  id: number;
  username: string;
  fullName: string;
  email?: string;
  phone?: string;
  isActive: boolean;
  roles: string[];
  createdAt?: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  fullName: string;
  email?: string;
  phone?: string;
  roles: string[];
}

export interface UpdateUserRequest {
  fullName: string;
  email?: string;
  phone?: string;
  isActive?: boolean;
  roles?: string[];
}

export interface JwtResponse {
  token?: string;
  type?: string;
  id: number;
  username: string;
  fullName: string;
  roles: string[];
}

export interface ProductGroup {
  id: number;
  code: string;
  name: string;
  description?: string;
}

export interface Unit {
  id: number;
  name: string;
  description?: string;
}

export interface Product {
  id: number;
  maSo: string;
  tenHang: string;
  nhomHang: string;
  nhomHangTen?: string;
  donViTinh: string;
  donGiaNhap: number;
  trangThai: 'HOAT_DONG' | 'NGUNG_SU_DUNG';
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  nameWarning?: boolean;
  warningMessage?: string;
}

export interface CreateProductRequest {
  maSo?: string;
  tenHang: string;
  nhomHang: string;
  donViTinh: string;
  donGiaNhap: number;
  forceSaveIfDuplicateName?: boolean;
}

export interface UpdateProductRequest {
  tenHang: string;
  donViTinh: string;
  donGiaNhap?: number;
  trangThai?: 'HOAT_DONG' | 'NGUNG_SU_DUNG';
}

export interface ImportDetail {
  id?: number;
  productId: number;
  maSo?: string;
  tenHang?: string;
  nhomHang?: string;
  donViTinh?: string;
  quantity: number;
  unitPrice: number;
  amount?: number;
}

export interface ImportDoc {
  id: number;
  soPhieu: string;
  ngayNhap: string;
  noiNhap?: string;
  noiMua?: string;
  soHoaDon?: string;
  ngayHoaDon?: string;
  tkNo?: string;
  tkCo?: string;
  tongTien: number;
  trangThai: 'HOAN_THANH' | 'HUY';
  nhomDaiDien?: string;
  workflowStatus?: string;
  createdBy?: string;
  creatorFullName?: string;
  createdAt?: string;
  details: ImportDetail[];
}

export interface CreateImportRequest {
  ngayNhap: string;
  noiNhap?: string;
  noiMua?: string;
  soHoaDon?: string;
  ngayHoaDon?: string;
  tkNo?: string;
  tkCo?: string;
  details: {
    productId: number;
    quantity: number;
    unitPrice: number;
  }[];
}

export interface ExportDetail {
  id?: number;
  productId: number;
  maSo?: string;
  tenHang?: string;
  nhomHang?: string;
  donViTinh?: string;
  quantity: number;
  unitPrice: number;
  amount?: number;
}

export interface ExportDoc {
  id: number;
  soPhieu: string;
  ngayXuat: string;
  noiNhan?: string;
  lyDoXuat?: string;
  tkNo?: string;
  tkCo?: string;
  tongTien: number;
  trangThai: 'HOAN_THANH' | 'HUY';
  nhomDaiDien?: string;
  workflowStatus?: string;
  createdBy?: string;
  creatorFullName?: string;
  createdAt?: string;
  details: ExportDetail[];
}

export interface CreateExportRequest {
  ngayXuat: string;
  noiNhan?: string;
  lyDoXuat?: string;
  tkNo?: string;
  tkCo?: string;
  details: {
    productId: number;
    quantity: number;
  }[];
}

export interface InventoryItem {
  inventoryId: number;
  productId: number;
  maSo: string;
  tenHang: string;
  nhomHang: string;
  nhomHangTen?: string;
  donViTinh: string;
  trangThai: string;
  openingQuantity: number;
  openingAmount: number;
  importQuantity: number;
  importAmount: number;
  exportQuantity: number;
  exportAmount: number;
  endingQuantity: number;
  endingAmount: number;
  averagePrice: number;
}

export interface StockCardEntry {
  id: number;
  entryDate: string;
  soPhieu: string;
  tranType: 'OPEN' | 'IMPORT' | 'EXPORT';
  importQuantity: number;
  importUnitPrice: number;
  importAmount: number;
  exportQuantity: number;
  exportUnitPrice: number;
  exportAmount: number;
  balanceQuantity: number;
  balanceAmount: number;
  note?: string;
  createdBy?: string;
}

export interface StockCard {
  productId: number;
  maSo: string;
  tenHang: string;
  nhomHang: string;
  donViTinh: string;
  entries: StockCardEntry[];
}

export interface KktsItem {
  id: number;
  productId: number;
  maSo: string;
  tenHang: string;
  nhomHang: string;
  donViTinh: string;
  year: number;
  month: number;
  openingQuantity: number;
  openingAmount: number;
  importQuantity: number;
  importAmount: number;
  exportQuantity: number;
  exportAmount: number;
  endingQuantity: number;
  endingAmount: number;
  averagePrice: number;
}

export interface MonthlyClosing {
  id: number;
  year: number;
  month: number;
  isLocked: boolean;
  lockedAt?: string;
  lockedBy?: string;
  unlockedAt?: string;
  unlockedBy?: string;
}

export interface DashboardData {
  totalActiveProducts: number;
  productsWithStock: number;
  totalInventoryAmount: number;
  importsThisMonth: number;
  exportsThisMonth: number;
  currentMonth: number;
  currentYear: number;
}

export interface AuditLogItem {
  id: number;
  username: string;
  fullName?: string;
  action: string;
  entityName: string | null;
  entityId: string | null;
  description: string | null;
  createdAt: string;
}
