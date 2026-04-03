/**
 * HTTP 요청·응답에 쓰는 데이터 전송 객체(record)만 둡니다.
 *
 * <p>엔티티({@code domain})와 분리하면 JSON 필드를 명확히 통제하고, 검증 어노테이션을 DTO에만 두기 쉽습니다.
 *
 * <ul>
 *   <li>{@link com.noa99kee.board.dto.auth} — 로그인·회원가입·프로필 수정·토큰 응답
 *   <li>{@link com.noa99kee.board.dto.user} — 사용자 공개 프로필 형태
 *   <li>{@link com.noa99kee.board.dto.posts} — 게시글 작성·수정·목록·상세
 * </ul>
 */
package com.noa99kee.board.dto;
