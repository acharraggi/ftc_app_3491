package org.firstinspires.ftc.teamcode.roboticslibrary;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by FIXIT on 16-10-02.
 */
public final class TaskHandler {

    private static String TAG = "TaskHandler";

    private static ExecutorService exec;

    private static HashMap<String, Future> futures = new HashMap<>();

    public static void init() {
        exec = Executors.newCachedThreadPool();
    }//init

    /*
    ADD TASKS
     */

    //Note: tasks are named in this style: "{Name of Class Adding Task}.{TASK NAME IN UPPER CASE}"
    //e.g. Fermion.VEERCHECK or Fermion.WALLFOLLOW
    public static boolean addTask(String name, Runnable task) {
        if (!containsTask(name)) {
            futures.put(name, exec.submit(task));

            return true;
        }//if

        Log.e(TAG, "Attempted to add pre-existing task!");
        return false;
    }//addTask

    public static boolean addLoopedTask(String name, Runnable task) {

        return addLoopedTask(name, task, 0);

    }//addLoopedTask

    public static boolean addLoopedTask(String name, Runnable task, int delay) {


        return addTask(name, loop(name, task, delay));
    }//addLoopedTask

    public static boolean addCountedTask(String name, Runnable task, int count){


        return addTask(name, count(name, task, count));
    }//addCountedTask

    public static boolean addDelayedTask(String name, final Runnable task, final int delay) {
        Runnable delayed = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }//catch

                task.run();
            }//run
        };//delayed

        return addTask(name, delayed);
    }//addDelayedTask

    /*
    PAUSE & RESUME TASKS
    Note: at the moment, this only works for looped and counted tasks
     */
    public static boolean pauseTask(String name) {
        if (containsTask(name)) {

            return true;
        }//if

        return false;
    }//pauseTask

    public static boolean resumeTask(String name) {
        if (containsTask(name)) {
            return true;
        }//if

        return false;
    }//resumeTask

    /*
    SEARCH TASKS
     */

    public static boolean containsTask(String key){
        return futures.containsKey(key);
    }

    public static boolean containsTaskStartingWith(String prefix){
        for (Map.Entry<String, Future> entry : futures.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                return true;
            }//if
        }//for

        return false;
    }//containsTaskStartingWith

    /*
    REMOVE TASKS
     */

    public static boolean removeTask(String name) {
        if (futures.containsKey(name)) {
            futures.get(name).cancel(true);
            futures.remove(name);
        }//if

        return futures.containsKey(name);
    }//removeTask

    public static void removeAllTasksWith(String prefix) {

        for (Map.Entry<String, Future> entry : futures.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                entry.getValue().cancel(true);
            }//if
        }//for

    }//removeAllTasksWith

    public static void removeAllTasks() {
        Log.i(TAG, "removeAllTasks: ");
        for (Map.Entry<String, Future> future : futures.entrySet()) {
            future.getValue().cancel(true);
        }//for

        futures.clear();
    }//removeAllTasks

    /*
    RUNNABLE MODIFIERS
     */

    private static Runnable loop (final String name, final Runnable r, final int delay) {

        return new Runnable() {
            @Override
            public void run() {
                while (true) {

                    r.run();

                    try {
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }//if

                    } catch (InterruptedException e) {
                        break;
                    }//catch

                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }//if
                }//while
            }//run
        };

    }//loop

    private static Runnable count(final String pauseLockName, final Runnable r, final int count){
        return new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i < count; i++){

//                    synchronized (pauseLocks) {
//                        if (pauseLocks.get(pauseLockName)) {
//                            i--;
//                            continue;
//                        }//if
//                    }//synchronized

                    r.run();
                }//for
            }
        };
    }//count

}
