package dev.riley.wildlands.item;

import dev.riley.wildlands.entity.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class SlugCarrierItem extends Item {
  public SlugCarrierItem(Properties p) {
    super(p);
  }

  @Override
  public InteractionResult useOn(UseOnContext ctx) {
    var player = ctx.getPlayer();
    var level = ctx.getLevel();
    var stack = ctx.getItemInHand();
    if (player == null) return InteractionResult.FAIL;
    var data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null) return InteractionResult.FAIL;
    var tag = data.copyTag();
    if (!tag.hasUUID("Owner") || !tag.getUUID("Owner").equals(player.getUUID()))
      return InteractionResult.FAIL;
    var pos = ctx.getClickedPos().relative(ctx.getClickedFace());
    if (!player.mayUseItemAt(pos, ctx.getClickedFace(), stack) || !level.mayInteract(player, pos))
      return InteractionResult.FAIL;
    if (level.isClientSide) return InteractionResult.SUCCESS;
    BananaSlug slug = Creatures.SLUG.get().create(level);
    if (slug == null) return InteractionResult.FAIL;
    slug.readAdditionalSaveData(tag);
    slug.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, player.getYRot(), 0);
    if (!level.noCollision(slug, slug.getBoundingBox())) return InteractionResult.FAIL;
    if (!level.addFreshEntity(slug)) return InteractionResult.FAIL;
    // Consume even in creative: duplicating a stored living entity is not a normal placement
    // operation.
    stack.shrink(1);
    return InteractionResult.CONSUME;
  }
}
