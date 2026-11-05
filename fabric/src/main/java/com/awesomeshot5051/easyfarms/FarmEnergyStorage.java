package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

/** A farm's ME buffer as Team Reborn Energy storage (receive only). Only loaded when that mod is present. */
public final class FarmEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
    private final FarmBlockEntity farm;

    private FarmEnergyStorage(FarmBlockEntity farm) {
        this.farm = farm;
    }

    public static void register() {
        EnergyStorage.SIDED.registerForBlockEntity((farm, side) -> new FarmEnergyStorage(farm), EasyFarms.FARM_BE.get());
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public boolean supportsInsertion() {
        return farm.energyCapacity() > 0;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        long accepted = farm.receiveEnergy(maxAmount, true);
        if (accepted <= 0) return 0;
        updateSnapshots(transaction);
        return farm.receiveEnergy(accepted, false);
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long getAmount() {
        return farm.energyStored();
    }

    @Override
    public long getCapacity() {
        return farm.energyCapacity();
    }

    @Override
    protected Long createSnapshot() {
        return farm.energyStored();
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        farm.setEnergy(snapshot);
    }
}
