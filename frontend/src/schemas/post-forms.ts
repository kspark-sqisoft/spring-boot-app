import { z } from 'zod';

const createPostSchema = z.object({
  title: z.string().trim().min(1, '제목을 입력하세요.').max(200),
  content: z.string().min(1, '내용을 입력하세요.'),
  imageUrls: z.array(z.string()).max(5).default([]),
});

export type CreatePostInput = z.infer<typeof createPostSchema>;

export function parseCreatePostForm(input: {
  title: string;
  content: string;
  imageUrls: string[];
}): CreatePostInput {
  return createPostSchema.parse({
    title: input.title,
    content: input.content,
    imageUrls: input.imageUrls ?? [],
  });
}

/** 삭제 폼 hidden id — UUID 가 아니면 악의적/깨진 요청으로 간주 */
export const postIdSchema = z.string().uuid();
