package stevekung.mods.indicatia.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.common.collect.Maps;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import stevekung.mods.indicatia.config.ExtendedConfig;
import stevekung.mods.indicatia.core.IndicatiaMod;

public class CapeUtils
{
    public static Map<String, DynamicTexture> CAPE_TEXTURE = Maps.newHashMap();
    public static boolean textureUploaded = true;

    public static void bindCapeTexture()
    {
        if (CapeUtils.CAPE_TEXTURE.get(GameProfileUtil.getUsername()) != null)
        {
            GlStateManager.bindTexture(CapeUtils.CAPE_TEXTURE.get(GameProfileUtil.getUsername()).getGlTextureId());
        }
    }

    public static void setCapeURL(String url, boolean startup)
    {
        URL jurl = null;
        boolean noConnection = false;
        JsonUtil json = new JsonUtil();

        try
        {
            jurl = new URL(url);
        }
        catch (MalformedURLException e)
        {
            noConnection = true;
            e.printStackTrace();
            return;
        }

        if (CapeUtils.textureUploaded && !noConnection)
        {
            try
            {
                CapeUtils.CAPE_TEXTURE.put(GameProfileUtil.getUsername(), new DynamicTexture(ImageIO.read(jurl)));
                ExtendedConfig.CAPE_URL = Base64Utils.encode(url);
                ExtendedConfig.save();
                CapeUtils.textureUploaded = false;
            }
            catch (MalformedURLException e)
            {
                if (IndicatiaMod.MC.player != null)
                {
                    IndicatiaMod.MC.player.sendMessage(json.text("Missing protocol or wrong URL format").setStyle(json.red()));
                }
                e.printStackTrace();
                return;
            }
            catch (IOException e)
            {
                if (IndicatiaMod.MC.player != null)
                {
                    IndicatiaMod.MC.player.sendMessage(json.text("Cannot read image from URL").setStyle(json.red()));
                }
                e.printStackTrace();
                return;
            }
        }

        if (!startup)
        {
            IndicatiaMod.MC.player.sendMessage(json.text("Downloaded new cape texture from URL"));
        }
    }
}