package org.stagemonitor.tracing.utils;


/*
 * Copyright (c) 2016, Uber Technologies, Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class RateLimiter {
	private final double creditsPerNanosecond;
	private double balance;
	private double maxBalance;
	private long lastTick;

	public RateLimiter(double creditsPerSecond, double maxBalance) {
		this.balance = maxBalance;
		this.maxBalance = maxBalance;
		this.creditsPerNanosecond = creditsPerSecond / 1.0e9;
	}

	public boolean checkCredit(double itemCost) {
		long currentTime = System.nanoTime();
		double elapsedTime = currentTime - lastTick;
		lastTick = currentTime;
		balance += elapsedTime * creditsPerNanosecond;
		if (balance > maxBalance) {
			balance = maxBalance;
		}
		if (balance >= itemCost) {
			balance -= itemCost;
			return true;
		}
		return false;
	}
}
