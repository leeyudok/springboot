# GEMINI 기술 스택 분석

이 문서는 현재 프로젝트의 기술 스택을 설명합니다.

## 백엔드

- **언어:** Java 8
- **프레임워크:** Spring Boot 2.7.18
- **웹 서버:** 임베디드 Tomcat (`spring-boot-starter-web`에 포함)
- **검색엔진:** OpenSearch

## 프론트엔드

- **마크업:** HTML

## 빌드 도구

- **도구:** Gradle

## 주요 의존성

- `spring-boot-starter-web`: Spring Boot로 웹 애플리케이션을 구축하기 위한 핵심 의존성.
- `logback-classic`: Java용 강력한 로깅 프레임워크.
- `lombok`: 모델/데이터 객체의 상용구 코드를 줄여주는 라이브러리.
- `spring-boot-devtools`: 자동 재시작 및 라이브 리로드와 같은 개발 시간 기능을 제공합니다.


## 기타

http://localhost:8000/swagger-ui/index.html
