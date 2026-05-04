import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { API_BASE_URL } from "../constants";
import { useAuthStore } from "../store/useAuthStore";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const { accessToken } = useAuthStore.getState();

  if (accessToken) {
    // Axios v1 권장: headers가 AxiosHeaders이면 set 사용 가능
    if (config.headers && "set" in config.headers) {
      config.headers.set("Authorization", `Bearer ${accessToken}`);
    } else {
      // 혹시 타입이 plain object처럼 보이는 환경 대비
      (config.headers as any).Authorization = `Bearer ${accessToken}`;
    }
  }

  return config;
});

let isRefreshing = false;
let waitQueue: Array<(token: string) => void> = [];
let failQueue: Array<(err: any) => void> = [];

function flushSuccess(token: string) {
  waitQueue.forEach((cb) => cb(token));
  waitQueue = [];
}

function flushFail(err: any) {
  failQueue.forEach((cb) => cb(err));
  failQueue = [];
}

// refresh는 인터셉터 없는 axios로 호출(무한루프 방지)
async function refreshAccessToken() {
  const { refreshToken } = useAuthStore.getState();
  if (!refreshToken) throw new Error("No refresh token");

  return axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
}

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (!error.response) return Promise.reject(error);

    const original = error.config;
    const status = error.response.status;

    const isRefreshRequest =
      typeof original?.url === "string" &&
      original.url.includes("/auth/refresh");

    if (status === 401 && !isRefreshRequest && !original?._retry) {
      original._retry = true;

      // 이미 refresh 진행 중이면 대기열에 붙기
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          waitQueue.push((newToken: string) => {
            original.headers = original.headers ?? {};
            original.headers.Authorization = `Bearer ${newToken}`;
            resolve(apiClient(original));
          });
          failQueue.push(reject);
        });
      }

      isRefreshing = true;

      try {
        const refreshRes = await refreshAccessToken();

        // 응답 구조는 백엔드에 맞춰서 꺼내야 함
        // 예: { success: true, data: { accessToken: "...", refreshToken: "..." } }
        const newAccessToken = refreshRes.data.data.accessToken;
        const newRefreshToken = refreshRes.data.data.refreshToken; // 없으면 undefined일 수 있음

        const { setTokens } = useAuthStore.getState();
        setTokens(newAccessToken, newRefreshToken);

        isRefreshing = false;
        flushSuccess(newAccessToken);

        original.headers = original.headers ?? {};
        original.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(original);
      } catch (err) {
        isRefreshing = false;
        flushFail(err);

        const { logout } = useAuthStore.getState();
        logout();
        return Promise.reject(err);
      }
    }

    // refresh 자체가 401이면(리프레시도 만료/무효) 바로 로그아웃
    if (status === 401) {
      const { logout } = useAuthStore.getState();
      logout();
    }

    return Promise.reject(error);
  }
);

export default apiClient;
