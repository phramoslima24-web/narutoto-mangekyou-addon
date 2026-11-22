package com.narutotoaddon.mangekyou;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** Agendador simples (lado servidor): roda tarefas daqui a N ticks. Limpo ao parar o servidor. */
public final class SusanooTasks {
    private SusanooTasks() {}

    private static final class Task {
        int left;
        final Runnable run;
        Task(int left, Runnable run) { this.left = left; this.run = run; }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> INCOMING = new ArrayList<>();

    public static void later(int ticks, Runnable r) {
        synchronized (INCOMING) {
            INCOMING.add(new Task(Math.max(0, ticks), r));
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        synchronized (INCOMING) {
            if (!INCOMING.isEmpty()) {
                TASKS.addAll(INCOMING);
                INCOMING.clear();
            }
        }
        Iterator<Task> it = TASKS.iterator();
        List<Runnable> due = new ArrayList<>();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.left-- <= 0) {
                due.add(t.run);
                it.remove();
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (Throwable t) {
                System.err.println("[narutoto_mangekyou] tarefa de Susanoo falhou: " + t);
            }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        TASKS.clear();
        synchronized (INCOMING) {
            INCOMING.clear();
        }
    }
}
