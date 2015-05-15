package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.gameObjs.tiles.AlchTankTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

public class AlchTankContainer extends Container
{
    // TODO: All the things
    public AlchTankContainer(IInventory playerInventory, AlchTankTile alchTankTile)
    {

    }

    @Override
    public boolean canInteractWith(EntityPlayer p_75145_1_)
    {
        return true;
    }
}
