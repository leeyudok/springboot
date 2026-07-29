package com.example.demo.sample.ledger;

import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.response.PageRequestDto;
import com.example.demo.common.response.PageResponse;
import com.example.demo.sample.ledger.dto.AccountResponse;
import com.example.demo.sample.ledger.dto.TransferRequest;
import com.example.demo.sample.ledger.dto.TransferResponse;
import com.example.demo.security.jwt.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 계좌 API.
 *
 * <p>모든 엔드포인트가 토큰 주체({@code @AuthenticationPrincipal})를 기준으로 소유권을 검증한다.
 * 요청 본문의 회원번호를 신뢰하면 다른 사람 계좌를 조회·이체할 수 있게 된다.
 */
@Tag(name = "계좌", description = "계좌 조회 / 이체")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "내 계좌 목록 조회")
    @GetMapping
    public ApiResponse<List<AccountResponse>> getMyAccounts(@AuthenticationPrincipal AuthenticatedMember member) {
        return ApiResponse.success(accountService.getMyAccounts(member.getMemberId()));
    }

    @Operation(summary = "계좌 단건 조회", description = "본인 명의 계좌만 조회할 수 있다.")
    @GetMapping("/{accountNo}")
    public ApiResponse<AccountResponse> getAccount(@AuthenticationPrincipal AuthenticatedMember member,
                                                   @PathVariable String accountNo) {
        return ApiResponse.success(accountService.getAccount(member.getMemberId(), accountNo));
    }

    @Operation(summary = "계좌 거래내역 조회", description = "출금·입금 양방향 내역을 최신순으로 반환한다.")
    @GetMapping("/{accountNo}/transfers")
    public ApiResponse<PageResponse<TransferHistory>> getTransferHistory(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String accountNo,
            @Valid @ModelAttribute PageRequestDto pageRequest) {
        return ApiResponse.success(accountService.getTransferHistory(member.getMemberId(), accountNo, pageRequest));
    }

    @Operation(summary = "계좌 이체", description = "출금계좌는 본인 명의여야 하며 1회 한도는 5,000,000원이다.")
    @PostMapping("/transfers")
    public ApiResponse<TransferResponse> transfer(@AuthenticationPrincipal AuthenticatedMember member,
                                                  @Valid @RequestBody TransferRequest request) {
        return ApiResponse.success(accountService.transfer(member.getMemberId(), request), "이체가 완료되었습니다.");
    }
}
