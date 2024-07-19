package com.saicone.savedata;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.function.Supplier;

public interface SaveDataPlugin {

    @NotNull
    Path getFolder();

    void log(int level, @NotNull Supplier<String> msg);

    void logException(int level, @NotNull Throwable throwable);

    void logException(int level, @NotNull Throwable throwable, @NotNull Supplier<String> msg);
}
