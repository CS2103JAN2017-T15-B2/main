/* @@author A0119505J */
package watodo.testutil;

import watodo.commons.exceptions.IllegalValueException;
import watodo.model.TaskManager;
import watodo.model.task.Task;
import watodo.model.task.UniqueTaskList;

/**
 *
 */
public class TypicalTestTasks {

    public TestTask alice, benson, carl, daniel, hoon, ida;

    public TypicalTestTasks() {
        try {

            alice = new TaskBuilder().withName("Alice Pauline").withStartTime("17/02/2017 22:00")
                    .withEndTime("17/02/2017 23:59").withPriority("high")
                    .withTags("friends").withPriority("med").withStatus(0).build();
            benson = new TaskBuilder().withName("Benson Meier").withStartTime("17/02/2017 22:00")
                    .withEndTime("17/02/2017 23:59").withTags("owesMoney", "friends")
                    .withPriority("med").withStatus(0).build();
            carl = new TaskBuilder().withName("Carl Kurz").withStartTime("17/02/2017 22:00")
                    .withEndTime("17/02/2017 23:59").withPriority("med").withStatus(0).build();
            daniel = new TaskBuilder().withName("Daniel Meier").withStartTime("17/02/2017 22:00")
                    .withEndTime("17/02/2017 23:59").withPriority("low").withStatus(0).build();

            // Manually added
            hoon = new TaskBuilder().withName("Hoon Meier").withStartTime("17/02/2017 22:00")
                    .withEndTime("17/02/2017 23:59").withPriority("med").withStatus(0).build();
            ida = new TaskBuilder().withName("Ida Mueller").withStartTime("17/02/2017 22:00")
                    .withEndTime("17/02/2017 23:59").withPriority("low").withStatus(0).build();
        } catch (IllegalValueException e) {
            e.printStackTrace();
            assert false : "not possible";
        }
    }

    public static void loadTaskManagerWithSampleData(TaskManager ab) {
        for (TestTask task : new TypicalTestTasks().getTypicalTasks()) {
            try {
                ab.addTask(new Task(task));
            } catch (UniqueTaskList.DuplicateTaskException e) {
                assert false : "not possible";
            }
        }
    }

    public TestTask[] getTypicalTasks() {
        return new TestTask[]{alice, benson, carl, daniel};
    }

    public TaskManager getTypicalTaskManager() {
        TaskManager ab = new TaskManager();
        loadTaskManagerWithSampleData(ab);
        return ab;
    }
}
