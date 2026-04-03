/**
 * 게시판 백엔드 루트 패키지입니다.
 *
 * <p>실무에서 자주 쓰는 나눔은 다음과 같습니다.
 *
 * <ul>
 *   <li>{@code api} — HTTP 요청을 받는 컨트롤러만
 *   <li>{@code application} — 여러 엔티티를 묶는 애플리케이션 서비스(유스케이스)
 *   <li>{@code dto} — 요청·응답용 record (엔티티와 분리)
 *   <li>{@code domain} — JPA 엔티티·리포지토리(데이터 접근)
 *   <li>{@code auth} — JWT·인증 세션(쿠키) 관련 서비스
 *   <li>{@code posts} — 게시글 유스케이스({@link com.noa99kee.board.posts.PostService})
 *   <li>{@code security} — Spring Security 필터·로그인 주체(Principal)
 *   <li>{@code config} — 설정 클래스(@Configuration)와 프로퍼티 바인딩
 *   <li>{@code common} — 전역 예외 처리·공통 유틸
 * </ul>
 */
package com.noa99kee.board;
