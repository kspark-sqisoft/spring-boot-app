import { useState, useEffect, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ZodError } from 'zod';
import { fetchPost, postsKeys, updatePost } from '@/api/posts';
import { parseCreatePostForm, postIdSchema } from '@/schemas/post-forms';
import { useAuthStore } from '@/features/auth/store/auth-store';
import { CreateSubmitButton } from '@/features/posts/components/CreateSubmitButton';
import { PostImageAttachments } from '@/features/posts/components/PostImageAttachments';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';

type FetchErr = Error & { status?: number };

export function PostEditPage() {
  const { id: rawId = '' } = useParams<{ id: string }>();
  const parsedId = postIdSchema.safeParse(rawId);
  const id = parsedId.success ? parsedId.data : '';

  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const accessToken = useAuthStore((s) => s.accessToken);

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [imageUrls, setImageUrls] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const { data, isPending, isError, error: loadError } = useQuery({
    queryKey: postsKeys.detail(id),
    queryFn: () => fetchPost(id),
    enabled: Boolean(id),
  });

  useEffect(() => {
    if (!data) return;
    setTitle(data.title);
    setContent(data.content);
    setImageUrls(data.imageUrls ?? []);
  }, [data]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!accessToken || !id) return;
    let parsed;
    try {
      parsed = parseCreatePostForm({ title, content, imageUrls });
    } catch (err) {
      if (err instanceof ZodError) {
        setError(err.issues.map((i) => i.message).join(' '));
        return;
      }
      throw err;
    }
    setPending(true);
    try {
      await updatePost(accessToken, id, {
        title: parsed.title,
        content: parsed.content,
        imageUrls: parsed.imageUrls,
      });
      await queryClient.invalidateQueries({ queryKey: postsKeys.detail(id) });
      await queryClient.invalidateQueries({ queryKey: postsKeys.list() });
      navigate(`/posts/${id}`, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : '수정에 실패했습니다.');
    } finally {
      setPending(false);
    }
  }

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
    const st = (loadError as FetchErr)?.status;
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
        {loadError instanceof Error
          ? loadError.message
          : '불러오지 못했습니다.'}
      </p>
    );
  }

  return (
    <form className="space-y-4" onSubmit={onSubmit}>
      <Button type="button" variant="ghost" size="sm" asChild>
        <Link to={`/posts/${id}`}>← 글 보기</Link>
      </Button>
      <div className="space-y-2">
        <Label htmlFor="edit-title">제목</Label>
        <Input
          id="edit-title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={200}
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="edit-content">내용</Label>
        <Textarea
          id="edit-content"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={12}
          required
          className="min-h-[200px] resize-y"
        />
      </div>
      <PostImageAttachments
        urls={imageUrls}
        onChange={setImageUrls}
        disabled={pending}
      />
      {error ? <p className="text-destructive text-sm">{error}</p> : null}
      <div className="flex flex-wrap justify-end gap-2 pt-2">
        <Button type="button" variant="outline" asChild>
          <Link to={`/posts/${id}`}>취소</Link>
        </Button>
        <CreateSubmitButton pending={pending} label="저장" />
      </div>
    </form>
  );
}
