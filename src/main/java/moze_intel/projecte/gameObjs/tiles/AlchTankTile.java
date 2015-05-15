package moze_intel.projecte.gameObjs.tiles;


import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

public class AlchTankTile extends TileEmc implements IInventory, IFluidHandler {

    public AlchTankTile()
    {

    }

    public TankEMC tank = new TankEMC(this);

    private ItemStack[] inv = new ItemStack[2];

    @Override
    public int getSizeInventory()
    {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int i)
    {
        if (i >= 1) return inv[i];
        return null;
    }

    @Override
    public ItemStack decrStackSize(int i, int i1)
    {
        if (i <= 1 && inv[i] == null)
            return null;
        ItemStack stack = inv[i].copy();
        stack.stackSize = Math.max(stack.getMaxStackSize(),Math.min(i1,stack.stackSize));
        if (stack.stackSize == inv[i].stackSize) inv[i] = null;
        else inv[i].stackSize -= stack.stackSize;
        return stack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i)
    {
        if (i <= 1 && inv[i] == null)
            return null;
        ItemStack stack = inv[i];
        inv[i] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemStack)
    {
        if (i <= 1) return;
        inv[i] = itemStack;
    }

    @Override
    public String getInventoryName()
    {
        return "Alchemical Tank";
    }

    @Override
    public boolean hasCustomInventoryName()
    {
        return false;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityPlayer)
    {
        return true;
    }

    @Override
    public void openInventory()
    {

    }

    @Override
    public void closeInventory()
    {

    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack)
    {
        return false;
    }

    @Override
    public boolean isRequestingEmc()
    {
        return false;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
    {
        return tank.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
    {
        if (!resource.isFluidEqual(tank.getFluid())) return null;
        return tank.drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
    {
        return tank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid)
    {
        return fluid.equals(tank.getFluidType());
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid)
    {
        return fluid.equals(tank.getFluidType());
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from)
    {
        return new FluidTankInfo[]{tank.getInfo()};
    }

    @Override
    public void readFromNBT(NBTTagCompound p_145839_1_)
    {
        super.readFromNBT(p_145839_1_);
        tank.readFromNBT(p_145839_1_);
    }

    @Override
    public void writeToNBT(NBTTagCompound p_145841_1_)
    {
        super.writeToNBT(p_145841_1_);
        tank.writeToNBT(p_145841_1_);
    }

    private static class TankEMC extends FluidTank {
        public TankEMC(AlchTankTile tile)
        {
            super(FluidContainerRegistry.BUCKET_VOLUME * 512);
            this.tile = tile;
        }

        public boolean isEmpty()
        {
            return getFluid() == null || getFluid().amount <= 0;
        }

        public boolean isFull()
        {
            return getFluid() != null && getFluid().amount >= getCapacity();
        }

        public Fluid getFluidType()
        {
            return getFluid() != null ? getFluid().getFluid() : null;
        }

        @Override
        public final NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            if (nbt != null) {
                NBTTagCompound tankData = new NBTTagCompound();
                super.writeToNBT(tankData);
                writeTankToNBT(tankData);
                nbt.setTag("AlchTank", tankData);
            }
            return nbt;
        }

        @Override
        public final FluidTank readFromNBT(NBTTagCompound nbt) {
            if (nbt.hasKey("AlchTank")) {
                NBTTagCompound tankData = nbt.getCompoundTag("AlchTank");
                super.readFromNBT(tankData);
                readTankFromNBT(tankData);
            }
            return this;
        }

        public void writeTankToNBT(NBTTagCompound nbt) {
        }

        public void readTankFromNBT(NBTTagCompound nbt) {
        }


    }
}
