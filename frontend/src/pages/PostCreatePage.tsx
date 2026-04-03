import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ZodError } from 'zod';
import { createPost } from '@/api/posts';
import { parseCreatePostForm } from '@/schemas/post-forms';
import { useAuthStore } from '@/features/auth/store/auth-store';
import { CreateSubmitButton } from '@/features/posts/components/CreateSubmitButton';
import { PostImageAttachments } from '@/features/posts/components/PostImageAttachments';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';

export function PostCreatePage() {
  const navigate = useNavigate();
  const accessToken = useAuthStore((s) => s.accessToken);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [imageUrls, setImageUrls] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!accessToken) {
      setError('로그인이 필요합니다.');
      return;
    }
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
      const post = await createPost(accessToken, parsed);
      navigate(`/posts/${post.id}`, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : '작성에 실패했습니다.');
    } finally {
      setPending(false);
    }
  }

  return (
    <form className="space-y-4" onSubmit={onSubmit}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Button type="button" variant="ghost" size="sm" asChild>
          <Link to="/posts">← 목록</Link>
        </Button>
      </div>
      <div className="space-y-2">
        <Label htmlFor="post-title">제목</Label>
        <Input
          id="post-title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={200}
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="post-content">내용</Label>
        <Textarea
          id="post-content"
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
          <Link to="/posts">취소</Link>
        </Button>
        <CreateSubmitButton pending={pending} />
      </div>
    </form>
  );
}
