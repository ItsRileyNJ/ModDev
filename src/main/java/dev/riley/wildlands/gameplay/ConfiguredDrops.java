package dev.riley.wildlands.gameplay;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riley.wildlands.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.*;

public final class ConfiguredDrops extends LootModifier {
  public static final MapCodec<ConfiguredDrops> CODEC =
      RecordCodecBuilder.mapCodec(i -> codecStart(i).apply(i, ConfiguredDrops::new));

  public ConfiguredDrops(LootItemCondition[] conditions) {
    super(conditions);
  }

  @Override
  public MapCodec<? extends IGlobalLootModifier> codec() {
    return CODEC;
  }

  @Override
  protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
    var state = ctx.getParamOrNull(LootContextParams.BLOCK_STATE);
    if (state == null) return loot;
    if (state.is(Content.BURNT_WOOD.get()) && loot.removeIf(s -> s.is(Items.CHARCOAL))) {
      int lo = Settings.CHARCOAL_MIN.get(), hi = Math.max(lo, Settings.CHARCOAL_MAX.get());
      int n = lo + ctx.getRandom().nextInt(hi - lo + 1);
      if (n > 0) loot.add(new ItemStack(Items.CHARCOAL, n));
    }
    if (state.is(Content.SPIRE_MATERIAL.get()) && loot.removeIf(s -> s.is(Items.GUNPOWDER))) {
      int max = Settings.GUNPOWDER_MAX.get();
      if (max > 0 && ctx.getRandom().nextDouble() < Settings.GUNPOWDER_CHANCE.get())
        loot.add(new ItemStack(Items.GUNPOWDER, 1 + ctx.getRandom().nextInt(max)));
    }
    return loot;
  }
}
