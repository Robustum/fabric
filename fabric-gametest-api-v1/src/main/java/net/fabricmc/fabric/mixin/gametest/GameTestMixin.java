package net.fabricmc.fabric.mixin.gametest;

import com.google.common.base.Stopwatch;

import net.fabricmc.fabric.impl.gametest.GameTestExtensions;

import net.minecraft.test.GameTest;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.TimeUnit;

@Mixin(GameTest.class)
public abstract class GameTestMixin implements GameTestExtensions {
	@Shadow
	@Final
	private Stopwatch stopwatch;

	@Override
	public long fabric_getElapsedMilliseconds() {
		return stopwatch.elapsed(TimeUnit.MILLISECONDS);
	}
}
