package moze_intel.gameObjs.tiles;

import moze_intel.MozeCore;
import moze_intel.gameObjs.ObjHandler;
import moze_intel.network.packets.CondenserSyncPKT;
import moze_intel.utils.Constants;
import moze_intel.utils.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class CondenserTile extends TileEmcConsumerDirection implements IInventory, ISidedInventory
{
	private ItemStack[] inventory = new ItemStack[92];
	private int[] validInventorySlots = new int[91];
	private ItemStack lock;
	private int ticksSinceSync;
	public int displayEmc;
	public float lidAngle;
    public float prevLidAngle;
    public int numPlayersUsing;
	public int requiredEmc;
	
	public CondenserTile()
	{
		super(100000000);
		
		for (int i = 1; i < 92; i++)
		{
			validInventorySlots[i - 1] = i;
		}
	}
	
	@Override
	public void updateEntity()
	{
		updateChest();
		
		if (this.worldObj.isRemote)
		{
			return;
		}
		
		displayEmc = (int) this.getStoredEMC();
		lock = getStackInSlot(0);
		
		if (lock == null)
		{
			if (requiredEmc != 0)
			{
				displayEmc = 0;
				requiredEmc = 0;
				this.isRequestingEmc = false;
			}
		}
		else
		{
			if (requiredEmc != Utils.getEmcValue(lock))
			{
				requiredEmc = Utils.getEmcValue(lock);
				this.isRequestingEmc = true;
			}
			
			if (this.getStoredEMC() > requiredEmc)
			{
				handleMassCondense();
			}
			
			condense();
		}
		
		if (numPlayersUsing > 0)
		{
			MozeCore.pktHandler.sendToAllAround(new CondenserSyncPKT(displayEmc, requiredEmc, this.xCoord, this.yCoord, this.zCoord),
				new TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 6));
		}
	}
	
	private void handleMassCondense()
	{
		while(hasSpace() && this.getStoredEMC() > requiredEmc)
		{
			double result = this.getStoredEMC() - requiredEmc;
			pushStack();
			this.setEmcValue(result);
		}
	}
	
	private void condense()
	{
		if (!hasSpace()) 
		{
			this.isRequestingEmc = false;
			return;
		}
		
		for (int i = 1; i < 92; i++)
		{
			ItemStack stack = getStackInSlot(i);
			
			if (stack == null || isStackEqualToLock(stack)) 
			{
				continue;
			}
			
			decrStackSize(i, 1);
			this.addEmc(Utils.getEmcValue(stack));
			break;
		}
		
		if (this.getStoredEMC() >= requiredEmc)
		{
			double result = this.getStoredEMC() - requiredEmc;
			pushStack();
			this.setEmcValue(result);
		}
	}
	
	private void pushStack()
	{
		int slot = getSlotForStack();
		
		if (slot == 0) 
		{
			return;
		}
		
		ItemStack stack = getStackInSlot(slot);
		
		if (stack == null)
		{
			ItemStack lockCopy = lock.copy();
			
			if (lockCopy.hasTagCompound())
			{
				lockCopy.setTagCompound(new NBTTagCompound());
			}
			
			setInventorySlotContents(slot, lockCopy);
		}
		else
		{
			stack.stackSize += 1;
			setInventorySlotContents(slot, stack);
		}
	}
	
	private int getSlotForStack()
	{
		for (int i = 1; i < 92; i++)
		{
			ItemStack stack = getStackInSlot(i);
			if (stack == null) 
			{
				return i;
			}
			
			if (isStackEqualToLock(stack) && stack.stackSize < stack.getMaxStackSize()) 
			{
				return i;
			}
		}
		return 0;
	}
	
	private boolean hasSpace()
	{
		for (int i = 1; i < 92; i++)
		{
			ItemStack stack = getStackInSlot(i);
			
			if (stack == null) 
			{
				return true;
			}
			
			if (isStackEqualToLock(stack) && stack.stackSize < stack.getMaxStackSize()) 
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isStackEqualToLock(ItemStack stack)
	{
		if (lock == null) 
		{
			return false;
		}
		
		ItemStack compare = stack.copy();
		compare.stackSize = 1;
		
		return compare.getItem() == lock.getItem() && compare.getItemDamage() == lock.getItemDamage();
	}
	
	public int getProgressScaled()
	{
		if (requiredEmc == 0) 
		{
			return 0;
		}
		if (displayEmc >= requiredEmc) 
		{
			return Constants.MAX_CONDENSER_PROGRESS;
		}
		
		return (displayEmc * Constants.MAX_CONDENSER_PROGRESS) / requiredEmc;
	}
	
	public void sendUpdate()
    {
    	worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		this.setEmcValue(nbt.getDouble("EMC"));
		NBTTagList list = nbt.getTagList("Items", 10);
		inventory = new ItemStack[92];
		
		for (int i = 0; i < list.tagCount(); i++)
		{
			NBTTagCompound subNBT = list.getCompoundTagAt(i);
			byte slot = subNBT.getByte("Slot");
			
			if (slot >= 0 && slot < 92)
			{
				inventory[slot] = ItemStack.loadItemStackFromNBT(subNBT);
			}
		}	
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setDouble("EMC", this.getStoredEMC());
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < 92; i++)
		{
			if (inventory[i] == null) 
			{
				continue;
			}
			
			NBTTagCompound subNBT = new NBTTagCompound();
			subNBT.setByte("Slot", (byte) i);
			inventory[i].writeToNBT(subNBT);
			list.appendTag(subNBT);
		}
		
		nbt.setTag("Items", list);
	}

	@Override
	public int getSizeInventory() 
	{
		return 92;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		return inventory[slot];
	}

	@Override
	public ItemStack decrStackSize(int slot, int qnt) 
	{
		ItemStack stack = inventory[slot];
		if (stack != null)
		{
			if (stack.stackSize <= qnt)
			{
				inventory[slot] = null;
			}
			else
			{
				stack = stack.splitStack(qnt);
				if (stack.stackSize == 0)
				{
					inventory[slot] = null;
				}
			}
		}
		
		return stack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) 
	{
		if (inventory[slot] != null)
		{
			ItemStack stack = inventory[slot];
			inventory[slot] = null;
			return stack;
		}
		return null;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) 
	{
		inventory[slot] = stack;
		
		if (stack != null && stack.stackSize > this.getInventoryStackLimit())
		{
			stack.stackSize = this.getInventoryStackLimit();
		}
		
		this.markDirty();
	}

	@Override
	public String getInventoryName() 
	{
		return "Condenser";
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
	public boolean isUseableByPlayer(EntityPlayer var1) 
	{
		return true;
	}

	public void updateChest()
    {
        if (++ticksSinceSync % 20 * 4 == 0)
            worldObj.addBlockEvent(xCoord, yCoord, zCoord, ObjHandler.condenser, 1, numPlayersUsing);

        prevLidAngle = lidAngle;
        float angleIncrement = 0.1F;
        double adjustedXCoord, adjustedZCoord;
        
        if (numPlayersUsing > 0 && lidAngle == 0.0F)
        {
            adjustedXCoord = xCoord + 0.5D;
            adjustedZCoord = zCoord + 0.5D;
            worldObj.playSoundEffect(adjustedXCoord, yCoord + 0.5D, adjustedZCoord, "random.chestopen", 0.5F, worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }

        if (numPlayersUsing == 0 && lidAngle > 0.0F || numPlayersUsing > 0 && lidAngle < 1.0F)
        {
            float var8 = lidAngle;

            if (numPlayersUsing > 0)
                lidAngle += angleIncrement;
            else
                lidAngle -= angleIncrement;

            if (lidAngle > 1.0F)
                lidAngle = 1.0F;

            if (lidAngle < 0.5F && var8 >= 0.5F)
            {
                adjustedXCoord = xCoord + 0.5D;
                adjustedZCoord = zCoord + 0.5D;
                worldObj.playSoundEffect(adjustedXCoord, yCoord + 0.5D, adjustedZCoord, "random.chestclosed", 0.5F, worldObj.rand.nextFloat() * 0.1F + 0.9F);
            }

            if (lidAngle < 0.0F)
                lidAngle = 0.0F;
        }
    }
	
	@Override
    public boolean receiveClientEvent(int number, int arg)
    {
        if (number == 1)
        {
            numPlayersUsing = arg;
            return true;
        }
        else return super.receiveClientEvent(number, arg);
    }
	
	@Override
    public void openInventory()
    {
    	++numPlayersUsing;
        worldObj.addBlockEvent(xCoord, yCoord, zCoord, ObjHandler.condenser, 1, numPlayersUsing);
    }
	
	@Override
    public void closeInventory()
    {
    	--numPlayersUsing;
    	worldObj.addBlockEvent(xCoord, yCoord, zCoord, ObjHandler.condenser, 1, numPlayersUsing);
    }

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) 
	{
		if (slot == 0) 
		{
			return false;
		}
		
		return !isStackEqualToLock(stack) && Utils.doesItemHaveEmc(stack);
	}
	
	@Override
	public int[] getAccessibleSlotsFromSide(int side) 
	{
		return validInventorySlots;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack item, int side) 
	{
		return isItemValidForSlot(slot, item);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack item, int side) 
	{
		return isStackEqualToLock(item);
	}
}