package watodo.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javafx.collections.transformation.FilteredList;
import watodo.commons.core.ComponentManager;
import watodo.commons.core.LogsCenter;
import watodo.commons.core.UnmodifiableObservableList;
import watodo.commons.events.model.TaskManagerChangedEvent;
import watodo.commons.util.CollectionUtil;
import watodo.commons.util.StringUtil;
import watodo.logic.commands.UndoCommand;
import watodo.model.task.ReadOnlyTask;
import watodo.model.task.Status;
import watodo.model.task.Task;
import watodo.model.task.UniqueTaskList;
import watodo.model.task.UniqueTaskList.DuplicateTaskException;
import watodo.model.task.UniqueTaskList.TaskNotFoundException;

/**
 * Represents the in-memory model of the task manager data.
 * All changes to any model should be synchronized.
 */
public class ModelManager extends ComponentManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    //@@author A0119505J
    private static LinkedList<UndoInfo> undoStack = new LinkedList<UndoInfo>();
    private final TaskManager taskManager;
    private final FilteredList<ReadOnlyTask> filteredTasks;

    /**
     * Initializes a ModelManager with the given taskManager and userPrefs.
     */
    public ModelManager(ReadOnlyTaskManager taskManager, UserPrefs userPrefs) {
        super();
        assert !CollectionUtil.isAnyNull(taskManager, userPrefs);

        logger.fine("Initializing with task manager: " + taskManager + " and user prefs " + userPrefs);

        this.taskManager = new TaskManager(taskManager);
        filteredTasks = new FilteredList<>(this.taskManager.getTaskList());
    }

    public ModelManager() {
        this(new TaskManager(), new UserPrefs());
    }

    //@@author A0119505J
    @Override
    public void resetData(ReadOnlyTaskManager newData) {
        if (newData.isEmpty()) { // clear was executed
            List<Task> listOfTasks = (List<Task>) (List<?>) taskManager.getTaskList();
            addToUndoStack(UndoCommand.CLR_CMD_ID, null, listOfTasks.toArray(new Task[listOfTasks.size()]));
        }
        taskManager.resetData(newData);
        indicateTaskManagerChanged();
    }

    @Override
    public ReadOnlyTaskManager getTaskManager() {
        return taskManager;
    }

    //@@author A0119505J

    @Override
    public void clearTaskUndo(ArrayList<Task> tasks) throws TaskNotFoundException {
        TaskManager oldTaskList = new TaskManager();
        try {
            oldTaskList.setTasks(tasks);
        } catch (DuplicateTaskException e) {
            e.printStackTrace();
        }
        taskManager.resetData(oldTaskList);
    }

    /** Raises an event to indicate the model has changed */
    private void indicateTaskManagerChanged() {
        raise(new TaskManagerChangedEvent(taskManager));
    }

    //@@author A0119505J
    @Override
    public void deleteTaskUndo(ReadOnlyTask target) throws TaskNotFoundException {
        taskManager.removeTask(target);
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
    }

    @Override
    public synchronized void deleteTask(ReadOnlyTask target) throws TaskNotFoundException {
        taskManager.removeTask(target);
        indicateTaskManagerChanged();
        addToUndoStack(UndoCommand.DEL_CMD_ID, null, (Task) target);
    }

    //@@author A0119505J
    @Override
    public void addTaskUndo(Task task) throws UniqueTaskList.DuplicateTaskException {
        taskManager.addTask(task);
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
    }

    @Override
    public synchronized void addTask(Task task) throws UniqueTaskList.DuplicateTaskException {
        taskManager.addTask(task);
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
        addToUndoStack(UndoCommand.ADD_CMD_ID, null, task);
    }

    @Override
    public void updateTask(int filteredTaskListIndex, ReadOnlyTask editedTask)
            throws UniqueTaskList.DuplicateTaskException {
        assert editedTask != null;
        int taskManagerIndex = filteredTasks.getSourceIndex(filteredTaskListIndex);
        taskManager.updateTask(taskManagerIndex, editedTask);
        indicateTaskManagerChanged();
    }

    // @@author A0164393Y

    @Override
    public void markTask(int index, Task editedTask) throws TaskNotFoundException {
        taskManager.markTask(index, editedTask);
        indicateTaskManagerChanged();
        addToUndoStack(UndoCommand.MARK_CMD_ID, null, editedTask);
    }

    //@@author A0119505J
    @Override
    public void markTaskUndo(Task task) throws TaskNotFoundException, UniqueTaskList.DuplicateTaskException {
        //taskManager.markTask(index, editedTask);
        taskManager.removeTask(task);
        Task editedTask = new Task(task.getName(), task.getStartTime(), task.getEndTime(), task.getPriority(),
                task.getTags(), new Status((1 - task.getStatus().status)));
        taskManager.addTask(editedTask);
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
    }

    //@@author A0119505J
    @Override
    public void addToUndoStack(int undoID, String filePath, Task... tasks) {
        UndoInfo undoInfo = new UndoInfo(undoID, filePath, tasks);
        undoStack.push(undoInfo);
    }

    //=========== Filtered Task List Accessors =============================================================

    @Override
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredTaskList() {
        return new UnmodifiableObservableList<>(filteredTasks);
    }

    @Override
    public void updateFilteredListToShowAll() {
        filteredTasks.setPredicate(null);
    }

    @Override
    public void updateFilteredTaskList(Set<String> keywords) {
        updateFilteredTaskList(new PredicateExpression(new NameQualifier(keywords)));
    }

    private void updateFilteredTaskList(Expression expression) {
        filteredTasks.setPredicate(expression::satisfies);
    }

    //========== Inner classes/interfaces used for filtering =================================================

    interface Expression {
        boolean satisfies(ReadOnlyTask task);
        @Override
        String toString();
    }

    private class PredicateExpression implements Expression {

        private final Qualifier qualifier;

        PredicateExpression(Qualifier qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public boolean satisfies(ReadOnlyTask task) {
            return qualifier.run(task);
        }

        @Override
        public String toString() {
            return qualifier.toString();
        }
    }

    interface Qualifier {
        boolean run(ReadOnlyTask task);
        @Override
        String toString();
    }

    private class NameQualifier implements Qualifier {
        private Set<String> nameKeyWords;

        NameQualifier(Set<String> nameKeyWords) {
            this.nameKeyWords = nameKeyWords;
        }

        @Override
        public boolean run(ReadOnlyTask task) {
            return nameKeyWords.stream()
                    .filter(keyword -> StringUtil.containsWordIgnoreCase(task.getName().fullName, keyword))
                    .findAny()
                    .isPresent();
        }

        @Override
        public String toString() {
            return "name=" + String.join(", ", nameKeyWords);
        }
    }

    //@@author A0119505J
    @Override
    public LinkedList<UndoInfo> getUndoStack() {
        return undoStack;
    }

}
