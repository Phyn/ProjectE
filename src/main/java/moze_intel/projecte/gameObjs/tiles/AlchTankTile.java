package moze_intel.projecte.gameObjs.tiles;


import moze_intel.projecte.gameObjs.ObjHandler;
import moze_intel.projecte.gameObjs.items.EvertideAmulet;
import moze_intel.projecte.gameObjs.items.KleinStar;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.FluidHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

public class AlchTankTile extends TileEmc implements IInventory, IFluidHandler {

    protected TankEMC tank;
    protected ItemStack[] inv = new ItemStack[4];
    protected int numUsing = 0;

    public AlchTankTile()
    {
        tank = new TankEMC(this);
    }

    protected AlchTankTile(TankEMC tank)
    {
        this.tank = tank;
    }

    @Override
    public int getSizeInventory()
    {
        return inv.length;
    }

    @Override
    public ItemStack getStackInSlot(int i)
    {
        return i >=3 ? inv[i] : null;
    }

    @Override
    public ItemStack decrStackSize(int i, int i1)
    {
        if (i <= 3 && inv[i] == null)
            return null;
        ItemStack stack = inv[i].copy();
        stack.stackSize = Math.max(stack.getMaxStackSize(),Math.min(i1,stack.stackSize));
        if (stack.stackSize >= inv[i].stackSize) inv[i] = null;
        else inv[i].stackSize -= stack.stackSize;
        return stack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i)
    {
        if (i <= 3 && inv[i] == null)
            return null;
        ItemStack stack = inv[i];
        inv[i] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemStack)
    {
        if (i <= 3) return;
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
        numUsing++;
    }

    @Override
    public void closeInventory()
    {
        numUsing--;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack)
    {
        if (itemStack == null) return false;
        switch (i)
        {
            case 0:
                if (itemStack.getItem().equals(inv[0].getItem()))
                    return true;
                if (inv[0] != null)
                    return false;
                if (itemStack.getItem() == ObjHandler.volcanite && (tank.isEmpty() || tank.getFluidType().equals(FluidRegistry.LAVA)))
                    return true;
                if (itemStack.getItem() instanceof IFluidContainerItem && ((IFluidContainerItem) itemStack.getItem()).getFluid(itemStack).isFluidEqual(tank.getFluid()))
                    return true;
                if (FluidContainerRegistry.isFilledContainer(itemStack) && FluidContainerRegistry.containsFluid(itemStack, tank.getFluid()))
                    return true;
                return FluidContainerRegistry.isEmptyContainer(itemStack);
            case 3:
                return itemStack.getItem() instanceof KleinStar;
            default:
                return false;
        }
    }

    @Override
    public boolean isRequestingEmc()
    {
        // TODO:get water EMC and logic
        return inv[0].getItem().equals(ObjHandler.volcanite);
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
    {
        return tank.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
    {
        if (tank.isEmpty() || !resource.isFluidEqual(tank.getFluid())) return null;
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

    @Override
    public void updateEntity()
    {
        if (!tank.isFull())
        {
            if (inv[0] != null && inv[0].getItem().equals(ObjHandler.everTide) && (tank.isEmpty() || tank.getFluidType().equals(FluidRegistry.WATER)))
            {
                tank.fill(new FluidStack(FluidRegistry.WATER, 512 * FluidContainerRegistry.BUCKET_VOLUME), true);

                // TODO: get value + logic
                //this.removeEmcWithPKT();
            }
            else if (inv[0] != null && inv[0].getItem().equals(ObjHandler.volcanite) && (tank.isEmpty() || tank.getFluidType().equals(FluidRegistry.LAVA)))
            {
                // TODO: get value + logic
                this.removeEmcWithPKT(64.0f * tank.fill(new FluidStack(FluidRegistry.LAVA, 512 * FluidContainerRegistry.BUCKET_VOLUME), true));
            }
            else if ((inv[0] != null && inv[0].getItem() instanceof IFluidContainerItem) || (inv[1] != null && inv[1].getItem() instanceof IFluidContainerItem))
            {
                IFluidContainerItem fluidItem = (IFluidContainerItem) inv[1].getItem();
                if (fluidItem.getFluid(inv[1]) == null || fluidItem.getFluid(inv[1]).amount == 0)
                {
                    if (inv[1] != null && inv[1].getItem().equals(inv[2].getItem()))
                        inv[2].stackSize++;
                    else if (inv[2] == null)
                        inv[2] = inv[1];
                    inv[1] = decrStackSize(0, 1);
                }

                if (inv[1] != null)
                {
                    fluidItem = (IFluidContainerItem) inv[1].getItem();
                    if (fluidItem.getFluid(inv[1]) != null && fluidItem.getFluid(inv[1]).amount > 0)
                        tank.fill(fluidItem.drain(inv[1], tank.fill(fluidItem.drain(inv[1], 512 * FluidContainerRegistry.BUCKET_VOLUME, false), false), true), true);
                    else
                        fluidItem.fill(inv[1], tank.drain(fluidItem.fill(inv[1], tank.drain(FluidContainerRegistry.BUCKET_VOLUME, false), false), true), true);
                }
            }
            else if (FluidContainerRegistry.containsFluid(inv[0], tank.getFluid()))
            {
                if (tank.fill(FluidContainerRegistry.getFluidForFilledItem(inv[0]),false) == 0 && (inv[2] == null || (inv[2].getItem().equals(FluidContainerRegistry.drainFluidContainer(inv[0]).getItem()))))
                {
                    tank.fill(FluidContainerRegistry.getFluidForFilledItem(inv[0]), true);
                    if (inv[2] == null) inv[2] = FluidContainerRegistry.drainFluidContainer(inv[0]);
                    else inv[2].stackSize++;
                }
            }
            else if (FluidContainerRegistry.isEmptyContainer(inv[0]))
            {
                ItemStack filled = FluidContainerRegistry.fillFluidContainer(tank.drain(FluidContainerRegistry.getContainerCapacity(tank.getFluid(),inv[0]),false), inv[0]);
                if (filled != null)
                {
                    if (filled.getItem().equals(inv[2].getItem()))
                    {
                        tank.drain(FluidContainerRegistry.getContainerCapacity(tank.getFluid(), inv[0]), true);
                        inv[2].stackSize++;
                    }
                    else if (inv[2] == null)
                    {
                        tank.drain(FluidContainerRegistry.getContainerCapacity(tank.getFluid(), inv[0]), true);
                        inv[2] = filled;
                    }
                }
            }
        }
    }

    protected static class TankEMC extends FluidTank {
        public TankEMC(AlchTankTile tile)
        {
            super(FluidContainerRegistry.BUCKET_VOLUME * 512);
            this.tile = tile;
        }

        public boolean isEmpty()
        {
            if (getFluid() != null)
                if (getFluid().amount <= 0)
                {
                    this.fluid = null;
                    return true;
                }
            else
                return true;
            return false;
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
        public FluidStack getFluid()
        {
            if (isEmpty()) return null;
            return super.getFluid();
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
