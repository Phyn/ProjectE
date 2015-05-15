package moze_intel.projecte.gameObjs.items.itemBlocks;

import moze_intel.projecte.utils.AchievementHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraft.world.World;

public class ItemAlchemyTankBlock extends ItemBlock
{
    public ItemAlchemyTankBlock(Block block)
    {
        super(block);
    }

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player)
    {
        Achievement achievement = AchievementHandler.getAchievementForItem(stack);
        if (achievement != null && world != null && player != null)
        {
            player.addStat(achievement,1);
        }
    }

}
