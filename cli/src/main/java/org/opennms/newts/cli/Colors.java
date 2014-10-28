package org.opennms.newts.cli;


import java.util.Map;

import com.google.common.collect.Maps;


public class Colors {

    private static final Map<String, String> s_colors = Maps.newHashMap();

    static {
        s_colors.put("red", "31");
        s_colors.put("green", "32");
        s_colors.put("yellow", "33");
        s_colors.put("blue", "34");
        s_colors.put("magenta", "35");
        s_colors.put("cyan", "36");
        s_colors.put("white", "37");
    }

    private static String bold(String code) {
        return String.format("1;%s");
    }

    private static String colorize(Object text, String color, boolean bold) {
        return String.format("\033[%sm%s\033[0m", bold ? bold(s_colors.get(color)) : s_colors.get(color), text.toString());
    }

    public static String red(Object text, boolean bold) {
        return colorize(text, "red", bold);
    }

    public static String red(Object text) {
        return red(text, false);
    }

    public static String green(Object text, boolean bold) {
        return colorize(text, "green", bold);
    }

    public static String green(Object text) {
        return green(text, false);
    }

    public static String yellow(Object text, boolean bold) {
        return colorize(text, "yellow", bold);
    }

    public static String yellow(Object text) {
        return yellow(text, false);
    }

    public static String blue(Object text, boolean bold) {
        return colorize(text, "blue", bold);
    }

    public static String blue(Object text) {
        return blue(text, false);
    }

    public static String magenta(Object text, boolean bold) {
        return colorize(text, "magenta", bold);
    }

    public static String magenta(Object text) {
        return magenta(text, false);
    }

    public static String cyan(Object text, boolean bold) {
        return colorize(text, "cyan", bold);
    }

    public static String cyan(Object text) {
        return cyan(text, false);
    }

    public static String white(Object text, boolean bold) {
        return colorize(text, "white", bold);
    }

    public static String white(Object text) {
        return white(text, false);
    }

}
