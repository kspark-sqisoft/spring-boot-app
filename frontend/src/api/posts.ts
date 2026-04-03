/**
 * 게시글 API. 작성·수정·삭제는 Bearer 토큰 필요, 목록·상세는 공개.
 */
import { apiFetch } from '@/lib/api-fetch';

export type PostListItem = {
  id: string;
  title: string;
  createdAt: string;
  authorId: string | null;
  authorName: string | null;
};

export type PostDetail = PostListItem & {
  content: string;
  updatedAt: string;
  imageUrls: string[];
};

type FetchError = Error & { status: number };

function throwHttpError(res: Response, message: string): never {
  const err = new Error(message) as FetchError;
  err.status = res.status;
  throw err;
}

async function parseErrorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const body = (await res.json()) as { message?: string | string[] };
    const m = body.message;
    if (Array.isArray(m)) return m.join(', ');
    if (typeof m === 'string') return m;
  } catch {
    /* ignore */
  }
  return fallback;
}

export const postsKeys = {
  all: ['posts'] as const,
  list: () => [...postsKeys.all, 'list'] as const,
  detail: (id: string) => [...postsKeys.all, 'detail', id] as const,
};

export async function fetchPostList(): Promise<PostListItem[]> {
  const res = await apiFetch('/api/posts', { credentials: 'include' });
  if (!res.ok) {
    throwHttpError(res, await parseErrorMessage(res, '목록을 불러오지 못했습니다.'));
  }
  return (await res.json()) as PostListItem[];
}

export async function fetchPost(id: string): Promise<PostDetail> {
  const res = await apiFetch(`/api/posts/${id}`, { credentials: 'include' });
  if (!res.ok) {
    throwHttpError(res, await parseErrorMessage(res, '글을 불러오지 못했습니다.'));
  }
  return (await res.json()) as PostDetail;
}

export async function uploadPostImage(
  token: string,
  file: File,
): Promise<{ url: string }> {
  const body = new FormData();
  body.set('file', file);
  const res = await apiFetch('/api/posts/images', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body,
    credentials: 'include',
  });
  if (!res.ok) {
    throwHttpError(res, await parseErrorMessage(res, '이미지 업로드에 실패했습니다.'));
  }
  return (await res.json()) as { url: string };
}

export async function createPost(
  token: string,
  input: { title: string; content: string; imageUrls: string[] },
): Promise<PostDetail> {
  const res = await apiFetch('/api/posts', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
    credentials: 'include',
  });
  if (!res.ok) {
    throwHttpError(res, await parseErrorMessage(res, '작성에 실패했습니다.'));
  }
  return (await res.json()) as PostDetail;
}

export async function updatePost(
  token: string,
  id: string,
  input: { title?: string; content?: string; imageUrls?: string[] },
): Promise<PostDetail> {
  const res = await apiFetch(`/api/posts/${id}`, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
    credentials: 'include',
  });
  if (!res.ok) {
    throwHttpError(res, await parseErrorMessage(res, '수정에 실패했습니다.'));
  }
  return (await res.json()) as PostDetail;
}

export async function deletePost(token: string, id: string): Promise<void> {
  const res = await apiFetch(`/api/posts/${id}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
    credentials: 'include',
  });
  if (!res.ok) {
    throwHttpError(res, await parseErrorMessage(res, '삭제에 실패했습니다.'));
  }
}
