package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    /**
     * 사용자 고유 ID
     */
    private Long id;
    /**
     * 사용자 이름
     */
    private String name;
    /**
     * 사용자 이메일 주소
     */
    private String email;
    /**
     * 유선 전화번호 (전북 지역)
     */
    private String tel;
    /**
     * 휴대폰 번호 (010-XXXX-XXXX 형식)
     */
    private String cellno;
    /**
     * 부서 정보
     */
    private String department;
    /**
     * 팀 정보
     */
    private String team;
}

