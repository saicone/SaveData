package com.saicone.savedata.module.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ClientType {

    boolean isDependencyPresent();

    @NotNull
    String getName();

    @Nullable
    String getDependency();

    @Nullable
    Map<String, String> getRelocations();
}
