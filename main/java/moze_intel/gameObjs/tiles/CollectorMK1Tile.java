package moze_intel.gameObjs.tiles;

import scala.actors.threadpool.Arrays;
import scala.tools.nsc.interpreter.Results;
import moze_intel.MozeCore;
import moze_intel.gameObjs.ObjHandler;
import moze_intel.gameObjs.items.ItemBase;
import moze_intel.network.packets.CollectorSyncPKT;
import moze_intel.utils.Constants;
import moze_intel.utils.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class CollectorMK1Tile extends TileEmcProducer implements IInventory, ISidedInventory
{
	private ItemStack[] inventory;
	private int[] accessibleSlots;
	private final int invBufferSize;
	private final int emcGen;
	private final int lockSlot;
	private final int upgradedSlot;
	private boolean hasKleinStar;
	private boolean hasFuel;
	public double storedFuelEmc;
	public int displayEmc;
	public int displaySunLevel;
	public int displayKleinCharge;
	private int numUsing;
	
	public CollectorMK1Tile()
	{
		super(Constants.COLLECTOR_MK1_MAX);
		inventory = new ItemStack[11];
		invBufferSize = 8;
		
		accessibleSlots = new int[invBufferSize];
		
		for (int i = 0; i < invBufferSize; i++)
		{
			accessibleSlots[i] = i + 1;
		}
		
		emcGen = Constants.COLLECTOR_MK1_GEN;
		upgradedSlot = 9;
		lockSlot = 10;
	}
	
	public CollectorMK1Tile(int maxEmc, int emcGen, int upgradedSlot, int lockSlot)
	{
		super(maxEmc);
		inventory = new ItemStack[lockSlot + 1];
		invBufferSize = lockSlot - 2;
		
		accessibleSlots = new int[invBufferSize];
		
		for (int i = 0; i < invBufferSize; i++)
		{
			accessibleSlots[i] = i + 1;
		}
		
		this.emcGen = emcGen;
		this.upgradedSlot = upgradedSlot;
		this.lockSlot = lockSlot;
	}
	
	@Override
	public void updateEntity()
	{
		if (worldObj.isRemote) 
		{
			return;
		}
		
		sortInventory();
		
		if (inventory[0] == null)
		{
			hasKleinStar = false;
			hasFuel = false;
			this.isRequestingEmc = false;
		}
		else 
		{
			checkFuelOrKlein();
		}
		
		if (!this.hasMaxedEmc())
		{
			this.addEmc(getSunRelativeEmc(emcGen) / 20.0f);
		}
		
		updateEmc();
		
		displayEmc = (int) this.getStoredEMC();
		displaySunLevel = getSunLevel();
		displayKleinCharge = getKleinStarCharge();
		
		if (numUsing > 0)
		{
			MozeCore.pktHandler.sendToAllAround(new CollectorSyncPKT(displayEmc, displayKleinCharge, this.xCoord, this.yCoord, this.zCoord),
					new TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 6));
		}
	}
	
	private void sortInventory()
	{
		for (int i = 1; i <= invBufferSize; i++)
		{
			ItemStack current = getStackInSlot(i);
			
			if (current == null)
			{
				continue;
			}
			
			int nextIndex = i < invBufferSize ? i + 1 : 0;
			
			ItemStack following = inventory[nextIndex];
			
			if (following == null)
			{
				inventory[nextIndex] = current;
				decrStackSize(i, current.stackSize);
				
				continue;
			}
			else if (Utils.areItemStacksEqual(current, following) && following.stackSize < following.getMaxStackSize())
			{
				int missingForFullStack = following.getMaxStackSize() - following.stackSize;
				
				if (current.stackSize <= missingForFullStack)
				{
					inventory[nextIndex].stackSize += current.stackSize;
					inventory[i] = null;
				}
				else
				{
					inventory[nextIndex].stackSize += missingForFullStack;
					decrStackSize(i, missingForFullStack);
				}
				
				continue;
			}
		}
	}
	
	public void checkFuelOrKlein()
	{
		if (inventory[0].getItem().equals(ObjHandler.kleinStars))
		{
			if(ItemBase.getEmc(inventory[0]) != Utils.getKleinStarMaxEmc(inventory[0]))
			{
				hasKleinStar = true;
				hasFuel = false;
				this.isRequestingEmc = true;
			}
			else
			{
				hasKleinStar = false;
				this.isRequestingEmc = false;
			}
		}
		else
		{
			hasFuel = true;
			hasKleinStar = false;
			this.isRequestingEmc = true;
		}
	}
	
	public void updateEmc()
	{
		this.checkSurroundingBlocks(false);
		int numRequest = this.getNumRequesting();
		
		if (this.getStoredEMC() == 0)
		{
			return;
		}
		else if (hasKleinStar)
		{
			double toSend = this.getStoredEMC() < emcGen ? this.getStoredEMC() : emcGen;
			
			double starEmc = ItemBase.getEmc(inventory[0]);
			int maxStarEmc = Utils.getKleinStarMaxEmc(inventory[0]);
			
			if ((starEmc + toSend) > maxStarEmc)
			{
				toSend = maxStarEmc - starEmc;
			}
			
			ItemBase.addEmc(inventory[0], toSend);
			this.removeEmc(toSend);
		}
		else if (hasFuel)
		{
			ItemStack result = inventory[lockSlot] == null ? Constants.getFuelUpgrade(inventory[0]) : inventory[lockSlot].copy();
			
			int upgradeCost = Utils.getEmcValue(result) - Utils.getEmcValue(inventory[0]);
			
			if (upgradeCost > 0 && this.getStoredEMC() >= upgradeCost)
			{
				ItemStack upgrade = inventory[upgradedSlot];

				if (inventory[upgradedSlot] == null)
				{
					this.removeEmc(upgradeCost);
					this.setInventorySlotContents(upgradedSlot, result);
					this.decrStackSize(0, 1);
				}
				else if (Utils.basicAreStacksEqual(result, upgrade) && upgrade.stackSize < upgrade.getMaxStackSize())
				{
					this.removeEmc(upgradeCost);
					inventory[upgradedSlot].stackSize++;
					this.decrStackSize(0, 1);
				}
			}
		}
		else if (numRequest > 0 && !this.isRequestingEmc)
		{
			double toSend = this.getStoredEMC() < emcGen ? this.getStoredEMC() : emcGen;
			this.sendEmcToRequesting(toSend / numRequest);
			this.sendRelayBonus();
			this.removeEmc(toSend);
		}
	}
	
	private float getSunRelativeEmc(int emc)
	{
		return (float) getSunLevel() * emc / 16;
	}
	
	public int getKleinStarCharge()
	{
		if (inventory[0] != null && inventory[0].getItem().equals(ObjHandler.kleinStars))
		{
			return (int) ItemBase.getEmc(inventory[0]);
		}
		
		return -1;
	}
	
	public int getKleinStarChargeScaled(int i)
	{
		if (inventory[0] == null || displayKleinCharge <= 0)
		{
			return 0;
		}
		
		return displayKleinCharge * i / Utils.getKleinStarMaxEmc(inventory[0]);
	}
	
	public int getSunLevel()
	{
		if (worldObj.provider.isHellWorld)
		{
			return 16;
		}
		return worldObj.getBlockLightValue(xCoord, yCoord + 1, zCoord) + 1;
	}
	
	public int getEmcScaled(int i)
	{
		if (displayEmc == 0) 
		{
			return 0;
		}
		return displayEmc * i / this.getMaxEmc();
	}
	
	public int getSunLevelScaled(int i)
	{
		return displaySunLevel * i / 16;
	}
	
	public int getFuelProgressScaled(int i)
	{
		if (inventory[0] == null || !Constants.isStackFuel(inventory[0]))
		{
			return 0;
		}
		
		int reqEmc = 0;
		
		if (inventory[lockSlot] != null)
		{
			reqEmc = Utils.getEmcValue(inventory[lockSlot]) - Utils.getEmcValue(inventory[0]);
			
			if (reqEmc < 0)
			{
				return 0;
			}
		}
		else
		{
			reqEmc = Utils.getEmcValue(Constants.getFuelUpgrade(inventory[0])) - Utils.getEmcValue(inventory[0]);
		}
		
		if (this.getStoredEMC() >= reqEmc)
		{
			return i;
		}
		
		return displayEmc * i / reqEmc;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		this.setEmcValue(nbt.getDouble("EMC"));
		storedFuelEmc = nbt.getDouble("FuelEMC");
		
		NBTTagList list = nbt.getTagList("Items", 10);
		inventory = new ItemStack[getSizeInventory()];
		for (int i = 0; i < list.tagCount(); i++)
		{
			NBTTagCompound subNBT = list.getCompoundTagAt(i);
			
			byte slot = subNBT.getByte("Slot");
			
			if (slot >= 0 && slot < getSizeInventory())
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
		nbt.setDouble("FuelEMC", storedFuelEmc);
		
		NBTTagList list = new NBTTagList();
		
		for (int i = 0; i < getSizeInventory(); i++)
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
		return inventory.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) 
	{
		return inventory[slot];
	}

	@Override
	public ItemStack decrStackSize(int slot, int qty) 
	{
		ItemStack stack = inventory[slot];
		if (stack != null)
		{
			if (stack.stackSize <= qty)
			{
				inventory[slot] = null;
			}
			else
			{
				stack = stack.splitStack(qty);
				if (stack.stackSize == 0)
					inventory[slot] = null;
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
			stack.stackSize = this.getInventoryStackLimit();
		this.markDirty();
	}

	@Override
	public String getInventoryName() 
	{
		return "Collector MK1";
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
	public boolean isItemValidForSlot(int slot, ItemStack stack) 
	{
		return true;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) 
	{
		if (side == 0 || side == 1)
		{
			return new int[] {upgradedSlot};
		}
		
		return accessibleSlots;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) 
	{
		if (side == 0 || side == 1)
		{
			return false;
		}
		
		return Constants.isStackFuel(stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) 
	{
		if (side == 0 || side == 1)
		{
			return slot == upgradedSlot;
		}
		
		return false;
	}
}