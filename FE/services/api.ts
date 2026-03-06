import apiClient from "../lib/axios";
import {
  ApiResponse,
  LoginRequest,
  SignupRequest,
  AuthResponse,
  User,
  PageResponse,
  PostSummary,
  PostDetail,
  PostCreateRequest,
  PostUpdateRequest,
  Comment,
  Category,
} from "../types";

type SearchPostsParams = {
  keyword: string;
  offset: number;
  limit: number;
};

type LegacySearchPostsParams = SearchPostsParams & {
  mode?: "infix" | "prefix";
};

// Auth Services
export const authApi = {
  signup: async (data: SignupRequest) => {
    return apiClient.post<ApiResponse<User>>("/auth/signup", data);
  },
  login: async (data: LoginRequest) => {
    return apiClient.post<ApiResponse<AuthResponse>>("/auth/login", data);
  },
  getMe: async () => {
    return apiClient.get<ApiResponse<User>>("/auth/me");
  },
};

// Post Services
export const postApi = {
  getList: async (opts?: {
    page?: number;
    size?: number;
    sort?: string; // "createdAt,DESC"
    categoryId?: number;
    keyword?: string;
    tag?: string;
  }) => {
    const {
      page = 0,
      size = 10,
      sort = "createdAt,DESC",
      categoryId,
      keyword,
      tag,
    } = opts ?? {};

    const params: any = { page, size, sort };
    if (categoryId !== undefined) params.categoryId = categoryId;
    if (keyword) params.keyword = keyword;
    if (tag) params.tag = tag;

    return apiClient.get<ApiResponse<PageResponse<PostSummary>>>("/posts", {
      params,
    });
  },

  getDetail: async (id: number) => {
    return apiClient.get<ApiResponse<PostDetail>>(`/posts/${id}`);
  },

  create: async (data: PostCreateRequest) => {
    return apiClient.post<ApiResponse<PostDetail>>("/posts", data);
  },

  update: async (id: number, data: PostUpdateRequest) => {
    return apiClient.patch<ApiResponse<null>>(`/posts/${id}`, data);
  },

  delete: async (id: number) => {
    return apiClient.delete<ApiResponse<null>>(`/posts/${id}`);
  },

  toggleLike: async (id: number) => {
    return apiClient.post<ApiResponse<{ liked: boolean; likeCount: number }>>(
      `/posts/${id}/like`,
    );
  },

  // ===== 검색 API (경로 기반) =====
  searchPostsInfix: (params: SearchPostsParams, config?: any) =>
    apiClient.get("/search/posts/infix", { params, ...config }),

  searchPostsPrefix: (params: SearchPostsParams, config?: any) =>
    apiClient.get("/search/posts/prefix", { params, ...config }),

  searchPostsFulltext: (params: SearchPostsParams, config?: any) =>
    apiClient.get("/search/posts/fulltext", { params, ...config }),

  searchPostsFulltextBoolean: (params: SearchPostsParams, config?: any) =>
    apiClient.get("/search/posts/fulltext-boolean", { params, ...config }),

  // ===== 기존 호환용 (mode 기반) =====
  // 기존 코드가 남아있어도 바로 안 깨지게 유지
  searchPosts: (params: LegacySearchPostsParams, config?: any) => {
    const { mode = "infix", ...rest } = params;

    if (mode === "prefix") {
      return apiClient.get("/search/posts/prefix", { params: rest, ...config });
    }

    // 기본은 infix
    return apiClient.get("/search/posts/infix", { params: rest, ...config });
  },
};
// Comment Services
export const commentApi = {
  getList: async (postId: number) => {
    return apiClient.get<ApiResponse<Comment[]>>(`/posts/${postId}/comments`);
  },
  create: async (postId: number, content: string) => {
    return apiClient.post<ApiResponse<null>>(`/posts/${postId}/comments`, {
      content,
    });
  },
  delete: async (commentId: number) => {
    return apiClient.delete<ApiResponse<null>>(`/comments/${commentId}`);
  },
};

// Category Services
export const categoryApi = {
  getList: async () => {
    return apiClient.get<ApiResponse<Category[]>>("/categories");
  },
};
