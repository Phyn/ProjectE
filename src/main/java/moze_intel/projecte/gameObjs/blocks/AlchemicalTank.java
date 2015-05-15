package moze_intel.projecte.gameObjs.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.tiles.AlchTankTile;
import moze_intel.projecte.gameObjs.tiles.TileEmc;
import moze_intel.projecte.utils.Constants;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;


public class AlchemicalTank extends BlockDirection {
    private IIcon textureTop;
    private IIcon textureSide;

    @Override
    public TileEntity createNewTileEntity(World world, int i) {
        return new AlchTankTile();
    }

    public AlchemicalTank() {
        super(Material.glass);
        this.setBlockName("pe_alchemy_tank");
        this.setBlockBounds(0.125F, 0F, 0.125F, 0.875F, 1F, 0.875F);
        this.setHardness(10.0f);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entLiving, ItemStack stack)
    {
        setFacingMeta(world, x, y, z, ((EntityPlayer) entLiving));

        TileEntity tile = world.getTileEntity(x, y, z);

        if (stack.hasTagCompound() && stack.stackTagCompound.getBoolean("ProjectEBlock") && tile instanceof TileEmc)
        {
            stack.stackTagCompound.setInteger("x", x);
            stack.stackTagCompound.setInteger("y", y);
            stack.stackTagCompound.setInteger("z", z);

            tile.readFromNBT(stack.stackTagCompound);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        if (!world.isRemote)
        {
            player.openGui(PECore.instance, Constants.ALCH_TANK_GUI, world, x, y, z);
        }

        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        switch (side) {
            case 0: // Top
            case 1: // Bottom
                return textureTop;
            default:
                return textureSide;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        textureSide = par1IconRegister.registerIcon("projecte:tank_side");
        textureTop = par1IconRegister.registerIcon("projecte:tank_top");
    }
}