import React, { useEffect, useState, useRef } from "react";
import { Link } from "react-router-dom";
import Layout from "../components/Layout";
import { postApi } from "../services/api";
import { PostSummary } from "../types";

const PAGE_SIZE = 12;

const Home: React.FC = () => {
  const [posts, setPosts] = useState<PostSummary[]>([]);
  const [loading, setLoading] = useState(true);

  const [menuOpen, setMenuOpen] = useState(false);
  const menuWrapRef = useRef<HTMLDivElement | null>(null);

  const [activeTab, setActiveTab] = useState<
    "trending" | "recent" | "feed" | "recommended"
  >("trending");
  const [timeRange, setTimeRange] = useState("week"); // 지금은 UI만, 백엔드 반영은 추후

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    // 탭/기간 바뀌면 0페이지부터 다시
    fetchPosts(0);
  }, [activeTab, timeRange]);

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

  const resolveSort = () => {
    if (activeTab === "trending") return "viewCount,DESC";
    if (activeTab === "recent") return "createdAt,DESC";
    if (activeTab === "recommended") return "viewCount,DESC"; // likeCount 구현되면 "likeCount,DESC"
    return "createdAt,DESC"; // feed 임시
  };

  const fetchPosts = async (nextPage: number) => {
    setLoading(true);
    try {
      const res = await postApi.getList({
        page: nextPage,
        size: PAGE_SIZE,
        sort: resolveSort(),
        // keyword/categoryId/tag 연결되면 여기 같이 넣으면 됨
      });

      if (res.data.success) {
        const data = res.data.data;
        setPosts(data.content);
        setPage(data.page);
        setTotalPages(data.totalPages);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };
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

  const StarIcon = ({ className = "" }) => (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M12 3l2.8 6.1 6.7.6-5 4.3 1.5 6.5L12 17.9 6 20.5 7.5 14 2.5 9.7l6.7-.6L12 3z"
        stroke="currentColor"
        strokeWidth="2"
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

  const RssIcon = ({ className = "" }) => (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M4 11a9 9 0 019 9"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M4 4a16 16 0 0116 16"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path d="M6 20a2 2 0 100-4 2 2 0 000 4z" fill="currentColor" />
    </svg>
  );

  const DotsIcon = ({ className = "" }) => (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M12 6.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3zM12 13.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3zM12 20.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3z"
        fill="currentColor"
      />
    </svg>
  );

  const canNext = !loading && page + 1 < totalPages;

  const tabs = [
    { key: "trending" as const, label: "트렌딩", Icon: TrendingIcon },
    { key: "recommended" as const, label: "추천", Icon: StarIcon },
    { key: "recent" as const, label: "최신", Icon: ClockIcon },
    { key: "feed" as const, label: "피드", Icon: RssIcon },
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

        {/* 검색 */}
        <Link
          to="/search"
          className="w-9 h-9 grid place-items-center rounded-md border border-transparent text-gray-500 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-200"
          aria-label="검색"
        >
          <SearchIcon className="w-5 h-5" />
        </Link>

        {/* 우측 기간 드롭다운 + 점3개 */}
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

          {/* 점3개 + 메뉴 */}
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
                {/* 메뉴 아이템 */}
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

                {/* 문의 섹션 */}
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
          ? Array.from({ length: 10 }).map((_, i) => (
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
                      {/* ... */}
                    </div>
                  )}
                </Link>
                <Link
                  to={`/posts/${post.id}`}
                  className="p-4 flex flex-col flex-grow"
                >
                  <div className="">
                    <h4 className="text-base font-bold text-gray-900 mb-2 line-clamp-1 group-hover:text-black leading-normal">
                      {post.title}
                    </h4>

                    <p className="text-sm text-gray-500 mb-6 line-clamp-3 leading-relaxed flex-grow break-keep">
                      {post.summary || "내용 요약이 없습니다."}
                    </p>

                    <div className="text-xs text-gray-400 mb-1">
                      {new Date(post.createdAt).toLocaleDateString("ko-KR", {
                        year: "numeric",
                        month: "long",
                        day: "numeric",
                      })}{" "}
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
                    {/* ... */}
                    <span>{post.likeCount ?? 0}</span>
                  </div>
                </div>
              </article>
            ))}
      </div>

      {!loading && posts.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-gray-500">
          <div className="text-xl font-bold">아직 게시물이 없습니다.</div>
        </div>
      )}

      {/* 페이지 이동(간단) */}
      {!loading && totalPages > 1 && (
        <div className="flex justify-center gap-3 mt-10">
          <button
            disabled={page === 0}
            onClick={() => fetchPosts(page - 1)}
            className="px-4 py-2 border rounded disabled:opacity-40"
          >
            이전
          </button>
          <div className="px-2 py-2 text-sm text-gray-600">
            {page + 1} / {totalPages}
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
