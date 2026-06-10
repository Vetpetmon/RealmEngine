package com.vetpetmon.realmengine.common;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused") // This is a LIBRARY, we will use these later.
public class DialogueBuilder {

    // Create Tellraw with dialogue
    public static String createDialogueTellraw(String characterName, String key) {
        // Create localized string for tellraw
        String localizedName = String.format("realmfall.dialogue.%s", characterName);
        String localizedString = String.format("realmfall.dialogue.%s.%s", characterName, key);
        MutableComponent name = Component.translatable(localizedName);
        MutableComponent text = Component.translatable(localizedString);
        return  "tellraw @a" + " " + buildNameplateNonLocale(name.getString()) +
                ",{\"text\":\"" + text.getString() + "\",\"color\":\"white\"}]";
    }

    // Create Tellraw with dialogue addressed to player
    public static String createDialogueTellraw(String characterName, String key, String playerName) {
        // Create localized string for tellraw
        String localizedName = String.format("realmfall.dialogue.%s", characterName);
        String localizedString = String.format("realmfall.dialogue.%s.%s", characterName, key);
        MutableComponent name = Component.translatable(localizedName);
        MutableComponent text = Component.translatable(localizedString);
        return  "tellraw " + playerName + " " + buildNameplateNonLocale(name.getString()) +
                ",{\"text\":\"" + text.getString() + "\",\"color\":\"white\"}]";
    }
    public static String createDialogueTellraw(String characterName, String key, String playerName, String color) {
        // Create localized string for tellraw
        String localizedName = String.format("realmfall.dialogue.%s", characterName);
        String localizedString = String.format("realmfall.dialogue.%s.%s", characterName, key);
        MutableComponent name = Component.translatable(localizedName);
        MutableComponent text = Component.translatable(localizedString);
        return  "tellraw " + playerName + " " + buildNameplateNonLocale(name.getString()) +
                ",{\"text\":\"" + text.getString() + "\",\"color\":\""+color+"\"}]";
    }
    // Create Tellraw with dialogue by using direct Component input instead of localization keys
    public static String createDialogueTellrawDirect(Component characterName, Component text, String color, Player player) {
        return  "tellraw " + player.getName().getString() + " " + buildNameplate(characterName) + "," + buildText(text, color) + "]";
    }

    // Build text from component
    public static String buildText(Component text, String color) {
        MutableComponent mutableText = text.copy();
        return "{\"text\":\"" + mutableText.getString() + "\",\"color\":\"" + color + "\"}";
    }


    // Build nameplate for character
    public static String buildNameplate(Component characterName) {
        MutableComponent name = characterName.copy();
        return "[{\"text\":\"<"+name.getString()+"> \",\"color\":\"white\"}";
    }
    public static String buildNameplate(String characterName) {
        String localizedName = String.format("realmfall.dialogue.%s", characterName);
        MutableComponent name = Component.translatable(localizedName);
        return "[{\"text\":\"<"+name.getString()+"> \",\"color\":\"white\"}";
    }
    public static String buildNameplateNonLocale(String characterName) {
        return "[{\"text\":\"<"+characterName+"> \",\"color\":\"white\"}";
    }

}
