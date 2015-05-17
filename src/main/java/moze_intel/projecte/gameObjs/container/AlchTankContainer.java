package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.gameObjs.tiles.AlchTankTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class AlchTankContainer extends Container
{
    // TODO: All the things
    private AlchTankTile tile;

    public AlchTankContainer(IInventory invPlayer, AlchTankTile alchTankTile)
    {
        tile = alchTankTile;

        //Player inventory
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 20 + j * 18, 84 + i * 18));

        //Player hotbar
        for (int i = 0; i < 9; i++)
            this.addSlotToContainer(new Slot(invPlayer, i, 20 + i * 18, 142));
    }

    @Override
    public boolean canInteractWith(EntityPlayer p_75145_1_)
    {
        return true;
    }
}
