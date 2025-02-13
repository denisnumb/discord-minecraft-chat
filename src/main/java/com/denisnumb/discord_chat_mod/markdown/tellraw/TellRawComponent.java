package com.denisnumb.discord_chat_mod.markdown.tellraw;

import com.google.gson.annotations.SerializedName;

public class TellRawComponent {
    @SerializedName("text")
    public String text;
    @SerializedName("bold")
    public Boolean bold;
    @SerializedName("italic")
    public Boolean italic;
    @SerializedName("strikethrough")
    public Boolean strikethrough;
    @SerializedName("underlined")
    public Boolean underlined;
    @SerializedName("obfuscated")
    public Boolean obfuscated;
    @SerializedName("color")
    public String color;
    @SerializedName("hoverEvent")
    public TellRawComponentEvent hoverEvent;
    @SerializedName("clickEvent")
    public TellRawComponentEvent clickEvent;

    public TellRawComponent(String text){
        this.text = text;
    }

    public TellRawComponent setBold(){
        bold = true;
        return this;
    }

    public TellRawComponent setItalic(){
        italic = true;
        return this;
    }

    public TellRawComponent setStrikethrough(){
        strikethrough = true;
        return this;
    }

    public TellRawComponent setUnderlined(){
        underlined = true;
        return this;
    }

    public TellRawComponent setObfuscated(){
        obfuscated = true;
        return this;
    }

    public TellRawComponent setColor(String color){
        this.color = color;
        return this;
    }

    public TellRawComponent addHoverEvent(TellRawComponentEvent hoverEvent){
        this.hoverEvent = hoverEvent;
        return this;
    }

    public TellRawComponent addClickEvent(TellRawComponentEvent clickEvent){
        this.clickEvent = clickEvent;
        return this;
    }
}
