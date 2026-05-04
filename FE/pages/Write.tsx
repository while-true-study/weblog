import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { postApi } from "../services/api";

const Write: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const editId = searchParams.get("edit");

  const [loading, setLoading] = useState(false);

  // Form State
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [currentTag, setCurrentTag] = useState("");

  useEffect(() => {
    if (editId) loadPostForEdit(Number(editId));
  }, [editId]);

  const loadPostForEdit = async (id: number) => {
    try {
      const res = await postApi.getDetail(id);
      if (res.data.success) {
        const post = res.data.data;
        setTitle(post.title);
        setContent(post.content);
        setTags(post.tags ?? []);
      }
    } catch (err) {
      navigate("/");
    }
  };

  const handleTagKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      const trimmed = currentTag.trim();
      if (trimmed && !tags.includes(trimmed)) {
        setTags([...tags, trimmed]);
        setCurrentTag("");
      }
    } else if (e.key === "Backspace" && !currentTag && tags.length > 0) {
      setTags(tags.slice(0, -1));
    }
  };

  const handlePublish = async () => {
    if (!title.trim() || !content.trim())
      return alert("제목과 내용을 입력하세요.");
    setLoading(true);
    try {
      const payload = {
        title,
        content,
        tags,
        status: "PUBLISHED" as const,
      };
      if (editId) {
        await postApi.update(Number(editId), payload);
        navigate(`/posts/${editId}`);
      } else {
        await postApi.create(payload);
        navigate("/posts");
      }
    } catch (err) {
      alert("게시글 저장 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex h-screen bg-white">
      {/* Editor Area (Left) */}
      <div className="flex-1 flex flex-col p-8 md:p-12 overflow-y-auto">
        {/* Title Input */}
        <input
          type="text"
          placeholder="제목을 입력하세요"
          className="text-4xl md:text-5xl font-bold placeholder-gray-300 border-none outline-none w-full bg-transparent mb-6"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />

        {/* Tag Input */}
        <div className="flex flex-wrap items-center gap-2 mb-8">
          {tags.map((tag) => (
            <span
              key={tag}
              className="bg-gray-100 text-primary px-3 py-1 rounded-3xl font-medium cursor-pointer hover:bg-gray-200 transition-colors"
              onClick={() => setTags(tags.filter((t) => t !== tag))}
            >
              {tag}
            </span>
          ))}
          <input
            type="text"
            placeholder="태그를 입력하세요"
            className="outline-none text-lg placeholder-gray-300 min-w-[150px]"
            value={currentTag}
            onChange={(e) => setCurrentTag(e.target.value)}
            onKeyDown={handleTagKeyDown}
          />
        </div>

        {/* Markdown Editor Placeholder */}
        <textarea
          className="flex-grow resize-none border-none outline-none text-lg leading-relaxed font-mono text-gray-800"
          placeholder="당신의 이야기를 적어보세요..."
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />

        {/* Bottom Toolbar */}
        <div className="fixed bottom-0 left-0 w-full md:w-1/2 bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)] px-6 py-4 flex justify-between items-center z-10">
          <button
            onClick={() => navigate(-1)}
            className="flex items-center gap-2 text-gray-600 font-medium hover:text-gray-900"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            나가기
          </button>
          <div className="flex gap-3">
            <button className="px-4 py-2 text-primary font-bold hover:bg-gray-50 rounded">
              임시저장
            </button>
            <button
              onClick={handlePublish}
              disabled={loading}
              className="px-6 py-2 bg-primary text-white font-bold rounded hover:bg-teal-600 transition-colors disabled:opacity-50"
            >
              출간하기
            </button>
          </div>
        </div>
      </div>

      {/* Preview Area (Right - Hidden on mobile) */}
      <div className="hidden md:flex flex-1 flex-col bg-gray-50 p-12 overflow-y-auto border-l border-gray-100">
        <h1 className="text-4xl md:text-5xl font-bold text-gray-900 mb-8 break-keep">
          {title || "제목 미리보기"}
        </h1>
        <div className="prose prose-lg max-w-none text-gray-800">
          {content ? (
            <div className="whitespace-pre-wrap">{content}</div>
          ) : (
            <p className="text-gray-400 italic">
              내용이 미리보기로 표시됩니다.
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default Write;
