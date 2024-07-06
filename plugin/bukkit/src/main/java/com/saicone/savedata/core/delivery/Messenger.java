package com.saicone.savedata.core.delivery;

import com.saicone.savedata.SaveData;
import com.saicone.savedata.module.delivery.AbstractMessenger;
import com.saicone.savedata.module.delivery.DeliveryClient;
import org.jetbrains.annotations.NotNull;

public class Messenger extends AbstractMessenger {

    public Messenger(@NotNull DeliveryClient deliveryClient) {
        this.deliveryClient = deliveryClient;
    }

    @Override
    protected @NotNull DeliveryClient loadDeliveryClient() {
        return deliveryClient;
    }

    @Override
    protected void log(int level, @NotNull String msg) {
        SaveData.log(level, msg);
    }
}
