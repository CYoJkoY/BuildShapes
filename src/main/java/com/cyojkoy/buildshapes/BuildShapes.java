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

    private static Set<BlockPos> previewBlocks = new HashSet<>();
    private static boolean showAllLayers = true;
    private static BlockPos centerPos;

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

        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
    }

    private static void generateCircle(BlockPos center, int radius) {
        previewBlocks.clear();
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();

        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                double distance = Math.sqrt(i * i + j * j);
                if (distance <= radius && distance > radius - 1) {
                    previewBlocks.add(new BlockPos(x + i, y, z + j));
                }
            }
        }
        centerPos = center;
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

    private static void generateEllipse(BlockPos center, int radiusX, int radiusZ) {
        previewBlocks.clear();
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();

        for (int i = -radiusX; i <= radiusX; i++) {
            for (int j = -radiusZ; j <= radiusZ; j++) {
                double normalizedDistance = Math.sqrt(
                    (i * i) / (float)(radiusX * radiusX) + 
                    (j * j) / (float)(radiusZ * radiusZ)
                );
                if (normalizedDistance <= 1 && normalizedDistance > 0.9) {
                    previewBlocks.add(new BlockPos(x + i, y, z + j));
                }
            }
        }
        centerPos = center;
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

            if (args.length == 0 || args[0].equals("help")) {
                sendDetailedHelpMessage(player);
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

        private void sendDetailedHelpMessage(EntityPlayer player) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=== 建筑形状生成器指令帮助 ===\n\n" +
                
                TextFormatting.YELLOW + "基础形状命令：\n" +
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
                TextFormatting.GRAY + "  示例：/shape ellipse 10 5\n\n" +
                
                TextFormatting.YELLOW + "预览控制命令：\n" +
                TextFormatting.WHITE + "/shape preview all\n" +
                TextFormatting.GRAY + "  显示形状的所有层级预览\n" +
                TextFormatting.GRAY + "  适用于观察整体形状\n\n" +
                
                TextFormatting.WHITE + "/shape preview layer\n" +
                TextFormatting.GRAY + "  只显示当前所在高度层的预览\n" +
                TextFormatting.GRAY + "  适用于精确建造每一层\n\n" +
                
                TextFormatting.WHITE + "/shape clear\n" +
                TextFormatting.GRAY + "  清除所有预览效果\n\n" +
                
                TextFormatting.WHITE + "/shape help\n" +
                TextFormatting.GRAY + "  显示此帮助信息\n\n" +
                
                TextFormatting.YELLOW + "使用提示：\n" +
                TextFormatting.GRAY + "1. 所有形状都以玩家当前位置为中心点生成\n" +
                TextFormatting.GRAY + "2. 预览使用蓝色半透明边框显示\n" +
                TextFormatting.GRAY + "3. 只显示空气方块处的预览\n" +
                TextFormatting.GRAY + "4. 生成新形状时会自动清除旧预览\n" +
                TextFormatting.GRAY + "5. 建议先用preview layer模式确认每层位置"
            ));
        }

        private void sendHelpMessage(EntityPlayer player) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "形状生成器简要帮助：\n" +
                TextFormatting.YELLOW + "输入 /shape help 查看详细帮助\n\n" +
                TextFormatting.WHITE + "快速参考：\n" +
                TextFormatting.GRAY + "/shape circle <半径>\n" +
                TextFormatting.GRAY + "/shape sphere <半径>\n" +
                TextFormatting.GRAY + "/shape ellipse <X半径> <Z半径>\n" +
                TextFormatting.GRAY + "/shape preview <all|layer>\n" +
                TextFormatting.GRAY + "/shape clear"
            ));
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true;
        }
    }
}