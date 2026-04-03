/**
 * 게시판 백엔드 루트 패키지입니다.
 *
 * <p>기능(feature)마다 {@code controller}, {@code service}, {@code repository}, {@code entity}, {@code dto} 하위 패키지를 두는
 * 구조입니다. JPA 엔티티는 해당 기능의 {@code entity}·{@code repository}에 모읍니다.
 *
 * <ul>
 *   <li>{@code auth} — 로그인·JWT·리프레시 쿠키·Security 필터({@code filter}, {@code principal})
 *   <li>{@code user} — 회원 프로필 API·서비스·엔티티
 *   <li>{@code post} — 게시글 API·서비스·엔티티
 *   <li>{@code health} — 헬스체크
 *   <li>{@code config} — Security, CORS, 업로드 등
 *   <li>{@code common} — 전역 예외·공통 유틸
 * </ul>
 */
package com.noa99kee.board;
