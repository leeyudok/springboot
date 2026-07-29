package com.example.demo.common.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis 설정.
 *
 * <p>매퍼 인터페이스는 도메인 패키지 안({@code domain/<도메인>/*Mapper.java})에 두고,
 * SQL 은 {@code resources/mapper/**.xml} 에 같은 이름으로 둔다.
 * SQL 을 XML 로 분리하는 이유는 실행계획 리뷰·튜닝·감사 대응 시 SQL 만 따로 열람하기 위해서다.
 *
 * <p>{@code mapper-locations} / {@code map-underscore-to-camel-case} 등 나머지 설정은
 * {@code application.yml} 의 {@code mybatis.*} 에 있다.
 */
@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "com.example.demo", annotationClass = Mapper.class)
public class MyBatisConfig {
}
