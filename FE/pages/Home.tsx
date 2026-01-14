import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Layout from "../components/Layout";
import { postApi } from "../services/api";
import { PostSummary } from "../types";

const PAGE_SIZE = 12;

const Home: React.FC = () => {
  const [posts, setPosts] = useState<PostSummary[]>([]);
  const [loading, setLoading] = useState(true);

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

  const canNext = !loading && page + 1 < totalPages;

  return (
    <Layout>
      {/* ... 기존 탭 UI 그대로 ... */}

      {/* Card Grid */}
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
