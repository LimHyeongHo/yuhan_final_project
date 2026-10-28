const trimTrailingSlash = (value) => value.replace(/\/+$/, '');

export const API_ORIGIN = trimTrailingSlash(
  process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'
);

export const API_BASE = `${API_ORIGIN}/api`;

export const WS_URL = process.env.REACT_APP_WS_URL
  ? trimTrailingSlash(process.env.REACT_APP_WS_URL)
  : `${API_ORIGIN}/ws`;
