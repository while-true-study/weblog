import React, { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/useAuthStore";

type Props = {
  children: React.ReactNode;
};

const Layout: React.FC<Props> = ({ children }) => {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuthStore();

  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (!menuRef.current) return;
      if (!menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const nickname = user?.nickname ?? "";
  const avatarUrl =
    (user as any)?.avatar ||
    (nickname
      ? `https://ui-avatars.com/api/?name=${encodeURIComponent(nickname)}`
      : "https://ui-avatars.com/api/?name=user");

  return (
    <div className="min-h-screen bg-[#f8f9fa]">
      {/* Header */}
      <header className="bg-white border-b border-gray-100">
        <div className="max-w-[1728px] mx-auto px-4 h-16 flex items-center justify-between">
          {/* Logo */}
          <Link to="/posts" className="text-2xl font-extrabold text-gray-900">
            velog
          </Link>

          {/* Right */}
          <div className="flex items-center gap-3">
            {isAuthenticated ? (
              <>
                <Link
                  to="/write"
                  className="px-4 py-2 rounded-full border border-gray-200 text-gray-900 font-bold hover:border-gray-400"
                >
                  새 글 작성
                </Link>

                <div className="relative" ref={menuRef}>
                  <button
                    onClick={() => setMenuOpen((v) => !v)}
                    className="w-10 h-10 rounded-full overflow-hidden border border-gray-200 hover:border-gray-400"
                    aria-label="profile"
                  >
                    <img
                      src={avatarUrl}
                      alt="profile"
                      className="w-full h-full object-cover"
                    />
                  </button>

                  {menuOpen && (
                    <div className="absolute right-0 mt-2 w-44 bg-white border border-gray-100 rounded-lg shadow-lg overflow-hidden">
                      <div className="px-4 py-3 text-sm text-gray-700 border-b">
                        <div className="font-bold">{nickname || "사용자"}</div>
                      </div>

                      <button
                        onClick={() => {
                          setMenuOpen(false);
                          logout();
                          navigate("/posts");
                        }}
                        className="w-full text-left px-4 py-3 text-sm hover:bg-gray-50"
                      >
                        로그아웃
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="px-4 py-2 rounded-full border border-gray-200 text-gray-900 font-bold hover:border-gray-400"
                >
                  로그인
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* Body */}
      <main className="max-w-[1728px] mx-auto px-4 py-10">{children}</main>
    </div>
  );
};

export default Layout;
