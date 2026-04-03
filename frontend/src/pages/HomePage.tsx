import { Link } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useAuthStore } from '@/features/auth/store/auth-store';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

/** 기본 랜딩 — 게시판·인증 안내 */
export function HomePage() {
  const ready = useAuthStore((s) => s.ready);
  const user = useAuthStore((s) => s.user);

  if (!ready) {
    return (
      <div className="text-muted-foreground flex items-center justify-center gap-2 py-16 text-sm">
        <Loader2 className="size-5 animate-spin" aria-hidden />
        세션 확인 중…
      </div>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Spring Boot API 데모</CardTitle>
        <CardDescription>
          상단 메뉴의 글에서 목록으로 갈 수 있습니다. 글쓰기는 목록 페이지
          버튼에서만 열 수 있고, 로그인 후 이용할 수 있습니다.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-wrap gap-2">
        <Button asChild>
          <Link to="/posts">글 목록</Link>
        </Button>
        {user ? (
          <>
            <Button asChild variant="outline">
              <Link to="/posts/new">글쓰기</Link>
            </Button>
            <Button asChild variant="outline">
              <Link to="/profile">프로필</Link>
            </Button>
          </>
        ) : (
          <>
            <Button asChild variant="outline">
              <Link to="/login">로그인</Link>
            </Button>
            <Button asChild variant="outline">
              <Link to="/register">회원가입</Link>
            </Button>
          </>
        )}
      </CardContent>
    </Card>
  );
}
