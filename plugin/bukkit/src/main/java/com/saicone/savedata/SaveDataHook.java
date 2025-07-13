package com.saicone.savedata;

import com.saicone.mcode.env.Awake;
import com.saicone.mcode.env.Executes;
import com.saicone.savedata.module.hook.PlayerProvider;

public class SaveDataHook {

    @Awake(when = {Executes.LOAD, Executes.RELOAD})
    public static void loadPlayerProvider() {
        PlayerProvider.compute(SaveData.settings().getIgnoreCase("plugin", "playerprovider").asString("AUTO"));
    }
}
