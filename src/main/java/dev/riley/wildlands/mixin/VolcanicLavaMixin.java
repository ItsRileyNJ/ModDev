package dev.riley.wildlands.mixin;

import dev.riley.wildlands.*;
import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fixed biome rule also covers player-placed vanilla lava and lava arriving from another biome. */
@Mixin(FlowingFluid.class)
public abstract class VolcanicLavaMixin {
  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void wildlands$sourceOnly(Level level, BlockPos pos, FluidState state, CallbackInfo ci) {
    if (state.is(FluidTags.LAVA) && level.getBiome(pos).is(Wildlands.id("volcanic_badlands"))) {
      level.setBlockAndUpdate(pos, Content.STILL_LAVA.get().defaultBlockState());
      ci.cancel();
    }
  }

  @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
  private void wildlands$noStreams(
      LevelAccessor level,
      BlockPos pos,
      BlockState before,
      Direction direction,
      FluidState state,
      CallbackInfo ci) {
    if (state.is(FluidTags.LAVA) && level.getBiome(pos).is(Wildlands.id("volcanic_badlands"))) {
      level.setBlock(pos, Content.STILL_LAVA.get().defaultBlockState(), 3);
      ci.cancel();
    }
  }
}
