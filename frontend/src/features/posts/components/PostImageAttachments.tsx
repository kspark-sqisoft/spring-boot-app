import { useState } from 'react';
import { ImagePlus, Loader2, X } from 'lucide-react';
import { uploadPostImage } from '@/api/posts';
import { useAuthStore } from '@/features/auth/store/auth-store';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';

const MAX_IMAGES = 5;
const MAX_BYTES = 5 * 1024 * 1024;

type PostImageAttachmentsProps = {
  urls: string[];
  onChange: (urls: string[]) => void;
  disabled?: boolean;
};

export function PostImageAttachments({
  urls,
  onChange,
  disabled = false,
}: PostImageAttachmentsProps) {
  const accessToken = useAuthStore((s) => s.accessToken);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !accessToken) return;
    if (urls.length >= MAX_IMAGES) {
      setError(`이미지는 최대 ${MAX_IMAGES}장입니다.`);
      return;
    }
    if (file.size > MAX_BYTES) {
      setError('파일은 5MB 이하여야 합니다.');
      return;
    }
    setError(null);
    setUploading(true);
    try {
      const { url } = await uploadPostImage(accessToken, file);
      onChange([...urls, url]);
    } catch (err) {
      setError(err instanceof Error ? err.message : '업로드 실패');
    } finally {
      setUploading(false);
    }
  }

  function removeAt(i: number) {
    onChange(urls.filter((_, j) => j !== i));
  }

  return (
    <div className="space-y-2">
      <Label>본문 이미지 (선택, 최대 {MAX_IMAGES}장)</Label>
      <div className="flex flex-wrap gap-2">
        {urls.map((u, i) => (
          <div
            key={u}
            className="border-border relative inline-flex h-16 w-16 overflow-hidden rounded-md border"
          >
            <img src={u} alt="" className="size-full object-cover" />
            <button
              type="button"
              disabled={disabled}
              onClick={() => removeAt(i)}
              className="bg-background/90 absolute top-0.5 right-0.5 rounded p-0.5"
              aria-label="첨부 제거"
            >
              <X className="size-3.5" />
            </button>
          </div>
        ))}
      </div>
      {urls.length < MAX_IMAGES ? (
        <div>
          <input
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            className="hidden"
            id="post-img-upload"
            disabled={disabled || uploading || !accessToken}
            onChange={(e) => void onPick(e)}
          />
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="gap-1.5"
            disabled={disabled || uploading || !accessToken}
            onClick={() => document.getElementById('post-img-upload')?.click()}
          >
            {uploading ? (
              <Loader2 className="size-3.5 animate-spin" aria-hidden />
            ) : (
              <ImagePlus className="size-3.5" aria-hidden />
            )}
            이미지 추가
          </Button>
        </div>
      ) : null}
      {error ? <p className="text-destructive text-xs">{error}</p> : null}
      {!accessToken ? (
        <p className="text-muted-foreground text-xs">
          이미지 업로드는 로그인 후 가능합니다.
        </p>
      ) : null}
    </div>
  );
}
