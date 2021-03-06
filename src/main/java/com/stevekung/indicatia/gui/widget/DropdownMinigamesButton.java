package com.stevekung.indicatia.gui.widget;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.stevekung.indicatia.config.ExtendedConfig;
import com.stevekung.stevekungslib.utils.ColorUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;

public class DropdownMinigamesButton extends Button
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("indicatia:textures/gui/dropdown.png");
    public boolean dropdownClicked;
    private int selectedMinigame = -1;
    private final List<String> minigameLists;
    private final IDropboxCallback parentClass;
    private final int displayLength;

    public DropdownMinigamesButton(IDropboxCallback parentClass, int x, int y, List<String> minigameLists)
    {
        super(x, y, 15, 15, "Minigame Dropdown Button", null);
        this.parentClass = parentClass;
        this.minigameLists = minigameLists;

        if (this.minigameLists.size() <= 6)
        {
            this.displayLength = this.minigameLists.size();
        }
        else
        {
            this.displayLength = 6;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        Minecraft mc = Minecraft.getInstance();
        int hoverColor = 150;
        int hoverPos = (mouseY - this.y) / this.height;
        this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        if (!this.dropdownClicked && this.isHovered)
        {
            hoverColor = 180;
        }

        if (this.selectedMinigame == -1)
        {
            int initSelect = this.parentClass.getInitialSelection(this);
            int size = this.minigameLists.size() + ExtendedConfig.INSTANCE.hypixelMinigameScrollPos;

            if (initSelect > size || ExtendedConfig.INSTANCE.selectedHypixelMinigame > size || size == 1)
            {
                initSelect = 0;
                ExtendedConfig.INSTANCE.hypixelMinigameScrollPos = 0;
                ExtendedConfig.INSTANCE.selectedHypixelMinigame = 0;
            }
            this.selectedMinigame = initSelect;
        }

        RenderSystem.pushMatrix();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        AbstractGui.fill(this.x, this.y, this.x + this.width - 15, this.y + (this.dropdownClicked ? this.height * this.displayLength + 15 : this.height), ColorUtils.to32BitColor(255, 0, 0, 0));
        AbstractGui.fill(this.x + 1, this.y + 1, this.x + this.width - 16, this.y + (this.dropdownClicked ? this.height * this.displayLength + 15 : this.height) - 1, ColorUtils.to32BitColor(255, hoverColor, hoverColor, hoverColor));

        if (this.dropdownClicked)
        {
            AbstractGui.fill(this.x + 1, this.y + 1, this.x + this.width - 16, this.y - 1 + this.height, ColorUtils.to32BitColor(255, 120, 120, 120));
        }

        AbstractGui.fill(this.x + this.width - 15, this.y, this.x + this.width - 1, this.y + this.height, ColorUtils.to32BitColor(255, 0, 0, 0));
        AbstractGui.fill(this.x + this.width - 15, this.y + 1, this.x + this.width - 2, this.y + this.height - 1, ColorUtils.to32BitColor(255, 150, 150, 150));

        if (this.displayLength > 1 && this.dropdownClicked)
        {
            if (this.isHoverDropdown(mouseX, mouseY))
            {
                AbstractGui.fill(this.x + 1, this.y + 2 + this.height * hoverPos - 1, this.x + this.width - 16, this.y + this.height * (hoverPos + 1) - 1, ColorUtils.to32BitColor(255, 180, 180, 180));
            }
            if (mouseX >= this.x && mouseY >= this.y + 16 && mouseX < this.x + this.width - 16 && mouseY < this.y + this.height * this.displayLength + 15)
            {
                AbstractGui.fill(this.x + 1, this.y + this.height * hoverPos - 1, this.x + this.width - 16, this.y + this.height * (hoverPos + 1) - 2, ColorUtils.to32BitColor(255, 180, 180, 180));
            }
        }

        for (int i = 0; i + ExtendedConfig.INSTANCE.hypixelMinigameScrollPos < this.minigameLists.size() && i < this.displayLength; ++i)
        {
            String minigames = this.minigameLists.get(i + ExtendedConfig.INSTANCE.hypixelMinigameScrollPos);

            if (minigames != null)
            {
                if (this.dropdownClicked)
                {
                    mc.fontRenderer.drawStringWithShadow(minigames, this.x + this.width / 2 - 7 - mc.fontRenderer.getStringWidth(minigames) / 2, this.y + (this.height + 22) / 2 + this.height * i, ColorUtils.to32BitColor(255, 255, 255, 255));
                    mc.fontRenderer.drawStringWithShadow(this.minigameLists.get(this.selectedMinigame), this.x + this.width / 2 - 7 - mc.fontRenderer.getStringWidth(this.minigameLists.get(this.selectedMinigame)) / 2, this.y + (this.height - 6) / 2, ColorUtils.to32BitColor(255, 255, 255, 255));
                }
                else
                {
                    mc.fontRenderer.drawStringWithShadow(this.minigameLists.get(this.selectedMinigame), this.x + this.width / 2 - 7 - mc.fontRenderer.getStringWidth(this.minigameLists.get(this.selectedMinigame)) / 2, this.y + (this.height - 6) / 2, ColorUtils.to32BitColor(255, 255, 255, 255));
                }
            }
        }
        mc.getTextureManager().bindTexture(DropdownMinigamesButton.TEXTURE);
        AbstractGui.blit(this.x + this.width - 12, this.y + 5, 0, 0, 7, 4, 7, 4);
        RenderSystem.popMatrix();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseEvent)
    {
        if (this.displayLength == 1)
        {
            return false;
        }
        if (!this.dropdownClicked)
        {
            if (this.isHovered)
            {
                this.dropdownClicked = true;
                this.playDownSound(Minecraft.getInstance().getSoundHandler());
                return true;
            }
        }
        else
        {
            if (mouseX >= this.x && mouseY >= this.y + 15 && mouseX < this.x + this.width - 16 && mouseY < this.y + 15 + this.height * this.displayLength)
            {
                double optionClicked = (mouseY - this.y - 16) / this.height + ExtendedConfig.INSTANCE.hypixelMinigameScrollPos;
                this.selectedMinigame = (int)optionClicked % this.minigameLists.size();
                this.dropdownClicked = false;
                this.parentClass.onSelectionChanged(this, this.selectedMinigame);
                this.playDownSound(Minecraft.getInstance().getSoundHandler());
                return true;
            }
            else
            {
                this.dropdownClicked = false;
                return false;
            }
        }
        return false;
    }

    public void scroll(double amount)
    {
        ExtendedConfig.INSTANCE.hypixelMinigameScrollPos -= amount;
        int i = this.minigameLists.size();

        if (ExtendedConfig.INSTANCE.hypixelMinigameScrollPos > i - this.displayLength)
        {
            ExtendedConfig.INSTANCE.hypixelMinigameScrollPos = i - this.displayLength;
        }
        if (ExtendedConfig.INSTANCE.hypixelMinigameScrollPos <= 0)
        {
            ExtendedConfig.INSTANCE.hypixelMinigameScrollPos = 0;
        }
    }

    public boolean isHoverDropdown(double mouseX, double mouseY)
    {
        return mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width - 16 && mouseY < this.y + this.height * this.displayLength + 15;
    }

    public interface IDropboxCallback
    {
        void onSelectionChanged(DropdownMinigamesButton dropdown, int selection);
        int getInitialSelection(DropdownMinigamesButton dropdown);
    }
}