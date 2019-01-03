package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTask;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

public class TaskerService {

    private String activePreference;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Map<TaskerEventType, TaskerTask> tasks = new HashMap<>();
    private Map<TaskerEventType, Long> threshold = new HashMap<>();
    private Map<TaskerEventType, TaskerTaskProvider> typeProvider = new HashMap<>();
    private TaskerTaskProvider defaultProvider;
    private long defaultThreshold = 50L;

    private TaskerService() {
    }

    public static TaskerService withPreference(String activePreference) {
        TaskerService taskerService = new TaskerService();
        taskerService.activePreference = activePreference;
        return taskerService;
    }

    public TaskerService withThreshold(TaskerEventType type, long threshold) {
        this.threshold.put(type, threshold);
        return this;
    }

    public TaskerService withThreshold(long threshold) {
        this.defaultThreshold = threshold;
        return this;
    }

    public TaskerService withTask(TaskerEventType type, final String task) {
        typeProvider.put(type, new TaskerTaskProvider() {
            @Override
            public String getTask(TaskerEvent event) {
                return task;
            }
        });
        return this;
    }

    public TaskerService withTask(final String task) {
        defaultProvider = new TaskerTaskProvider() {
            @Override
            public String getTask(TaskerEvent event) {
                return task;
            }
        };
        return this;
    }

    public TaskerService withProvider(TaskerTaskProvider provider) {
        this.defaultProvider = provider;
        return this;
    }

    public TaskerService withProvider(TaskerEventType type, TaskerTaskProvider provider) {
        typeProvider.put(type, provider);
        return this;
    }


    public boolean isActive() {
        return GBApplication.getPrefs().getBoolean(activePreference, false);
    }

    public boolean buttonPressed(int index) {
        return runForType(TaskerEventType.BUTTON.setIndex(index));
    }

    public boolean deviceConnected(int index) {
        return runForType(TaskerEventType.BUTTON.setIndex(index));
    }

    public boolean dataReceived(int index) {
        return runForType(TaskerEventType.BUTTON.setIndex(index));
    }

    public static boolean ready() {
        return TaskerIntent.testStatus(GBApplication.getContext()).equals(TaskerIntent.Status.OK);
    }

    private boolean runForType(TaskerEventType type) {
        if (isActive() && ready()) {
            if (!tasks.containsKey(type)) {
                tasks.put(type, new TaskerTask(type, taskProvider(type), threshold(type)));
            }
            tasks.get(type).schedule(executor);
            return true;
        }
        return false;
    }

    private long threshold(TaskerEventType type) {
        return threshold.containsKey(type) ? threshold.get(type) : defaultThreshold;
    }

    private TaskerTaskProvider taskProvider(TaskerEventType type) {
        return typeProvider.containsKey(type) ? typeProvider.get(type) : defaultProvider;
    }

}
