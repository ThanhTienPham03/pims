const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8085/api';

class ApiClient {
  private getToken(): string | null {
    return localStorage.getItem('pims_token');
  }

  public setToken(token: string) {
    localStorage.setItem('pims_token', token);
  }

  public removeToken() {
    localStorage.removeItem('pims_token');
    localStorage.removeItem('pims_user');
  }

  private getHeaders(): HeadersInit {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
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
    });

    return this.handleResponse<T>(response);
  }

  public async post<T>(endpoint: string, body?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: body ? JSON.stringify(body) : undefined,
    });

    return this.handleResponse<T>(response);
  }

  public async put<T>(endpoint: string, body?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: body ? JSON.stringify(body) : undefined,
    });

    return this.handleResponse<T>(response);
  }

  public async patch<T>(endpoint: string, body?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'PATCH',
      headers: this.getHeaders(),
      body: body ? JSON.stringify(body) : undefined,
    });

    return this.handleResponse<T>(response);
  }

  public async delete<T>(endpoint: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'DELETE',
      headers: this.getHeaders(),
    });

    return this.handleResponse<T>(response);
  }

  private async handleResponse<T>(response: Response): Promise<T> {
    if (response.status === 401) {
      this.removeToken();
      // Do not dispatch unauthorized event if it's a login attempt
      if (!response.url.includes('/auth/login')) {
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
