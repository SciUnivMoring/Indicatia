package com.stevekung.indicatia.gui.exconfig.screen;

import com.stevekung.stevekungslib.utils.LangUtils;

import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;

public class CustomColorSettingsScreen extends Screen
{
    private final Screen parent;

    CustomColorSettingsScreen(Screen parent)
    {
        super(NarratorChatListener.EMPTY);
        this.parent = parent;
    }

    @Override
    public void init()
    {
        this.addButton(new Button(this.width / 2 - 155, this.height / 6 - 12, 150, 20, LangUtils.translate("menu.render_info_custom_color.title"), button -> this.minecraft.displayGuiScreen(new CustomRenderInfoColorSettingsScreen(this))));
        this.addButton(new Button(this.width / 2 - 100, this.height / 6 + 175, 200, 20, LangUtils.translate("gui.done"), button -> this.minecraft.displayGuiScreen(this.parent)));
    }

    @Override
    public void onClose()
    {
        this.minecraft.displayGuiScreen(this.parent);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground();
        this.drawCenteredString(this.font, LangUtils.translate("menu.custom_color.title"), this.width / 2, 15, 16777215);
        super.render(mouseX, mouseY, partialTicks);
    }
}