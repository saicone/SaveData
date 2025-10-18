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

import com.saicone.savedata.module.data.ClientType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum SqlType implements ClientType {

    MYSQL(
            true,
            "jdbc:mysql://{host}:{port}/{database}{flags}",
            "com{}mysql:mysql-connector-j:8.4.0",
            Map.of("com{}mysql{}cj{}jdbc{}Driver", "com.mysql.cj.jdbc.Driver",
                    "com{}mysql{}jdbc{}Driver", "com.mysql.jdbc.Driver")
    ),
    MARIADB(
            true,
            "jdbc:mariadb://{host}:{port}/{database}{flags}",
            "org{}mariadb{}jdbc:mariadb-java-client:3.5.6",
            Map.of("org{}mariadb{}jdbc{}Driver", "org.mariadb.jdbc.Driver")
    ),
    POSTGRESQL(
            true,
            "jdbc:mariadb://{host}:{port}/{database}{flags}",
            "org{}postgresql:postgresql:42.7.8",
            Map.of("org{}postgresql{}Driver", "org.postgresql.Driver")
    ),
    H2(
            false,
            "jdbc:h2:./{path}",
            "com{}h2database:h2:2.4.240",
            Map.of("org{}h2{}Driver", "org.h2.Driver")
    ),
    SQLITE(
            false,
            "jdbc:sqlite:{path}.db",
            "org{}xerial:sqlite-jdbc:3.50.3.0",
            Map.of("org{}sqlite{}JDBC", "org.sqlite.JDBC")
    );

    public static final SqlType[] VALUES = values();

    private final boolean external;
    private final String format;
    private final String dependency;
    private final Map<String, String> relocations;

    SqlType(boolean external, @NotNull String format, @NotNull String dependency, @NotNull Map<String, String> relocations) {
        this.external = external;
        this.format = format;
        this.dependency = dependency.replace("{}", ".");
        this.relocations = new HashMap<>();
        relocations.forEach((key, value) -> this.relocations.put(key.replace("{}", "."), value));
    }

    public boolean isExternal() {
        return external;
    }

    @Override
    public boolean isDependencyPresent() {
        for (Map.Entry<String, String> entry : relocations.entrySet()) {
            try {
                Class.forName(entry.getValue());
                return true;
            } catch (ClassNotFoundException ignored) { }
        }
        return false;
    }

    @Override
    public @NotNull String getName() {
        return name();
    }

    @NotNull
    public String getFormat() {
        return format;
    }

    @Nullable
    @Override
    public String getDependency() {
        return dependency;
    }

    @Nullable
    @Override
    public Map<String, String> getRelocations() {
        return relocations;
    }

    @NotNull
    public Set<String> getDrivers() {
        return relocations.keySet();
    }

    @NotNull
    public String getDriver() {
        for (Map.Entry<String, String> entry : relocations.entrySet()) {
            try {
                Class.forName(entry.getValue());
                return entry.getValue();
            } catch (ClassNotFoundException ignored) { }
        }
        throw new RuntimeException("Cannot find driver class name for sql type: " + name());
    }

    @NotNull
    public String getUrl(@NotNull String host, int port, @NotNull String database, @NotNull String... flags) {
        String url = format.replace("{host}", host).replace("{port}", String.valueOf(port)).replace("{database}", database);
        if (flags.length < 1) {
            return url.replace("{flags}", "");
        } else {
            return url.replace("{flags}", "?" + String.join("&", flags));
        }
    }

    @NotNull
    public String getUrl(@NotNull String path) {
        return format.replace("{path}", path);
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static SqlType of(@NotNull String name, @Nullable SqlType def) {
        for (SqlType value : VALUES) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return def;
    }
}
