package org.stagemonitor;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.stagemonitor.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectionUtilsTest {

	@Test
	public void getIndexOf() throws Exception {
		final List<Object> list = Arrays.asList("", 1, 1.1);
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(CollectionUtils.getIndexOf(list, String.class)).isEqualTo(0);
		softly.assertThat(CollectionUtils.getIndexOf(list, Number.class)).isEqualTo(1);
		softly.assertThat(CollectionUtils.getIndexOf(list, Integer.class)).isEqualTo(1);
		softly.assertThat(CollectionUtils.getIndexOf(list, Double.class)).isEqualTo(2);
		softly.assertThat(CollectionUtils.getIndexOf(list, Serializable.class)).isEqualTo(0);
		softly.assertThat(CollectionUtils.getIndexOf(list, BigDecimal.class)).isEqualTo(-1);
		softly.assertAll();
	}

	@Test
	public void testInsertAfter() {
		final List<Object> list = new ArrayList<>(Arrays.asList("", 1, 1.1));
		CollectionUtils.addAfter(list, String.class, "inserted");
		assertThat(list).isEqualTo(Arrays.asList("", "inserted", 1, 1.1));
	}

	@Test
	public void testInsertAfter_NotInList() {
		final List<Object> list = new ArrayList<>(Arrays.asList("", 1, 1.1));
		CollectionUtils.addAfter(list, BigDecimal.class, "inserted");
		assertThat(list).isEqualTo(Arrays.asList("inserted", "", 1, 1.1));
	}

}
