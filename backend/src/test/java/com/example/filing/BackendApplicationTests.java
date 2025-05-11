package com.example.filing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
		// 测试Spring上下文是否成功加载
	}

}
