package net.fabricmc.fabric.impl.gametest.vanilla;

import net.minecraft.test.FailureLoggingTestCompletionListener;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestCompletionListener;

public class TestFailureLogger {
	private static TestCompletionListener completionListener = new FailureLoggingTestCompletionListener();

	public static void setCompletionListener(TestCompletionListener listener) {
		completionListener = listener;
	}

	public static void failTest(GameTest test) {
		completionListener.onTestFailed(test);
	}

	public static void passTest(GameTest test) {
//		completionListener.onTestPassed(test);
	}

	public static void stop() {
//		completionListener.onStopped();
	}
}
