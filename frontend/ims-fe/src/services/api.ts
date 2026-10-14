const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8085/api';

class ApiClient {
  public clearAuth() {
    localStorage.removeItem('pims_user');
    localStorage.removeItem('pims_token');
    localStorage.removeItem('pims_token_expiry');
    localStorage.removeItem('pims_session_expiry');
  }

  public setToken(_token: string) {
    // Không lưu token vào localStorage, backend đã thiết lập HttpOnly Cookie
  }

  public removeToken() {
    this.clearAuth();
  }

  private getHeaders(): HeadersInit {
    return {
      'Content-Type': 'application/json',
    };
  }

  public async get<T>(endpoint: string, params?: Record<string, any>): Promise<T> {
    let url = `${API_BASE_URL}${endpoint}`;
    if (params) {
      const searchParams = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          searchParams.append(key, String(value));
        }
      });
      const queryString = searchParams.toString();
      if (queryString) {
        url += (url.includes('?') ? '&' : '?') + queryString;
      }
    }

    const response = await fetch(url, {
      method: 'GET',
      headers: this.getHeaders(),
      credentials: 'include',
    });

    return this.handleResponse<T>(response);
  }

  public async post<T>(endpoint: string, body?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'POST',
      headers: this.getHeaders(),
      credentials: 'include',
      body: body ? JSON.stringify(body) : undefined,
    });

    return this.handleResponse<T>(response);
  }

  public async put<T>(endpoint: string, body?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'PUT',
      headers: this.getHeaders(),
      credentials: 'include',
      body: body ? JSON.stringify(body) : undefined,
    });

    return this.handleResponse<T>(response);
  }

  public async patch<T>(endpoint: string, body?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'PATCH',
      headers: this.getHeaders(),
      credentials: 'include',
      body: body ? JSON.stringify(body) : undefined,
    });

    return this.handleResponse<T>(response);
  }

  public async delete<T>(endpoint: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'DELETE',
      headers: this.getHeaders(),
      credentials: 'include',
    });

    return this.handleResponse<T>(response);
  }

  private async handleResponse<T>(response: Response): Promise<T> {
    if (response.status === 401) {
      this.removeToken();
      // Do not dispatch unauthorized event if it's a login or OTP verification attempt
      if (!response.url.includes('/auth/login') && !response.url.includes('/auth/verify-otp')) {
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
    }

    const json = await response.json().catch(() => ({}));

    if (!response.ok) {
      const errorMessage = json.message || `Lỗi yêu cầu: ${response.statusText} (${response.status})`;
      throw new Error(errorMessage);
    }

    if (json && typeof json.success === 'boolean' && json.data !== undefined) {
      return json.data as T;
    }

    return json as T;
  }
}

export const api = new ApiClient();
