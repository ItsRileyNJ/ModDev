package dev.riley.wildlands.item;

import dev.riley.wildlands.*;
import dev.riley.wildlands.gameplay.SlimeTraps;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class SlimeJarItem extends Item {
  public SlimeJarItem(Properties p) {
    super(p);
  }

  @Override
  public InteractionResult useOn(UseOnContext ctx) {
    var player = ctx.getPlayer();
    var pos = ctx.getClickedPos();
    var level = ctx.getLevel();
    var stack = ctx.getItemInHand();
    if (player == null
        || ctx.getClickedFace() != Direction.UP
        || !level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)
        || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
        || !level.mayInteract(player, pos)
        || !player.mayUseItemAt(pos, Direction.UP, stack)) return InteractionResult.FAIL;
    if (level.isClientSide) return InteractionResult.SUCCESS;
    var snapshot =
        net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos);
    if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS
        .post(
            new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(
                snapshot, level.getBlockState(pos), player))
        .isCanceled()) return InteractionResult.FAIL;
    if (!SlimeTraps.get((ServerLevel) level).place((ServerLevel) level, pos))
      return InteractionResult.FAIL;
    player.setItemInHand(
        ctx.getHand(),
        ItemUtils.createFilledResult(stack, player, new ItemStack(Content.EMPTY_JAR.get()), false));
    return InteractionResult.CONSUME;
  }
}
