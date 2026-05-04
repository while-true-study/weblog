import React, { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import axios from "axios";

import Layout from "../components/Layout";
import { postApi } from "../services/api";
import { PostDetail as PostDetailType, Comment } from "../types";
import { useAuthStore } from "../store/useAuthStore";

const PostDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();

  const [post, setPost] = useState<PostDetailType | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);

  const [likeCount, setLikeCount] = useState(0);
  const [liked, setLiked] = useState<boolean | null>(null);
  const [likeLoading, setLikeLoading] = useState(false);
  const [commentContent, setCommentContent] = useState("");
  const [commentsError, setCommentsError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    const postId = Number(id);
    if (Number.isNaN(postId)) {
      navigate("/posts");
      return;
    }
    load(postId);
  }, [id]);

  const load = async (postId: number) => {
    setLoading(true);
    setCommentsError("댓글 기능은 아직 백엔드 미구현 상태입니다.");

    try {
      const postRes = await postApi.getDetail(postId);
      if (!postRes.data?.success) {
        navigate("/");
        return;
      }
      const p = postRes.data.data;
      setPost(p);
      setLikeCount(p.likeCount ?? 0);
      setLiked(null);
      setComments([]);
    } catch (e) {
      console.error(e);
      navigate("/");
    } finally {
      setLoading(false);
    }
  };

  const handleToggleLike = async () => {
    if (!post || likeLoading) return;

    if (!isAuthenticated) {
      alert("로그인이 필요합니다.");
      return;
    }

    setLikeLoading(true);

    try {
      const res = await postApi.toggleLike(post.id);
      const toggleResult = res.data.data;

      setLiked(toggleResult.liked);
      setLikeCount(toggleResult.likeCount);
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        alert("로그인이 필요한 기능입니다.");
      } else {
        alert("좋아요 처리에 실패했습니다.");
      }
    } finally {
      setLikeLoading(false);
    }
  };

  const handleSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!post || !commentContent.trim()) return;

    alert("댓글 기능은 아직 백엔드 미구현 상태입니다.");
  };

  if (loading) {
    return (
      <Layout>
        <div className="min-h-[70vh] flex items-center justify-center text-gray-500">
          로딩중...
        </div>
      </Layout>
    );
  }

  if (!post) {
    return (
      <Layout>
        <div className="min-h-[70vh] flex items-center justify-center text-gray-500">
          포스트를 찾을 수 없습니다.
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      {/* velog 라이트 느낌: 흰 배경 + 검정 텍스트 */}
      <div className="bg-white text-gray-900">
        <div className="mx-auto max-w-4xl px-4 pb-24 pt-10">
          {/* 제목 */}
          <h1 className="text-4xl md:text-5xl font-extrabold leading-tight break-keep">
            {post.title}
          </h1>

          {/* 메타 */}
          <div className="mt-6 flex items-center justify-between gap-4">
            <div className="flex items-center gap-3 text-sm">
              <span className="font-bold">
                {post.author?.nickname ?? "알 수 없음"}
              </span>
              <span className="text-gray-300">·</span>
              <span className="text-gray-600">
                {post.createdAt
                  ? new Date(post.createdAt).toLocaleDateString("ko-KR")
                  : ""}
              </span>
              {post.updatedAt && (
                <>
                  <span className="text-gray-300">·</span>
                  <span className="text-gray-500">
                    수정 {new Date(post.updatedAt).toLocaleDateString("ko-KR")}
                  </span>
                </>
              )}
            </div>

            <div className="text-sm text-gray-500">
              조회 {post.viewCount ?? 0}
            </div>
          </div>

          {/* 태그 */}
          <div className="mt-6 flex flex-wrap gap-2">
            {(post.tags ?? []).map((t) => (
              <span
                key={t}
                className="bg-gray-100 text-gray-800 px-4 py-1.5 rounded-3xl font-medium text-sm"
              >
                {t}
              </span>
            ))}
          </div>

          {/* 좋아요: 데스크탑은 왼쪽 고정, 모바일은 본문 상단에 버튼 노출 */}
          <div className="mt-8 flex items-center gap-3 md:hidden">
            <button
              onClick={handleToggleLike}
              disabled={likeLoading}
              className={`px-4 py-2 rounded-full border transition-colors disabled:opacity-60 disabled:cursor-not-allowed ${
                liked
                  ? "border-red-300 bg-red-50 text-red-600"
                  : "border-gray-200 hover:border-gray-400"
              }`}
            >
              {likeLoading ? "처리 중..." : `좋아요 ${likeCount}`}
            </button>
          </div>

          <div className="relative mt-10">
            {/* 데스크탑 좋아요 사이드바 */}
            <div className="hidden md:block absolute -left-24 top-0">
              <div className="sticky top-28 flex flex-col items-center gap-2">
                <button
                  onClick={handleToggleLike}
                  disabled={likeLoading}
                  className={`w-12 h-12 rounded-full border flex items-center justify-center transition-colors disabled:opacity-60 disabled:cursor-not-allowed ${
                    liked
                      ? "border-red-300 bg-red-50 text-red-600"
                      : "border-gray-200 hover:border-gray-400"
                  }`}
                  aria-label="like"
                >
                  {likeLoading ? "..." : "❤"}
                </button>
                <div className="text-sm font-bold text-gray-700">{likeCount}</div>
              </div>
            </div>

            {/* 본문: 마크다운을 velog처럼 */}
            <div className="prose prose-lg max-w-none prose-headings:font-extrabold prose-a:text-teal-600">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {post.content ?? ""}
              </ReactMarkdown>
            </div>
          </div>

          {/* 댓글 */}
          <div className="mt-20 border-t pt-10">
            <h4 className="text-lg font-bold">{comments.length}개의 댓글</h4>

            {commentsError && (
              <div className="mt-6 text-sm text-gray-500">
                {commentsError}{" "}
                <button
                  className="text-teal-600 underline"
                  onClick={() => id && load(Number(id))}
                >
                  다시 시도
                </button>
              </div>
            )}

            {isAuthenticated ? (
              <form onSubmit={handleSubmitComment} className="mt-6">
                <textarea
                  className="w-full p-4 border border-gray-200 rounded-md focus:outline-none focus:border-teal-500 resize-none min-h-[110px]"
                  placeholder="댓글을 작성하세요"
                  value={commentContent}
                  onChange={(e) => setCommentContent(e.target.value)}
                />
                <div className="flex justify-end mt-3">
                  <button
                    type="submit"
                    className="px-5 py-2 bg-teal-600 text-white font-bold rounded hover:bg-teal-700"
                  >
                    댓글 작성
                  </button>
                </div>
              </form>
            ) : (
              <div className="mt-6 text-center py-10 bg-gray-50 rounded text-gray-600">
                <Link to="/login" className="text-teal-600 underline">
                  로그인
                </Link>{" "}
                후 댓글을 작성할 수 있습니다.
              </div>
            )}

            <div className="mt-10 space-y-8">
              {comments.map((c) => (
                <div key={c.id} className="border-b border-gray-100 pb-8">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <img
                        src={`https://ui-avatars.com/api/?name=${encodeURIComponent(
                          c.author?.nickname ?? "user"
                        )}`}
                        className="w-10 h-10 rounded-full bg-gray-200"
                        alt="commenter"
                      />
                      <div>
                        <div className="font-bold">
                          {c.author?.nickname ?? "알 수 없음"}
                        </div>
                        <div className="text-xs text-gray-400">
                          {c.createdAt
                            ? new Date(c.createdAt).toLocaleDateString("ko-KR")
                            : ""}
                        </div>
                      </div>
                    </div>

                    {user?.id === c.author?.id && (
                      <button className="text-xs text-gray-400 hover:text-red-500">
                        삭제
                      </button>
                    )}
                  </div>

                  <p className="mt-4 text-gray-800 leading-relaxed">
                    {c.content}
                  </p>
                </div>
              ))}
            </div>
          </div>

          {/* 디버깅용: 진짜로 값 들어오는지 1분 확인할 때만 켜세요 */}
          {/* <pre className="mt-10 text-xs bg-gray-50 p-4 rounded">{JSON.stringify(post, null, 2)}</pre> */}
        </div>
      </div>
    </Layout>
  );
};

export default PostDetail;
