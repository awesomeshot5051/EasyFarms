package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Receive-only view of a farm's ME buffer for cables and generators (FE). */
public final class FarmEnergyAdapter extends SnapshotJournal<Long> implements EnergyHandler {
    private final FarmBlockEntity farm;

    public FarmEnergyAdapter(FarmBlockEntity farm) {
        this.farm = farm;
    }

    @Override
    public long getAmountAsLong() {
        return farm.energyStored();
    }

    @Override
    public long getCapacityAsLong() {
        return farm.energyCapacity();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        long accepted = farm.receiveEnergy(amount, true);
        if (accepted <= 0) return 0;
        updateSnapshots(transaction);
        return (int) farm.receiveEnergy(accepted, false);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    protected Long createSnapshot() {
        return farm.energyStored();
    }

    @Override
    protected void revertToSnapshot(Long snapshot) {
        farm.setEnergy(snapshot);
    }
}
