package com.legendfps;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Lightweight, original voxel cosmetics. Geometry is assembled from vanilla
 * block models so it stays small and needs no external texture pack.
 */
public final class CosmeticModelRenderer {
    private CosmeticModelRenderer() {}

    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (EntityType<? extends LivingEntity> type,
                 net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> renderer,
                 LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helper,
                 net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) -> {
                    if (renderer instanceof AvatarRenderer<?> avatarRenderer) {
                        helper.register(new Layer(avatarRenderer));
                    }
                });
    }

    private static final class Layer extends RenderLayer<AvatarRenderState, PlayerModel> {
        Layer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector queue, int light,
                           AvatarRenderState state, float limbAngle, float limbDistance) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || state.id != mc.player.getId() || state.isInvisibleToPlayer) return;

            Cosmetic wing = Cosmetic.equipped(Cosmetic.Category.WING);
            Cosmetic cape = Cosmetic.equipped(Cosmetic.Category.CAPE);
            Cosmetic pet = Cosmetic.equipped(Cosmetic.Category.PETS);
            Cosmetic[] extras = new Cosmetic[] {
                Cosmetic.equipped(Cosmetic.Category.HAT), Cosmetic.equipped(Cosmetic.Category.HAIR),
                Cosmetic.equipped(Cosmetic.Category.FACE), Cosmetic.equipped(Cosmetic.Category.EARS),
                Cosmetic.equipped(Cosmetic.Category.SHOULDERS), Cosmetic.equipped(Cosmetic.Category.ARMS),
                Cosmetic.equipped(Cosmetic.Category.BACK), Cosmetic.equipped(Cosmetic.Category.TOPS),
                Cosmetic.equipped(Cosmetic.Category.PANTS), Cosmetic.equipped(Cosmetic.Category.OUTFITS),
                Cosmetic.equipped(Cosmetic.Category.SHOES), Cosmetic.equipped(Cosmetic.Category.OUTERWEAR),
                Cosmetic.equipped(Cosmetic.Category.HALO)
            };
            boolean hasExtra = false;
            for (Cosmetic extra : extras) if (extra != null) { hasExtra = true; break; }
            if (wing == null && cape == null && pet == null && !hasExtra) return;

            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            if (wing != null) drawWings(pose, queue, light, wing, state);
            if (cape != null) drawCape(pose, queue, light, cape, state);
            if (pet != null) drawPet(pose, queue, light, pet, state);
            for (Cosmetic extra : extras) {
                if (extra == null) continue;
                if (extra.category() == Cosmetic.Category.HAT || extra.category() == Cosmetic.Category.HAIR
                        || extra.category() == Cosmetic.Category.FACE || extra.category() == Cosmetic.Category.EARS) continue;
                drawAccessory(pose, queue, light, extra, state);
            }
            pose.popPose();

            Cosmetic[] headItems = new Cosmetic[] {
                Cosmetic.equipped(Cosmetic.Category.HAT), Cosmetic.equipped(Cosmetic.Category.HAIR),
                Cosmetic.equipped(Cosmetic.Category.FACE), Cosmetic.equipped(Cosmetic.Category.EARS)
            };
            for (Cosmetic headItem : headItems) {
                if (headItem == null) continue;
                pose.pushPose();
                getParentModel().head.translateAndRotate(pose);
                pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                drawHeadAccessory(pose, queue, light, headItem);
                pose.popPose();
            }
        }

        private static void drawWings(PoseStack pose, SubmitNodeCollector queue, int light,
                                      Cosmetic item, AvatarRenderState state) {
            BlockState main = wingBlock(item.id());
            BlockState edge = item.id().contains("frost") ? Blocks.PACKED_ICE.defaultBlockState()
                    : item.id().contains("void") || item.id().contains("shadow") ? Blocks.PURPLE_CONCRETE.defaultBlockState()
                    : item.id().contains("forest") ? Blocks.GREEN_CONCRETE.defaultBlockState()
                    : item.id().contains("soul") ? Blocks.CYAN_CONCRETE.defaultBlockState()
                    : Blocks.RED_CONCRETE.defaultBlockState();
            float size = item.id().endsWith("_small") ? 0.30F
                    : item.id().endsWith("_medium") ? 0.50F
                    : item.id().endsWith("_large") ? 0.70F : 1.0F;
            float flap = (float)Math.sin(state.walkAnimationPos * 0.22F) * Math.min(10.0F, state.walkAnimationSpeed * 12.0F);
            for (int side : new int[]{-1, 1}) {
                pose.pushPose();
                pose.translate(side * 3.0F, -1.0F, 2.1F);
                pose.mulPose(Axis.ZP.rotationDegrees(side * flap));
                pose.scale(size, size, size);
                // Feathered stepped silhouette, mirrored on both sides.
                block(pose, queue, main, light, side * 1.0F, 0, 0);
                block(pose, queue, edge, light, side * 2.0F, -1, 0);
                block(pose, queue, main, light, side * 3.0F, -2, 0);
                block(pose, queue, edge, light, side * 4.0F, -3, 0);
                block(pose, queue, main, light, side * 4.0F, -4, 0);
                block(pose, queue, main, light, side * 3.0F, -5, 0);
                block(pose, queue, edge, light, side * 2.0F, -6, 0);
                block(pose, queue, main, light, side * 1.0F, -7, 0);
                block(pose, queue, edge, light, side * 0.0F, -8, 0);
                block(pose, queue, main, light, side * 2.0F, -3, -1);
                block(pose, queue, main, light, side * 3.0F, -4, -1);
                block(pose, queue, edge, light, side * 2.0F, -5, -1);
                pose.popPose();
            }
        }

        private static void drawCape(PoseStack pose, SubmitNodeCollector queue, int light,
                                     Cosmetic item, AvatarRenderState state) {
            BlockState cloth = capeBlock(item.id());
            BlockState trim = item.id().contains("crimson") || item.id().contains("inferno")
                    ? Blocks.RED_CONCRETE.defaultBlockState() : Blocks.GOLD_BLOCK.defaultBlockState();
            float sway = (float)Math.sin(state.walkAnimationPos * 0.12F) * state.walkAnimationSpeed * 1.8F;
            pose.pushPose();
            pose.translate(0, 1.0F, 2.45F);
            pose.mulPose(Axis.XP.rotationDegrees(7.0F + sway));
            for (int row = 0; row < 8; row++) {
                int width = row < 2 ? 2 : 3;
                for (int col = 0; col < width; col++) {
                    float x = (col - (width - 1) / 2.0F) * 1.0F;
                    block(pose, queue, cloth, light, x, row, 0);
                }
                if (row == 7) {
                    block(pose, queue, trim, light, -1, row, -0.1F);
                    block(pose, queue, trim, light, 1, row, -0.1F);
                }
            }
            pose.popPose();
        }

        private static void drawPet(PoseStack pose, SubmitNodeCollector queue, int light,
                                    Cosmetic pet, AvatarRenderState state) {
            String id = pet.id();
            boolean shoulder = id.contains("dragon") || id.contains("fox") || id.contains("phantom");
            pose.pushPose();
            if (shoulder) {
                pose.translate(id.contains("fox") ? 5.0F : -5.0F, -5.0F, 1.0F);
            } else {
                pose.translate(0.0F, -10.0F + (float)Math.sin(state.ageInTicks * 0.08F) * 0.7F, 0.0F);
            }
            BlockState body = id.contains("bee") ? Blocks.YELLOW_CONCRETE.defaultBlockState()
                    : id.contains("slime") ? Blocks.LIME_CONCRETE.defaultBlockState()
                    : id.contains("phantom") ? Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
                    : id.contains("fox") ? Blocks.ORANGE_CONCRETE.defaultBlockState()
                    : Blocks.RED_CONCRETE.defaultBlockState();
            BlockState detail = id.contains("bee") || id.contains("dragon") ? Blocks.BLACK_CONCRETE.defaultBlockState()
                    : id.contains("fox") ? Blocks.WHITE_CONCRETE.defaultBlockState()
                    : Blocks.PURPLE_CONCRETE.defaultBlockState();
            block(pose, queue, body, light, 0, 0, 0);
            block(pose, queue, body, light, 0, -1, 0);
            block(pose, queue, body, light, 0, -1, -1);
            block(pose, queue, detail, light, -0.5F, -1, -1.6F);
            block(pose, queue, detail, light, 0.5F, -1, -1.6F);
            if (id.contains("bee")) {
                block(pose, queue, Blocks.WHITE_CONCRETE.defaultBlockState(), light, -1, 0, 0);
                block(pose, queue, Blocks.WHITE_CONCRETE.defaultBlockState(), light, 1, 0, 0);
                block(pose, queue, detail, light, 0, 0, -1);
            } else if (id.contains("fox")) {
                block(pose, queue, body, light, -1, -2, 0);
                block(pose, queue, body, light, 1, -2, 0);
                block(pose, queue, Blocks.WHITE_CONCRETE.defaultBlockState(), light, 0, 0, -1);
                block(pose, queue, Blocks.ORANGE_CONCRETE.defaultBlockState(), light, 0, 1, 1);
            } else if (id.contains("dragon")) {
                block(pose, queue, detail, light, -1, 0, 0);
                block(pose, queue, detail, light, 1, 0, 0);
                block(pose, queue, body, light, -2, 0, 0);
                block(pose, queue, body, light, 2, 0, 0);
                block(pose, queue, detail, light, 0, -2, 1);
            } else if (id.contains("phantom")) {
                block(pose, queue, body, light, -2, 0, 0);
                block(pose, queue, body, light, 2, 0, 0);
                block(pose, queue, detail, light, -1, 1, 0);
                block(pose, queue, detail, light, 1, 1, 0);
            } else {
                block(pose, queue, detail, light, -1, -1, 0);
                block(pose, queue, detail, light, 1, -1, 0);
            }
            pose.popPose();
        }

        /** Small voxel accessories positioned relative to the player's torso. */
        private static void drawAccessory(PoseStack pose, SubmitNodeCollector queue, int light,
                                          Cosmetic item, AvatarRenderState state) {
            String id = item.id();
            Cosmetic.Category category = item.category();
            BlockState primary = accessoryBlock(id);
            BlockState accent = id.contains("gold") || id.contains("royal") ? Blocks.GOLD_BLOCK.defaultBlockState()
                    : id.contains("frost") || id.contains("ice") ? Blocks.PACKED_ICE.defaultBlockState()
                    : id.contains("cyber") || id.contains("neon") ? Blocks.CYAN_CONCRETE.defaultBlockState()
                    : Blocks.BLACK_CONCRETE.defaultBlockState();
            pose.pushPose();
            switch (category) {
                case HAT, HAIR, FACE, EARS -> { /* rendered on the moving head part */ }
                case SHOULDERS -> {
                    block(pose,queue,primary,light,-5,-1,0);
                    block(pose,queue,primary,light,5,-1,0);
                    block(pose,queue,accent,light,-5,-2,0);
                    block(pose,queue,accent,light,5,-2,0);
                }
                case ARMS -> {
                    for (int y=-1;y<=3;y++) {
                        block(pose,queue,primary,light,-6,y,0);
                        block(pose,queue,primary,light,6,y,0);
                    }
                }
                case BACK -> {
                    for (int y=-2;y<=2;y++) {
                        block(pose,queue,primary,light,-2,y,3);
                        block(pose,queue,primary,light,2,y,3);
                    }
                    block(pose,queue,accent,light,0,0,3);
                }
                case TOPS, OUTFITS, OUTERWEAR -> {
                    for (int y=-1;y<=4;y++) for (int x=-3;x<=3;x++) {
                        if (x==-3 || x==3 || y==4 || y==-1 || category != Cosmetic.Category.TOPS)
                            block(pose,queue,primary,light,x,y,2);
                    }
                    if (category == Cosmetic.Category.OUTERWEAR || category == Cosmetic.Category.OUTFITS)
                        block(pose,queue,accent,light,0,1,3);
                }
                case PANTS -> {
                    for (int y=5;y<=8;y++) {
                        for (int x=-2;x<=2;x++) if (x==-2 || x==2 || y==8)
                            block(pose,queue,primary,light,x,y,1);
                    }
                }
                case SHOES -> {
                    for (int x=-3;x<=-1;x++) block(pose,queue,primary,light,x,9,1);
                    for (int x=1;x<=3;x++) block(pose,queue,primary,light,x,9,1);
                    block(pose,queue,accent,light,-2,9,0);
                    block(pose,queue,accent,light,2,9,0);
                }
                case HALO -> {
                    for (int i=0;i<12;i++) {
                        double a=i*Math.PI/6.0;
                        float bob=(float)Math.sin(state.ageInTicks*0.08F)*0.35F;
                        block(pose,queue,primary,light,(float)Math.cos(a)*4.0F,-11.0F+bob,(float)Math.sin(a)*2.0F);
                    }
                }
                default -> { }
            }
            pose.popPose();
        }

        /** Head cosmetics follow head rotation instead of floating from the torso. */
        private static void drawHeadAccessory(PoseStack pose, SubmitNodeCollector queue, int light, Cosmetic item) {
            String id = item.id();
            BlockState primary = accessoryBlock(id);
            BlockState accent = id.contains("gold") || id.contains("royal") ? Blocks.YELLOW_CONCRETE.defaultBlockState()
                    : id.contains("frost") || id.contains("ice") ? Blocks.PACKED_ICE.defaultBlockState()
                    : Blocks.BLACK_CONCRETE.defaultBlockState();
            switch (item.category()) {
                case HAT -> {
                    for (int x=-4;x<=4;x++) block(pose,queue,primary,light,x,-4,0);
                    for (int x=-3;x<=3;x++) block(pose,queue,primary,light,x,-5,0);
                    block(pose,queue,accent,light,-2,-5,0);
                    block(pose,queue,accent,light,2,-5,0);
                }
                case HAIR -> {
                    for (int x=-4;x<=4;x++) block(pose,queue,primary,light,x,-3,1);
                    for (int x=-3;x<=3;x++) block(pose,queue,primary,light,x,-4,1);
                    block(pose,queue,primary,light,-3,-2,-3);
                    block(pose,queue,primary,light,3,-2,-3);
                }
                case FACE -> {
                    for (int x=-3;x<=3;x++) block(pose,queue,primary,light,x,-1,-4);
                    block(pose,queue,accent,light,-3,-1,-4);
                    block(pose,queue,accent,light,3,-1,-4);
                }
                case EARS -> {
                    block(pose,queue,primary,light,-4,-2,0);
                    block(pose,queue,primary,light,4,-2,0);
                    block(pose,queue,accent,light,-4,-3,0);
                    block(pose,queue,accent,light,4,-3,0);
                }
                default -> { }
            }
        }

        private static BlockState accessoryBlock(String id) {
            if (id.contains("frost") || id.contains("ice")) return Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
            if (id.contains("gold") || id.contains("royal")) return Blocks.YELLOW_CONCRETE.defaultBlockState();
            if (id.contains("green") || id.contains("forest") || id.contains("emerald")) return Blocks.GREEN_CONCRETE.defaultBlockState();
            if (id.contains("void") || id.contains("shadow") || id.contains("phantom")) return Blocks.PURPLE_CONCRETE.defaultBlockState();
            if (id.contains("cyber") || id.contains("neon")) return Blocks.CYAN_CONCRETE.defaultBlockState();
            if (id.contains("pink") || id.contains("sakura") || id.contains("heart")) return Blocks.PINK_CONCRETE.defaultBlockState();
            return Blocks.RED_CONCRETE.defaultBlockState();
        }

        private static void block(PoseStack pose, SubmitNodeCollector queue, BlockState block,
                                  int light, float x, float y, float z) {
            pose.pushPose();
            pose.translate(x, y, z);
            queue.submitBlock(pose, block, light, 0, 0);
            pose.popPose();
        }

        private static BlockState wingBlock(String id) {
            if (id.contains("frost")) return Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
            if (id.contains("void") || id.contains("shadow")) return Blocks.PURPLE_CONCRETE.defaultBlockState();
            if (id.contains("forest")) return Blocks.LIME_CONCRETE.defaultBlockState();
            if (id.contains("soul")) return Blocks.CYAN_CONCRETE.defaultBlockState();
            if (id.contains("storm")) return Blocks.BLUE_CONCRETE.defaultBlockState();
            if (id.contains("sakura")) return Blocks.PINK_CONCRETE.defaultBlockState();
            if (id.contains("arcane")) return Blocks.MAGENTA_CONCRETE.defaultBlockState();
            return Blocks.RED_CONCRETE.defaultBlockState();
        }

        private static BlockState capeBlock(String id) {
            if (id.contains("ocean") || id.contains("glacier")) return Blocks.BLUE_CONCRETE.defaultBlockState();
            if (id.contains("forest") || id.contains("emerald")) return Blocks.GREEN_CONCRETE.defaultBlockState();
            if (id.contains("void") || id.contains("shadow")) return Blocks.PURPLE_CONCRETE.defaultBlockState();
            if (id.contains("sakura") || id.contains("aurora")) return Blocks.PINK_CONCRETE.defaultBlockState();
            return Blocks.RED_CONCRETE.defaultBlockState();
        }
    }
}
