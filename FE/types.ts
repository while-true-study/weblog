// API Response Wrapper
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string };
}

// User Types
export interface User {
  id: number;
  email: string;
  nickname: string;
  username: string;
  role: "USER" | "ADMIN";
  avatar?: string; // Added avatar
  createdAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

// Post Types
export interface PostSummary {
  id: number;
  title: string;
  summary?: string | null;

  author: {
    id: number;
    nickname: string;
    avatar?: string | null;
  };

  tags?: string[]; // ["spring", "blog"]
  viewCount?: number;

  createdAt: string;

  // UI에서 쓰는 값(백엔드에 없으면 추후 추가하거나, 일단 optional로)
  thumbnail?: string | null;
  commentCount?: number;
  likeCount?: number;
}

export interface PostDetail extends PostSummary {
  content: string;
  seriesId?: number | null;
  seriesName?: string | null;
  updatedAt?: string;
}

export interface PostCreateResponse {
  id: number;
  title: string;
  status: "PUBLISHED" | "DRAFT" | "DELETED";
  createdAt: string;
}

export interface PostUpdateResponse {
  postId: number;
  title: string;
  version: number;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// Comment Types
export interface Comment {
  id: number;
  content: string;
  author: {
    id: number;
    nickname: string;
    avatar?: string;
  };
  createdAt: string;
}

// Category Types
export interface Category {
  id: number;
  code: string;
  name: string;
}

// Request DTOs
export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
  username: string;
}

export interface PostCreateRequest {
  title: string;
  content: string;
  seriesId?: number;
  tags: string[];
  status: "PUBLISHED" | "DRAFT";
}

export interface PostUpdateRequest {
  title?: string;
  content?: string;
  seriesId?: number;
  tags?: string[];
  status?: "PUBLISHED" | "DRAFT";
}
