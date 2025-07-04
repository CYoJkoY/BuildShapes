package com.cyojkoy.buildshapes;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Set;

import org.lwjgl.opengl.GL11;

@Mod(modid = BuildShapes.MODID, name = BuildShapes.NAME, version = BuildShapes.VERSION, clientSideOnly = true)
public class BuildShapes {
    public static final String MODID = "buildshapes";
    public static final String NAME = "Build Shapes";
    public static final String VERSION = "1.0.0";
    // 添加三角函数查找表
    private static final int LOOKUP_PRECISION = 1000;
    private static final double[] sinTable = new double[LOOKUP_PRECISION + 1];
    private static final double[] sqrtTable = new double[LOOKUP_PRECISION + 1];

    static {
        // 初始化三角函数查找表
        for (int i = 0; i <= LOOKUP_PRECISION; i++) {
            double x = i * Math.PI / (2 * LOOKUP_PRECISION);
            sinTable[i] = Math.sin(x);
        }
        
        // 初始化平方根查找表
        for (int i = 0; i <= LOOKUP_PRECISION; i++) {
            sqrtTable[i] = Math.sqrt(i / (double)LOOKUP_PRECISION);
        }
    }
    
    // 添加方向枚举
    private enum Orientation {
        HORIZONTAL, // XZ平面
        VERTICAL_X, // XY平面
        VERTICAL_Z  // ZY平面
    }

    // 增加显示模式的枚举
    private enum DisplayMode {
        ALL,           // 显示所有层
        CURRENT_LAYER, // 显示当前层
        BELOW_LAYER    // 显示下方一层
    }

    private static DisplayMode currentDisplayMode = DisplayMode.ALL;
    private static Set<BlockPos> previewBlocks = new HashSet<>();
    private static BlockPos centerPos;
    private static Orientation currentOrientation = Orientation.HORIZONTAL;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new ShapeCommand());
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (previewBlocks.isEmpty() || centerPos == null) return;

        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
        World world = player.world;
        if (player == null || world == null) return;

        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

        // 创建视锥体检查器
        net.minecraft.client.renderer.culling.Frustum frustum = new net.minecraft.client.renderer.culling.Frustum();
        frustum.setPosition(playerX, playerY, playerZ);

        // 启用深度测试，这样方块会被正确遮挡
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        
        // 渲染设置
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );

        // 开始使用Tessellator进行渲染
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        // 渲染预览方块
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        for (BlockPos pos : previewBlocks) {
            // 根据显示模式判断是否渲染
        	boolean shouldRender;
        	switch (currentDisplayMode) {
        	    case ALL:
        	        shouldRender = true;
        	        break;
        	    case CURRENT_LAYER:
        	        shouldRender = pos.getY() == player.getPosition().getY();
        	        break;
        	    case BELOW_LAYER:
        	        shouldRender = pos.getY() == player.getPosition().getY() - 1;
        	        break;
        	    default:
        	        shouldRender = true;
        	        break;
        	}
            
            if (shouldRender && world.isAirBlock(pos)) {
                // 计算相对位置
                double x = pos.getX() - playerX;
                double y = pos.getY() - playerY;
                double z = pos.getZ() - playerZ;

                // 检查是否在视野内
                if (frustum.isBoxInFrustum(
                    pos.getX() - 0.1, pos.getY() - 0.1, pos.getZ() - 0.1,
                    pos.getX() + 0.1, pos.getY() + 0.1, pos.getZ() + 0.1)) {
                    
                    // 渲染实心的蓝色小方块
                    // 前面
                    buffer.pos(x + 0.4, y + 0.4, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.4, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.6, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.4, y + 0.6, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();

                    // 后面
                    buffer.pos(x + 0.4, y + 0.4, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.4, y + 0.6, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.6, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.4, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();

                    // 顶面
                    buffer.pos(x + 0.4, y + 0.6, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.4, y + 0.6, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.6, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.6, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();

                    // 底面
                    buffer.pos(x + 0.4, y + 0.4, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.4, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.4, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.4, y + 0.4, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();

                    // 右面
                    buffer.pos(x + 0.6, y + 0.4, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.6, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.6, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.6, y + 0.4, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();

                    // 左面
                    buffer.pos(x + 0.4, y + 0.4, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.4, y + 0.4, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.4, y + 0.6, z + 0.6).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                    buffer.pos(x + 0.4, y + 0.6, z + 0.4).color(0.0f, 0.5f, 1.0f, 0.8f).endVertex();
                }
            }
        }

        // 渲染中心点
        double centerX = centerPos.getX() - playerX;
        double centerY = centerPos.getY() - playerY;
        double centerZ = centerPos.getZ() - playerZ;
        
        if (frustum.isBoxInFrustum(
            centerPos.getX() - 0.05, centerPos.getY() - 0.05, centerPos.getZ() - 0.05,
            centerPos.getX() + 0.05, centerPos.getY() + 0.05, centerPos.getZ() + 0.05)) {
            
            // 渲染实心的红色小方块
            // 前面
            buffer.pos(centerX + 0.45, centerY + 0.45, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.45, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.55, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.45, centerY + 0.55, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();

            // 后面
            buffer.pos(centerX + 0.45, centerY + 0.45, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.45, centerY + 0.55, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.55, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.45, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();

            // 顶面
            buffer.pos(centerX + 0.45, centerY + 0.55, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.45, centerY + 0.55, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.55, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.55, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();

            // 底面
            buffer.pos(centerX + 0.45, centerY + 0.45, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.45, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.45, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.45, centerY + 0.45, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();

            // 右面
            buffer.pos(centerX + 0.55, centerY + 0.45, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.55, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.55, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.55, centerY + 0.45, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();

            // 左面
            buffer.pos(centerX + 0.45, centerY + 0.45, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.45, centerY + 0.45, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.45, centerY + 0.55, centerZ + 0.55).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
            buffer.pos(centerX + 0.45, centerY + 0.55, centerZ + 0.45).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
        }

        // 完成渲染
        tessellator.draw();

        // 恢复GL状态
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private static void generateCircle(BlockPos center, int radius) {
        previewBlocks.clear();
        
        // 使用八分法计算圆形
        int x = 0;
        int y = radius;
        int d = 1 - radius;
        
        switch (currentOrientation) {
            case HORIZONTAL:
                while (y >= x) {
                    addCirclePointsHorizontal(center, x, y);
                    x++;
                    
                    if (d < 0) {
                        d += 2 * x + 1;
                    } else {
                        y--;
                        d += 2 * (x - y) + 1;
                    }
                }
                break;
                
            case VERTICAL_X:
                while (y >= x) {
                    addCirclePointsVerticalX(center, x, y);
                    x++;
                    
                    if (d < 0) {
                        d += 2 * x + 1;
                    } else {
                        y--;
                        d += 2 * (x - y) + 1;
                    }
                }
                break;
                
            case VERTICAL_Z:
                while (y >= x) {
                    addCirclePointsVerticalZ(center, x, y);
                    x++;
                    
                    if (d < 0) {
                        d += 2 * x + 1;
                    } else {
                        y--;
                        d += 2 * (x - y) + 1;
                    }
                }
                break;
        }
        
        centerPos = center;
    }

    private static void addCirclePointsHorizontal(BlockPos center, int x, int y) {
        // XZ平面的八分法
        previewBlocks.add(new BlockPos(center.getX() + x, center.getY(), center.getZ() + y));
        previewBlocks.add(new BlockPos(center.getX() + x, center.getY(), center.getZ() - y));
        previewBlocks.add(new BlockPos(center.getX() - x, center.getY(), center.getZ() + y));
        previewBlocks.add(new BlockPos(center.getX() - x, center.getY(), center.getZ() - y));
        previewBlocks.add(new BlockPos(center.getX() + y, center.getY(), center.getZ() + x));
        previewBlocks.add(new BlockPos(center.getX() + y, center.getY(), center.getZ() - x));
        previewBlocks.add(new BlockPos(center.getX() - y, center.getY(), center.getZ() + x));
        previewBlocks.add(new BlockPos(center.getX() - y, center.getY(), center.getZ() - x));
    }

    private static void addCirclePointsVerticalX(BlockPos center, int x, int y) {
        // XY平面的八分法
        previewBlocks.add(new BlockPos(center.getX() + x, center.getY() + y, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() + x, center.getY() - y, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() - x, center.getY() + y, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() - x, center.getY() - y, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() + y, center.getY() + x, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() + y, center.getY() - x, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() - y, center.getY() + x, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() - y, center.getY() - x, center.getZ()));
    }

    private static void addCirclePointsVerticalZ(BlockPos center, int x, int y) {
        // ZY平面的八分法
        previewBlocks.add(new BlockPos(center.getX(), center.getY() + y, center.getZ() + x));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() + y, center.getZ() - x));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() - y, center.getZ() + x));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() - y, center.getZ() - x));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() + x, center.getZ() + y));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() + x, center.getZ() - y));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() - x, center.getZ() + y));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() - x, center.getZ() - y));
    }

    private static void generateSphere(BlockPos center, int radius) {
        previewBlocks.clear();
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();

        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                for (int k = -radius; k <= radius; k++) {
                    double distance = Math.sqrt(i * i + j * j + k * k);
                    if (distance <= radius && distance > radius - 1) {
                        previewBlocks.add(new BlockPos(x + i, y + j, z + k));
                    }
                }
            }
        }
        centerPos = center;
    }

    // 修改的椭圆生成方法
    private static void generateEllipse(BlockPos center, int radiusX, int radiusZ) {
        previewBlocks.clear();
        
        switch (currentOrientation) {
            case HORIZONTAL:
                generateEllipseHorizontal(center, radiusX, radiusZ);
                break;
            case VERTICAL_X:
                generateEllipseVertical(center, radiusX, radiusZ, true);
                break;
            case VERTICAL_Z:
                generateEllipseVertical(center, radiusX, radiusZ, false);
                break;
        }
        
        centerPos = center;
    }

    private static void generateEllipseHorizontal(BlockPos center, int radiusX, int radiusZ) {
        int x = 0;
        int z = radiusZ;
        long rx2 = radiusX * radiusX;
        long rz2 = radiusZ * radiusZ;
        long twoRx2 = 2 * rx2;
        long twoRz2 = 2 * rz2;
        long p;
        long px = 0;
        long pz = twoRx2 * z;

        // Region 1
        p = Math.round(rz2 - (rx2 * radiusZ) + (0.25 * rx2));
        while (px < pz) {
        	addEllipsePoints(center, x, z, Orientation.HORIZONTAL);
            x++;
            px += twoRz2;
            if (p < 0) {
                p += rz2 + px;
            } else {
                z--;
                pz -= twoRx2;
                p += rz2 + px - pz;
            }
        }

        // Region 2
        p = Math.round(rz2 * (x + 0.5) * (x + 0.5) + rx2 * (z - 1) * (z - 1) - rx2 * rz2);
        while (z >= 0) {
        	addEllipsePoints(center, x, z, Orientation.HORIZONTAL);
            z--;
            pz -= twoRx2;
            if (p > 0) {
                p += rx2 - pz;
            } else {
                x++;
                px += twoRz2;
                p += rx2 - pz + px;
            }
        }
    }

    private static void generateEllipseVertical(BlockPos center, int radiusX, int radiusY, boolean isXPlane) {
        int x = 0;
        int y = radiusY;
        long rx2 = radiusX * radiusX;
        long ry2 = radiusY * radiusY;
        long twoRx2 = 2 * rx2;
        long twoRy2 = 2 * ry2;
        long p;
        long px = 0;
        long py = twoRx2 * y;

        // Region 1
        p = Math.round(ry2 - (rx2 * radiusY) + (0.25 * rx2));
        while (px < py) {
            if (isXPlane) {
            	addEllipsePoints(center, x, y, Orientation.VERTICAL_X);
            } else {
            	addEllipsePoints(center, x, y, Orientation.VERTICAL_Z);
            }
            x++;
            px += twoRy2;
            if (p < 0) {
                p += ry2 + px;
            } else {
                y--;
                py -= twoRx2;
                p += ry2 + px - py;
            }
        }

        // Region 2
        p = Math.round(ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2);
        while (y >= 0) {
            if (isXPlane) {
            	addEllipsePoints(center, x, y, Orientation.VERTICAL_X);
            } else {
            	addEllipsePoints(center, x, y, Orientation.VERTICAL_Z);
            }
            y--;
            py -= twoRx2;
            if (p > 0) {
                p += rx2 - py;
            } else {
                x++;
                px += twoRy2;
                p += rx2 - py + px;
            }
        }
    }

    private static void addEllipsePoints(BlockPos center, int a, int b, Orientation orientation) {
        switch (orientation) {
            case HORIZONTAL:
                previewBlocks.add(new BlockPos(center.getX() + a, center.getY(), center.getZ() + b));
                previewBlocks.add(new BlockPos(center.getX() - a, center.getY(), center.getZ() + b));
                previewBlocks.add(new BlockPos(center.getX() + a, center.getY(), center.getZ() - b));
                previewBlocks.add(new BlockPos(center.getX() - a, center.getY(), center.getZ() - b));
                break;
            case VERTICAL_X:
                previewBlocks.add(new BlockPos(center.getX() + a, center.getY() + b, center.getZ()));
                previewBlocks.add(new BlockPos(center.getX() - a, center.getY() + b, center.getZ()));
                previewBlocks.add(new BlockPos(center.getX() + a, center.getY() - b, center.getZ()));
                previewBlocks.add(new BlockPos(center.getX() - a, center.getY() - b, center.getZ()));
                break;
            case VERTICAL_Z:
                previewBlocks.add(new BlockPos(center.getX(), center.getY() + b, center.getZ() + a));
                previewBlocks.add(new BlockPos(center.getX(), center.getY() + b, center.getZ() - a));
                previewBlocks.add(new BlockPos(center.getX(), center.getY() - b, center.getZ() + a));
                previewBlocks.add(new BlockPos(center.getX(), center.getY() - b, center.getZ() - a));
                break;
        }
    }

    // 优化后的椭球体生成方法
    private static void generateEllipsoid(BlockPos center, int radiusX, int radiusY, int radiusZ) {
    previewBlocks.clear();
    
    // 验证半径范围
    if (radiusX <= 0 || radiusY <= 0 || radiusZ <= 0) {
        return;
    }

    // 使用单线程替代多线程（避免线程安全问题）
    Set<BlockPos> points = new HashSet<>();
    for (int i = -radiusX; i <= radiusX; i++) {
        for (int j = -radiusY; j <= radiusY; j++) {
            for (int k = -radiusZ; k <= radiusZ; k++) {
                // 调整顶点位置计算，使端点缩进一格
                double adjustedI = i + (i > 0 ? -0.5 : (i < 0 ? 0.5 : 0));
                double adjustedJ = j + (j > 0 ? -0.5 : (j < 0 ? 0.5 : 0));
                double adjustedK = k + (k > 0 ? -0.5 : (k < 0 ? 0.5 : 0));
                
                double termX = Math.pow(adjustedI, 2) / Math.pow(radiusX, 2);
                double termY = Math.pow(adjustedJ, 2) / Math.pow(radiusY, 2);
                double termZ = Math.pow(adjustedK, 2) / Math.pow(radiusZ, 2);
                
                if (termX + termY + termZ <= 1.0) {
                    points.add(new BlockPos(
                        center.getX() + i,
                        center.getY() + j,
                        center.getZ() + k
                    ));
                }
            }
        }
    }
    
    // 优化：只保留表面点
    Set<BlockPos> surfacePoints = new HashSet<>();
    for (BlockPos pos : points) {
        boolean isSurface = false;
        
        // 检查六个方向是否有空缺
        if (!points.contains(pos.east())) isSurface = true;
        else if (!points.contains(pos.west())) isSurface = true;
        else if (!points.contains(pos.up())) isSurface = true;
        else if (!points.contains(pos.down())) isSurface = true;
        else if (!points.contains(pos.north())) isSurface = true;
        else if (!points.contains(pos.south())) isSurface = true;
        
        if (isSurface) {
            surfacePoints.add(pos);
        }
    }
    
    previewBlocks.addAll(surfacePoints);
    centerPos = center;
    }
    
    // 新增圆锥生成方法
    private static void generateCone(BlockPos center, int baseRadius, int height, EntityPlayer player) {
        previewBlocks.clear();
        float pitch = player.rotationPitch;
        float yaw = player.rotationYaw;
        
        switch (currentOrientation) {
            case HORIZONTAL: // XZ平面圆锥
                generateHorizontalCone(center, baseRadius, height, pitch);
                break;
            case VERTICAL_X: // XY平面圆锥
                generateVerticalXCone(center, baseRadius, height, yaw);
                break;
            case VERTICAL_Z: // ZY平面圆锥
                generateVerticalZCone(center, baseRadius, height, yaw);
                break;
        }
        centerPos = center;
    }
    
    // 水平方向圆锥生成
    private static void generateHorizontalCone(BlockPos center, int radius, int height, float pitch) {
        Set<BlockPos> baseCircle = new HashSet<>();
        generateHollowCircle(center, radius, baseCircle, Orientation.HORIZONTAL);
        
        // 根据俯仰角确定顶点方向（抬头向上，低头向下）
        int direction = pitch < 0 ? 1 : -1;
        BlockPos apex = new BlockPos(
            center.getX(),
            center.getY() + height * direction,
            center.getZ()
        );
        
        for (BlockPos base : baseCircle) {
            generateLine(base, apex);
        }
        
        fillConeGaps(center);
    }
    
    // 垂直X方向圆锥（XY平面）
    private static void generateVerticalXCone(BlockPos center, int radius, int height, float yaw) {
        Set<BlockPos> baseCircle = new HashSet<>();
        generateHollowCircle(center, radius, baseCircle, Orientation.VERTICAL_X);
        
        // 根据水平角判断Z轴方向（正负）
        int directionZ = 1;
        
        if ((yaw > -90 && yaw < 90)) {
            directionZ = 1;
        } else if ((yaw > 90 && yaw < 180) || 
                  (yaw < -90 && yaw > -180)) {
            directionZ = -1;
        }
        
        BlockPos apex = new BlockPos(
            center.getX(),
            center.getY(),
            center.getZ() + height * directionZ
        );
        
        for (BlockPos base : baseCircle) {
            generateLine(base, apex);
        }
        
        fillConeGaps(center);
    }
    
    // 垂直Z方向圆锥（ZY平面）
    private static void generateVerticalZCone(BlockPos center, int radius, int height, float yaw) {
        Set<BlockPos> baseCircle = new HashSet<>();
        generateHollowCircle(center, radius, baseCircle, Orientation.VERTICAL_Z);
        
        // 根据水平角判断X轴方向（正负）
        int directionX = 1;
        
        if ((yaw > -180 && yaw < 0)) {
        	directionX = 1;
        } else if ((yaw > 0 && yaw < 180)) {
        	directionX = -1;
        }
        
        BlockPos apex = new BlockPos(
            center.getX() + height * directionX,
            center.getY(),
            center.getZ()
        );
        
        for (BlockPos base : baseCircle) {
            generateLine(base, apex);
        }
        
        fillConeGaps(center);
    }
    
    // 优化后的八分法空心圆生成方法
    private static void generateHollowCircle(BlockPos center, int radius, Set<BlockPos> collection, Orientation orientation) {
        int x = 0;
        int y = radius;
        int d = 1 - radius;
        
        while (y >= x) {
            // 根据方向调用对应的八分法添加点
            switch (orientation) {
                case HORIZONTAL:
                    addCirclePointsHorizontal(center, x, y, collection);
                    break;
                case VERTICAL_X:
                    addCirclePointsVerticalX(center, x, y, collection);
                    break;
                case VERTICAL_Z:
                    addCirclePointsVerticalZ(center, x, y, collection);
                    break;
            }
            
            x++;
            if (d < 0) {
                d += 2 * x + 1;
            } else {
                y--;
                d += 2 * (x - y) + 1;
            }
        }
    }
    
 // 修改后的八分法添加点方法（水平方向）
    private static void addCirclePointsHorizontal(BlockPos center, int x, int y, Set<BlockPos> collection) {
        collection.add(new BlockPos(center.getX() + x, center.getY(), center.getZ() + y));
        collection.add(new BlockPos(center.getX() + x, center.getY(), center.getZ() - y));
        collection.add(new BlockPos(center.getX() - x, center.getY(), center.getZ() + y));
        collection.add(new BlockPos(center.getX() - x, center.getY(), center.getZ() - y));
        collection.add(new BlockPos(center.getX() + y, center.getY(), center.getZ() + x));
        collection.add(new BlockPos(center.getX() + y, center.getY(), center.getZ() - x));
        collection.add(new BlockPos(center.getX() - y, center.getY(), center.getZ() + x));
        collection.add(new BlockPos(center.getX() - y, center.getY(), center.getZ() - x));
    }
    
 // 修改后的八分法添加点方法（垂直X方向）
    private static void addCirclePointsVerticalX(BlockPos center, int x, int y, Set<BlockPos> collection) {
        collection.add(new BlockPos(center.getX() + x, center.getY() + y, center.getZ()));
        collection.add(new BlockPos(center.getX() + x, center.getY() - y, center.getZ()));
        collection.add(new BlockPos(center.getX() - x, center.getY() + y, center.getZ()));
        collection.add(new BlockPos(center.getX() - x, center.getY() - y, center.getZ()));
        collection.add(new BlockPos(center.getX() + y, center.getY() + x, center.getZ()));
        collection.add(new BlockPos(center.getX() + y, center.getY() - x, center.getZ()));
        collection.add(new BlockPos(center.getX() - y, center.getY() + x, center.getZ()));
        collection.add(new BlockPos(center.getX() - y, center.getY() - x, center.getZ()));
    }

    // 修改后的八分法添加点方法（垂直Z方向）
    private static void addCirclePointsVerticalZ(BlockPos center, int x, int y, Set<BlockPos> collection) {
        collection.add(new BlockPos(center.getX(), center.getY() + y, center.getZ() + x));
        collection.add(new BlockPos(center.getX(), center.getY() + y, center.getZ() - x));
        collection.add(new BlockPos(center.getX(), center.getY() - y, center.getZ() + x));
        collection.add(new BlockPos(center.getX(), center.getY() - y, center.getZ() - x));
        collection.add(new BlockPos(center.getX(), center.getY() + x, center.getZ() + y));
        collection.add(new BlockPos(center.getX(), center.getY() + x, center.getZ() - y));
        collection.add(new BlockPos(center.getX(), center.getY() - x, center.getZ() + y));
        collection.add(new BlockPos(center.getX(), center.getY() - x, center.getZ() - y));
    }
    
    // 新增圆锥空缺检测方法
    private static void fillConeGaps(BlockPos center) {
        Set<BlockPos> newPoints = new HashSet<>();
        Set<BlockPos> surfacePoints = new HashSet<>();
        Set<BlockPos> originalPoints = new HashSet<>(previewBlocks);
        
        // 获取中心点坐标
        int centerX = center.getX();
        int centerZ = center.getZ();

        for (BlockPos pos : originalPoints) {
            newPoints.add(pos);

            // 根据坐标差判断方向
            boolean eastValid = pos.getX() < centerX;  // 东方向新增点是否朝向中心
            boolean westValid = pos.getX() > centerX;  // 西方向新增点是否朝向中心
            boolean northValid = pos.getZ() > centerZ;  // 北方向新增点是否朝向中心
            boolean southValid = pos.getZ() < centerZ;  // 南方向新增点是否朝向中心
            
            // 只添加朝向中心的新点
            if (eastValid) newPoints.add(pos.east());
            if (westValid) newPoints.add(pos.west());
            if (northValid) newPoints.add(pos.north());
            if (southValid) newPoints.add(pos.south());
        }

        for (BlockPos pos : newPoints) {
            boolean isSurface = false;
            
            // 检查六个方向是否有空缺
            if (!newPoints.contains(pos.east())) isSurface = true;
            else if (!newPoints.contains(pos.west())) isSurface = true;
            else if (!newPoints.contains(pos.up())) isSurface = true;
            else if (!newPoints.contains(pos.down())) isSurface = true;
            else if (!newPoints.contains(pos.north())) isSurface = true;
            else if (!newPoints.contains(pos.south())) isSurface = true;

            if (isSurface) {
                surfacePoints.add(pos);
            }
        }
        
        previewBlocks.clear();
        previewBlocks.addAll(surfacePoints);
    }
    
    // Bresenham三维直线算法
    private static void generateLine(BlockPos start, BlockPos end) {
        int x1 = start.getX(), y1 = start.getY(), z1 = start.getZ();
        int x2 = end.getX(), y2 = end.getY(), z2 = end.getZ();
        
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);
        
        int xs = x2 > x1 ? 1 : -1;
        int ys = y2 > y1 ? 1 : -1;
        int zs = z2 > z1 ? 1 : -1;

        // 主导轴判断逻辑
        if (dx >= dy && dx >= dz) {
            // X轴主导的直线生成逻辑
            int p1 = 2*dy - dx;
            int p2 = 2*dz - dx;
            while (x1 != x2) {
                previewBlocks.add(new BlockPos(x1, y1, z1));
                x1 += xs;
                if (p1 >= 0) { y1 += ys; p1 -= 2*dx; }
                if (p2 >= 0) { z1 += zs; p2 -= 2*dx; }
                p1 += 2*dy;
                p2 += 2*dz;
            }
        } else if (dy >= dx && dy >= dz) {
            // Y轴主导的直线生成逻辑
            int p1 = 2*dx - dy;
            int p2 = 2*dz - dy;
            while (y1 != y2) {
                previewBlocks.add(new BlockPos(x1, y1, z1));
                y1 += ys;
                if (p1 >= 0) { x1 += xs; p1 -= 2*dy; }
                if (p2 >= 0) { z1 += zs; p2 -= 2*dy; }
                p1 += 2*dx;
                p2 += 2*dz;
            }
        } else {
            // Z轴主导的直线生成逻辑
            int p1 = 2*dx - dz;
            int p2 = 2*dy - dz;
            while (z1 != z2) {
                previewBlocks.add(new BlockPos(x1, y1, z1));
                z1 += zs;
                if (p1 >= 0) { x1 += xs; p1 -= 2*dz; }
                if (p2 >= 0) { y1 += ys; p2 -= 2*dz; }
                p1 += 2*dx;
                p2 += 2*dy;
            }
        }
        previewBlocks.add(new BlockPos(x2, y2, z2));
    }

    public static class ShapeCommand extends CommandBase {
        @Override
        public String getName() {
            return "shape";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/shape <type> <parameters...> OR /shape preview <all|layer> OR /shape clear OR /shape help";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (!(sender instanceof EntityPlayer)) return;

            EntityPlayer player = (EntityPlayer) sender;
            BlockPos pos = player.getPosition();

            if (args.length == 0) {
                sendHelpMessage(player);
                return;
            }

            if (args[0].equals("help")) {
                int page = 1;
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "页码必须是数字！"
                        ));
                        return;
                    }
                }
                sendDetailedHelpMessage(player, page);
                return;
            }

            if (args[0].equals("orientation")) {
                if (args.length < 2) {
                    player.sendMessage(new TextComponentString("用法: /shape orientation <horizontal|vertical_x|vertical_z>"));
                    return;
                }
                switch (args[1].toLowerCase()) {
                    case "horizontal":
                        currentOrientation = Orientation.HORIZONTAL;
                        break;
                    case "vertical_x":
                        currentOrientation = Orientation.VERTICAL_X;
                        break;
                    case "vertical_z":
                        currentOrientation = Orientation.VERTICAL_Z;
                        break;
                    default:
                        player.sendMessage(new TextComponentString("无效的方向！使用 horizontal, vertical_x 或 vertical_z"));
                        return;
                }
                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "形状方向已设置为: " + args[1]
                ));
                return;
            }

            if (args[0].equals("preview")) {
                if (args.length < 2) {
                    player.sendMessage(new TextComponentString(
                        "用法: /shape preview <all|layer|below>"
                    ));
                    return;
                }
                switch (args[1].toLowerCase()) {
                    case "all":
                        currentDisplayMode = DisplayMode.ALL;
                        break;
                    case "layer":
                        currentDisplayMode = DisplayMode.CURRENT_LAYER;
                        break;
                    case "below":
                        currentDisplayMode = DisplayMode.BELOW_LAYER;
                        break;
                    default:
                        player.sendMessage(new TextComponentString(
                            "无效的预览模式！使用 all, layer 或 below"
                        ));
                        return;
                }
                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "预览模式已设置为: " + args[1]
                ));
                return;
            }

            if (args[0].equals("clear")) {
                previewBlocks.clear();
                centerPos = null;
                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "已清除形状预览"
                ));
                return;
            }

            try {
                switch (args[0].toLowerCase()) {
                    case "circle":
                        if (args.length < 2) {
                            player.sendMessage(new TextComponentString("用法: /shape circle <半径>"));
                            return;
                        }
                        int radius = Integer.parseInt(args[1]);
                        generateCircle(pos, radius);
                        break;

                    case "sphere":
                        if (args.length < 2) {
                            player.sendMessage(new TextComponentString("用法: /shape sphere <半径>"));
                            return;
                        }
                        radius = Integer.parseInt(args[1]);
                        generateSphere(pos, radius);
                        break;

                    case "ellipse":
                        if (args.length < 3) {
                            player.sendMessage(new TextComponentString("用法: /shape ellipse <X半径> <Z半径>"));
                            return;
                        }
                        int radiusX = Integer.parseInt(args[1]);
                        int radiusZ = Integer.parseInt(args[2]);
                        generateEllipse(pos, radiusX, radiusZ);
                        break;

                    case "ellipsoid":
                        if (args.length < 4) {
                            player.sendMessage(new TextComponentString(
                                "用法: /shape ellipsoid <X半径> <Y半径> <Z半径>"
                            ));
                            return;
                        }
                        int radiusX1 = Integer.parseInt(args[1]);
                        int radiusY1 = Integer.parseInt(args[2]);
                        int radiusZ1 = Integer.parseInt(args[3]);
                        generateEllipsoid(pos, radiusX1, radiusY1, radiusZ1);
                        break;

                    case "line":
                        if (args.length < 3) {
                            player.sendMessage(new TextComponentString("用法: /shape line <x1,y1,z1> <x2,y2,z2>"));
                            return;
                        }
                        try {
                            BlockPos start = parseAbsoluteBlockPos(args[1]);
                            BlockPos end = parseAbsoluteBlockPos(args[2]);
                            generateLine(start, end);
                            centerPos = start; // 设置中心点为起点
                        } catch (NumberFormatException e) {
                            player.sendMessage(new TextComponentString(TextFormatting.RED + "坐标格式错误！使用 绝对坐标x,y,z 格式"));
                        }
                        break;

                    case "cone":
                        if (args.length < 3) {
                            player.sendMessage(new TextComponentString("用法: /shape cone <半径> <高度>"));
                            return;
                        }
                        int baseRadius = Integer.parseInt(args[1]);
                        int height = Integer.parseInt(args[2]);
                        generateCone(pos, baseRadius, height, player);
                        break;

                    default:
                        sendHelpMessage(player);
                        return;
                }

                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "形状预览已生成！\n" +
                    TextFormatting.YELLOW + "使用 /shape preview <all|layer|below> 切换预览模式"
                ));

            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "参数必须是数字！"
                ));
            }
        }

        // 新增坐标解析方法
        private static BlockPos parseAbsoluteBlockPos(String coordStr) {
            String[] parts = coordStr.split(",");
            if (parts.length != 3) throw new NumberFormatException();
            
            return new BlockPos(
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim()),
                Integer.parseInt(parts[2].trim())
            );
        }
        
        private void sendDetailedHelpMessage(EntityPlayer player, int page) {
            int maxPage = 4; // 总页数
            if (page < 1 || page > maxPage) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "无效的页码！页码范围：1-" + maxPage
                ));
                return;
            }

            String header = TextFormatting.GOLD + "\n === 建筑形状生成器指令帮助 === \n" + 
                        TextFormatting.GRAY + "[第" + page + "页，共" + maxPage + "页]\n" +
                        TextFormatting.YELLOW + "使用 /shape help <页码> 查看其他页面\n" +
                        TextFormatting.YELLOW + "╰(*°▽°*)╯最后一页有小Tip\n\n";


            String content = "";
            switch (page) {
                case 1:
                    content = TextFormatting.YELLOW + "基础形状命令(1/2)：\n" +
                        TextFormatting.WHITE + "/shape circle <半径>\n" +
                        TextFormatting.GRAY + "  生成一个圆形预览\n" +
                        TextFormatting.GRAY + "  - 半径：圆的大小（方块数）\n" +
                        TextFormatting.GRAY + "  示例：/shape circle 5\n\n" +
                        
                        TextFormatting.WHITE + "/shape sphere <半径>\n" +
                        TextFormatting.GRAY + "  生成一个球体预览\n" +
                        TextFormatting.GRAY + "  - 半径：球体的大小（方块数）\n" +
                        TextFormatting.GRAY + "  示例：/shape sphere 10\n\n" +
                        
                        TextFormatting.WHITE + "/shape ellipse <X半径> <Z半径>\n" +
                        TextFormatting.GRAY + "  生成一个椭圆预览\n" +
                        TextFormatting.GRAY + "  - X半径：椭圆X轴方向的大小\n" +
                        TextFormatting.GRAY + "  - Z半径：椭圆Z轴方向的大小\n" +
                        TextFormatting.GRAY + "  示例：/shape ellipse 10 5\n\n"+

                        TextFormatting.WHITE + "/shape ellipsoid <X半径> <Y半径> <Z半径>\n" +
                        TextFormatting.GRAY + "  生成一个椭圆体预览\n" +
                        TextFormatting.GRAY + "  - X半径：椭圆体X轴方向的大小\n" +
                        TextFormatting.GRAY + "  - Y半径：椭圆体Y轴方向的大小\n" +
                        TextFormatting.GRAY + "  - Z半径：椭圆体Z轴方向的大小\n" +
                        TextFormatting.GRAY + "  示例：/shape ellipsoid 10 8 6";
                break;

                case 2:
                    content = TextFormatting.YELLOW + "基础形状命令(2/2)：\n" +
                        TextFormatting.WHITE + "/shape cone <半径> <高度>\n" +
                        TextFormatting.GRAY + "  智能方向圆锥：\n" +
                        TextFormatting.GRAY + "  - 抬头/平视：水平方向（随水平视角旋转）\n" +
                        TextFormatting.GRAY + "  - 抬头超过45度：垂直向上\n" +
                        TextFormatting.GRAY + "  - 低头超过45度：垂直向下\n\n" +
                        
                        TextFormatting.WHITE + "/shape line <起点> <终点>\n" +
                        TextFormatting.GRAY + "  生成绝对坐标间的直线\n" +
                        TextFormatting.GRAY + "  示例：/shape line 100,64,200 120,70,180\n\n";
                    break;

                case 3:
                    content = TextFormatting.YELLOW + "预览控制命令(1/1)：\n" +
                        TextFormatting.WHITE + "/shape preview all\n" +
                        TextFormatting.GRAY + "  显示形状的所有层级预览\n" +
                        TextFormatting.GRAY + "  适用于观察整体形状\n\n" +
                        
                        TextFormatting.WHITE + "/shape preview layer\n" +
                        TextFormatting.GRAY + "  只显示当前所在高度层的预览\n" +
                        TextFormatting.GRAY + "  适用于创造建造每一层\n\n" +

                        TextFormatting.WHITE + "/shape preview below\n" +
                        TextFormatting.GRAY + "  只显示脚下一层的预览\n" +
                        TextFormatting.GRAY + "  适用于生存建造每一层\n\n" +

                        TextFormatting.WHITE + "/shape clear\n" +
                        TextFormatting.GRAY + "  清除所有预览效果";
                    break;

                case 4:
                    content = TextFormatting.YELLOW + "方向控制命令(1/1)：\n" +
                        TextFormatting.WHITE + "/shape orientation <方向>\n" +
                        TextFormatting.GRAY + "  设置平面图形的方向\n" +
                        TextFormatting.GRAY + "  可用的方向：\n" +
                        TextFormatting.GRAY + "  - horizontal: 水平面 (XZ平面)\n" +
                        TextFormatting.GRAY + "  - vertical_x: 垂直X面 (XY平面)\n" +
                        TextFormatting.GRAY + "  - vertical_z: 垂直Z面 (ZY平面)\n" +
                        TextFormatting.GRAY + "  示例：/shape orientation vertical_x" +
                        
                        TextFormatting.GRAY + "1. 所有形状都以玩家当前位置为中心点生成\n" +
                        TextFormatting.GRAY + "2. 预览使用蓝色半透明边框显示\n" +
                        TextFormatting.GRAY + "3. 只显示空气方块处的预览\n" +
                        TextFormatting.GRAY + "4. 生成新形状时会自动清除旧预览(线条除外)\n" +
                        TextFormatting.GRAY + "5. 建议先用preview layer模式确认每层位置\n\n";
                    break;
            }

            player.sendMessage(new TextComponentString(header + content));
        }

        private void sendHelpMessage(EntityPlayer player) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=== 建筑形状生成器 ===\n" +
                TextFormatting.YELLOW + "输入 /shape help 查看详细帮助\n\n" +
                TextFormatting.WHITE + "常用命令：\n" +
                
                TextFormatting.GRAY + "/shape circle <半径>\n" +
                TextFormatting.GRAY + "/shape sphere <半径>\n" +
                TextFormatting.GRAY + "/shape ellipse <X半径> <Z半径>\n" +
                TextFormatting.GRAY + "/shape ellipsoid <X半径> <Y半径> <Z半径>\n" +
                TextFormatting.GRAY + "/shape cone <底部半径> <高度>\n" +
                TextFormatting.GRAY + "/shape line <起点> <终点>\n" +
                
                TextFormatting.GRAY + "/shape preview <all|layer|below>\n" +
                TextFormatting.GRAY + "/shape orientation <方向>\n" +
                TextFormatting.GRAY + "/shape clear\n\n" +
                TextFormatting.YELLOW + "提示：使用 /shape help <页码> 查看详细说明"
            ));
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true;
        }
    }
}