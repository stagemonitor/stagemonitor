package org.stagemonitor.core.util;

import net.bytebuddy.ByteBuddy;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionUtilsTest {

	private static final String BYTEBUDDY_GROUP = "net.bytebuddy";
	private static final String VERSION_PATTERN = "\\d\\.\\d+\\.\\d+";

	@Test
	public void getVersionFromPomProperties() throws Exception {
		assertThat(VersionUtils.getVersionFromPomProperties(ByteBuddy.class, BYTEBUDDY_GROUP, "byte-buddy")).matches(VERSION_PATTERN);
	}

	@Test
	public void testGetMavenCentralDownloadLink() throws Exception {
		assertThat(VersionUtils.getMavenCentralDownloadLink(BYTEBUDDY_GROUP, "byte-buddy-agent", "1.7.5"))
				.isEqualTo("http://central.maven.org/maven2/net/bytebuddy/byte-buddy-agent/1.7.5/byte-buddy-agent-1.7.5.jar");
	}

	@Test
	public void testByteBuddyDownloadUrl() throws Exception {
		final String byteBuddyVersion = VersionUtils.getVersionFromPomProperties(ByteBuddy.class, BYTEBUDDY_GROUP, "byte-buddy");
		final String mavenCentralDownloadLink = VersionUtils.getMavenCentralDownloadLink(BYTEBUDDY_GROUP, "byte-buddy-agent", byteBuddyVersion);
		assertThat(mavenCentralDownloadLink)
				.isEqualTo("http://central.maven.org/maven2/net/bytebuddy/byte-buddy-agent/" + byteBuddyVersion + "/byte-buddy-agent-" + byteBuddyVersion + ".jar");
	}
}
