import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { fetchPostList, postsKeys } from '@/api/posts';
import { useAuthStore } from '@/features/auth/store/auth-store';
import { Button } from '@/components/ui/button';

export function PostList() {
  const ready = useAuthStore((s) => s.ready);
  const user = useAuthStore((s) => s.user);
  const { data, isPending, isError, error } = useQuery({
    queryKey: postsKeys.list(),
    queryFn: fetchPostList,
  });

  if (isPending) {
    return (
      <p className="text-muted-foreground text-sm">목록을 불러오는 중…</p>
    );
  }

  if (isError) {
    return (
      <p className="text-destructive text-sm">
        {error instanceof Error ? error.message : '목록을 불러오지 못했습니다.'}
      </p>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        {ready && user ? (
          <Button asChild size="sm" className="gap-1">
            <Link to="/posts/new">
              <Plus className="size-4" aria-hidden />
              글쓰기
            </Link>
          </Button>
        ) : null}
      </div>
      {data.length === 0 ? (
        <p className="text-muted-foreground text-sm">등록된 글이 없습니다.</p>
      ) : (
        <ul className="border-border divide-border divide-y overflow-hidden rounded-lg border">
          {data.map((post) => (
            <li key={post.id}>
              <Link
                to={`/posts/${post.id}`}
                className="hover:bg-accent/50 block px-3 py-3 transition-colors sm:px-4"
              >
                <span className="font-medium">{post.title}</span>
                <div className="text-muted-foreground mt-1 text-xs">
                  {(post.authorName ?? '익명') +
                    ' · ' +
                    new Date(post.createdAt).toLocaleString()}
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
