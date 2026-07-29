package com.example.demo.domain.member;

import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.response.PageRequestDto;
import com.example.demo.common.response.PageResponse;
import com.example.demo.domain.member.dto.MemberCreateRequest;
import com.example.demo.domain.member.dto.MemberResponse;
import com.example.demo.security.jwt.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 회원 API.
 *
 * <p>컨트롤러의 역할은 요청 검증·인가 확인·응답 포장까지다. 업무 로직과 트랜잭션은 서비스가 갖는다.
 */
@Tag(name = "회원", description = "회원 조회/등록")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Validated
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 목록 조회", description = "이름 부분일치 및 상태로 필터링한다. 관리자 권한 필요.")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ApiResponse<PageResponse<MemberResponse>> getMembers(
            @RequestParam(required = false) String memberName,
            @RequestParam(required = false) String status,
            @Valid @ModelAttribute PageRequestDto pageRequest) {
        return ApiResponse.success(memberService.getMembers(memberName, status, pageRequest));
    }

    @Operation(summary = "내 정보 조회", description = "토큰 주체의 회원 정보를 반환한다.")
    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyInfo(@AuthenticationPrincipal AuthenticatedMember member) {
        return ApiResponse.success(memberService.getMember(member.getMemberId()));
    }

    @Operation(summary = "회원 단건 조회", description = "관리자 권한 필요.")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/{memberId}")
    public ApiResponse<MemberResponse> getMember(@PathVariable Long memberId) {
        return ApiResponse.success(memberService.getMember(memberId));
    }

    @Operation(summary = "회원 등록", description = "관리자 권한 필요.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<MemberResponse> createMember(@Valid @RequestBody MemberCreateRequest request) {
        return ApiResponse.success(memberService.createMember(request), "회원이 등록되었습니다.");
    }
}
