package com.example.demo.sample.ledger;

import com.example.demo.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이체 통합테스트.
 *
 * <p>잔액이 실제로 증감하는지, 업무 규칙 위반이 정확한 에러코드로 거절되는지 확인한다.
 * 시드 계좌: user01 = 110-0001-000001(100만), 110-0001-000002(50만) / user02 = 110-0002-000001(200만)
 */
class TransferIntegrationTest extends IntegrationTestSupport {

    private static final String USER01_MAIN = "110-0001-000001";
    private static final String USER01_SUB = "110-0001-000002";
    private static final String USER02_MAIN = "110-0002-000001";

    @Test
    @DisplayName("본인 계좌 간 이체 시 출금·입금 잔액이 모두 반영된다")
    void transferBetweenOwnAccounts() throws Exception {
        String token = loginAndGetAccessToken("user01", "User1234!");
        BigDecimal fromBefore = balanceOf(token, USER01_MAIN);
        BigDecimal toBefore = balanceOf(token, USER01_SUB);
        BigDecimal amount = new BigDecimal("10000");

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(USER01_MAIN, USER01_SUB, "10000", "테스트 이체")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                // 상대 계좌번호는 마스킹되어 내려간다
                .andExpect(jsonPath("$.data.toAccountNo").value("110*******002"));

        assertThat(balanceOf(token, USER01_MAIN)).isEqualByComparingTo(fromBefore.subtract(amount));
        assertThat(balanceOf(token, USER01_SUB)).isEqualByComparingTo(toBefore.add(amount));
    }

    @Test
    @DisplayName("잔액을 초과하는 이체는 4002 로 거절되고 잔액은 변하지 않는다")
    void transferWithInsufficientBalance() throws Exception {
        String token = loginAndGetAccessToken("user01", "User1234!");
        BigDecimal before = balanceOf(token, USER01_MAIN);

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        // 1회 한도(5,000,000) 이내지만 잔액(100만)은 초과하는 금액이어야
                        // 한도 초과(4004)가 아니라 잔액 부족(4002)이 검증된다.
                        .content(transferBody(USER01_MAIN, USER02_MAIN, "4999999", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("4002"));

        assertThat(balanceOf(token, USER01_MAIN)).isEqualByComparingTo(before);
    }

    @Test
    @DisplayName("타인 명의 계좌에서는 출금할 수 없다 (4006)")
    void transferFromOthersAccount() throws Exception {
        String token = loginAndGetAccessToken("user01", "User1234!");

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(USER02_MAIN, USER01_MAIN, "1000", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("4006"));
    }

    @Test
    @DisplayName("동일 계좌 간 이체는 4003 으로 거절된다")
    void transferToSameAccount() throws Exception {
        String token = loginAndGetAccessToken("user01", "User1234!");

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(USER01_MAIN, USER01_MAIN, "1000", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("4003"));
    }

    @Test
    @DisplayName("1회 한도(5,000,000)를 넘는 이체는 4004 로 거절된다")
    void transferOverLimit() throws Exception {
        String token = loginAndGetAccessToken("user02", "User1234!");

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(USER02_MAIN, USER01_MAIN, "5000001", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("4004"));
    }

    @Test
    @DisplayName("0 이하 금액은 검증 단계에서 1000 으로 거절된다")
    void transferWithInvalidAmount() throws Exception {
        String token = loginAndGetAccessToken("user01", "User1234!");

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(USER01_MAIN, USER01_SUB, "0", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1000"));
    }

    @Test
    @DisplayName("이체 후 거래내역이 조회된다")
    void transferHistoryIsRecorded() throws Exception {
        String token = loginAndGetAccessToken("user01", "User1234!");

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(USER01_MAIN, USER01_SUB, "5000", "내역확인")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts/" + USER01_MAIN + "/transfers")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].fromAccountNo").value(USER01_MAIN))
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    private String transferBody(String from, String to, String amount, String memo) {
        StringBuilder body = new StringBuilder()
                .append("{\"fromAccountNo\":\"").append(from)
                .append("\",\"toAccountNo\":\"").append(to)
                .append("\",\"amount\":").append(amount);
        if (memo != null) {
            body.append(",\"memo\":\"").append(memo).append("\"");
        }
        return body.append("}").toString();
    }

    private BigDecimal balanceOf(String token, String accountNo) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/accounts/" + accountNo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return new BigDecimal(data.get("balance").asText());
    }
}
