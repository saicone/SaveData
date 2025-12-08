package com.saicone.savedata.module.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class BukkitCommand {

    private static final CommandMap COMMAND_MAP;
    private static final MethodHandle COMMANDS;

    private static final boolean TIMINGS_REGISTRABLE;

    static {
        CommandMap commandMap = null;
        MethodHandle commands = null;
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            commandMap = (CommandMap) f.get(Bukkit.getServer());

            Class<?> c = commandMap.getClass();
            if (c.getSimpleName().equals("CraftCommandMap")) {
                c = c.getSuperclass();
            }

            f = c.getDeclaredField("knownCommands");
            f.setAccessible(true);
            commands = MethodHandles.lookup().unreflectGetter(f);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        COMMAND_MAP = commandMap;
        COMMANDS = commands;

        boolean timingsRegistrable = false;
        try {
            final Field field = Command.class.getDeclaredField("timings");
            timingsRegistrable = field.getType().getName().equals("co.aikar.timings.Timing");
        } catch (Throwable ignored) { }
        TIMINGS_REGISTRABLE = timingsRegistrable;
    }

    BukkitCommand() {
    }

    @NotNull
    public static CommandMap map() {
        return COMMAND_MAP;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static Map<String, Command> all() {
        try {
            return (Map<String, Command>) COMMANDS.invoke(COMMAND_MAP);
        } catch (Throwable t) {
            throw new RuntimeException("Cannot get known commands from Bukkit CommandMap", t);
        }
    }

    public static boolean register(@NotNull Plugin plugin, @NotNull Command command) {
        final Map<String, Command> map = all();
        map.put(command.getName(), command);
        for (String alias : command.getAliases()) {
            map.put(alias, command);
        }
        if (TIMINGS_REGISTRABLE) {
            try {
                final Class<?> manager = Class.forName("co.aikar.timings.TimingsManager");
                final Method method = manager.getDeclaredMethod("getCommandTiming", String.class, Command.class);
                final Object timings = method.invoke(null, plugin.getName().toLowerCase(), command);
                Command.class.getDeclaredField("timings").set(command, timings);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return command.register(COMMAND_MAP);
    }

    public static boolean unregister(@NotNull Command command) {
        final Map<String, Command> map = all();
        Command cmd = map.get(command.getName());
        if (cmd == command) {
            map.remove(command.getName());
        }
        for (String alias : command.getAliases()) {
            cmd = map.get(alias);
            if (cmd == command) {
                map.remove(alias);
            }
        }
        return command.unregister(COMMAND_MAP);
    }
}
