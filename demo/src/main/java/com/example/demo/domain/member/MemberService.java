package com.example.demo.domain.member;

import com.example.demo.common.audit.Auditable;
import com.example.demo.common.error.BusinessException;
import com.example.demo.common.response.PageRequestDto;
import com.example.demo.common.response.PageResponse;
import com.example.demo.common.trace.TraceContext;
import com.example.demo.domain.member.dto.MemberCreateRequest;
import com.example.demo.domain.member.dto.MemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 회원 서비스.
 *
 * <p>트랜잭션 규약
 * <ul>
 *   <li>클래스 레벨은 {@code readOnly = true} — 조회가 기본, 쓰기 메서드에만 명시적으로 재선언한다.
 *       읽기 전용 트랜잭션은 플러시를 생략하고 커넥션을 조기에 반납한다.</li>
 *   <li>트랜잭션 경계는 <b>서비스 계층</b>에만 둔다. 컨트롤러·매퍼에는 {@code @Transactional} 을 붙이지 않는다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    /** 회원 목록 조회. */
    public PageResponse<MemberResponse> getMembers(String memberName, String status, PageRequestDto pageRequest) {
        long total = memberMapper.countList(memberName, status);
        if (total == 0) {
            return PageResponse.of(pageRequest.getPage(), pageRequest.getSize(), 0, new ArrayList<MemberResponse>());
        }
        List<Member> members = memberMapper.selectList(memberName, status, pageRequest.getSort(),
                pageRequest.getOffset(), pageRequest.getSize());

        List<MemberResponse> content = new ArrayList<MemberResponse>(members.size());
        for (Member member : members) {
            content.add(MemberResponse.from(member));
        }
        return PageResponse.of(pageRequest.getPage(), pageRequest.getSize(), total, content);
    }

    /** 회원 단건 조회. */
    public MemberResponse getMember(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND, "memberId=" + memberId);
        }
        return MemberResponse.from(member);
    }

    /**
     * 회원 등록.
     *
     * <p>중복 체크를 먼저 하지만, 동시 요청 경합(race)은 유니크 제약이 최종적으로 막는다.
     * 따라서 {@link DuplicateKeyException} 도 같은 업무 오류로 변환한다.
     */
    @Transactional
    @Auditable(eventType = "MEMBER_CREATE", targetExpression = "loginId",
            params = {"loginId", "memberName"})
    public MemberResponse createMember(MemberCreateRequest request) {
        if (memberMapper.countByLoginId(request.getLoginId()) > 0) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_LOGIN_ID, "loginId=" + request.getLoginId());
        }

        Member member = Member.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .memberName(request.getMemberName())
                .email(request.getEmail())
                .cellNo(request.getCellNo())
                .role("ROLE_USER")
                .status(Member.STATUS_ACTIVE)
                .createdBy(TraceContext.getUserId())
                .build();

        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_LOGIN_ID, "loginId=" + request.getLoginId(), e);
        }

        log.info("[MEMBER] created. memberId={} loginId={}", member.getMemberId(), member.getLoginId());
        return MemberResponse.from(member);
    }
}
