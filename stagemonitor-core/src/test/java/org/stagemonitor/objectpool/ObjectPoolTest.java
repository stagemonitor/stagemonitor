package org.stagemonitor.objectpool;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectPoolTest {

	private ObjectPool objectPool;

	@Before
	public void setUp() throws Exception {
		objectPool = new ObjectPool();
		objectPool.registerRecyclableObjectFactory(TestRecyclable::new, TestRecyclable.class, 10);
	}

	@Test
	public void testMaxElements() throws Exception {
		for (int i = 0; i < 20; i++) {
			objectPool.recycle(new TestRecyclable(i), TestRecyclable.class);
		}
		assertThat(objectPool.getCurrentThreadsQueueSize(TestRecyclable.class)).isEqualTo(10);
	}

	@Test
	public void testDifferentThreads_DifferentQueues() throws Exception {
		objectPool.recycle(new TestRecyclable(), TestRecyclable.class);
		assertThat(objectPool.getCurrentThreadsQueueSize(TestRecyclable.class)).isEqualTo(1);

		final Thread t1 = new Thread(() -> {
			objectPool.recycle(new TestRecyclable(), TestRecyclable.class);
			objectPool.recycle(new TestRecyclable(), TestRecyclable.class);
			assertThat(objectPool.getCurrentThreadsQueueSize(TestRecyclable.class)).isEqualTo(2);
		});
		t1.start();
		t1.join();

		final Thread t2 = new Thread(() -> {
			objectPool.recycle(new TestRecyclable(), TestRecyclable.class);
			objectPool.recycle(new TestRecyclable(), TestRecyclable.class);
			objectPool.recycle(new TestRecyclable(), TestRecyclable.class);
			assertThat(objectPool.getCurrentThreadsQueueSize(TestRecyclable.class)).isEqualTo(3);
		});
		t2.start();
		t2.join();
	}

	@Test
	public void testRecycle() throws Exception {
		final TestRecyclable instance = objectPool.getInstance(TestRecyclable.class);
		instance.state = 1;
		objectPool.recycle(instance, TestRecyclable.class);
		assertThat(instance.state).isEqualTo(0);
		assertThat(instance).isSameAs(objectPool.getInstance(TestRecyclable.class));
	}

	private static class TestRecyclable implements Recyclable {

		private int state;

		TestRecyclable() {
		}

		TestRecyclable(int state) {
			this.state = state;
		}

		@Override
		public void resetState() {
			state = 0;
		}
	}
}
