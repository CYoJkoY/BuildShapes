package com.cyojkoy.buildshapes;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
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

@Mod(modid = BuildShapes.MODID, name = BuildShapes.NAME, version = BuildShapes.VERSION, clientSideOnly = true)
public class BuildShapes {
    public static final String MODID = "buildshapes";
    public static final String NAME = "Build Shapes";
    public static final String VERSION = "1.0.0";

    // 添加方向枚举
    private enum Orientation {
        HORIZONTAL, // XZ平面
        VERTICAL_X, // XY平面
        VERTICAL_Z  // ZY平面
    }

    private static Set<BlockPos> previewBlocks = new HashSet<>();
    private static boolean showAllLayers = true;
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

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );

        // 绘制形状预览
        for (BlockPos pos : previewBlocks) {
            if ((showAllLayers || pos.getY() == player.getPosition().getY()) && 
                world.isAirBlock(pos)) {
                double x = pos.getX() - playerX;
                double y = pos.getY() - playerY;
                double z = pos.getZ() - playerZ;

                AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
                RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 1.0F, 1.0F, 0.4F);
            }
        }

        // 绘制中心点（红色）
        double centerX = centerPos.getX() - playerX;
        double centerY = centerPos.getY() - playerY;
        double centerZ = centerPos.getZ() - playerZ;
        
        AxisAlignedBB centerBox = new AxisAlignedBB(
            centerX + 0.25, centerY + 0.25, centerZ + 0.25,
            centerX + 0.75, centerY + 0.75, centerZ + 0.75
        );
        RenderGlobal.drawSelectionBoundingBox(centerBox, 1.0F, 0.0F, 0.0F, 1.0F);

        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
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
            addEllipsePointsHorizontal(center, x, z);
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
            addEllipsePointsHorizontal(center, x, z);
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

    private static void addEllipsePointsHorizontal(BlockPos center, int x, int z) {
        previewBlocks.add(new BlockPos(center.getX() + x, center.getY(), center.getZ() + z));
        previewBlocks.add(new BlockPos(center.getX() - x, center.getY(), center.getZ() + z));
        previewBlocks.add(new BlockPos(center.getX() + x, center.getY(), center.getZ() - z));
        previewBlocks.add(new BlockPos(center.getX() - x, center.getY(), center.getZ() - z));
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
                addEllipsePointsVerticalX(center, x, y);
            } else {
                addEllipsePointsVerticalZ(center, x, y);
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
                addEllipsePointsVerticalX(center, x, y);
            } else {
                addEllipsePointsVerticalZ(center, x, y);
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

    private static void addEllipsePointsVerticalX(BlockPos center, int x, int y) {
        previewBlocks.add(new BlockPos(center.getX() + x, center.getY() + y, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() - x, center.getY() + y, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() + x, center.getY() - y, center.getZ()));
        previewBlocks.add(new BlockPos(center.getX() - x, center.getY() - y, center.getZ()));
    }

    private static void addEllipsePointsVerticalZ(BlockPos center, int x, int y) {
        previewBlocks.add(new BlockPos(center.getX(), center.getY() + y, center.getZ() + x));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() + y, center.getZ() - x));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() - y, center.getZ() + x));
        previewBlocks.add(new BlockPos(center.getX(), center.getY() - y, center.getZ() - x));
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
                    player.sendMessage(new TextComponentString("用法: /shape preview <all|layer>"));
                    return;
                }
                showAllLayers = args[1].equals("all");
                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "预览模式已设置为: " + 
                    (showAllLayers ? "显示所有层" : "仅显示当前层")
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

                    default:
                        sendHelpMessage(player);
                        return;
                }

                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "形状预览已生成！使用 /shape preview <all|layer> 切换预览模式"
                ));

            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "参数必须是数字！"
                ));
            }
        }

        private void sendDetailedHelpMessage(EntityPlayer player, int page) {
            int maxPage = 3; // 总页数
            if (page < 1 || page > maxPage) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "无效的页码！页码范围：1-" + maxPage
                ));
                return;
            }

            String header = TextFormatting.GOLD + "=== 建筑形状生成器指令帮助 === " + 
                        TextFormatting.GRAY + "[第" + page + "页，共" + maxPage + "页]\n" +
                        TextFormatting.YELLOW + "使用 /shape help <页码> 查看其他页面\n\n" +
                        TextFormatting.GRAY + "1. 所有形状都以玩家当前位置为中心点生成\n" +
                        TextFormatting.GRAY + "2. 预览使用蓝色半透明边框显示\n" +
                        TextFormatting.GRAY + "3. 只显示空气方块处的预览\n" +
                        TextFormatting.GRAY + "4. 生成新形状时会自动清除旧预览\n" +
                        TextFormatting.GRAY + "5. 建议先用preview layer模式确认每层位置\n\n";

            String content = "";
            switch (page) {
                case 1:
                    content = TextFormatting.YELLOW + "基础形状命令：\n" +
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
                        TextFormatting.GRAY + "  示例：/shape ellipse 10 5";
                    break;

                case 2:
                    content = TextFormatting.YELLOW + "预览控制命令：\n" +
                        TextFormatting.WHITE + "/shape preview all\n" +
                        TextFormatting.GRAY + "  显示形状的所有层级预览\n" +
                        TextFormatting.GRAY + "  适用于观察整体形状\n\n" +
                        
                        TextFormatting.WHITE + "/shape preview layer\n" +
                        TextFormatting.GRAY + "  只显示当前所在高度层的预览\n" +
                        TextFormatting.GRAY + "  适用于精确建造每一层\n\n" +
                        
                        TextFormatting.WHITE + "/shape clear\n" +
                        TextFormatting.GRAY + "  清除所有预览效果";
                    break;

                case 3:
                    content = TextFormatting.YELLOW + "方向控制命令：\n" +
                        TextFormatting.WHITE + "/shape orientation <方向>\n" +
                        TextFormatting.GRAY + "  设置平面图形的方向\n" +
                        TextFormatting.GRAY + "  可用的方向：\n" +
                        TextFormatting.GRAY + "  - horizontal: 水平面 (XZ平面)\n" +
                        TextFormatting.GRAY + "  - vertical_x: 垂直X面 (XY平面)\n" +
                        TextFormatting.GRAY + "  - vertical_z: 垂直Z面 (ZY平面)\n" +
                        TextFormatting.GRAY + "  示例：/shape orientation vertical_x";
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
                TextFormatting.GRAY + "/shape preview <all|layer>\n" +
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