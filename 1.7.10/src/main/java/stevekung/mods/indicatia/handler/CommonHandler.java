package stevekung.mods.indicatia.handler;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.input.Keyboard;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import io.netty.channel.ChannelOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import stevekung.mods.indicatia.config.ConfigGuiFactory;
import stevekung.mods.indicatia.config.ConfigManager;
import stevekung.mods.indicatia.config.ExtendedConfig;
import stevekung.mods.indicatia.core.IndicatiaMod;
import stevekung.mods.indicatia.gui.*;
import stevekung.mods.indicatia.renderer.HUDInfo;
import stevekung.mods.indicatia.renderer.KeystrokeRenderer;
import stevekung.mods.indicatia.utils.*;

public class CommonHandler
{
    private JsonUtil json;
    public static final List<String> PLAYER_LIST = new ArrayList<>();
    public static final Map<String, Integer> PLAYER_PING_MAP = Maps.newHashMap();
    private final Minecraft mc;
    public static final List<Long> LEFT_CLICK = new ArrayList<>();
    public static final List<Long> RIGHT_CLICK = new ArrayList<>();
    public static final GuiNewChatUtil chatGui = new GuiNewChatUtil();
    public static final GuiSleepMPNew sleepGui = new GuiSleepMPNew();
    public static final GuiNewChatUtil chatGuiSlash = new GuiNewChatUtil("/");
    public static final GuiCustomCape customCapeGui = new GuiCustomCape();
    public static final GuiDonator donatorGui = new GuiDonator();

    // AFK Stuff
    public static boolean isAFK;
    public static String afkMode = "idle";
    public static String afkReason;
    public static int afkMoveTicks;
    public static int afkTicks;

    public static boolean autoClick;
    public static String autoClickMode = "right";
    public static int autoClickTicks = 0;

    private static long sneakTimeOld = 0L;
    private static boolean sneakingOld = false;

    public static boolean setTCPNoDelay = false;

    public CommonHandler(Minecraft mc)
    {
        this.json = new JsonUtil();
        this.mc = mc;
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent event)
    {
        if (event.modID.equalsIgnoreCase(IndicatiaMod.MOD_ID))
        {
            ConfigManager.syncConfig(false);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (this.mc.thePlayer != null)
        {
            CommonHandler.getPingAndPlayerList(this.mc);

            if (this.mc.getNetHandler() != null && CommonHandler.setTCPNoDelay)
            {
                this.mc.getNetHandler().getNetworkManager().channel().config().setOption(ChannelOption.TCP_NODELAY, true);
                ModLogger.info("Set TCP_NODELAY to true");
                CommonHandler.setTCPNoDelay = false;
            }
            if (event.phase == TickEvent.Phase.START)
            {
                CommonHandler.runAFK(this.mc.thePlayer);

                if (IndicatiaMod.isSteveKunG() && CommonHandler.autoClick)
                {
                    CommonHandler.autoClickTicks++;

                    if (CommonHandler.autoClickMode.equals("left"))
                    {
                        this.mc.func_147115_a(true);
                    }
                    else
                    {
                        if (CommonHandler.autoClickTicks % 4 == 0)
                        {
                            this.mc.func_147121_ag();
                        }
                    }
                }
            }
            if (!(this.mc.thePlayer.movementInput instanceof MovementInputFromOptionsIU))
            {
                this.mc.thePlayer.movementInput = new MovementInputFromOptionsIU(this.mc.gameSettings);
            }
            CommonHandler.replaceGui(this.mc, this.mc.currentScreen);
        }
        GuiIngameForge.renderBossHealth = ConfigManager.enableRenderBossHealthStatus;
        GuiIngameForge.renderObjective = ConfigManager.enableRenderScoreboard;
    }

    @SubscribeEvent
    public void onMouseClick(MouseEvent event)
    {
        if (event.button == 0 && event.buttonstate)
        {
            CommonHandler.LEFT_CLICK.add(Long.valueOf(System.currentTimeMillis()));
        }
        if (event.button == 1 && event.buttonstate)
        {
            CommonHandler.RIGHT_CLICK.add(Long.valueOf(System.currentTimeMillis()));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (ConfigManager.enableVersionChecker)
        {
            if (!IndicatiaMod.CHECK_NO_CONNECTION && VersionChecker.INSTANCE.noConnection())
            {
                event.player.addChatMessage(this.json.text("Unable to check latest version, Please check your internet connection").setChatStyle(this.json.red()));
                event.player.addChatMessage(this.json.text(VersionChecker.INSTANCE.getExceptionMessage()).setChatStyle(this.json.red()));
                IndicatiaMod.CHECK_NO_CONNECTION = true;
                return;
            }
            if (!IndicatiaMod.FOUND_LATEST && !IndicatiaMod.CHECK_NO_CONNECTION && VersionChecker.INSTANCE.isLatestVersion())
            {
                event.player.addChatMessage(this.json.text("New version of ").appendSibling(this.json.text("Indicatia").setChatStyle(this.json.style().setColor(EnumChatFormatting.AQUA)).appendSibling(this.json.text(" is available ").setChatStyle(this.json.white()).appendSibling(this.json.text("v" + VersionChecker.INSTANCE.getLatestVersion().replace("[" + IndicatiaMod.MC_VERSION + "]=", "")).setChatStyle(this.json.style().setColor(EnumChatFormatting.GREEN)).appendSibling(this.json.text(" for ").setChatStyle(this.json.white()).appendSibling(this.json.text("MC-" + IndicatiaMod.MC_VERSION).setChatStyle(this.json.style().setColor(EnumChatFormatting.GOLD))))))));
                event.player.addChatMessage(this.json.text("Download Link ").setChatStyle(this.json.style().setColor(EnumChatFormatting.YELLOW)).appendSibling(this.json.text("[CLICK HERE]").setChatStyle(this.json.style().setColor(EnumChatFormatting.BLUE).setChatHoverEvent(this.json.hover(HoverEvent.Action.SHOW_TEXT, this.json.text("Click Here!").setChatStyle(this.json.style().setColor(EnumChatFormatting.DARK_GREEN)))).setChatClickEvent(this.json.click(ClickEvent.Action.OPEN_URL, IndicatiaMod.URL)))));
                IndicatiaMod.FOUND_LATEST = true;
            }
            if (!IndicatiaMod.SHOW_ANNOUNCE_MESSAGE && !IndicatiaMod.CHECK_NO_CONNECTION)
            {
                for (String log : VersionChecker.INSTANCE.getAnnounceMessage())
                {
                    if (ConfigManager.enableAnnounceMessage)
                    {
                        event.player.addChatMessage(this.json.text(log).setChatStyle(this.json.style().setColor(EnumChatFormatting.GRAY)));
                    }
                }
                event.player.addChatMessage(this.json.text("To read Indicatia full change log. Use /inchangelog command!").setChatStyle(this.json.colorFromConfig("gray").setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/inchangelog"))));
                IndicatiaMod.SHOW_ANNOUNCE_MESSAGE = true;
            }
        }
    }

    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event)
    {
        this.mc.ingameGUI.persistantChatGUI = new GuiNewChatFast();
        CommonHandler.setTCPNoDelay = true;
    }

    @SubscribeEvent
    public void onDisconnectedFromServerEvent(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        CommonHandler.PLAYER_PING_MAP.clear();
        CommonHandler.PLAYER_LIST.clear();
        CommonHandler.stopCommandTicks();
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        CommonHandler.stopCommandTicks();
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        CommonHandler.stopCommandTicks();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            if (ConfigManager.enableSmoothEyeHeight)
            {
                if (this.mc.thePlayer != null)
                {
                    this.mc.thePlayer.eyeHeight = CommonHandler.getSmoothEyeHeight(this.mc.thePlayer);
                }
            }
        }
        if (event.phase == TickEvent.Phase.END)
        {
            if (!this.mc.gameSettings.hideGUI && !this.mc.gameSettings.showDebugInfo)
            {
                if (ConfigManager.enableKeystroke)
                {
                    if (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiChat || this.mc.currentScreen instanceof GuiRenderStatusSettings || this.mc.currentScreen instanceof GuiKeystrokeColorSettings)
                    {
                        KeystrokeRenderer.init(this.mc);
                    }
                }
                if (ConfigManager.enableRenderInfo && ExtendedConfig.CPS_POSITION.equalsIgnoreCase("custom") && (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiChat || this.mc.currentScreen instanceof GuiRenderStatusSettings || this.mc.currentScreen instanceof GuiKeystrokeColorSettings))
                {
                    String space = ConfigManager.enableRCPS ? " " : "";
                    RenderUtil.drawRect(ExtendedConfig.CPS_X_OFFSET, ExtendedConfig.CPS_Y_OFFSET, ExtendedConfig.CPS_X_OFFSET + this.mc.fontRenderer.getStringWidth(HUDInfo.getCPS() + space + HUDInfo.getRCPS()) + 4, ExtendedConfig.CPS_Y_OFFSET + 11, 16777216, ExtendedConfig.CPS_OPACITY);
                    this.mc.fontRenderer.drawString(HUDInfo.getCPS() + space + HUDInfo.getRCPS(), ExtendedConfig.CPS_X_OFFSET + 2, ExtendedConfig.CPS_Y_OFFSET + 2, 16777215, true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPressKey(InputEvent.KeyInputEvent event)
    {
        if (this.mc.currentScreen == null && this.mc.gameSettings.keyBindCommand.isPressed())
        {
            this.mc.displayGuiScreen(CommonHandler.chatGuiSlash);
        }
        if (KeyBindingHandler.KEY_QUICK_CONFIG.getIsKeyPressed())
        {
            this.mc.displayGuiScreen(new ConfigGuiFactory.GuiMainConfig(this.mc.currentScreen));
        }
        if (KeyBindingHandler.KEY_REC_COMMAND.getIsKeyPressed())
        {
            HUDRenderHandler.recordEnable = !HUDRenderHandler.recordEnable;
        }
        if (ConfigManager.enableCustomCape && KeyBindingHandler.KEY_CUSTOM_CAPE_GUI != null && KeyBindingHandler.KEY_CUSTOM_CAPE_GUI.getIsKeyPressed())
        {
            this.mc.displayGuiScreen(CommonHandler.customCapeGui);
        }
        if (ExtendedConfig.TOGGLE_SPRINT_USE_MODE.equals("key_binding"))
        {
            String[] keyTS = ConfigManager.keyToggleSprint.split(",");
            int keyTGCtrl = InfoUtil.INSTANCE.parseInt(keyTS[0], "Toggle Sprint");
            int keyTGOther = InfoUtil.INSTANCE.parseInt(keyTS[1], "Toggle Sprint");

            if (Keyboard.isKeyDown(keyTGCtrl) && Keyboard.isKeyDown(keyTGOther))
            {
                ExtendedConfig.TOGGLE_SPRINT = !ExtendedConfig.TOGGLE_SPRINT;
                InfoUtil.INSTANCE.setOverlayMessage(this.json.text(ExtendedConfig.TOGGLE_SPRINT ? "Toggle Sprint Enabled" : "Toggle Sprint Disabled").getFormattedText(), false);
                ExtendedConfig.save();
            }
        }
        if (ExtendedConfig.TOGGLE_SNEAK_USE_MODE.equals("key_binding"))
        {
            String[] keyTS = ConfigManager.keyToggleSneak.split(",");
            int keyTGCtrl = InfoUtil.INSTANCE.parseInt(keyTS[0], "Toggle Sneak");
            int keyTGOther = InfoUtil.INSTANCE.parseInt(keyTS[1], "Toggle Sneak");

            if (Keyboard.isKeyDown(keyTGCtrl) && Keyboard.isKeyDown(keyTGOther))
            {
                ExtendedConfig.TOGGLE_SNEAK = !ExtendedConfig.TOGGLE_SNEAK;
                InfoUtil.INSTANCE.setOverlayMessage(this.json.text(ExtendedConfig.TOGGLE_SPRINT ? "Toggle Sneak Enabled" : "Toggle Sneak Disabled").getFormattedText(), false);
                ExtendedConfig.save();
            }
        }
        if (ExtendedConfig.AUTO_SWIM_USE_MODE.equals("key_binding"))
        {
            String[] keyAW = ConfigManager.keyAutoSwim.split(",");
            int keyAWCtrl = InfoUtil.INSTANCE.parseInt(keyAW[0], "Auto Swim");
            int keyAWOther = InfoUtil.INSTANCE.parseInt(keyAW[1], "Auto Swim");

            if (Keyboard.isKeyDown(keyAWCtrl) && Keyboard.isKeyDown(keyAWOther))
            {
                ExtendedConfig.AUTO_SWIM = !ExtendedConfig.AUTO_SWIM;
                InfoUtil.INSTANCE.setOverlayMessage(this.json.text(ExtendedConfig.TOGGLE_SPRINT ? "Auto Swim Enabled" : "Auto Swim Disabled").getFormattedText(), false);
                ExtendedConfig.save();
            }
        }
        if (KeyBindingHandler.KEY_DONATOR_GUI.getIsKeyPressed())
        {
            this.mc.displayGuiScreen(CommonHandler.donatorGui);
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event)
    {
        if (event.gui instanceof GuiIngameMenu)
        {
            event.buttonList.add(new GuiButton(200, event.gui.width - 145, 20, 135, 20, "Paypal"));
            event.buttonList.add(new GuiButton(201, event.gui.width - 145, 41, 135, 20, "Truemoney"));
        }
    }

    @SubscribeEvent
    public void onActionGui(GuiScreenEvent.ActionPerformedEvent.Post event)
    {
        if (event.gui instanceof GuiIngameMenu)
        {
            switch (event.button.id)
            {
            case 200:
                CommonHandler.openLink("https://twitch.streamlabs.com/stevekung");
                break;
            case 201:
                CommonHandler.openLink("https://tipme.in.th/stevekung");
                break;
            }
        }
    }

    @SubscribeEvent
    public void onRenderGui(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (event.gui instanceof GuiIngameMenu)
        {
            event.gui.drawString(this.mc.fontRenderer, "Support Indicatia!", event.gui.width - 120, 8, 65481);
        }
    }

    private static void runAFK(EntityClientPlayerMP player)
    {
        if (CommonHandler.isAFK)
        {
            CommonHandler.afkTicks++;
            int tick = CommonHandler.afkTicks;
            int messageMin = 1200 * ConfigManager.afkMessageTime;
            String s = "s";
            float angle = tick % 2 == 0 ? 0.0001F : -0.0001F;

            if (tick == 0)
            {
                s = "";
            }
            if (ConfigManager.enableAFKMessage)
            {
                if (tick % messageMin == 0)
                {
                    String reason = CommonHandler.afkReason;
                    reason = reason.isEmpty() ? "" : ", Reason : " + reason;
                    player.sendChatMessage("AFK : " + StringUtils.ticksToElapsedTime(tick) + " minute" + s + reason);
                }
            }

            if (CommonHandler.afkMode.equals("idle"))
            {
                player.setAngles(angle, angle);
            }
            else if (CommonHandler.afkMode.equals("360"))
            {
                player.setAngles(1.0F, 0.0F);
            }
            else if (CommonHandler.afkMode.equals("360_move"))
            {
                player.setAngles(1.0F, 0.0F);
                CommonHandler.afkMoveTicks++;
                CommonHandler.afkMoveTicks %= 8;
            }
            else
            {
                player.setAngles(angle, angle);
                CommonHandler.afkMoveTicks++;
                CommonHandler.afkMoveTicks %= 8;
            }
        }
        else
        {
            CommonHandler.afkTicks = 0;
        }
    }

    private static float getSmoothEyeHeight(EntityPlayerSP player)
    {
        if (CommonHandler.sneakingOld != player.isSneaking() || CommonHandler.sneakTimeOld <= 0L)
        {
            CommonHandler.sneakTimeOld = System.currentTimeMillis();
        }

        CommonHandler.sneakingOld = player.isSneaking();
        float defaultEyeHeight = 1.62F;
        double sneakPress = 0.0004D;
        double sneakValue = 0.015D;
        int sneakTime = -50;
        long systemTime = 58L;

        if (player.isSneaking())
        {
            int sneakSystemTime = (int)(CommonHandler.sneakTimeOld + systemTime - System.currentTimeMillis());

            if (sneakSystemTime > sneakTime)
            {
                defaultEyeHeight += (float)(sneakSystemTime * sneakPress);

                if (defaultEyeHeight < 0.0F || defaultEyeHeight > 10.0F)
                {
                    defaultEyeHeight = 1.54F;
                }
            }
            else
            {
                defaultEyeHeight = (float)(defaultEyeHeight - sneakValue);
            }
        }
        else
        {
            int sneakSystemTime = (int)(CommonHandler.sneakTimeOld + systemTime - System.currentTimeMillis());

            if (sneakSystemTime > sneakTime)
            {
                defaultEyeHeight -= (float)(sneakSystemTime * sneakPress);
                defaultEyeHeight = (float)(defaultEyeHeight - sneakValue);

                if (defaultEyeHeight < 0.0F)
                {
                    defaultEyeHeight = 1.62F;
                }
            }
            else
            {
                defaultEyeHeight -= 0.0F;
            }
        }
        return defaultEyeHeight;
    }

    private static void replaceGui(Minecraft mc, GuiScreen currentScreen)
    {
        if (currentScreen != null)
        {
            if (currentScreen instanceof GuiChat && !(currentScreen instanceof GuiNewChatUtil || currentScreen instanceof GuiSleepMP))
            {
                mc.displayGuiScreen(CommonHandler.chatGui);
            }
            if (currentScreen instanceof GuiSleepMP && !(currentScreen instanceof GuiSleepMPNew))
            {
                mc.displayGuiScreen(CommonHandler.sleepGui);
            }
            if (currentScreen instanceof GuiSleepMPNew && !mc.thePlayer.isPlayerSleeping())
            {
                mc.displayGuiScreen((GuiScreen)null);
            }
        }
    }

    private static void stopCommandTicks()
    {
        if (CommonHandler.isAFK)
        {
            CommonHandler.isAFK = false;
            CommonHandler.afkReason = "";
            CommonHandler.afkTicks = 0;
            CommonHandler.afkMoveTicks = 0;
            CommonHandler.afkMode = "idle";
            ModLogger.info("Stopping AFK Command");
        }
        if (CommonHandler.autoClick)
        {
            CommonHandler.autoClickTicks = 0;
            CommonHandler.autoClickMode = "left";
            ModLogger.info("Stopping Auto Click Command");
        }
    }

    private static void getPingAndPlayerList(Minecraft mc)
    {
        NetHandlerPlayClient handler = mc.thePlayer.sendQueue;
        List<GuiPlayerInfo> players = handler.playerInfoList;
        int maxPlayers = handler.currentServerMaxPlayers;

        for (int i = 0; i < maxPlayers; i++)
        {
            if (i < players.size())
            {
                GuiPlayerInfo player = players.get(i);
                CommonHandler.PLAYER_LIST.add(player.name);
                Set<String> hs = Sets.newHashSet();
                hs.addAll(CommonHandler.PLAYER_LIST);
                CommonHandler.PLAYER_LIST.clear();
                CommonHandler.PLAYER_LIST.addAll(hs);
                CommonHandler.PLAYER_PING_MAP.put(player.name, player.responseTime);
            }
        }
    }

    private static void openLink(String url)
    {
        try
        {
            URI uri = new URI(url);
            Desktop.getDesktop().browse(uri);
        }
        catch (Exception e)
        {
            ModLogger.info("Couldn't open link {}", url);
            e.printStackTrace();
        }
    }
}