import { apiFetch } from "./http";

const API_PREFIX = "/v1";

export class ApiError extends Error {
  readonly status: number;
  readonly statusText: string;
  readonly body: unknown;

  constructor(status: number, statusText: string, body: unknown) {
    super(`API ${status} ${statusText}`);
    this.name = "ApiError";
    this.status = status;
    this.statusText = statusText;
    this.body = body;
  }
}

async function unwrap<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let body: unknown = null;
    try {
      body = await response.json();
    } catch {
      try {
        body = await response.text();
      } catch {
      }
    }
    throw new ApiError(response.status, response.statusText, body);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await apiFetch(`${API_PREFIX}${path}`);
  return unwrap<T>(res);
}

export async function apiPost<TReq, TRes>(path: string, body: TReq): Promise<TRes> {
  const res = await apiFetch(`${API_PREFIX}${path}`, {
    method: "POST",
    body: JSON.stringify(body),
  });
  return unwrap<TRes>(res);
}

export async function apiPostNoBody<TRes>(path: string): Promise<TRes> {
  const res = await apiFetch(`${API_PREFIX}${path}`, { method: "POST" });
  return unwrap<TRes>(res);
}

export async function apiPut<TReq, TRes>(path: string, body: TReq): Promise<TRes> {
  const res = await apiFetch(`${API_PREFIX}${path}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
  return unwrap<TRes>(res);
}

export async function apiPatch<TReq, TRes>(path: string, body: TReq): Promise<TRes> {
  const res = await apiFetch(`${API_PREFIX}${path}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
  return unwrap<TRes>(res);
}

export async function apiDelete(path: string): Promise<void> {
  const res = await apiFetch(`${API_PREFIX}${path}`, { method: "DELETE" });
  await unwrap<void>(res);
}
