import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Trash2 } from 'lucide-react';
import { deletePost, fetchPost, postsKeys } from '@/api/posts';
import { postIdSchema } from '@/schemas/post-forms';
import { useAuthStore } from '@/features/auth/store/auth-store';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';

type FetchErr = Error & { status?: number };

export function PostDetailPage() {
  const { id: rawId = '' } = useParams<{ id: string }>();
  const parsedId = postIdSchema.safeParse(rawId);
  const id = parsedId.success ? parsedId.data : '';

  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const accessToken = useAuthStore((s) => s.accessToken);

  const [deleteOpen, setDeleteOpen] = useState(false);

  const { data, isPending, isError, error } = useQuery({
    queryKey: postsKeys.detail(id),
    queryFn: () => fetchPost(id),
    enabled: Boolean(id),
  });

  const del = useMutation({
    mutationFn: async () => {
      if (!accessToken) throw new Error('로그인이 필요합니다.');
      await deletePost(accessToken, id);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: postsKeys.list() });
      navigate('/posts', { replace: true });
    },
  });

  if (!parsedId.success) {
    return (
      <p className="text-muted-foreground text-sm">잘못된 글 주소입니다.</p>
    );
  }

  if (isPending) {
    return (
      <p className="text-muted-foreground text-sm">불러오는 중…</p>
    );
  }

  if (isError) {
    const st = (error as FetchErr)?.status;
    if (st === 404) {
      return (
        <p className="text-muted-foreground text-sm">
          글을 찾을 수 없습니다.{' '}
          <Link to="/posts" className="text-primary underline-offset-4 hover:underline">
            목록으로
          </Link>
        </p>
      );
    }
    return (
      <p className="text-destructive text-sm">
        {error instanceof Error ? error.message : '불러오지 못했습니다.'}
      </p>
    );
  }

  const isOwner =
    user && data.authorId != null && data.authorId === user.id;

  return (
    <article className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <Button type="button" variant="ghost" size="sm" asChild>
          <Link to="/posts">← 목록</Link>
        </Button>
        {isOwner ? (
          <div className="flex flex-wrap gap-2">
            <Button type="button" variant="outline" size="sm" asChild>
              <Link to={`/posts/${id}/edit`} className="gap-1">
                <Pencil className="size-3.5" aria-hidden />
                수정
              </Link>
            </Button>
            <Button
              type="button"
              variant="destructive"
              size="sm"
              className="gap-1"
              onClick={() => setDeleteOpen(true)}
            >
              <Trash2 className="size-3.5" aria-hidden />
              삭제
            </Button>
          </div>
        ) : null}
      </div>

      <header className="space-y-1">
        <h2 className="font-heading text-lg font-semibold sm:text-xl">
          {data.title}
        </h2>
        <p className="text-muted-foreground text-xs sm:text-sm">
          {(data.authorName ?? '익명') +
            ' · 작성 ' +
            new Date(data.createdAt).toLocaleString()}
          {data.updatedAt !== data.createdAt
            ? ' · 수정 ' + new Date(data.updatedAt).toLocaleString()
            : ''}
        </p>
      </header>

      <div className="border-border space-y-4 border-t pt-4">
        {data.imageUrls?.length ? (
          <ul className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {data.imageUrls.map((url) => (
              <li key={url} className="overflow-hidden rounded-lg border">
                <img
                  src={url}
                  alt=""
                  className="max-h-80 w-full object-contain"
                />
              </li>
            ))}
          </ul>
        ) : null}
        <div className="whitespace-pre-wrap text-sm leading-relaxed">
          {data.content}
        </div>
      </div>

      <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>이 글을 삭제할까요?</AlertDialogTitle>
            <AlertDialogDescription>
              삭제하면 되돌릴 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {del.error ? (
            <p className="text-destructive text-sm">
              {del.error instanceof Error
                ? del.error.message
                : '삭제에 실패했습니다.'}
            </p>
          ) : null}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={del.isPending}>취소</AlertDialogCancel>
            <Button
              type="button"
              variant="destructive"
              disabled={del.isPending}
              onClick={() => void del.mutateAsync()}
            >
              {del.isPending ? '삭제 중…' : '삭제'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </article>
  );
}
