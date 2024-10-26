package com.saicone.savedata.core.command;

import java.util.List;

public interface MainCommand {

    List<String> TYPE = List.of(
            "reload",
            "player",
            "global",
            "server"
    );

    List<String> OPERATOR = List.of(
            "get",
            "contains",
            "delete",
            "set",
            "add",
            "substract",
            "multiply",
            "divide"
    );
}
