// Chuyển đổi số tiền VND sang chữ tiếng Việt có dấu chuẩn

const UNITS_VN = ['', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín'];
const TENS_VN  = ['', 'mười', 'hai mươi', 'ba mươi', 'bốn mươi', 'năm mươi', 'sáu mươi', 'bảy mươi', 'tám mươi', 'chín mươi'];

function readGroup(n: number): string {
  if (n === 0) return '';
  const hundred = Math.floor(n / 100);
  const ten     = Math.floor((n % 100) / 10);
  const unit    = n % 10;

  let result = '';
  if (hundred > 0) result += UNITS_VN[hundred] + ' trăm';
  if (ten === 0 && unit === 0) return result;
  if (hundred > 0) result += ' ';

  if (ten === 0) {
    result += 'lẻ ' + UNITS_VN[unit];
  } else if (ten === 1) {
    if (unit === 0) result += 'mười';
    else if (unit === 5) result += 'mười lăm';
    else result += 'mười ' + UNITS_VN[unit];
  } else {
    result += TENS_VN[ten];
    if (unit === 1) result += ' mốt';
    else if (unit === 5) result += ' lăm';
    else if (unit > 0) result += ' ' + UNITS_VN[unit];
  }
  return result;
}

function capitalize(str: string): string {
  if (!str) return str;
  return str.charAt(0).toUpperCase() + str.slice(1);
}

export function numberToVietnamese(amount: number): string {
  if (!amount || isNaN(amount) || amount === 0) return 'Không đồng';
  const n      = Math.round(Math.abs(amount));
  const prefix = amount < 0 ? 'Âm ' : '';

  if (n < 1000) return prefix + capitalize(readGroup(n)) + ' đồng';

  const billions  = Math.floor(n / 1_000_000_000);
  const millions  = Math.floor((n % 1_000_000_000) / 1_000_000);
  const thousands = Math.floor((n % 1_000_000) / 1_000);
  const remainder = n % 1_000;

  const parts: string[] = [];
  if (billions  > 0) parts.push(readGroup(billions)  + ' tỷ');
  if (millions  > 0) parts.push(readGroup(millions)  + ' triệu');
  if (thousands > 0) parts.push(readGroup(thousands) + ' nghìn');
  if (remainder > 0) parts.push(readGroup(remainder));

  return prefix + capitalize(parts.join(' ')) + ' đồng';
}