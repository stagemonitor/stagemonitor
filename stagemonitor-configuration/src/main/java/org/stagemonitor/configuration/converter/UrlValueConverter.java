package org.stagemonitor.configuration.converter;

import org.stagemonitor.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlValueConverter extends AbstractValueConverter<URL> {

	public static final ValueConverter<URL> INSTANCE = new UrlValueConverter();

	private UrlValueConverter() {
	}

	@Override
	public URL convert(String s) throws IllegalArgumentException {
		try {
			return new URL(StringUtils.removeTrailingSlash(s));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	@Override
	public String toString(URL value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	@Override
	public String toSafeString(URL value) {
		if (value == null) {
			return null;
		}
		final String userInfo = value.getUserInfo();
		final String urlAsString = value.toString();
		if (userInfo != null) {
			return urlAsString.replace(userInfo, getSafeUserInfo(userInfo));
		} else {
			return urlAsString;
		}
	}

	private String getSafeUserInfo(String userInfo) {
		return userInfo.split(":", 2)[0] + ":XXX";
	}

}
