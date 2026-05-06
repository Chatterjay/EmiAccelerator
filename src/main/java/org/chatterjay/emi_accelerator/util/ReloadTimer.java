package org.chatterjay.emi_accelerator.util;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ReloadTimer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path TIMINGS_FILE = Path.of("config", "emi-accelerator", "reload-timings.json");
    private static final int MAX_RECORDS = 20;

    private static long reloadStartTime;
    private static long stepStartTime;
    private static final List<StepRecord> currentSteps = new ArrayList<>();
    private static int reloadCount;
    private static long lastReloadDuration;

    // Phase profiling (decomposes sub-sections within a step)
    private static final Deque<String> phaseStack = new ArrayDeque<>();
    private static final Deque<Long> phaseStartStack = new ArrayDeque<>();
    private static final List<PhaseRecord> phaseRecords = new ArrayList<>();

    // Checkpoint profiling — records wall-clock time at named points
    private static final List<Checkpoint> checkpoints = new ArrayList<>();

    public static void startReload() {
        if (!ModConfig.diagnosticsEnabled) return;
        reloadStartTime = System.currentTimeMillis();
        stepStartTime = reloadStartTime;
        currentSteps.clear();
        phaseStack.clear();
        phaseStartStack.clear();
        phaseRecords.clear();
        checkpoints.clear();
    }

    public static void checkpoint(String name) {
        if (!ModConfig.diagnosticsEnabled || reloadStartTime == 0) return;
        checkpoints.add(new Checkpoint(name, System.currentTimeMillis()));
    }

    public static void onStep(String name) {
        if (!ModConfig.diagnosticsEnabled || reloadStartTime == 0) return;
        long now = System.currentTimeMillis();
        currentSteps.add(new StepRecord(name, now - stepStartTime));
        stepStartTime = now;
    }

    public static void finishReload(boolean cacheHit) {
        if (!ModConfig.diagnosticsEnabled || reloadStartTime == 0) return;
        lastReloadDuration = System.currentTimeMillis() - reloadStartTime;

        long now = System.currentTimeMillis();
        currentSteps.add(new StepRecord("complete", now - stepStartTime));

        // Collect remaining phase data
        while (!phaseStack.isEmpty()) {
            String name = phaseStack.pop();
            long start = phaseStartStack.pop();
            phaseRecords.add(new PhaseRecord(name, System.currentTimeMillis() - start));
        }

        var record = new TimingRecord(now, ++reloadCount, lastReloadDuration, new ArrayList<>(currentSteps), cacheHit, new ArrayList<>(phaseRecords), new ArrayList<>(checkpoints));
        phaseRecords.clear();
        checkpoints.clear();
        saveTimingRecord(record);

        LOGGER.debug("EMI reload completed in {}ms{}", lastReloadDuration, cacheHit ? " (cache hit)" : "");
    }

    public static void finishReload() {
        finishReload(EmiStackCache.wasCacheUsed());
    }

    public static long getLastReloadDuration() {
        return lastReloadDuration;
    }

    // Phase profiling: enter/exit a sub-section within the current step
    public static void enterPhase(String name) {
        if (!ModConfig.diagnosticsEnabled || reloadStartTime == 0) return;
        phaseStack.push(name);
        phaseStartStack.push(System.currentTimeMillis());
    }

    public static void exitPhase() {
        if (!ModConfig.diagnosticsEnabled || reloadStartTime == 0 || phaseStack.isEmpty()) return;
        String name = phaseStack.pop();
        long start = phaseStartStack.pop();
        phaseRecords.add(new PhaseRecord(name, System.currentTimeMillis() - start));
    }

    private static void saveTimingRecord(TimingRecord record) {
        try {
            List<TimingRecord> records = new ArrayList<>();

            if (Files.exists(TIMINGS_FILE)) {
                try (Reader reader = Files.newBufferedReader(TIMINGS_FILE)) {
                    JsonArray arr = GSON.fromJson(reader, JsonArray.class);
                    if (arr != null) {
                        for (JsonElement elem : arr) {
                            records.add(TimingRecord.fromJson(elem.getAsJsonObject()));
                        }
                    }
                }
            }

            records.add(record);
            if (records.size() > MAX_RECORDS) {
                records = records.subList(records.size() - MAX_RECORDS, records.size());
            }

            Files.createDirectories(TIMINGS_FILE.getParent());
            JsonArray arr = new JsonArray();
            for (var r : records) {
                arr.add(r.toJson());
            }
            try (Writer writer = Files.newBufferedWriter(TIMINGS_FILE)) {
                GSON.toJson(arr, writer);
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to save timing record", e);
        }
    }

    private record StepRecord(String name, long durationMs) {
        JsonObject toJson() {
            var obj = new JsonObject();
            obj.addProperty("name", name);
            obj.addProperty("duration_ms", durationMs);
            return obj;
        }

        static StepRecord fromJson(JsonObject obj) {
            return new StepRecord(obj.get("name").getAsString(), obj.get("duration_ms").getAsLong());
        }
    }

    private record PhaseRecord(String name, long durationMs) {
        JsonObject toJson() {
            var obj = new JsonObject();
            obj.addProperty("name", name);
            obj.addProperty("duration_ms", durationMs);
            return obj;
        }

        static PhaseRecord fromJson(JsonObject obj) {
            return new PhaseRecord(obj.get("name").getAsString(), obj.get("duration_ms").getAsLong());
        }
    }

    private record Checkpoint(String name, long timestampMs) {
        JsonObject toJson() {
            var obj = new JsonObject();
            obj.addProperty("name", name);
            obj.addProperty("timestamp_ms", timestampMs);
            return obj;
        }

        static Checkpoint fromJson(JsonObject obj) {
            return new Checkpoint(obj.get("name").getAsString(), obj.get("timestamp_ms").getAsLong());
        }
    }

    private record TimingRecord(long timestamp, int reloadNumber, long totalMs, List<StepRecord> steps, boolean cacheHit, List<PhaseRecord> phases, List<Checkpoint> checkpoints) {
        JsonObject toJson() {
            var obj = new JsonObject();
            obj.addProperty("timestamp", timestamp);
            obj.addProperty("reload_number", reloadNumber);
            obj.addProperty("total_ms", totalMs);
            obj.addProperty("cache_hit", cacheHit);
            var stepsArr = new JsonArray();
            for (var step : steps) {
                stepsArr.add(step.toJson());
            }
            obj.add("steps", stepsArr);
            if (!phases.isEmpty()) {
                var phasesArr = new JsonArray();
                for (var phase : phases) {
                    phasesArr.add(phase.toJson());
                }
                obj.add("phases", phasesArr);
            }
            if (!checkpoints.isEmpty()) {
                var cpArr = new JsonArray();
                for (var cp : checkpoints) {
                    cpArr.add(cp.toJson());
                }
                obj.add("checkpoints", cpArr);
            }
            return obj;
        }

        static TimingRecord fromJson(JsonObject obj) {
            List<StepRecord> steps = new ArrayList<>();
            var stepsArr = obj.getAsJsonArray("steps");
            if (stepsArr != null) {
                for (var elem : stepsArr) {
                    steps.add(StepRecord.fromJson(elem.getAsJsonObject()));
                }
            }
            List<PhaseRecord> phases = new ArrayList<>();
            if (obj.has("phases")) {
                var phasesArr = obj.getAsJsonArray("phases");
                if (phasesArr != null) {
                    for (var elem : phasesArr) {
                        phases.add(PhaseRecord.fromJson(elem.getAsJsonObject()));
                    }
                }
            }
            List<Checkpoint> checkpoints = new ArrayList<>();
            if (obj.has("checkpoints")) {
                var cpArr = obj.getAsJsonArray("checkpoints");
                if (cpArr != null) {
                    for (var elem : cpArr) {
                        checkpoints.add(Checkpoint.fromJson(elem.getAsJsonObject()));
                    }
                }
            }
            return new TimingRecord(
                    obj.get("timestamp").getAsLong(),
                    obj.get("reload_number").getAsInt(),
                    obj.get("total_ms").getAsLong(),
                    steps,
                    obj.get("cache_hit").getAsBoolean(),
                    phases,
                    checkpoints
            );
        }
    }
}
