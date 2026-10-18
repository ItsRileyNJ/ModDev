package dev.riley.wildlands.block;

import dev.riley.wildlands.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.Vec3;

/** A real lava fluid source, deliberately without a fluid-spreading tick. */
public final class StillLava extends LavaFluid.Source {
  @Override
  public FluidState getFlowing(int amount, boolean falling) {
    return getSource(false);
  }

  @Override
  public Fluid getSource() {
    return this;
  }

  @Override
  public Fluid getFlowing() {
    return this;
  }

  @Override
  public boolean isSame(Fluid fluid) {
    return fluid == this;
  }

  @Override
  public void tick(Level level, BlockPos pos, FluidState state) {}

  @Override
  public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
    return Vec3.ZERO;
  }

  @Override
  public BlockState createLegacyBlock(FluidState state) {
    return Content.STILL_LAVA.get().defaultBlockState();
  }

  @Override
  public net.neoforged.neoforge.fluids.FluidType getFluidType() {
    return net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value();
  }
}
