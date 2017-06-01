package org.stagemonitor.util;

import java.util.List;

public final class CollectionUtils {

	public static <T> int getIndexOf(List<T> configurationSources, Class<? extends T> clazz) {
		int i = 0;
		for (; i < configurationSources.size(); i++) {
			if (clazz.isInstance(configurationSources.get(i))) {
				return i;
			}
		}
		return -1;
	}

	public static <T> void addAfter(List<T> list, Class<? extends T> clazz, T toInsert) {
		list.add(getIndexOf(list, clazz) + 1, toInsert);
	}
}
