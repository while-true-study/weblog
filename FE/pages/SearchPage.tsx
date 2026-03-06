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

type SearchStrategy = "infix" | "prefix" | "fulltext" | "fulltextBoolean";

const SearchPage: React.FC = () => {
  const [params, setParams] = useSearchParams();

  const initialQ = useMemo(() => params.get("q") ?? "", [params]);
  const initialStrategy = useMemo<SearchStrategy>(() => {
    const m = params.get("m");
    if (m === "prefix") return "prefix";
    if (m === "fulltext") return "fulltext";
    if (m === "fulltextBoolean") return "fulltextBoolean";
    return "infix";
  }, [params]);

  const [q, setQ] = useState(initialQ);
  const [strategy, setStrategy] = useState<SearchStrategy>(initialStrategy);

  const [items, setItems] = useState<PostSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [offset, setOffset] = useState(0);
  const [hasNext, setHasNext] = useState(false);

  const inputRef = useRef<HTMLInputElement | null>(null);
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  // 요청 경쟁 방지용
  const abortRef = useRef<AbortController | null>(null);
  const requestSeqRef = useRef(0);

  useEffect(() => {
    setQ(initialQ);
  }, [initialQ]);

  useEffect(() => {
    setStrategy(initialStrategy);
  }, [initialStrategy]);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const callSearchApi = async (
    selectedStrategy: SearchStrategy,
    payload: { keyword: string; offset: number; limit: number },
    config?: { signal?: AbortSignal },
  ) => {
    switch (selectedStrategy) {
      case "prefix":
        return postApi.searchPostsPrefix(payload, config);
      case "fulltext":
        return postApi.searchPostsFulltext(payload, config);
      case "fulltextBoolean":
        return postApi.searchPostsFulltextBoolean(payload, config);
      case "infix":
      default:
        return postApi.searchPostsInfix(payload, config);
    }
  };

  // 검색 실행 함수
  const fetchSearch = async (
    keyword: string,
    nextOffset: number,
    append: boolean,
    selectedStrategy: SearchStrategy,
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

    // 이전 요청 취소
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const seq = ++requestSeqRef.current;

    try {
      if (append) setLoadingMore(true);
      else setLoading(true);

      setError(null);

      const res = await callSearchApi(
        selectedStrategy,
        {
          keyword: safeKeyword,
          offset: nextOffset,
          limit: PAGE_SIZE,
        },
        { signal: controller.signal },
      );

      // 늦게 도착한 응답이 최신 결과를 덮지 않도록 방지
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

  // 타이핑 멈추면 자동 검색 + URL q/m 갱신(replace)
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

    setParams({ q: keyword, m: strategy }, { replace: true });

    const t = window.setTimeout(() => {
      fetchSearch(keyword, 0, false, strategy);
    }, DEBOUNCE_MS);

    return () => window.clearTimeout(t);
  }, [q, strategy, setParams]);

  // 무한 스크롤: sentinel이 보이면 다음 페이지 요청
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

        fetchSearch(keyword, offset, true, strategy);
      },
      { root: null, rootMargin: "400px", threshold: 0 },
    );

    io.observe(el);
    return () => io.disconnect();
  }, [q, offset, hasNext, loading, loadingMore, strategy]);

  const strategyLabelMap: Record<SearchStrategy, string> = {
    infix: "infix(%키워드%)",
    prefix: "prefix(키워드%)",
    fulltext: "fulltext(natural)",
    fulltextBoolean: "fulltext(boolean)",
  };

  return (
    <Layout>
      <div className="min-h-[70vh] flex items-start justify-center pt-16 px-4">
        <div className="w-full max-w-3xl">
          <div className="flex items-center justify-between gap-3 mb-3">
            <div className="text-sm text-gray-700 font-semibold">검색</div>

            {/* 실험용 전략 토글 */}
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => setStrategy("infix")}
                className={[
                  "px-3 py-1.5 rounded-md text-xs border",
                  strategy === "infix"
                    ? "border-gray-900 text-gray-900"
                    : "border-gray-200 text-gray-500 hover:text-gray-700",
                ].join(" ")}
              >
                infix(%키워드%)
              </button>

              <button
                type="button"
                onClick={() => setStrategy("prefix")}
                className={[
                  "px-3 py-1.5 rounded-md text-xs border",
                  strategy === "prefix"
                    ? "border-gray-900 text-gray-900"
                    : "border-gray-200 text-gray-500 hover:text-gray-700",
                ].join(" ")}
              >
                prefix(키워드%)
              </button>

              <button
                type="button"
                onClick={() => setStrategy("fulltext")}
                className={[
                  "px-3 py-1.5 rounded-md text-xs border",
                  strategy === "fulltext"
                    ? "border-gray-900 text-gray-900"
                    : "border-gray-200 text-gray-500 hover:text-gray-700",
                ].join(" ")}
              >
                fulltext
              </button>

              <button
                type="button"
                onClick={() => setStrategy("fulltextBoolean")}
                className={[
                  "px-3 py-1.5 rounded-md text-xs border",
                  strategy === "fulltextBoolean"
                    ? "border-gray-900 text-gray-900"
                    : "border-gray-200 text-gray-500 hover:text-gray-700",
                ].join(" ")}
              >
                fulltext boolean
              </button>
            </div>
          </div>

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

          {/* 상태 메시지 */}
          <div className="mt-4 text-sm text-gray-500">
            {q.trim() ? (
              <div>
                <span className="font-medium text-gray-700">“{q.trim()}”</span>
                <span className="ml-2">
                  [{strategyLabelMap[strategy]}]{" "}
                  {loading ? "검색 중..." : `${items.length}개 로드됨`}
                </span>
              </div>
            ) : (
              <div>키워드를 입력하면 결과가 표시됩니다.</div>
            )}
          </div>

          {error && <div className="mt-3 text-sm text-red-500">{error}</div>}

          {/* 결과 리스트 */}
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

            {/* 무한 스크롤 트리거 */}
            <div ref={sentinelRef} className="h-10" />

            {loadingMore && (
              <div className="py-6 text-center text-sm text-gray-500">
                더 불러오는 중...
              </div>
            )}

            {!loading &&
              !loadingMore &&
              q.trim() &&
              items.length > 0 &&
              !hasNext && (
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
