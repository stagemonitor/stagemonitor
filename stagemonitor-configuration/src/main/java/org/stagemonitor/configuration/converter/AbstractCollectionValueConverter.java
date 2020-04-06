package org.stagemonitor.configuration.converter;

import java.util.Collection;
import java.util.Iterator;

public abstract class AbstractCollectionValueConverter<C extends Collection<V>, V> extends AbstractValueConverter<C> {

	protected final ValueConverter<V> valueConverter;

	protected final String delimiter;

	public AbstractCollectionValueConverter(ValueConverter<V> valueConverter) {
		this.valueConverter = valueConverter;
		this.delimiter = ",";
	}

	public AbstractCollectionValueConverter(ValueConverter<V> valueConverter, String delimiter) {
		this.valueConverter = valueConverter;
		this.delimiter = delimiter;
	}

	@Override
	public String toString(C value) {
		return getString(value, false);
	}

	@Override
	public String toSafeString(C value) {
		return getString(value, true);
	}

	private String getString(C value, boolean safeString) {
		Iterator<V> it = value.iterator();
		if (!it.hasNext()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (; ; ) {
			V e = it.next();
			if (safeString) {
				sb.append(valueConverter.toSafeString(e));
			} else {
				sb.append(valueConverter.toString(e));
			}
			if (!it.hasNext()) {
				return sb.toString();
			}
			sb.append(',');
		}
	}
}
