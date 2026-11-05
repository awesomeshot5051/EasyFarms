package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Take-only view of a Liquid Farm's tank (1 mB = 81 droplets). */
public final class FarmFluidStorage extends SnapshotParticipant<FarmFluidStorage.Tank> implements SingleSlotStorage<FluidVariant> {
    record Tank(Identifier id, int amount) {
    }

    private final FarmBlockEntity farm;

    FarmFluidStorage(FarmBlockEntity farm) {
        this.farm = farm;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!resource.equals(getResource())) return 0;
        int taken = farm.extractFluid((int) Math.min(Integer.MAX_VALUE, maxAmount / FluidConstants.BUCKET * 1000 + (maxAmount % FluidConstants.BUCKET) / 81), true);
        if (taken <= 0) return 0;
        updateSnapshots(transaction);
        return farm.extractFluid(taken, false) * 81L;
    }

    @Override
    public boolean isResourceBlank() {
        return farm.fluidId() == null;
    }

    @Override
    public FluidVariant getResource() {
        var id = farm.fluidId();
        return id == null ? FluidVariant.blank() : FluidVariant.of(BuiltInRegistries.FLUID.getValue(id));
    }

    @Override
    public long getAmount() {
        return farm.fluidAmount() * 81L;
    }

    @Override
    public long getCapacity() {
        return farm.fluidCapacity() * 81L;
    }

    @Override
    protected Tank createSnapshot() {
        return new Tank(farm.fluidId(), farm.fluidAmount());
    }

    @Override
    protected void readSnapshot(Tank snapshot) {
        farm.restoreFluid(snapshot.id(), snapshot.amount());
    }
}
