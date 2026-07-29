package com.example.demo.domain.member.dto;

import com.example.demo.common.mask.SensitiveMasker;
import com.example.demo.domain.member.Member;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 회원 조회 응답.
 *
 * <p>비밀번호·주민번호는 아예 담지 않고, 이름·연락처·이메일은 마스킹해서 내보낸다.
 * 마스킹 해제가 필요한 화면은 별도 권한 검증을 거친 전용 API 를 만든다.
 */
@Getter
@Builder
@Schema(description = "회원 정보")
public class MemberResponse {

    @Schema(description = "회원 번호", example = "1")
    private final Long memberId;

    @Schema(description = "로그인 ID", example = "user01")
    private final String loginId;

    @Schema(description = "회원명 (마스킹)", example = "홍*동")
    private final String memberName;

    @Schema(description = "이메일 (마스킹)", example = "us****@example.com")
    private final String email;

    @Schema(description = "휴대폰 (마스킹)", example = "010-****-5678")
    private final String cellNo;

    @Schema(description = "권한", example = "ROLE_USER")
    private final String role;

    @Schema(description = "상태", example = "ACTIVE")
    private final String status;

    @Schema(description = "최종 로그인 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime lastLoginAt;

    @Schema(description = "가입 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createdAt;

    /** 엔티티를 마스킹 적용 응답으로 변환한다. */
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .memberId(member.getMemberId())
                .loginId(member.getLoginId())
                .memberName(SensitiveMasker.maskName(member.getMemberName()))
                .email(SensitiveMasker.maskEmail(member.getEmail()))
                .cellNo(SensitiveMasker.maskCellNo(member.getCellNo()))
                .role(member.getRole())
                .status(member.getStatus())
                .lastLoginAt(member.getLastLoginAt())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
