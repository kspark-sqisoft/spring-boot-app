/**
 * API 오류 응답을 사람이 읽을 문자열로 바꿉니다.
 * GlobalExceptionHandler JSON, RFC7807 스타일, 프록시 HTML(502 등)을 처리합니다.
 */
export async function parseApiErrorResponse(
  res: Response,
  fallback: string,
): Promise<string> {
  const text = await res.text();
  const statusHint =
    res.statusText && res.statusText !== 'OK'
      ? `HTTP ${res.status} ${res.statusText}`
      : `HTTP ${res.status}`;

  if (!text.trim()) {
    return `${fallback} (${statusHint})`;
  }

  try {
    const body = JSON.parse(text) as Record<string, unknown>;
    const m = body.message;
    if (Array.isArray(m)) {
      const joined = m.filter((x) => typeof x === 'string').join(', ');
      if (joined) return joined;
    }
    if (typeof m === 'string' && m.trim()) return m;
    const detail = body.detail;
    if (typeof detail === 'string' && detail.trim()) return detail;
    const err = body.error;
    if (typeof err === 'string' && err.trim()) return err;
    const title = body.title;
    if (typeof title === 'string' && title.trim()) return title;
  } catch {
    /* not JSON */
  }

  const plain = text
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  if (plain.length > 0 && plain.length < 400) {
    return `${fallback} (${statusHint}: ${plain.slice(0, 240)})`;
  }

  return `${fallback} (${statusHint})`;
}
