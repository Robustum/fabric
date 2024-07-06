/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.api.transfer.v1.storage.base;

import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.nbt.CompoundTag;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/**
 * A storage that can store a single transfer variant at any given time.
 * Implementors should at least override {@link #getCapacity(TransferVariant)},
 * and probably {@link #onFinalCommit} as well for {@code markDirty()} and similar calls.
 *
 * <p>{@link #canInsert} and {@link #canExtract} can be used for more precise control over which variants may be inserted or extracted.
 * If one of these two functions is overridden to always return false, implementors may also wish to override
 * {@link #supportsInsertion} and/or {@link #supportsExtraction}.
 *
 * @see net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage SingleFluidStorage for fluid variants.
 * @see net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage SingleItemStorage for item variants.
 */
public abstract class SingleVariantStorage<T extends TransferVariant<?>> extends SnapshotParticipant<ResourceAmount<T>> implements SingleSlotStorage<T> {
	private static final Logger LOGGER = LogManager.getLogger("fabric-transfer-api-v1/variant-storage");

	public T variant = getBlankVariant();
	public long amount = 0;

	/**
	 * Return the blank variant.
	 *
	 * <p>Note: this is called very early in the constructor.
	 * If fields need to be accessed from this function, make sure to re-initialize {@link #variant} yourself.
	 */
	protected abstract T getBlankVariant();

	/**
	 * Return the maximum capacity of this storage for the passed transfer variant.
	 * If the passed variant is blank, an estimate should be returned.
	 */
	protected abstract long getCapacity(T variant);

	/**
	 * @return {@code true} if the passed non-blank variant can be inserted, {@code false} otherwise.
	 */
	protected boolean canInsert(T variant) {
		return true;
	}

	/**
	 * @return {@code true} if the passed non-blank variant can be extracted, {@code false} otherwise.
	 */
	protected boolean canExtract(T variant) {
		return true;
	}

	@Override
	public long insert(T insertedVariant, long maxAmount, TransactionContext transaction) {
		StoragePreconditions.notBlankNotNegative(insertedVariant, maxAmount);

		if ((insertedVariant.equals(variant) || variant.isBlank()) && canInsert(insertedVariant)) {
			long insertedAmount = Math.min(maxAmount, getCapacity(insertedVariant) - amount);

			if (insertedAmount > 0) {
				updateSnapshots(transaction);

				if (variant.isBlank()) {
					variant = insertedVariant;
					amount = insertedAmount;
				} else {
					amount += insertedAmount;
				}

				return insertedAmount;
			}
		}

		return 0;
	}

	@Override
	public long extract(T extractedVariant, long maxAmount, TransactionContext transaction) {
		StoragePreconditions.notBlankNotNegative(extractedVariant, maxAmount);

		if (extractedVariant.equals(variant) && canExtract(extractedVariant)) {
			long extractedAmount = Math.min(maxAmount, amount);

			if (extractedAmount > 0) {
				updateSnapshots(transaction);
				amount -= extractedAmount;

				if (amount == 0) {
					variant = getBlankVariant();
				}

				return extractedAmount;
			}
		}

		return 0;
	}

	@Override
	public boolean isResourceBlank() {
		return variant.isBlank();
	}

	@Override
	public T getResource() {
		return variant;
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public long getCapacity() {
		return getCapacity(variant);
	}

	@Override
	protected ResourceAmount<T> createSnapshot() {
		return new ResourceAmount<>(variant, amount);
	}

	@Override
	protected void readSnapshot(ResourceAmount<T> snapshot) {
		variant = snapshot.resource();
		amount = snapshot.amount();
	}

	@Override
	public String toString() {
		return "SingleVariantStorage[%d %s]".formatted(amount, variant);
	}

	/**
	 * Read a {@link SingleVariantStorage} from NBT.
	 *
	 * @param storage  the {@link SingleVariantStorage} to read into
	 * @param fromNbt  the item variant codec
	 * @param fallback the fallback item variant, used when the NBT is invalid
	 * @param nbt      the NBT to read from
	 * @param <T>      the type of the item variant
	 */
	public static <T extends TransferVariant<?>> void readNbt(SingleVariantStorage<T> storage, Function<CompoundTag, T> fromNbt, Supplier<T> fallback, CompoundTag nbt) {
		try {
			storage.variant = fromNbt.apply(nbt.getCompound("variant"));
		} catch (Exception e) {
			LOGGER.debug("Failed to load an ItemVariant from NBT: {}", e.getLocalizedMessage());
			storage.variant = fallback.get();
		}

		storage.amount = nbt.getLong("amount");
	}

	/**
	 * Write a {@link SingleVariantStorage} to NBT.
	 *
	 * @param storage the {@link SingleVariantStorage} to write from
	 * @param toNbt   the item variant codec
	 * @param nbt     the NBT to write to
	 * @param <T>     the type of the item variant
	 */
	public static <T extends TransferVariant<?>> void writeNbt(SingleVariantStorage<T> storage, Function<T, CompoundTag> toNbt, CompoundTag nbt) {
		nbt.put("variant", toNbt.apply(storage.variant));
		nbt.putLong("amount", storage.amount);
	}
}
