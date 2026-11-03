package com.Nbbang.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"BOOK_REPORT_COLLECTION_ENABLED=true",
		"BOOK_REPORT_COLLECTION_CRON=0 30 5 * * *",
		"BOOK_REPORT_COLLECTION_ZONE=Asia/Seoul",
		"BOOK_REPORT_ANALYSIS_PROVIDER=baseline",
		"BOOK_REPORT_ANALYSIS_MIN_CONFIDENCE=0.80",
		"BOOK_REPORT_RECENT_DAYS=14"
})
@ActiveProfiles("test")
class BackendApplicationTests {
	@Autowired
	private Environment environment;

	@Test
	void contextLoads() {
	}

	@Test
	void mapsBookReportEnvironmentVariables() {
		assertThat(environment.getProperty("book-report.collection.enabled")).isEqualTo("true");
		assertThat(environment.getProperty("book-report.collection.cron")).isEqualTo("0 30 5 * * *");
		assertThat(environment.getProperty("book-report.collection.zone")).isEqualTo("Asia/Seoul");
		assertThat(environment.getProperty("book-report.analysis.provider")).isEqualTo("baseline");
		assertThat(environment.getProperty("book-report.analysis.min-confidence")).isEqualTo("0.80");
		assertThat(environment.getProperty("book-report.recent-days")).isEqualTo("14");
	}

}
