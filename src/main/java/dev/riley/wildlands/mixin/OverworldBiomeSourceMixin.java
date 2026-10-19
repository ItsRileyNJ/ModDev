package dev.riley.wildlands.mixin;

import com.mojang.datafixers.util.Either;
import dev.riley.wildlands.world.OverworldRegions;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class OverworldBiomeSourceMixin implements OverworldRegions.Access {
  @Shadow @Final
  private Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>>
      parameters;

  @Override
  public OverworldRegions wildlands$regions() {
    return parameters
        .right()
        .map(h -> ((OverworldRegions.Access) h.value()).wildlands$regions())
        .orElse(null);
  }

  @Inject(
      method =
          "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
      at = @At("HEAD"),
      cancellable = true)
  private void wildlands$sample(
      int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> ci) {
    var regions = wildlands$regions();
    if (regions != null && regions.custom(x, z))
      ci.setReturnValue(regions.source().getNoiseBiome(x, y, z, sampler));
  }

  @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
  private void wildlands$possible(CallbackInfoReturnable<Stream<Holder<Biome>>> ci) {
    var regions = wildlands$regions();
    if (regions != null)
      ci.setReturnValue(Stream.concat(ci.getReturnValue(), regions.biomes().stream()));
  }
}
