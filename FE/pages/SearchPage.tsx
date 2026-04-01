import React, { useEffect, useMemo, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import Layout from "../components/Layout";
import { postApi } from "../services/api";

const PAGE_SIZE = 20;
const DEBOUNCE_MS = 250;

const SearchIcon = ({ className = "" }) => (
  <svg viewBox="0 0 24 24" className={className} fill="none" aria-hidden="true">
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

type Author = { id: number; nickname: string };
type PostSummary = {
  id: number;
  title: string;
  summary?: string;
  author?: Author;
  createdAt: string;
  likeCount?: number;
  viewCount?: number;
};

const SearchPage: React.FC = () => {
  const [params, setParams] = useSearchParams();

  const initialQ = useMemo(() => params.get("q") ?? "", [params]);

  const [q, setQ] = useState(initialQ);

  const [items, setItems] = useState<PostSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [offset, setOffset] = useState(0);
  const [hasNext, setHasNext] = useState(false);

  const inputRef = useRef<HTMLInputElement | null>(null);
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  const abortRef = useRef<AbortController | null>(null);
  const requestSeqRef = useRef(0);

  useEffect(() => {
    setQ(initialQ);
  }, [initialQ]);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const fetchSearch = async (
    keyword: string,
    nextOffset: number,
    append: boolean,
  ) => {
    const safeKeyword = keyword.trim();

    if (!safeKeyword) {
      abortRef.current?.abort();
      setItems([]);
      setOffset(0);
      setHasNext(false);
      setError(null);
      return;
    }

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const seq = ++requestSeqRef.current;

    try {
      if (append) setLoadingMore(true);
      else setLoading(true);

      setError(null);

      const res = await postApi.searchPostsEs(
        { keyword: safeKeyword, offset: nextOffset, limit: PAGE_SIZE },
        { signal: controller.signal },
      );

      if (seq !== requestSeqRef.current) return;

      if (res.data?.success) {
        const data = res.data.data;
        const newItems: PostSummary[] = data.items ?? [];

        setItems((prev) => (append ? [...prev, ...newItems] : newItems));
        setOffset(data.nextOffset ?? nextOffset + newItems.length);
        setHasNext(Boolean(data.hasNext));
      } else {
        setError("검색에 실패했습니다.");
        if (!append) setItems([]);
      }
    } catch (e: any) {
      if (e?.name === "CanceledError" || e?.name === "AbortError") return;
      setError("검색 중 오류가 발생했습니다.");
      if (!append) setItems([]);
    } finally {
      if (append) setLoadingMore(false);
      else setLoading(false);
    }
  };

  // 타이핑 멈추면 자동 검색 + URL q 갱신
  useEffect(() => {
    const keyword = q.trim();

    if (!keyword) {
      setParams({}, { replace: true });
      setItems([]);
      setOffset(0);
      setHasNext(false);
      setError(null);
      return;
    }

    setParams({ q: keyword }, { replace: true });

    const t = window.setTimeout(() => {
      fetchSearch(keyword, 0, false);
    }, DEBOUNCE_MS);

    return () => window.clearTimeout(t);
  }, [q, setParams]);

  // 무한 스크롤
  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;

    const io = new IntersectionObserver(
      (entries) => {
        const first = entries[0];
        if (!first.isIntersecting) return;
        if (loading || loadingMore) return;
        if (!hasNext) return;

        const keyword = q.trim();
        if (!keyword) return;

        fetchSearch(keyword, offset, true);
      },
      { root: null, rootMargin: "400px", threshold: 0 },
    );

    io.observe(el);
    return () => io.disconnect();
  }, [q, offset, hasNext, loading, loadingMore]);

  return (
    <Layout>
      <div className="min-h-[70vh] flex items-start justify-center pt-16 px-4">
        <div className="w-full max-w-3xl">
          <div className="mb-3 text-sm text-gray-700 font-semibold">검색</div>

          <div className="relative">
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">
              <SearchIcon className="w-6 h-6" />
            </span>

            <input
              ref={inputRef}
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="검색어를 입력하세요"
              className="w-full h-14 rounded-md border border-gray-200 bg-white pl-14 pr-4 text-base text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-200"
              aria-label="검색"
            />
          </div>

          <div className="mt-4 text-sm text-gray-500">
            {q.trim() ? (
              <span>
                <span className="font-medium text-gray-700">"{q.trim()}"</span>
                <span className="ml-2">
                  {loading ? "검색 중..." : `${items.length}개 결과`}
                </span>
              </span>
            ) : (
              <span>키워드를 입력하면 결과가 표시됩니다.</span>
            )}
          </div>

          {error && <div className="mt-3 text-sm text-red-500">{error}</div>}

          <div className="mt-6 space-y-3">
            {!loading && q.trim() && items.length === 0 && !error && (
              <div className="py-12 text-center text-gray-500">
                검색 결과가 없습니다.
              </div>
            )}

            {items.map((p) => (
              <Link
                key={p.id}
                to={`/posts/${p.id}`}
                className="block rounded-md border border-gray-100 bg-white hover:shadow-sm transition px-4 py-4"
              >
                <div className="text-base font-semibold text-gray-900">
                  {p.title}
                </div>
                <div className="mt-2 text-sm text-gray-600 line-clamp-2">
                  {p.summary || "내용 요약이 없습니다."}
                </div>
                <div className="mt-3 text-xs text-gray-400">
                  {new Date(p.createdAt).toLocaleDateString("ko-KR", {
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                  })}
                  {p.author?.nickname ? ` · ${p.author.nickname}` : ""}
                </div>
              </Link>
            ))}

            <div ref={sentinelRef} className="h-10" />

            {loadingMore && (
              <div className="py-6 text-center text-sm text-gray-500">
                더 불러오는 중...
              </div>
            )}

            {!loading && !loadingMore && q.trim() && items.length > 0 && !hasNext && (
              <div className="py-8 text-center text-sm text-gray-400">
                마지막 결과입니다.
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default SearchPage;
