package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Receive-only view of a farm's ME buffer for cables and generators (FE). */
public record FarmEnergyAdapter(FarmBlockEntity farm) implements IEnergyStorage {
    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return (int) farm.receiveEnergy(toReceive, simulate);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, farm.energyStored());
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, farm.energyCapacity());
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return farm.energyCapacity() > 0;
    }
}
