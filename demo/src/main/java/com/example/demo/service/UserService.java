package com.example.demo.service;

import com.example.demo.dto.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class UserService {

    private static final List<String> KOREAN_SINGER_NAMES = List.of(
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

    private static final List<String> DEPARTMENTS = List.of("개발부", "기획부", "디자인부", "마케팅부", "인사부");
    private static final List<String> TEAMS = List.of("A팀", "B팀", "C팀", "D팀", "E팀");

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 50; i++) {
            long id = i + 1;
            String name = KOREAN_SINGER_NAMES.get(i);
            String email = name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase() + "@example.com";
            
            // Generate a random phone number with Jeonbuk area code (063)
            int telMiddle = 1000 + random.nextInt(9000);
            int telLast = random.nextInt(10000);
            String tel = String.format("063-%04d-%04d", telMiddle, telLast);

            // Generate a random cell phone number in the format 010-XXXX-XXXX
            int cellMiddle = 1000 + random.nextInt(9000);
            int cellLast = random.nextInt(10000);
            String cellno = String.format("010-%04d-%04d", cellMiddle, cellLast);

            String department = DEPARTMENTS.get(random.nextInt(DEPARTMENTS.size()));
            String team = TEAMS.get(random.nextInt(TEAMS.size()));
            
            users.add(new User(id, name, email, tel, cellno, department, team));
        }
        return users;
    }
}
