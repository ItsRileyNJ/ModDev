package dev.riley.wildlands.block;

import dev.riley.wildlands.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RootFlowerBlock extends BushBlock {
  public static final com.mojang.serialization.MapCodec<RootFlowerBlock> CODEC =
      simpleCodec(RootFlowerBlock::new);

  @Override
  protected com.mojang.serialization.MapCodec<RootFlowerBlock> codec() {
    return CODEC;
  }

  public RootFlowerBlock(Properties p) {
    super(p);
  }

  @Override
  protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    if (!(entity instanceof LivingEntity)
        || (entity instanceof Player
            ? !Settings.FLOWER_PLAYERS.get()
            : !Settings.FLOWER_MOBS.get())) return;
    if (Settings.FLOWER_SLOW.get()) entity.makeStuckInBlock(state, new Vec3(0.8, 0.8, 0.8));
    if (!level.isClientSide
        && entity.tickCount % 10 == 0
        && (Math.abs(entity.getX() - entity.xOld) > .003
            || Math.abs(entity.getZ() - entity.zOld) > .003))
      entity.hurt(
          level.damageSources().sweetBerryBush(), Settings.FLOWER_DAMAGE.get().floatValue());
  }
}
