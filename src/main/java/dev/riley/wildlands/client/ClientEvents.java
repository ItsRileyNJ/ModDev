package dev.riley.wildlands.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.riley.wildlands.*;
import dev.riley.wildlands.entity.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = Wildlands.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
  public static final String[] MODELS = {
    "root_worm", "banana_slug", "flytrap", "flytrap_digesting", "depth_stalker"
  };

  @SubscribeEvent
  public static void models(ModelEvent.RegisterAdditional e) {
    for (String id : MODELS)
      e.register(ModelResourceLocation.standalone(Wildlands.id("entity/" + id)));
  }

  @SubscribeEvent
  public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
    e.registerEntityRenderer(
        Creatures.WORM.get(), ctx -> new ResourceCreatureRenderer<>(ctx, "root_worm", 1));
    e.registerEntityRenderer(
        Creatures.SLUG.get(), ctx -> new ResourceCreatureRenderer<>(ctx, "banana_slug", 1));
    e.registerEntityRenderer(
        Creatures.FLYTRAP.get(), ctx -> new ResourceCreatureRenderer<>(ctx, "flytrap", 2.2f));
    e.registerEntityRenderer(
        Creatures.STALKER.get(), ctx -> new ResourceCreatureRenderer<>(ctx, "depth_stalker", 6));
    ItemBlockRenderTypes.setRenderLayer(Content.LAVA.get(), RenderType.translucent());
  }

  static final class ResourceCreatureRenderer<T extends Entity> extends EntityRenderer<T> {
    private final String model;
    private final float scale;

    ResourceCreatureRenderer(EntityRendererProvider.Context ctx, String model, float scale) {
      super(ctx);
      this.model = model;
      this.scale = scale;
      shadowRadius = .25f;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
      return Wildlands.id("textures/entity/" + model + ".png");
    }

    @Override
    public void render(
        T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      pose.pushPose();
      float size = entity instanceof BananaSlug slug ? slug.size() : 1;
      pose.scale(scale * size, scale * size, scale * size);
      if (entity instanceof BananaSlug slug) {
        switch (slug.face()) {
          case UP -> {
            pose.translate(0, .18, 0);
            pose.mulPose(Axis.XP.rotationDegrees(180));
          }
          case EAST -> pose.mulPose(Axis.ZP.rotationDegrees(-90));
          case WEST -> pose.mulPose(Axis.ZP.rotationDegrees(90));
          case NORTH -> pose.mulPose(Axis.XP.rotationDegrees(90));
          case SOUTH -> pose.mulPose(Axis.XP.rotationDegrees(-90));
          default -> {}
        }
      }
      pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
      if (entity instanceof RootWorm worm && worm.grazing())
        pose.scale(1, .85f + .08f * (float) Math.sin((entity.tickCount + partial) * .8), 1);
      pose.translate(-.5, 0, -.5);
      String path = entity instanceof Flytrap f && f.digesting() ? "flytrap_digesting" : model;
      var baked =
          Minecraft.getInstance()
              .getModelManager()
              .getModel(ModelResourceLocation.standalone(Wildlands.id("entity/" + path)));
      Minecraft.getInstance()
          .getBlockRenderer()
          .getModelRenderer()
          .renderModel(
              pose.last(),
              buffers.getBuffer(
                  RenderType.entityCutoutNoCull(
                      net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
              Blocks.STONE.defaultBlockState(),
              baked,
              1,
              1,
              1,
              light,
              net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
      pose.popPose();
      super.render(entity, yaw, partial, pose, buffers, light);
    }
  }

  @EventBusSubscriber(modid = Wildlands.ID, value = Dist.CLIENT)
  public static final class Fog {
    private static boolean depths(ViewportEvent e) {
      var camera = e.getCamera();
      return camera.getFluidInCamera() == FogType.WATER
          && camera
              .getEntity()
              .level()
              .getBiome(camera.getBlockPosition())
              .is(Wildlands.id("depths"));
    }

    @SubscribeEvent
    public static void distance(ViewportEvent.RenderFog e) {
      if (depths(e)) {
        e.setNearPlaneDistance(0);
        e.setFarPlaneDistance(5);
        e.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
        e.setCanceled(true);
      }
    }

    @SubscribeEvent
    public static void color(ViewportEvent.ComputeFogColor e) {
      if (depths(e)) {
        e.setRed(.006f);
        e.setGreen(.012f);
        e.setBlue(.02f);
      }
    }

    @SubscribeEvent
    public static void ash(net.neoforged.neoforge.client.event.ClientTickEvent.Post e) {
      var mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) return;
      if (mc.level.getBiome(mc.player.blockPosition()).is(Wildlands.id("volcanic_badlands"))
          && mc.level.random.nextDouble() < (Settings.SPEC.isLoaded() ? Settings.ASH.get() : .12)) {
        var r = mc.level.random;
        mc.level.addParticle(
            net.minecraft.core.particles.ParticleTypes.WHITE_ASH,
            mc.player.getX() + r.nextInt(20) - 10,
            mc.player.getY() + r.nextInt(10),
            mc.player.getZ() + r.nextInt(20) - 10,
            0,
            -.01,
            0);
      }
    }
  }
}
