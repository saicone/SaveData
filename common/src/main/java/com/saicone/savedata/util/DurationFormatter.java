/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.saicone.savedata.util;

import com.saicone.savedata.SaveData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// Copied from https://github.com/LuckPerms/placeholders/blob/master/common/src/main/java/me/lucko/luckperms/placeholders/DurationFormatter.java
// Uses String instead of Chat Components

/**
 * Formats durations to a readable form
 */
public class DurationFormatter {
    public static final DurationFormatter LONG = new DurationFormatter(false);
    public static final DurationFormatter CONCISE = new DurationFormatter(true);
    public static final DurationFormatter CONCISE_LOW_ACCURACY = new DurationFormatter(true, 3);

    private static final ChronoUnit[] UNITS = new ChronoUnit[]{
            ChronoUnit.YEARS,
            ChronoUnit.MONTHS,
            ChronoUnit.WEEKS,
            ChronoUnit.DAYS,
            ChronoUnit.HOURS,
            ChronoUnit.MINUTES,
            ChronoUnit.SECONDS
    };

    private final boolean concise;
    private final int accuracy;

    public DurationFormatter(boolean concise) {
        this(concise, Integer.MAX_VALUE);
    }

    public DurationFormatter(boolean concise, int accuracy) {
        this.concise = concise;
        this.accuracy = accuracy;
    }

    public static long format(@NotNull String duration, @NotNull TimeUnit timeUnit) {
        long time = 0L;
        for (String s : duration.toUpperCase().split(" AND |, ")) {
            final String[] split = s.split(" ", 2);
            try {
                if (split.length < 2) {
                    if (split.length == 1) {
                        time += Long.parseLong(split[0]);
                    }
                    continue;
                }
                String unit = split[1];
                if (!unit.endsWith("S")) {
                    unit = unit + "S";
                }
                time += TimeUnit.valueOf(unit).convert(Long.parseLong(split[0]), timeUnit);
            } catch (Throwable t) {
                throw new IllegalArgumentException("The duration '" + duration + "' cannot be parsed as " + timeUnit.name());
            }
        }
        return time;
    }

    @NotNull
    public static String format(@Nullable Object language, @NotNull Duration duration, @NotNull String type) {
        switch (type.toLowerCase()) {
            case "time_long":
            case "long":
                return LONG.format(language, duration);
            case "time":
            case "time_concise":
            case "concise":
                return CONCISE.format(language, duration);
            case "time_concise_low_accuracy":
            case "concise_low_accuracy":
                return CONCISE_LOW_ACCURACY.format(language, duration);
        }
        return CONCISE.format(language, duration);
    }

    /**
     * Formats {@code duration} as a string.
     *
     * @param duration the duration
     * @return the formatted string
     */
    @NotNull
    public String format(@NotNull Duration duration) {
        return format(null, duration);
    }

    /**
     * Formats {@code duration} as a string.
     *
     * @param language the texts language
     * @param duration the duration
     * @return the formatted string
     */
    @NotNull
    public String format(@Nullable Object language, @NotNull Duration duration) {
        long seconds = duration.getSeconds();
        StringBuilder builder = new StringBuilder();
        int outputSize = 0;

        for (ChronoUnit unit : UNITS) {
            long n = seconds / unit.getDuration().getSeconds();
            if (n > 0) {
                seconds -= unit.getDuration().getSeconds() * n;
                if (outputSize != 0) {
                    builder.append(' ');
                }
                builder.append(formatPart(language, n, unit));
                outputSize++;
            }
            if (seconds <= 0 || outputSize >= this.accuracy) {
                break;
            }
        }

        if (outputSize == 0) {
            return formatPart(language, 0, ChronoUnit.SECONDS);
        }
        return builder.toString();
    }

    @NotNull
    private String formatPart(@Nullable Object language, long amount, @NotNull ChronoUnit unit) {
        final String format = this.concise ? "short" : amount == 1 ? "singular" : "plural";
        final String translationKey = "duration.unit." + unit.name().toLowerCase(Locale.ROOT) + "." + format;
        return amount + SaveData.get().getLang().getDisplay(language, translationKey).getText().getAsString().getValue();
    }

}
