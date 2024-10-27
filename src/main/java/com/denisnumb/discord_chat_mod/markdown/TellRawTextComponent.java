package com.denisnumb.discord_chat_mod.markdown;

import com.google.gson.annotations.SerializedName;

public class TellRawTextComponent{
    @SerializedName("text")
    String text;
    @SerializedName("bold")
    Boolean bold;
    @SerializedName("italic")
    Boolean italic;
    @SerializedName("strikethrough")
    Boolean strikethrough;
    @SerializedName("underlined")
    Boolean underlined;
    @SerializedName("obfuscated")
    Boolean obfuscated;
    @SerializedName("color")
    String color;
    @SerializedName("hoverEvent")
    TellRawTextComponentEvent hoverEvent;
    @SerializedName("clickEvent")
    TellRawTextComponentEvent clickEvent;

    public TellRawTextComponent(String text){
        this.text = text;
    }

    public TellRawTextComponent setBold(){
        bold = true;
        return this;
    }

    public TellRawTextComponent setItalic(){
        italic = true;
        return this;
    }

    public TellRawTextComponent setStrikethrough(){
        strikethrough = true;
        return this;
    }

    public TellRawTextComponent setUnderlined(){
        underlined = true;
        return this;
    }

    public TellRawTextComponent setObfuscated(){
        obfuscated = true;
        return this;
    }

    public TellRawTextComponent setColor(String color){
        this.color = color;
        return this;
    }

    public TellRawTextComponent addHoverEvent(TellRawTextComponentEvent hoverEvent){
        this.hoverEvent = hoverEvent;
        return this;
    }

    public TellRawTextComponent addClickEvent(TellRawTextComponentEvent clickEvent){
        this.clickEvent = clickEvent;
        return this;
    }
}
