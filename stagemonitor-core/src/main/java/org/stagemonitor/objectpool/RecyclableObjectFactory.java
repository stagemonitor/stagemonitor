package org.stagemonitor.objectpool;

public interface RecyclableObjectFactory<T extends Recyclable> {

	T createInstance();
}
