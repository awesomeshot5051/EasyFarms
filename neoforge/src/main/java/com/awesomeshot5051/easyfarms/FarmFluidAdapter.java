package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Take-only view of a Liquid Farm's tank for pipes and tanks. */
public record FarmFluidAdapter(FarmBlockEntity farm) implements IFluidHandler {
    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        var id = farm.fluidId();
        return id == null ? FluidStack.EMPTY : new FluidStack(BuiltInRegistries.FLUID.get(id), farm.fluidAmount());
    }

    @Override
    public int getTankCapacity(int tank) {
        return farm.fluidCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        var id = farm.fluidId();
        if (id == null || resource.isEmpty() || resource.getFluid() != BuiltInRegistries.FLUID.get(id)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        var id = farm.fluidId();
        if (id == null) return FluidStack.EMPTY;
        int taken = farm.extractFluid(maxDrain, action.simulate());
        return taken <= 0 ? FluidStack.EMPTY : new FluidStack(BuiltInRegistries.FLUID.get(id), taken);
    }
}
