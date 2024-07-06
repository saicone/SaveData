/*
 * This file is part of mcode, licensed under the MIT License
 *
 * Copyright (c) Rubenicos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.saicone.savedata.util;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to handle Strings with different Minecraft options, like center text or colorize.<br>
 * So legacy, RGB and special colors are supported like 'rainbow' and 'gradient'.
 * <br>
 * <br>
 * <h2>Minecraft chat text</h2>
 * The default Minecraft font length is counted by the number of pixels that chars are using, the in-game
 * chat by default has a maximum width of 300px, so it only can display a maximum number of words per line
 * depending on total px length, this may be different on Bedrock clients that adjust font size to allow more
 * words per line. Take in count that legacy and RGB colors are ignored in text length count.
 *
 * @author Rubenicos
 */
public class MStrings {

    /**
     * Use or not Bungeecord HEX color format.
     */
    public static boolean BUNGEE_HEX = true;
    /**
     * Default Minecraft chat width, this may be different on client-side
     */
    public static final int CHAT_WIDTH = 300;
    /**
     * Unmodifiable map of character pixel-length depending on Minecraft default font.
     */
    public static final Map<Character, Integer> FONT_LENGTH;
    /**
     * Minecraft color character.
     */
    public static final char COLOR_CHAR = '\u00a7';
    /**
     * Unmodifiable set of Minecraft legacy color codes.
     */
    public static final Set<Character> COLOR_CODES = Set.of(
            // Dark colors
            '0', '1', '2', '3', '4', '5', '6', '7',
            // Light colors
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
            // Other
            'k', 'l', 'm', 'n', 'o', 'r',
            // Uppercase
            'A', 'B', 'C', 'D', 'E', 'F', 'K', 'L', 'M', 'N', 'O', 'R'
    );
    /**
     * Unmodifiable set of special color types supported by this class.
     */
    public static final Set<String> COLOR_SPECIAL = Set.of("rainbow", "r", "lgbt", "gradient", "g");
    /**
     * Unmodifiable set of forms to write loop option on special color parser.
     */
    public static final Set<String> COLOR_SPECIAL_LOOP = Set.of("looping", "loop", "l");
    /**
     * Text detection to stop the text colorization from special colors.
     */
    public static final String COLOR_SPECIAL_STOP = "$stop$";

    static {
        Map<Character, Integer> map = new HashMap<>();
        Arrays.asList('i', 'l', '!', ':', ';', '\'', '|', '.', ',').forEach(c -> map.put(c, 1));
        map.put('`', 2);
        Arrays.asList('I', '[', ']', '"', ' ').forEach(c -> map.put(c, 3));
        Arrays.asList('f', 'k', 't', '(', ')', '{', '}', '<', '>').forEach(c -> map.put(c, 4));
        map.put('@', 6);
        FONT_LENGTH = Collections.unmodifiableMap(map);
    }

    MStrings() {
    }

    public static int getChatWidth() {
        return CHAT_WIDTH;
    }

    public static int getFontLength(char c) {
        return FONT_LENGTH.getOrDefault(c, 5);
    }

    public static int getFontLength(@NotNull String s) {
        return getFontLength(s, '&');
    }

    public static int getFontLength(@NotNull String s, char colorChar) {
        int px = 0;
        boolean bold = false;
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            // Verify color char
            final boolean mcChar;
            if (i + 1 < chars.length && ((mcChar = (c == COLOR_CHAR)) || c == colorChar)) {
                final char c1 = chars[i + 1];
                // Skip RGB color
                if (BUNGEE_HEX && c1 == 'x' && isHexFormat(chars, i + 2, 2, mcChar ? COLOR_CHAR : colorChar)) {
                    i = i + 12;
                } else if (c1 == '#' && isHexFormat(chars, i + 2, 1, mcChar ? COLOR_CHAR : colorChar)) {
                    i = i + 6;
                } else if (isColorCode(c1)) { // Skip legacy color code, so (un)mark text as bold depending on color char
                    switch (c1) {
                        case 'l':
                        case 'L':
                            bold = true;
                            break;
                        case 'r':
                        case 'R':
                            bold = false;
                            break;
                        default:
                            break;
                    }
                } else {
                    // Non-color character detected, so append pixel-length from verified characters
                    final int i1;
                    if (bold) {
                        if (c1 == ' ') {
                            // 2 px separator + 1 bold px for first char due the second is space
                            i1 = 3;
                        } else {
                            // 2 px separator + 2 bold px for both chars
                            i1 = 4;
                        }
                    } else {
                        // 2 px separator
                        i1 = 2;
                    }
                    px += getFontLength(c) + getFontLength(c1) + i1;
                }
                i++;
            } else {
                if (bold && c != ' ') {
                    px += getFontLength(c) + 2;
                } else {
                    px += getFontLength(c) + 1;
                }
            }
        }
        return px;
    }

    public static boolean isColorCode(char c) {
        return COLOR_CODES.contains(c);
    }

    public static boolean isColorType(char c) {
        return COLOR_CODES.contains(c) || c == '#' || c == '$';
    }

    public static boolean isAnyColorCode(char c) {
        return COLOR_CODES.contains(c) || (BUNGEE_HEX && c == 'x');
    }

    public static boolean isValidHex(@NotNull String s) {
        try {
            Integer.parseInt(s, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isHexFormat(char[] chars, int start, int sum, char colorChar) {
        final int max = start + 12;
        if (max > chars.length) {
            return false;
        }
        final StringBuilder builder = new StringBuilder();
        for (int i = start; i < max; i = i + sum) {
            if (chars[i] != colorChar) {
                return false;
            }
            builder.append(chars[i + 1]);
        }
        return isValidHex(builder.toString());
    }

    @NotNull
    public static List<String> justifyText(@NotNull Collection<String> collection) {
        return justifyText(collection, '&');
    }

    @NotNull
    public static List<String> justifyText(@NotNull Collection<String> collection, char colorChar) {
        int width = 0;
        for (String s : collection) {
            int i = getFontLength(s);
            if (i > width) {
                width = i;
            }
        }
        return justifyText(collection, width, colorChar);
    }

    @NotNull
    public static List<String> justifyText(@NotNull Collection<String> collection, int width) {
        return justifyText(collection, width, '&');
    }

    @NotNull
    public static List<String> justifyText(@NotNull Collection<String> collection, int width, char colorChar) {
        return justifyText(String.join(" ", collection), width, colorChar);
    }

    @NotNull
    public static List<String> justifyText(@NotNull String text, int width) {
        return justifyText(text, width, '&');
    }

    @NotNull
    public static List<String> justifyText(@NotNull String text, int width, char colorChar) {
        final List<String> list = new ArrayList<>();
        if (text.isBlank()) {
            return list;
        }
        final String[] words = text.split(" ");
        if (words.length < 2) {
            return list;
        }
        final List<String> pending = new ArrayList<>();
        int px = 0;
        for (int i = 0; i < words.length; i++) {
            final String word = words[i];
            if (word.isBlank()) {
                continue;
            }
            final int length = getFontLength(word, colorChar);
            if ((px + length + pending.size() * 4) >= width || i + 1 >= words.length) {
                if (px == 0) {
                    list.add(word);
                    continue;
                }
                final int size = pending.size() - 1;
                if (size == 0) {
                    list.add(pending.get(0));
                } else {
                    final StringBuilder builder = new StringBuilder();
                    int spaces = (width - px) / 4;
                    for (int i1 = 0; i1 < size; i1++) {
                        // Round up remaining spaces with current size
                        final int count = (spaces - 1) / (size - i1) + 1;
                        builder.append(pending.get(i1)).append(" ".repeat(count));
                        // Subtract appended spaces
                        spaces -= count;
                    }
                    builder.append(pending.get(size));
                    list.add(builder.toString());
                }
                px = 0;
                pending.clear();
            }
            px = px + length;
            pending.add(word);
        }
        return list;
    }

    @NotNull
    public static String centerText(@NotNull String text) {
        return centerText(text, CHAT_WIDTH);
    }

    @NotNull
    public static String centerText(@NotNull String text, int width) {
        return centerText(text, width, '&');
    }

    @NotNull
    public static String centerText(@NotNull String text, int width, char colorChar) {
        if (text.length() >= width) {
            return text;
        }
        return spacesToCenter(getFontLength(text, colorChar), width) + text;
    }

    @NotNull
    public static String spacesToCenter(int length, int width) {
        int px = width - length;
        // 3 px for space + 1 px separator between spaces
        if (px < 4) {
            return "";
        }
        int count = 0;
        while (px >= 4) {
            count++;
            px -= 4;
        }
        return " ".repeat(count);
    }

    @NotNull
    public static List<String> color(@NotNull List<String> list) {
        final List<String> finalList = new ArrayList<>();
        for (String s : list) {
            finalList.add(color(s));
        }
        return finalList;
    }

    @NotNull
    public static String color(@NotNull String s) {
        return color('&', s);
    }

    @NotNull
    public static String color(char colorChar, @NotNull String s) {
        if (s.indexOf(colorChar) < 0) {
            return s;
        }
        final StringBuilder builder = new StringBuilder();
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == colorChar && i + 1 < chars.length) {
                final char colorType = chars[i + 1];

                if (isColorCode(colorType)) { // Legacy color
                    builder.append(COLOR_CHAR).append(colorType);
                    i++;
                    continue;
                }

                final int total;
                if (colorType == '#') { // Hex / RGB
                    total = colorHex(colorChar, builder, chars, i);
                } else if (colorType == '$') { // Special type
                    total = colorSpecial(colorChar, s, builder, chars, i);
                } else {
                    builder.append(chars[i]);
                    continue;
                }

                if (total > 0) {
                    i += total;
                    continue;
                }
            }
            builder.append(chars[i]);
        }
        return builder.toString();
    }

    private static int colorHex(char colorChar, @NotNull StringBuilder builder, char[] chars, int i) {
        if (i + 7 >= chars.length) {
            return 0;
        }
        StringBuilder color = new StringBuilder();
        for (int c = i + 2; c < chars.length && color.length() < 6; c++) {
            color.append(chars[c]);
        }
        if (color.length() == 6) {
            // Format: <char>#RRGGBB
            builder.append(toRgb(colorChar, color.toString()));
            return color.length() + 1;
        } else {
            return 0;
        }
    }

    private static int colorSpecial(char colorChar, @NotNull String s, @NotNull StringBuilder builder, char[] chars, int i) {
        if (i + 3 >= chars.length) {
            return 0;
        }

        // Find inside: <char>$<block>$
        int blockIndex = -1;
        for (int i1 = i + 3; i1 < chars.length; i1++) {
            if (chars[i1] == '$') {
                blockIndex = i1;
                break;
            }
        }
        if (blockIndex < 0 || blockIndex + 1 >= s.length()) {
            return 0;
        }

        // Block: <special color>:option1:option2:option3...
        final String[] block = s.substring(i + 2, blockIndex).split(":");
        if (block.length < 1) {
            return 0;
        }
        // Special color: <type>[#speed]
        final int speed;
        final int speedIndex = block[0].indexOf('#');
        if (speedIndex > 1) {
            speed = intValue(block[0].substring(speedIndex + 1), 0);
            block[0] = block[0].substring(0, speedIndex);
        } else {
            speed = 0;
        }
        // Verify if special color type exists
        if (!COLOR_SPECIAL.contains(block[0].toLowerCase())) {
            // Verify if special color is equal to "$stop$"
            if (block[0].equalsIgnoreCase("stop")) {
                return COLOR_SPECIAL_STOP.length() + 1;
            }
            return 0;
        }

        // Text after special color declaration
        String text = s.substring(blockIndex + 1);

        // Find stop declaration
        final String stopText = colorChar + COLOR_SPECIAL_STOP;
        int stopIndex;
        final int finalInt;
        if ((stopIndex = text.toLowerCase().indexOf(stopText)) >= 0) {
            // Stop after found: <char>$stop$
            text = color(colorChar, text.substring(0, stopIndex)); // Colorize text inside
            finalInt = stopIndex + stopText.length();
        } else if ((stopIndex = text.indexOf(colorChar)) >= 0 && stopIndex + 1 < text.length() && isColorType(text.charAt(stopIndex + 1))) {
            // Stop after found: <char><any type of color>
            text = text.substring(0, stopIndex);
            finalInt = stopIndex;
        } else {
            finalInt = text.length();
        }

        builder.append(toSpecial(text, speed, block));

        return finalInt + (blockIndex - i) + 1;
    }

    @NotNull
    public static String toRgb(@NotNull String color) {
        return toRgb('&', color);
    }

    @NotNull
    public static String toRgb(char colorChar, @NotNull String color) {
        if (!isValidHex(color)) {
            return colorChar + "#" + color;
        }

        if (BUNGEE_HEX) {
            final StringBuilder hex = new StringBuilder(COLOR_CHAR + "x");
            for (char c : color.toCharArray()) {
                hex.append(COLOR_CHAR).append(c);
            }
            return hex.toString();
        } else {
            return COLOR_CHAR + '#' + color;
        }
    }

    @NotNull
    public static String toRgb(@NotNull Color color) {
        if (BUNGEE_HEX) {
            final StringBuilder hex = new StringBuilder(COLOR_CHAR + "x");
            for (char c : String.format("%08x", color.getRGB()).substring(2).toCharArray()) {
                hex.append(COLOR_CHAR).append(c);
            }
            return hex.toString();
        } else {
            return COLOR_CHAR + '#' + String.format("%08x", color.getRGB()).substring(2);
        }
    }

    @NotNull
    public static String toSpecial(@NotNull String text, @NotNull String... args) {
        return toSpecial(text, 0, args);
    }

    @NotNull
    public static String toSpecial(@NotNull String text, int speed, @NotNull String... args) {
        if (args.length < 1) {
            return text;
        }
        switch (args[0].toLowerCase()) {
            case "rainbow":
            case "r":
            case "lgbt":
            case "lgtv":
                return toRainbow(text, speed, args);
            case "gradient":
            case "g":
                return toGradient(text, speed, args);
            default:
                return text;
        }
    }

    @NotNull
    public static String toRainbow(@NotNull String text, int speed, @NotNull String... args) {
        // Argument base objects
        final float saturation = args.length > 1 ? floatValue(args[1], 1.0F) : 1.0F;
        final float brightness = args.length > 2 ? floatValue(args[2], 1.0F) : 1.0F;
        final boolean looping = args.length > 1 && COLOR_SPECIAL_LOOP.contains(args[args.length - 1].toLowerCase());

        // Text base objects
        int length = text.length();
        final char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == COLOR_CHAR && isAnyColorCode(chars[i + 1])) {
                length -= 2;
            }
        }
        final int totalColors = Math.max(looping ? Math.min(length, 30) : length, 1);
        final float hueStep = 1.0F / totalColors;

        float hue = speed != 0 ? (float) ((((Math.floor(System.currentTimeMillis() / 50.0)) / 360) * speed) % 1) : 0;

        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            if (c == COLOR_CHAR && i + 1 < chars.length) {
                final char c1 = chars[i + 1];
                if (isAnyColorCode(c1)) {
                    i++;
                    builder.append(c).append(c1);
                    continue;
                }
            }
            builder.append(toRgb(Color.getHSBColor(hue, saturation, brightness))).append(c);
            hue += hueStep;
        }

        return builder.toString();
    }

    @NotNull
    public static String toGradient(@NotNull String text, int speed, @NotNull String... args) {
        if (args.length < 3) {
            return text;
        }

        // Argument base values
        final boolean looping = COLOR_SPECIAL_LOOP.contains(args[args.length - 1].toLowerCase());
        final List<Color> colors = Arrays.stream(args)
                .filter(MStrings::isValidHex)
                .map(s -> "#" + s)
                .map(Color::decode)
                .collect(Collectors.toList());

        if (colors.isEmpty()) {
            return text;
        }
        if (colors.size() < 2) {
            return toRgb(colors.get(0)) + text;
        }

        // Text base objects
        int length = text.length();
        final char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == COLOR_CHAR && isAnyColorCode(chars[i + 1])) {
                length -= 2;
            }
        }
        final int totalSteps = (looping ? Math.min(length, 30) : length) - 1;

        long hexStep = speed != 0 ? System.currentTimeMillis() / speed : 0;
        final int roundSize = (colors.size() - 1) / 2 + 1;
        final float segment = (float) totalSteps / roundSize;
        final float increment = (float) totalSteps / (colors.size() - 1);

        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            if (c == COLOR_CHAR && i + 1 < chars.length) {
                final char c1 = chars[i + 1];
                if (isAnyColorCode(c1)) {
                    i++;
                    builder.append(c).append(c1);
                    continue;
                }
            }
            // Formula taken from RoseColors and created by BomBardyGamer
            // Return the absolute rounded value of "2 * ASIN(SIN(hexStep * (PI / (2 * totalSteps))) / PI) * totalSteps"
            final int adjustedStep = (int) Math.round(Math.abs(((2 * Math.asin(Math.sin(hexStep * (Math.PI / (2 * totalSteps))))) / Math.PI) * totalSteps));

            final int index = (int) Math.min(colors.size() - 2, Math.min(Math.floor(adjustedStep / segment), roundSize - 1) * 2);

            final float lowerRange = increment * index;
            final float range = increment * (index + 1) - lowerRange;

            final Color fromColor = colors.get(index);
            final Color toColor = colors.get(index + 1);

            final Color finalColor = new Color(
                    calculateHexPiece(range, lowerRange, adjustedStep, fromColor.getRed(), toColor.getRed()),
                    calculateHexPiece(range, lowerRange, adjustedStep, fromColor.getGreen(), toColor.getGreen()),
                    calculateHexPiece(range, lowerRange, adjustedStep, fromColor.getBlue(), toColor.getBlue())
            );

            builder.append(toRgb(finalColor)).append(c);
            hexStep++;
        }

        return builder.toString();
    }

    private static int intValue(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static float floatValue(String s, float def) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // Taken from RoseColors
    private static int calculateHexPiece(float range, float lowerRange, int step, int fromChannel, int toChannel) {
        final float interval = (toChannel - fromChannel) / range;
        return Math.round(interval * (step - lowerRange) + fromChannel);
    }
}
