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
      `/posts/${id}/like`
    );
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
