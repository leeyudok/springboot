package com.example.demo.service;

import com.example.demo.dto.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 샘플 사용자 데이터 생성 서비스.
 * 한국 가수 이름 목록을 기반으로 매 호출마다 랜덤 사용자 목록을 만든다.
 */
@Service
public class UserService {

    /** 샘플 이름 풀 — 인원수는 이 목록 크기를 따라간다 */
    private static final List<String> KOREAN_SINGER_NAMES = Arrays.asList(
            "K.Will", "카이 (Kai)", "강다니엘 (Kang Daniel)", "강승윤 (Kang Seung-yoon)", "강타 (Kangta)",
            "키 (Key)", "기현 (Kihyun)", "김동한 (Kim Dong-han)", "김동준 (Kim Dong-jun)", "에디킴 (Eddy Kim)",
            "지드래곤 (G-Dragon)", "지오 (G.O)", "지소울 (G.Soul)", "개코 (Gaeko)", "가호 (Gaho)",
            "개리 (Gary)", "길 (Gill)", "공명 (Gong Myung)", "공찬 (Gongchan)", "유토 아다치 (Yuto Adachi)",
            "데니 안 (Danny Ahn)", "토니 안 (Tony An)", "아주 (Ajoo)", "아우라 (Aoora)", "아론 (Aron)",
            "비범 (B-Bomb)", "비아이 (B.I)", "바빌론 (Babylon)", "배진영 (Bae Jin-young)", "배기성 (Bae Ki-sung)",
            "백호 (Baekho)", "백현 (Baekhyun)", "뱀뱀 (BamBam)", "방찬 (Bang Chan)", "방예담 (Bang Ye-dam)",
            "방용국 (Bang Yong-guk)", "바로 (Baro)", "범규 (Beomgyu)", "비엠 (BM)", "바비 (Bobby)",
            "보이비 (Boi B)", "봉재현 (Bong Jae-hyun)", "범키 (Bumkey)", "범주 (Bumzu)", "병헌 (Byung Hun)",
            "마커스 카바이스 (Marcus Cabais)", "차은우 (Cha Eun-woo)", "차훈 (Cha Hun)", "차인하 (Cha In-ha)", "채보훈 (Chae Bo-hun)"
    );

    private static final List<String> DEPARTMENTS = Arrays.asList("개발부", "기획부", "디자인부", "마케팅부", "인사부");
    private static final List<String> TEAMS = Arrays.asList("A팀", "B팀", "C팀", "D팀", "E팀");

    /** 전체 사용자 목록을 생성해 반환한다 (연락처·부서·팀은 호출마다 랜덤). */
    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < KOREAN_SINGER_NAMES.size(); i++) {
            long id = i + 1;
            String name = KOREAN_SINGER_NAMES.get(i);
            String email = name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase() + "@example.com";
            String tel = randomPhone(random, "063");     // 전북 지역번호
            String cellno = randomPhone(random, "010");
            String department = DEPARTMENTS.get(random.nextInt(DEPARTMENTS.size()));
            String team = TEAMS.get(random.nextInt(TEAMS.size()));

            users.add(new User(id, name, email, tel, cellno, department, team));
        }
        return users;
    }

    /** {@code prefix-XXXX-XXXX} 형식의 랜덤 전화번호를 생성한다. */
    private String randomPhone(Random random, String prefix) {
        return String.format("%s-%04d-%04d", prefix, 1000 + random.nextInt(9000), random.nextInt(10000));
    }
}
