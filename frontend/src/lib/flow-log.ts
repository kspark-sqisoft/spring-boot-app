/** 개발 모드에서만 주요 사용자·API 흐름을 콘솔에 남깁니다. */

const PREFIX = '[board:flow]';

export function flowLog(message: string, detail?: Record<string, unknown>): void {
  if (!import.meta.env.DEV) return;
  if (detail !== undefined) {
    console.info(PREFIX, message, detail);
  } else {
    console.info(PREFIX, message);
  }
}
