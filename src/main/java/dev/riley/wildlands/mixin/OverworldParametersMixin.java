package dev.riley.wildlands.mixin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riley.wildlands.Wildlands;
import dev.riley.wildlands.world.*;
import java.util.ArrayList;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.biome.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiNoiseBiomeSourceParameterList.class)
public abstract class OverworldParametersMixin implements OverworldRegions.RegistryAccess {
  @Shadow @Final @Mutable public static Codec<MultiNoiseBiomeSourceParameterList> DIRECT_CODEC;
  @Shadow @Final @Mutable public static Codec<Holder<MultiNoiseBiomeSourceParameterList>> CODEC;
  @Unique private OverworldRegions wildlands$overlay;

  // Resolve mod holders only during datapack decoding. Vanilla's bootstrap lookup
  // intentionally has no mod biomes and must not acquire dangling references.
  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void wildlands$codec(CallbackInfo ci) {
    DIRECT_CODEC =
        RecordCodecBuilder.create(
            i ->
                i.group(
                        MultiNoiseBiomeSourceParameterList.Preset.CODEC
                            .fieldOf("preset")
                            .forGetter(p -> ((OverworldParametersMixin) (Object) p).preset),
                        RegistryOps.<Biome, MultiNoiseBiomeSourceParameterList>retrieveGetter(
                            Registries.BIOME))
                    .apply(
                        i,
                        (preset, biomes) -> {
                          var result = new MultiNoiseBiomeSourceParameterList(preset, biomes);
                          if (preset.equals(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD))
                            ((OverworldRegions.RegistryAccess) result).wildlands$configure(biomes);
                          return result;
                        }));
    CODEC =
        RegistryFileCodec.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, DIRECT_CODEC);
  }

  @Shadow @Final private MultiNoiseBiomeSourceParameterList.Preset preset;

  @Override
  public void wildlands$configure(HolderGetter<Biome> biomes) {
    var holders = new ArrayList<Holder<Biome>>();
    for (String name : BiomeLayout.NAMES)
      holders.add(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Wildlands.id(name))));
    holders.add(biomes.getOrThrow(Biomes.PLAINS));
    wildlands$overlay = new OverworldRegions(holders);
  }

  @Override
  public OverworldRegions wildlands$regions() {
    return wildlands$overlay;
  }
}
