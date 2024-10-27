/*
 * This file is part of PixelBuy, licensed under the MIT License
 *
 * Copyright (c) 2024 Rubenicos
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
package com.saicone.savedata.module.data.sql;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class SqlSchema {

    private final SqlType defaultType;

    private boolean loaded;
    private final Map<SqlType, Map<String, List<String>>> queries = new HashMap<>();

    public SqlSchema(@NotNull SqlType defaultType) {
        this.defaultType = defaultType;
    }

    @NotNull
    public SqlType getDefaultType() {
        return defaultType;
    }

    @NotNull
    public String get(@NotNull SqlType sql, @NotNull String type) {
        final Map<String, List<String>> map = queries.containsKey(sql) ? queries.get(sql) : queries.get(defaultType);
        return map.get(type).get(0);
    }

    @NotNull
    public String get(@NotNull SqlType sql, @NotNull String type, @NotNull String... replacements) {
        String query = get(sql, type);
        for (int i = 0; i < replacements.length; i = i + 2) {
            query = query.replace(replacements[i], replacements[i + 1]);
        }
        return query;
    }

    @NotNull
    public String getSelect(@NotNull SqlType sql, @NotNull String type, @NotNull List<String> columns, @NotNull String... replacements) {
        final String query = get(sql, type, replacements);
        if (columns.contains("*")) {
            return query.replace("{column_set}", "*");
        }
        final StringJoiner joiner = new StringJoiner(", ");
        if (sql == SqlType.POSTGRESQL) {
            for (String column : columns) {
                joiner.add('"' + column + '"');
            }
        } else {
            for (String column : columns) {
                joiner.add('`' + column + '`');
            }
        }
        return query.replace("{column_set}", joiner.toString());
    }

    @NotNull
    public String getUpdate(@NotNull SqlType sql, @NotNull String type, @NotNull List<String> columns, @NotNull String... replacements) {
        final String query = get(sql, type, replacements);
        final StringJoiner joiner = new StringJoiner(", ");
        if (sql == SqlType.POSTGRESQL) {
            for (String column : columns) {
                joiner.add('"' + column + "\" = ?");
            }
        } else {
            for (String column : columns) {
                joiner.add("`" + column + "` = ?");
            }
        }
        return query.replace("{column_set}", joiner.toString());
    }

    @NotNull
    public String getDelete(@NotNull SqlType sql, @NotNull String type, @NotNull List<String> columns, @NotNull String... replacements) {
        final String query = get(sql, type, replacements);
        final StringJoiner joiner = new StringJoiner(" AND ");
        if (sql == SqlType.POSTGRESQL) {
            for (String column : columns) {
                joiner.add('"' + column + "\" = ?");
            }
        } else {
            for (String column : columns) {
                joiner.add("`" + column + "` = ?");
            }
        }
        return query.replace("{column_set}", joiner.toString());
    }

    @NotNull
    public List<String> getList(@NotNull SqlType sql, @NotNull String type) {
        final Map<String, List<String>> map = queries.containsKey(sql) ? queries.get(sql) : queries.get(defaultType);
        return map.get(type);
    }

    @NotNull
    public List<String> getList(@NotNull SqlType sql, @NotNull String type, @NotNull String... replacements) {
        final Map<String, List<String>> map = queries.containsKey(sql) ? queries.get(sql) : queries.get(defaultType);
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            final List<String> list = new ArrayList<>();
            for (String s : entry.getValue()) {
                for (int i = 0; i < replacements.length; i = i + 2) {
                    list.add(s.replace(replacements[i], replacements[i + 1]));
                }
            }
            entry.setValue(list);
        }
        return map.get(type);
    }

    @NotNull
    public Map<SqlType, Map<String, List<String>>> getQueries() {
        return queries;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public void load(@NotNull String path) throws IOException {
        load(path, SqlSchema.class.getClassLoader());
    }

    public void load(@NotNull ClassLoader classLoader) throws IOException {
        load(SqlSchema.class.getPackageName().replace('.', '/'), classLoader);
    }

    public void load(@NotNull String path, @NotNull ClassLoader classLoader) throws IOException {
        for (SqlType type : SqlType.VALUES) {
            final InputStream in = classLoader.getResourceAsStream(path + '/' + type.name().toLowerCase() + ".sql");
            if (in != null) {
                load(type, new BufferedInputStream(in));
            }
        }
    }

    public void load(@NotNull SqlType type, @NotNull InputStream in) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(in)) {
            load(type, reader);
        }
    }

    public void load(@NotNull SqlType type, @NotNull Reader reader) throws IOException {
        String queryType = null;
        List<String> queries = new ArrayList<>();
        StringJoiner joiner = new StringJoiner(" ");
        try (BufferedReader bf = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader)) {
            String line;
            while ((line = bf.readLine()) != null) {
                line = line.trim();
                boolean comment = line.startsWith("#");
                if (comment || line.startsWith("--")) {
                    if (line.length() == 1 || line.substring(1).isBlank()) {
                        continue;
                    }
                    if (queryType != null) {
                        this.queries.computeIfAbsent(type, __ -> new HashMap<>()).put(queryType, queries);
                        queries = new ArrayList<>();
                        joiner = new StringJoiner(" ");
                    }
                    queryType = line.substring(comment ? 1 : 2).trim();
                    continue;
                }

                if (line.endsWith(";")) {
                    joiner.add(line.substring(0, line.length() - 1));
                    String query = joiner.toString();
                    if (!query.isBlank()) {
                        queries.add(query);
                    }
                    joiner = new StringJoiner(" ");
                } else {
                    joiner.add(line);
                }
            }

            if (queryType != null) {
                this.queries.computeIfAbsent(type, __ -> new HashMap<>()).put(queryType, queries);
            }
        }
        this.loaded = true;
    }
}
