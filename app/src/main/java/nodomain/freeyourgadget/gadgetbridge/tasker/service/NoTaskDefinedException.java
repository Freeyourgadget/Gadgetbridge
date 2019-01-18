package nodomain.freeyourgadget.gadgetbridge.tasker.service;

/**
 * Gets thrown if tasker is enabled but no task name is defined. Triggers {@link TaskerUtil#noTaskDefinedInformation()}.
 */
public class NoTaskDefinedException extends RuntimeException {

    public NoTaskDefinedException() {
        super("Tasker enabled but no task defined! Please define a task either in settings or in service.");
    }

}
