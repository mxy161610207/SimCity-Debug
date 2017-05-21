package nju.xiaofanli.util;


import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.consistency.context.Rule;

import java.io.BufferedWriter;
import java.util.*;

public class Counter {
    private static boolean print2console = false;
    private static String file = "log"+System.currentTimeMillis()+".txt";
    private static int totalCtx = 0, relocations = 0, successfulRelocations = 0, fixedErrors = 0, completedDelivTasks = 0;
    private static int stop2stop = 0, enter2enter = 0, enter2stop = 0;
    private static long startTime = 0, suspendTime = 0;
    private static Set<Context> inconCtxs = new HashSet<>();
    private static Map<Rule, int[]> ruleEvals = new HashMap<>(), ruleViols = new HashMap<>();

    private static BufferedWriter bw;
    static {
//        try {
//            bw = new BufferedWriter(new FileWriter(file));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static void increaseStop2Stop() {
        stop2stop++;
        log();
    }

    public static void increaseEnter2Stop() {
        enter2stop++;
        log();
    }

    public static void increaseEnter2Enter() {
        enter2enter++;
        log();
    }

    public static void increaseCompletedDelivTask() {
        completedDelivTasks++;
        log();
    }

    public static void increaseRuleEvals(Rule rule) {
        if (!ruleEvals.containsKey(rule))
            ruleEvals.put(rule, new int[]{1});
        else
            ruleEvals.get(rule)[0]++;
        log();
    }

    public static void increaseRuleviols(Rule rule) {
        if (!ruleViols.containsKey(rule))
            ruleViols.put(rule, new int[]{1});
        else
            ruleViols.get(rule)[0]++;
        log();
    }

    public static void increaseCtx() {
        totalCtx++;
        log();
    }

    public static void addInconCtx(Collection<Context> ctxs) {
        inconCtxs.addAll(ctxs);
        log();
    }

    public static void increaseRelocation() {
        relocations++;
        log();
    }

    public static void increaseSuccessfulRelocation() {
        successfulRelocations++;
        log();
    }

    public static void increaseFixedError() {
        fixedErrors++;
        log();
    }

    public static void startTimer() {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        else if (suspendTime != 0) {
            startTime += System.currentTimeMillis() - suspendTime;
            suspendTime = 0;
            log();
        }
    }

    public static void stopTimer() {
        if (suspendTime == 0) {
            suspendTime = System.currentTimeMillis();
            log();
        }
    }

    private static void log() {
        if (!print2console)
            return;
        StringBuilder sb = new StringBuilder("\n");
        long totalTime = startTime == 0 ? 0 : System.currentTimeMillis() - startTime;
        sb.append("\nTotalTime: ").append(totalTime);
        sb.append("\nTotalContexts: ").append(totalCtx).append("\tInconsistentContexts: ").append(inconCtxs.size()).append(" (plus FN in Relocations)");
        sb.append("\nFixedErrors: ").append(fixedErrors).append("\tTotalErrors: ").append(fixedErrors).append(" (plus FN in Relocations)");
        for (Map.Entry<Rule, int[]> entry : ruleEvals.entrySet()) {
            Rule rule = entry.getKey();
            int evals = entry.getValue()[0];
            int viols = ruleViols.containsKey(rule) ? ruleViols.get(rule)[0] : 0;
            double vioRate = viols <= 0 ? 0 : ((double) viols)/evals;
            sb.append("\nRule: ").append(rule.getName()).append("\tEvals: ").append(evals).append("\tviols: ").append(viols).append("\tViolRate: ").append(vioRate);
        }
        double rate = successfulRelocations <= 0 ? 0 : ((double) successfulRelocations)/relocations;
        sb.append("\nRelocations: ").append(relocations).append("\tSuccessfulRelocations: ").append(successfulRelocations).append("\tsuccessfulRate: ").append(rate);
        long avgTime = completedDelivTasks <= 0 ? 0 : totalTime/completedDelivTasks;
        sb.append("\nCompletedDelivTasks: ").append(completedDelivTasks).append("\tAVGTime: ").append(avgTime);
        sb.append("\nScheduleStop2Stop: ").append(stop2stop).append("\tScheduleEnter2Stop: ").append(enter2stop).append("\tScheduleEnter2Enter: ").append(enter2enter);
        //        try {
//            bw.write(sb.toString());
//            bw.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        System.out.println(sb.toString());
    }
}
