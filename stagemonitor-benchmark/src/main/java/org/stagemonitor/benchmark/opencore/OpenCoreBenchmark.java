package org.stagemonitor.benchmark.opencore;

import org.stagemonitor.collector.profiler.CallStackElement;
import org.stagemonitor.collector.profiler.Profiler;

import static java.lang.System.nanoTime;

public class OpenCoreBenchmark implements Runnable {
	public static final int THREAD_COUNT = 50;
	public static final int TEST_COUNT = 10;
	public static final int RUN_COUNT = 1000;
	public static final int SLEEP = 5;

	public static void main(String[] args) throws Exception {
		for (int i = TEST_COUNT; i > 0; i--) {
			test();
		}
	}

	private static void test() throws InterruptedException {
		final Thread[] threads = new Thread[THREAD_COUNT];
		for (int i = 0; i < THREAD_COUNT; i++)
			threads[i] = new Thread(new OpenCoreBenchmark());

		long start = nanoTime();

		for (int i = 0; i < THREAD_COUNT; i++)
			threads[i].start();

		for (int i = 0; i < THREAD_COUNT; i++)
			threads[i].join();

		long end = nanoTime();
		System.out.println("clocktime=" + (end - start));
	}

	public void run() {
		final CallStackElement root = Profiler.activateProfiling();
		for (int i = 0; i < RUN_COUNT; i++)
			iterate();
		Profiler.stop("root");
		System.out.println(root.toString());
	}

	private void iterate() {
		$0();
	}

	private void l() {
		nanoTime();
	}

	private void r() {
		nanoTime();
	}

	private void $0() {
		l();
		$1();
		r();
	}

	private void $1() {
		l();
		$2();
		r();
	}

	private void $2() {
		l();
		$3();
		r();
	}

	private void $3() {
		l();
		$4();
		r();
	}

	private void $4() {
		l();
		$5();
		r();
	}

	private void $5() {
		l();
		$6();
		r();
	}

	private void $6() {
		l();
		$7();
		r();
	}

	private void $7() {
		l();
		$8();
		r();
	}

	private void $8() {
		l();
		$9();
		r();
	}

	private void $9() {
		l();
		$10();
		r();
	}

	private void $10() {
		l();
		$11();
		r();
	}

	private void $11() {
		l();
		$12();
		r();
	}

	private void $12() {
		l();
		$13();
		r();
	}

	private void $13() {
		l();
		$14();
		r();
	}

	private void $14() {
		l();
		$15();
		r();
	}

	private void $15() {
		l();
		$16();
		r();
	}

	private void $16() {
		l();
		$17();
		r();
	}

	private void $17() {
		l();
		$18();
		r();
	}

	private void $18() {
		l();
		$19();
		r();
	}

	private void $19() {
		l();
		$20();
		r();
	}

	private void $20() {
		l();
		$21();
		r();
	}

	private void $21() {
		l();
		$22();
		r();
	}

	private void $22() {
		l();
		$23();
		r();
	}

	private void $23() {
		l();
		$24();
		r();
	}

	private void $24() {
		l();
		$25();
		r();
	}

	private void $25() {
		l();
		$26();
		r();
	}

	private void $26() {
		l();
		$27();
		r();
	}

	private void $27() {
		l();
		$28();
		r();
	}

	private void $28() {
		l();
		$29();
		r();
	}

	private void $29() {
		l();
		$30();
		r();
	}

	private void $30() {
		l();
		$31();
		r();
	}

	private void $31() {
		l();
		$32();
		r();
	}

	private void $32() {
		l();
		$33();
		r();
	}

	private void $33() {
		l();
		$34();
		r();
	}

	private void $34() {
		l();
		$35();
		r();
	}

	private void $35() {
		l();
		$36();
		r();
	}

	private void $36() {
		l();
		$37();
		r();
	}

	private void $37() {
		l();
		$38();
		r();
	}

	private void $38() {
		l();
		$39();
		r();
	}

	private void $39() {
		l();
		$40();
		r();
	}

	private void $40() {
		l();
		$41();
		r();
	}

	private void $41() {
		l();
		$42();
		r();
	}

	private void $42() {
		l();
		$43();
		r();
	}

	private void $43() {
		l();
		$44();
		r();
	}

	private void $44() {
		l();
		$45();
		r();
	}

	private void $45() {
		l();
		$46();
		r();
	}

	private void $46() {
		l();
		$47();
		r();
	}

	private void $47() {
		l();
		$48();
		r();
	}

	private void $48() {
		l();
		$49();
		r();
	}

	private void $49() {
		l();
		$50();
		r();
	}

	private void $50() {
		l();
		$51();
		r();
	}

	private void $51() {
		l();
		$52();
		r();
	}

	private void $52() {
		l();
		$53();
		r();
	}

	private void $53() {
		l();
		$54();
		r();
	}

	private void $54() {
		l();
		$55();
		r();
	}

	private void $55() {
		l();
		$56();
		r();
	}

	private void $56() {
		l();
		$57();
		r();
	}

	private void $57() {
		l();
		$58();
		r();
	}

	private void $58() {
		l();
		$59();
		r();
	}

	private void $59() {
		l();
		$60();
		r();
	}

	private void $60() {
		l();
		$61();
		r();
	}

	private void $61() {
		l();
		$62();
		r();
	}

	private void $62() {
		l();
		$63();
		r();
	}

	private void $63() {
		l();
		$64();
		r();
	}

	private void $64() {
		l();
		$65();
		r();
	}

	private void $65() {
		l();
		$66();
		r();
	}

	private void $66() {
		l();
		$67();
		r();
	}

	private void $67() {
		l();
		$68();
		r();
	}

	private void $68() {
		l();
		$69();
		r();
	}

	private void $69() {
		l();
		$70();
		r();
	}

	private void $70() {
		l();
		$71();
		r();
	}

	private void $71() {
		l();
		$72();
		r();
	}

	private void $72() {
		l();
		$73();
		r();
	}

	private void $73() {
		l();
		$74();
		r();
	}

	private void $74() {
		l();
		$75();
		r();
	}

	private void $75() {
		l();
		$76();
		r();
	}

	private void $76() {
		l();
		$77();
		r();
	}

	private void $77() {
		l();
		$78();
		r();
	}

	private void $78() {
		l();
		$79();
		r();
	}

	private void $79() {
		l();
		$80();
		r();
	}

	private void $80() {
		l();
		$81();
		r();
	}

	private void $81() {
		l();
		$82();
		r();
	}

	private void $82() {
		l();
		$83();
		r();
	}

	private void $83() {
		l();
		$84();
		r();
	}

	private void $84() {
		l();
		$85();
		r();
	}

	private void $85() {
		l();
		$86();
		r();
	}

	private void $86() {
		l();
		$87();
		r();
	}

	private void $87() {
		l();
		$88();
		r();
	}

	private void $88() {
		l();
		$89();
		r();
	}

	private void $89() {
		l();
		$90();
		r();
	}

	private void $90() {
		l();
		$91();
		r();
	}

	private void $91() {
		l();
		$92();
		r();
	}

	private void $92() {
		l();
		$93();
		r();
	}

	private void $93() {
		l();
		$94();
		r();
	}

	private void $94() {
		l();
		$95();
		r();
	}

	private void $95() {
		l();
		$96();
		r();
	}

	private void $96() {
		l();
		$97();
		r();
	}

	private void $97() {
		l();
		$98();
		r();
	}

	private void $98() {
		l();
		$99();
		r();
	}

	private void $99() {
		l();
		sleep();
		r();
	}

	private void sleep() {
		try {
			Thread.sleep(SLEEP);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
