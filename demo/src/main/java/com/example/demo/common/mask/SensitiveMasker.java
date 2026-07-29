package com.example.demo.common.mask;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 민감정보 마스킹 유틸.
 *
 * <p>두 가지 용도로 쓴다.
 * <ul>
 *   <li>{@link #maskAll(String)} — 로그/감사로그로 나가는 <b>임의 문자열</b>을 패턴 기반으로 일괄 치환.
 *       {@link MaskingPatternLayout} 과 감사 AOP 가 사용한다.</li>
 *   <li>{@code maskXxx} — DTO 응답 조립 시 <b>필드 단위</b> 치환.</li>
 * </ul>
 *
 * <p>패턴 적용 순서가 중요하다. 주민등록번호를 먼저 처리하지 않으면 계좌번호 패턴에 먼저 잡혀
 * 성별자리가 노출될 수 있다.
 */
public final class SensitiveMasker {

    private static final String EMPTY = "";

    /** 주민등록번호 / 외국인등록번호 — 990101-1234567 */
    private static final Pattern REG_NO = Pattern.compile("(\\d{6})[-\\s]?([1-8])\\d{6}");

    /** 카드번호 — 1234-5678-9012-3456 또는 16자리 연속 */
    private static final Pattern CARD_NO = Pattern.compile("\\b(\\d{4})[-\\s]?(\\d{4})[-\\s]?(\\d{4})[-\\s]?(\\d{4})\\b");

    /** 휴대폰 번호 — 010-1234-5678 */
    private static final Pattern CELL_NO = Pattern.compile("\\b(01[016789])[-\\s]?(\\d{3,4})[-\\s]?(\\d{4})\\b");

    /**
     * 계좌번호 — 110-0001-000001 형태.
     * 마지막 그룹을 4자리 이상으로 강제해 날짜(2026-07-29)가 오탐되지 않게 한다.
     */
    private static final Pattern ACCOUNT_NO = Pattern.compile("\\b(\\d{3,6})-(\\d{2,6})-(\\d{4,8})\\b");

    /** 이메일 — 로컬파트 앞 2자만 남긴다 */
    private static final Pattern EMAIL = Pattern.compile("\\b([\\w.+-]{1,2})([\\w.+-]*)@([\\w.-]+\\.[A-Za-z]{2,})\\b");

    /** JSON/폼 형태의 비밀번호·토큰 키 — 값 전체를 지운다 */
    private static final Pattern SECRET_FIELD = Pattern.compile(
            "(?i)(\"?(?:password|passwd|pwd|secret|token|accessToken|refreshToken|authorization|regNo)\"?\\s*[:=]\\s*)(\"[^\"]*\"|[^,;&}\\s]+)");

    private SensitiveMasker() {
    }

    /**
     * 문자열 전체에 모든 마스킹 패턴을 적용한다.
     *
     * @param text 원본 (null 허용)
     * @return 마스킹된 문자열, 입력이 null 이면 null
     */
    public static String maskAll(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String masked = SECRET_FIELD.matcher(text).replaceAll("$1\"****\"");
        masked = REG_NO.matcher(masked).replaceAll("$1-*******");
        masked = CARD_NO.matcher(masked).replaceAll("$1-****-****-$4");
        masked = CELL_NO.matcher(masked).replaceAll("$1-****-$3");
        masked = ACCOUNT_NO.matcher(masked).replaceAll("$1-****-****");
        masked = maskEmails(masked);
        return masked;
    }

    /** 주민등록번호 — 앞 6자리만 노출. */
    public static String maskRegNo(String regNo) {
        if (isBlank(regNo)) {
            return regNo;
        }
        String digits = regNo.replaceAll("[^0-9]", EMPTY);
        if (digits.length() < 7) {
            return "*******";
        }
        return digits.substring(0, 6) + "-*******";
    }

    /** 계좌번호 — 앞 3자리와 뒤 3자리만 노출. */
    public static String maskAccountNo(String accountNo) {
        if (isBlank(accountNo)) {
            return accountNo;
        }
        String digits = accountNo.replaceAll("[^0-9]", EMPTY);
        if (digits.length() <= 6) {
            return repeat('*', digits.length());
        }
        return digits.substring(0, 3) + repeat('*', digits.length() - 6) + digits.substring(digits.length() - 3);
    }

    /** 휴대폰 번호 — 가운데 자리 마스킹. */
    public static String maskCellNo(String cellNo) {
        if (isBlank(cellNo)) {
            return cellNo;
        }
        String digits = cellNo.replaceAll("[^0-9]", EMPTY);
        if (digits.length() < 9) {
            return repeat('*', digits.length());
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }

    /** 이름 — 가운데 글자 마스킹 (2글자면 뒷글자). */
    public static String maskName(String name) {
        if (isBlank(name)) {
            return name;
        }
        int length = name.length();
        if (length == 1) {
            return name;
        }
        if (length == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + repeat('*', length - 2) + name.charAt(length - 1);
    }

    /** 이메일 — 로컬파트 앞 2자만 노출. */
    public static String maskEmail(String email) {
        if (isBlank(email)) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return repeat('*', email.length());
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return repeat('*', local.length()) + domain;
        }
        return local.substring(0, 2) + repeat('*', local.length() - 2) + domain;
    }

    private static String maskEmails(String text) {
        Matcher matcher = EMAIL.matcher(text);
        StringBuffer sb = new StringBuffer(text.length());
        while (matcher.find()) {
            String head = matcher.group(1);
            String rest = matcher.group(2);
            String domain = matcher.group(3);
            String replacement = head + repeat('*', Math.max(rest.length(), 1)) + "@" + domain;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String repeat(char c, int count) {
        if (count <= 0) {
            return EMPTY;
        }
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, c);
        return new String(chars);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
