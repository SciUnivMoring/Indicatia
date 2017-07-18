package stevekung.mods.indicatia.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import stevekung.mods.indicatia.config.ExtendedConfig;
import stevekung.mods.indicatia.utils.AutoLogin.AutoLoginData;
import stevekung.mods.indicatia.utils.Base64Utils;
import stevekung.mods.indicatia.utils.GameProfileUtil;

public class PlayerChatHandler
{
    private final Minecraft mc;

    public PlayerChatHandler(Minecraft mc)
    {
        this.mc = mc;
    }

    @SubscribeEvent
    public void onClientConnectedToServer(ClientConnectedToServerEvent event)
    {
        this.mc.addScheduledTask(() -> { MinecraftForge.EVENT_BUS.register(new PlayerRunCommandHandler()); });
    }

    public class PlayerRunCommandHandler
    {
        @SubscribeEvent
        public void onEntityJoinWorld(EntityJoinWorldEvent event)
        {
            if (event.getEntity() instanceof EntityPlayerSP)
            {
                EntityPlayerSP player = (EntityPlayerSP) event.getEntity();
                ServerData data = PlayerChatHandler.this.mc.getCurrentServerData();
                this.runAutoLoginCommand(player, data);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }

        private void runAutoLoginCommand(EntityPlayerSP player, ServerData data)
        {
            if (data != null)
            {
                for (AutoLoginData login : ExtendedConfig.loginData.getAutoLoginList())
                {
                    if (data.serverIP.equalsIgnoreCase(login.getServerIP()) && GameProfileUtil.getUUID().equals(login.getUUID()))
                    {
                        player.sendChatMessage(login.getCommand() + Base64Utils.decode(login.getValue()));
                    }
                }
            }
        }
    }
}