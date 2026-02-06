import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import Layout from "../components/Layout";

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

const SearchPage: React.FC = () => {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();

  const initialQ = useMemo(() => params.get("q") ?? "", [params]);
  const [q, setQ] = useState(initialQ);

  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    setQ(initialQ);
  }, [initialQ]);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const submit = (e?: React.FormEvent) => {
    e?.preventDefault();
    const keyword = q.trim();

    // 빈 검색이면 URL 파라미터 제거만
    if (!keyword) {
      setParams({});
      return;
    }

    // /search?q=키워드 로 유지
    setParams({ q: keyword });

    // 결과 페이지를 따로 만들 계획이면 여기서 navigate("/search/results?q=...") 같은 식으로 변경 가능
  };

  return (
    <Layout>
      <div className="min-h-[70vh] flex items-start justify-center pt-24 px-4">
        <div className="w-full max-w-3xl">
          <form onSubmit={submit}>
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
          </form>

          {/* 임시 결과 영역 (원하면 삭제 가능) */}
          <div className="mt-8 text-sm text-gray-500">
            {q.trim() ? (
              <div>
                <div className="font-medium text-gray-700">
                  “{q.trim()}” 검색 결과
                </div>
                <div className="mt-2">
                  아직 검색 API를 연결하지 않았습니다. (여기에 결과 리스트가
                  들어가면 됩니다.)
                </div>
              </div>
            ) : (
              <div>키워드를 입력하면 검색 결과를 표시할 수 있습니다.</div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default SearchPage;
