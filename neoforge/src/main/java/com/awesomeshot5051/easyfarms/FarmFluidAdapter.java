package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Take-only view of a Liquid Farm's tank for pipes and tanks. */
public final class FarmFluidAdapter extends SnapshotJournal<FarmFluidAdapter.Tank> implements ResourceHandler<FluidResource> {
    record Tank(Identifier id, int amount) {
    }

    private final FarmBlockEntity farm;

    public FarmFluidAdapter(FarmBlockEntity farm) {
        this.farm = farm;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        var id = farm.fluidId();
        return id == null ? FluidResource.EMPTY : FluidResource.of(BuiltInRegistries.FLUID.getValue(id));
    }

    @Override
    public long getAmountAsLong(int index) {
        return farm.fluidAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return farm.fluidCapacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return false;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        var id = farm.fluidId();
        if (id == null || resource.isEmpty() || resource.getFluid() != BuiltInRegistries.FLUID.getValue(id)) return 0;
        int taken = farm.extractFluid(amount, true);
        if (taken <= 0) return 0;
        updateSnapshots(transaction);
        return farm.extractFluid(taken, false);
    }

    @Override
    protected Tank createSnapshot() {
        return new Tank(farm.fluidId(), farm.fluidAmount());
    }

    @Override
    protected void revertToSnapshot(Tank snapshot) {
        farm.restoreFluid(snapshot.id(), snapshot.amount());
    }
}
