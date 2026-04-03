import { Outlet, useLocation } from 'react-router-dom';

/**
 * 게시판 구역: 목록 루트(`/posts`)에서만 안내 문구 표시
 */
export function PostsLayout() {
  const { pathname } = useLocation();
  const showIntro =
    pathname === '/posts' || pathname === '/posts/';

  return (
    <div className="space-y-4">
      {showIntro ? (
        <p className="text-muted-foreground text-sm">
          목록은 로그인 없이 볼 수 있고, 글쓰기는 로그인이 필요합니다.
        </p>
      ) : null}
      <Outlet />
    </div>
  );
}
