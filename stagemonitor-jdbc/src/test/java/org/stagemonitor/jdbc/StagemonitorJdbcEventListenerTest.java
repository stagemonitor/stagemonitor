package org.stagemonitor.jdbc;

import org.junit.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class StagemonitorJdbcEventListenerTest {
	
	@Test
	public void getMethod() throws Exception {
		assertSoftly(softly -> {
			softly.assertThat(StagemonitorJdbcEventListener.getMethod("select * from stagemonitor")).isEqualTo("SELECT");
			softly.assertThat(StagemonitorJdbcEventListener.getMethod(" update stagemonitor set bug=false")).isEqualTo("UPDATE");
			softly.assertThat(StagemonitorJdbcEventListener.getMethod("Commit")).isEqualTo("COMMIT");
			softly.assertThat(StagemonitorJdbcEventListener.getMethod("")).isEqualTo("");
			softly.assertThat(StagemonitorJdbcEventListener.getMethod(" ")).isEqualTo("");
			softly.assertThat(StagemonitorJdbcEventListener.getMethod(null)).isNull();
		});
	}

}
