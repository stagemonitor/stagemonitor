package org.stagemonitor.vertx.wrappers.rxJava;

import rx.Observable;

/**
 * Created by Glamarre on 3/23/2017.
 */
public class ObservableWrapper extends Observable {

	private String behavior;

	public ObservableWrapper(OnSubscribe f, String behavior) {
		super(f);
		this.behavior = behavior;
	}

	public String getBehavior() {
		return behavior;
	}
}
