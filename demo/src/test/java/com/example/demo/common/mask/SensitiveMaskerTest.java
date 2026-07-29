package com.example.demo.common.mask;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 민감정보 마스킹 단위테스트. 컨테이너 없이 항상 수행된다.
 */
class SensitiveMaskerTest {

    @Test
    @DisplayName("주민등록번호는 뒤 7자리가 지워진다")
    void masksResidentRegistrationNumber() {
        assertThat(SensitiveMasker.maskAll("regNo is 900101-1234567 end"))
                .contains("900101-*******")
                .doesNotContain("1234567");
    }

    @Test
    @DisplayName("계좌번호는 앞자리만 남는다")
    void masksAccountNumber() {
        assertThat(SensitiveMasker.maskAll("출금계좌 110-0001-000001 입금"))
                .contains("110-****-****")
                .doesNotContain("000001");
    }

    @Test
    @DisplayName("휴대폰 번호는 가운데 자리가 지워진다")
    void masksCellPhoneNumber() {
        assertThat(SensitiveMasker.maskAll("연락처 010-1234-5678"))
                .contains("010-****-5678")
                .doesNotContain("1234-5678");
    }

    @Test
    @DisplayName("카드번호는 가운데 8자리가 지워진다")
    void masksCardNumber() {
        assertThat(SensitiveMasker.maskAll("card 1234-5678-9012-3456"))
                .contains("1234-****-****-3456");
    }

    @Test
    @DisplayName("비밀번호·토큰 값은 통째로 지워진다")
    void masksSecretFields() {
        String masked = SensitiveMasker.maskAll("{\"loginId\":\"user01\",\"password\":\"User1234!\"}");
        assertThat(masked).doesNotContain("User1234!");
        assertThat(masked).contains("user01");
    }

    @Test
    @DisplayName("날짜는 계좌번호로 오탐되지 않는다")
    void doesNotMaskDates() {
        assertThat(SensitiveMasker.maskAll("2026-07-29 10:11:12.345 처리완료"))
                .contains("2026-07-29");
    }

    @Test
    @DisplayName("필드 단위 마스킹 — 이름/이메일/계좌번호")
    void masksIndividualFields() {
        assertThat(SensitiveMasker.maskName("홍길동")).isEqualTo("홍*동");
        assertThat(SensitiveMasker.maskName("김철")).isEqualTo("김*");
        assertThat(SensitiveMasker.maskEmail("user01@example.com")).isEqualTo("us****@example.com");
        assertThat(SensitiveMasker.maskAccountNo("110-0001-000001")).isEqualTo("110*******001");
    }

    @Test
    @DisplayName("null 과 빈 문자열은 그대로 통과한다")
    void handlesNullAndEmpty() {
        assertThat(SensitiveMasker.maskAll(null)).isNull();
        assertThat(SensitiveMasker.maskAll("")).isEmpty();
        assertThat(SensitiveMasker.maskName(null)).isNull();
    }
}
