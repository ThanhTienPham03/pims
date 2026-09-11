// Chuyển đổi số tiền VND sang chữ tiếng Việt

const UNITS_VN = ['', 'mot', 'hai', 'ba', 'bon', 'nam', 'sau', 'bay', 'tam', 'chin'];
const TENS_VN  = ['', 'muoi', 'hai muoi', 'ba muoi', 'bon muoi', 'nam muoi', 'sau muoi', 'bay muoi', 'tam muoi', 'chin muoi'];

function readGroup(n: number): string {
  if (n === 0) return '';
  const hundred = Math.floor(n / 100);
  const ten     = Math.floor((n % 100) / 10);
  const unit    = n % 10;

  let result = '';
  if (hundred > 0) result += UNITS_VN[hundred] + ' tram';
  if (ten === 0 && unit === 0) return result;
  if (hundred > 0) result += ' ';

  if (ten === 0) {
    result += 'le ' + UNITS_VN[unit];
  } else if (ten === 1) {
    if (unit === 0) result += 'muoi';
    else if (unit === 5) result += 'muoi lam';
    else result += 'muoi ' + UNITS_VN[unit];
  } else {
    result += TENS_VN[ten];
    if (unit === 5) result += ' lam';
    else if (unit > 0) result += ' ' + UNITS_VN[unit];
  }
  return result;
}

function capitalize(str: string): string {
  if (!str) return str;
  return str.charAt(0).toUpperCase() + str.slice(1);
}

export function numberToVietnamese(amount: number): string {
  if (!amount || isNaN(amount) || amount === 0) return 'Khong dong';
  const n      = Math.round(Math.abs(amount));
  const prefix = amount < 0 ? 'Am ' : '';

  if (n < 1000) return prefix + capitalize(readGroup(n)) + ' dong';

  const billions  = Math.floor(n / 1_000_000_000);
  const millions  = Math.floor((n % 1_000_000_000) / 1_000_000);
  const thousands = Math.floor((n % 1_000_000) / 1_000);
  const remainder = n % 1_000;

  const parts: string[] = [];
  if (billions  > 0) parts.push(readGroup(billions)  + ' ty');
  if (millions  > 0) parts.push(readGroup(millions)  + ' trieu');
  if (thousands > 0) parts.push(readGroup(thousands) + ' nghin');
  if (remainder > 0) parts.push(readGroup(remainder));

  return prefix + capitalize(parts.join(' ')) + ' dong';
}