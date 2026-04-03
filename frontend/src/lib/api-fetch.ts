import { flowLog } from '@/lib/flow-log';

function requestLabel(input: RequestInfo | URL, init?: RequestInit): string {
	let method = init?.method;
	if (!method && typeof input === 'object' && !(input instanceof URL) && 'method' in input) {
		method = input.method;
	}
	const m = (method && method.length > 0 ? method : 'GET').toUpperCase();
	const url =
		typeof input === 'string'
			? input
			: input instanceof URL
				? input.href
				: input.url;
	return `${m} ${url}`;
}

/** 모든 API 호출에 대해 개발 모드에서 요청 시작·응답 종료 로그를 남깁니다. */
export async function apiFetch(
	input: RequestInfo | URL,
	init?: RequestInit,
): Promise<Response> {
	const label = requestLabel(input, init);
	const t0 = performance.now();
	flowLog('http.request', { label });
	try {
		const res = await fetch(input, init);
		flowLog('http.response', {
			label,
			status: res.status,
			ok: res.ok,
			ms: Math.round(performance.now() - t0),
		});
		return res;
	} catch (e) {
		flowLog('http.error', {
			label,
			ms: Math.round(performance.now() - t0),
		});
		throw e;
	}
}
