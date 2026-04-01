import React, { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import Layout from "../components/Layout";
import { postApi } from "../services/api";
import { PostSummary } from "../types";

const PAGE_SIZE = 12;

type BackendPostSummary = {
  id?: number;
  title?: string;
  summary?: string;
  contentPreview?: string;
  thumbnail?: string | null;
  thumbnailUrl?: string | null;
  createdAt?: string;
  commentCount?: number;
  likeCount?: number;
  author?: {
    nickname?: string;
    avatar?: string | null;
  } | null;
  authorNickname?: string;
  authorAvatar?: string | null;
};

const Home: React.FC = () => {
  const [posts, setPosts] = useState<PostSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [menuOpen, setMenuOpen] = useState(false);
  const menuWrapRef = useRef<HTMLDivElement | null>(null);

  const [activeTab, setActiveTab] = useState<"popular" | "recent">("popular");
  const [timeRange, setTimeRange] = useState("week");

  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);

  const resolveSort = useCallback(() => {
    if (activeTab === "popular") return "viewCount,DESC";
    return "createdAt,DESC";
  }, [activeTab]);

  const fetchPosts = useCallback(
    async (nextPage: number) => {
      setLoading(true);
      setError(null);

      try {
        const res = await postApi.getList({
          page: nextPage,
          size: PAGE_SIZE,
          sort: resolveSort(),
        });

        const data = res.data;

        console.log("data =", data);
        console.log("content[0] =", data?.content?.[0]);

        const rawContent = Array.isArray(data?.content) ? data.content : [];

        const normalizedPosts = rawContent.map((post: any) => ({
          id: post.id ?? 0,
          title: post.title ?? "제목 없음",
          summary: post.summary ?? post.contentPreview ?? "",
          thumbnail: post.thumbnail ?? post.thumbnailUrl ?? null,
          createdAt: post.createdAt ?? new Date().toISOString(),
          commentCount: post.commentCount ?? 0,
          likeCount: post.likeCount ?? 0,
          author: {
            id: post.author?.id ?? post.authorId ?? 0,
            nickname: post.author?.nickname ?? post.authorNickname ?? "unknown",
            avatar: post.author?.avatar ?? post.authorAvatar ?? null,
          },
        }));

        setPosts(normalizedPosts);
        setPage(data?.page ?? 0);
        setHasNext(data?.hasNext ?? false);
      } catch (err) {
        console.error(err);
        setPosts([]);
        setHasNext(false);
        setError("게시글 목록을 불러오는 중 오류가 발생했습니다.");
      } finally {
        setLoading(false);
      }
    },
    [resolveSort],
  );

  useEffect(() => {
    setPage(0);
    setHasNext(false);
    fetchPosts(0);
  }, [fetchPosts, timeRange]);

  useEffect(() => {
    const onMouseDown = (e: MouseEvent) => {
      if (!menuOpen) return;
      const target = e.target as Node;
      if (menuWrapRef.current && !menuWrapRef.current.contains(target)) {
        setMenuOpen(false);
      }
    };

    const onKeyDown = (e: KeyboardEvent) => {
      if (!menuOpen) return;
      if (e.key === "Escape") setMenuOpen(false);
    };

    document.addEventListener("mousedown", onMouseDown);
    document.addEventListener("keydown", onKeyDown);

    return () => {
      document.removeEventListener("mousedown", onMouseDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [menuOpen]);

  const TrendingIcon = ({ className = "" }) => (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M4 17l6-6 4 4 6-8"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M20 7v6h-6"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );

  const BellIcon = ({ className = "" }) => (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M13.73 21a2 2 0 01-3.46 0"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  );

  const SearchIcon = ({ className = "" }) => (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M11 19a8 8 0 100-16 8 8 0 000 16z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M21 21l-4.35-4.35"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );

  const ClockIcon = ({ className = "" }) => (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M12 7v6l4 2"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
        stroke="currentColor"
        strokeWidth="2"
      />
    </svg>
  );

  const canNext = !loading && hasNext;

  const tabs = [
    { key: "popular" as const, label: "인기", Icon: TrendingIcon },
    { key: "recent" as const, label: "최신", Icon: ClockIcon },
  ];

  const timeRanges = [
    { value: "week", label: "이번 주" },
    { value: "month", label: "이번 달" },
    { value: "year", label: "올해" },
  ];

  return (
    <Layout>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-6 border-b border-gray-100 w-full">
          {tabs.map(({ key, label, Icon }) => {
            const active = activeTab === key;
            return (
              <button
                key={key}
                type="button"
                onClick={() => setActiveTab(key)}
                className={[
                  "relative pb-3 -mb-px flex items-center gap-2 text-sm",
                  active
                    ? "text-gray-900 font-semibold"
                    : "text-gray-400 hover:text-gray-700",
                ].join(" ")}
                aria-current={active ? "page" : undefined}
              >
                <Icon className="w-4 h-4" />
                <span>{label}</span>
                {active && (
                  <span className="absolute left-0 right-0 -bottom-[1px] h-[2px] bg-gray-900 rounded-full" />
                )}
              </button>
            );
          })}
          <div className="flex-1" />
        </div>

        <Link
          to="/notifications"
          className="w-9 h-9 grid place-items-center rounded-md border border-transparent text-gray-500 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-200"
          aria-label="알림"
        >
          <BellIcon className="w-5 h-5" />
        </Link>

        <Link
          to="/search"
          className="w-9 h-9 grid place-items-center rounded-md border border-transparent text-gray-500 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-200"
          aria-label="검색"
        >
          <SearchIcon className="w-5 h-5" />
        </Link>

        <div className="flex items-center gap-2 ml-4">
          <div className="relative">
            <select
              value={timeRange}
              onChange={(e) => setTimeRange(e.target.value)}
              className="appearance-none text-sm bg-white border border-gray-200 rounded-md px-3 py-2 pr-8 text-gray-700 hover:border-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-200"
            >
              {timeRanges.map((r) => (
                <option key={r.value} value={r.value}>
                  {r.label}
                </option>
              ))}
            </select>
            <svg
              viewBox="0 0 24 24"
              className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"
              fill="none"
              aria-hidden="true"
            >
              <path
                d="M7 10l5 5 5-5"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </div>

          <div className="relative" ref={menuWrapRef}>
            <button
              type="button"
              onClick={() => setMenuOpen((v) => !v)}
              className="w-9 h-9 grid place-items-center rounded-md border border-transparent text-gray-500 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-200"
              aria-label="더보기"
              aria-expanded={menuOpen}
            >
              <svg
                viewBox="0 0 24 24"
                className="w-5 h-5"
                fill="none"
                aria-hidden="true"
              >
                <path
                  d="M12 6.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3zM12 13.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3zM12 20.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3z"
                  fill="currentColor"
                />
              </svg>
            </button>

            {menuOpen && (
              <div
                className="absolute right-0 mt-2 w-60 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden z-50"
                role="menu"
              >
                <Link
                  to="/notice"
                  onClick={() => setMenuOpen(false)}
                  className="block px-4 py-3 text-sm text-gray-800 hover:bg-gray-50"
                  role="menuitem"
                >
                  공지사항
                </Link>

                <Link
                  to="/tags"
                  onClick={() => setMenuOpen(false)}
                  className="block px-4 py-3 text-sm text-gray-800 hover:bg-gray-50"
                  role="menuitem"
                >
                  태그 목록
                </Link>

                <Link
                  to="/policy"
                  onClick={() => setMenuOpen(false)}
                  className="block px-4 py-3 text-sm text-gray-800 hover:bg-gray-50"
                  role="menuitem"
                >
                  서비스 정책
                </Link>

                <a
                  href="https://slack.com"
                  target="_blank"
                  rel="noreferrer"
                  className="block px-4 py-3 text-sm text-gray-800 hover:bg-gray-50"
                  role="menuitem"
                  onClick={() => setMenuOpen(false)}
                >
                  Slack
                </a>

                <div className="h-px bg-gray-100" />

                <div className="px-4 py-3">
                  <div className="text-sm font-medium text-gray-800">문의</div>
                  <a
                    href="mailto:aodaod128@naver.com"
                    className="text-sm text-gray-500 hover:underline"
                    onClick={() => setMenuOpen(false)}
                  >
                    contact@Maeng.com
                  </a>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
        {loading
          ? Array.from({ length: PAGE_SIZE }).map((_, i) => (
              <div
                key={i}
                className="bg-white rounded-md shadow-sm h-80 animate-pulse"
              >
                <div className="h-40 bg-gray-200 rounded-t-md"></div>
                <div className="p-4 space-y-3">
                  <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                  <div className="h-4 bg-gray-200 rounded w-1/2"></div>
                </div>
              </div>
            ))
          : posts.map((post) => (
              <article
                key={post.id}
                className="bg-white rounded-md shadow-sm hover:shadow-xl hover:-translate-y-2 transition-all duration-300 flex flex-col overflow-hidden h-full group cursor-pointer"
              >
                <Link
                  to={`/posts/${post.id}`}
                  className="block h-40 overflow-hidden bg-gray-100 relative"
                >
                  {post.thumbnail ? (
                    <img
                      src={post.thumbnail}
                      alt={post.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                  ) : (
                    <div className="w-full h-full flex flex-col items-center justify-center bg-gray-100 text-gray-400">
                      <span className="text-sm">썸네일 없음</span>
                    </div>
                  )}
                </Link>

                <Link
                  to={`/posts/${post.id}`}
                  className="p-4 flex flex-col flex-grow"
                >
                  <div>
                    <h4 className="text-base font-bold text-gray-900 mb-2 line-clamp-1 group-hover:text-black leading-normal">
                      {post.title}
                    </h4>

                    <p className="text-sm text-gray-500 mb-6 line-clamp-3 leading-relaxed flex-grow break-keep">
                      {post.summary || "내용 요약이 없습니다."}
                    </p>

                    <div className="text-xs text-gray-400 mb-1">
                      {post.createdAt
                        ? new Date(post.createdAt).toLocaleDateString("ko-KR", {
                            year: "numeric",
                            month: "long",
                            day: "numeric",
                          })
                        : "-"}{" "}
                      · {post.commentCount ?? 0}개의 댓글
                    </div>
                  </div>
                </Link>

                <div className="px-4 py-2.5 border-t border-gray-50 flex items-center justify-between text-xs">
                  <Link
                    to="#"
                    className="flex items-center gap-2 group-hover:underline"
                  >
                    {post.author?.avatar ? (
                      <img
                        src={post.author.avatar}
                        alt="avatar"
                        className="w-5 h-5 rounded-full object-cover"
                      />
                    ) : (
                      <div className="w-5 h-5 rounded-full bg-gray-300"></div>
                    )}
                    <span className="text-gray-600 font-medium text-xs">
                      by{" "}
                      <span className="font-bold text-gray-800">
                        {post.author?.nickname ?? "unknown"}
                      </span>
                    </span>
                  </Link>

                  <div className="flex items-center gap-1 text-gray-700">
                    <span>{post.likeCount ?? 0}</span>
                  </div>
                </div>
              </article>
            ))}
      </div>

      {!loading && error && (
        <div className="flex flex-col items-center justify-center py-20 text-red-500">
          <div className="text-base font-semibold">{error}</div>
        </div>
      )}

      {!loading && !error && posts.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-gray-500">
          <div className="text-xl font-bold">아직 게시물이 없습니다.</div>
        </div>
      )}

      {!loading && !error && (page > 0 || hasNext) && (
        <div className="flex justify-center gap-3 mt-10">
          <button
            disabled={page === 0}
            onClick={() => fetchPosts(page - 1)}
            className="px-4 py-2 border rounded disabled:opacity-40"
          >
            이전
          </button>

          <div className="px-2 py-2 text-sm text-gray-600">
            {page + 1}페이지
          </div>

          <button
            disabled={!canNext}
            onClick={() => fetchPosts(page + 1)}
            className="px-4 py-2 border rounded disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </Layout>
  );
};

export default Home;
