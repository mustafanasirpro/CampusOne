type DateValue = Date | number | string | null | undefined;

function validDate(value: DateValue) {
  if (value === null || value === undefined || value === "") return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function formatDate(
  value: DateValue,
  fallback = "Date unavailable",
) {
  const date = validDate(value);
  if (!date) return fallback;
  return new Intl.DateTimeFormat("en-PK", {
    dateStyle: "medium",
  }).format(date);
}

export function formatDateTime(
  value: DateValue,
  fallback = "Date unavailable",
) {
  const date = validDate(value);
  if (!date) return fallback;
  return new Intl.DateTimeFormat("en-PK", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function formatCurrency(
  amount: number,
  currency: string,
  fallback = "Price unavailable",
) {
  if (!Number.isFinite(amount) || !currency.trim()) return fallback;
  const normalizedCurrency = currency.trim().toUpperCase();
  try {
    return new Intl.NumberFormat("en-PK", {
      currency: normalizedCurrency,
      maximumFractionDigits: 2,
      style: "currency",
    }).format(amount);
  } catch {
    return `${normalizedCurrency} ${amount.toLocaleString("en-PK", {
      maximumFractionDigits: 2,
    })}`;
  }
}
