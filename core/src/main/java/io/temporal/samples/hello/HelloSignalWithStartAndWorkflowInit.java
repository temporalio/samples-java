package io.temporal.samples.hello;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Sample Temporal workflow that demonstrates how to use WorkflowInit with clients starting
 * execution using SignalWithStart
 */
public class HelloSignalWithStartAndWorkflowInit {
  static final String TASK_QUEUE = "HelloWithInitTaskQueue";

  public interface MyWorkflow {
    @WorkflowMethod
    String greet(Person person);

    @SignalMethod
    void addGreeting(Person person);
  }

  @WorkflowInterface
  public interface MyWorkflowWithInit extends MyWorkflow {}

  @WorkflowInterface
  public interface MyWorkflowNoInit extends MyWorkflow {}

  public static class WithInitMyWorkflowImpl implements MyWorkflowWithInit {
    // We dont initialize peopleToGreet on purpose
    private List<Person> peopleToGreet;
    private MyGreetingActivities activities =
        Workflow.newActivityStub(
            MyGreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @WorkflowInit
    public WithInitMyWorkflowImpl(Person person) {
      peopleToGreet = new ArrayList<>();
    }

    @Override
    public String greet(Person person) {
      peopleToGreet.add(person);
      List<String> greetings = new ArrayList<>();

      while (!peopleToGreet.isEmpty()) {
        // run activity...
        greetings.add(activities.greet(peopleToGreet.get(0)));
        peopleToGreet.remove(0);
      }
      return StringUtils.join(greetings, ",");
    }

    @Override
    public void addGreeting(Person person) {
      peopleToGreet.add(person);
    }
  }

  public static class WithoutInitMyWorkflowImpl implements MyWorkflowNoInit {
    // We dont initialize peopleToGreet on purpose
    private List<Person> peopleToGreet;
    private MyGreetingActivities activities =
        Workflow.newActivityStub(
            MyGreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public String greet(Person person) {
      peopleToGreet.add(person);
      List<String> greetings = new ArrayList<>();

      while (!peopleToGreet.isEmpty()) {
        // run activity...
        greetings.add(activities.greet(peopleToGreet.get(0)));
        peopleToGreet.remove(0);
      }
      return StringUtils.join(greetings, ",");
    }

    @Override
    public void addGreeting(Person person) {
      peopleToGreet.add(person);
    }
  }

  @ActivityInterface
  public interface MyGreetingActivities {
    public String greet(Person person);
  }

  public static class MyGreetingActivitiesImpl implements MyGreetingActivities {
    @Override
    public String greet(Person person) {
      return "Hello " + person.firstName + " " + person.lastName;
    }
  }

  public static class Person {
    String firstName;
    String lastName;
    int age;

    public Person() {}

    public Person(String firstName, String lastName, int age) {
      this.firstName = firstName;
      this.lastName = lastName;
      this.age = age;
    }

    public String getFirstName() {
      return firstName;
    }

    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public void setLastName(String lastName) {
      this.lastName = lastName;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(WithInitMyWorkflowImpl.class);
    // We explicitly want to fail this workflow on NPE as thats what we expect without WorkflowInit
    // As we didnt initialize peopleToGreet on purpose
    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            .setFailWorkflowExceptionTypes(NullPointerException.class)
            .build(),
        WithoutInitMyWorkflowImpl.class);
    worker.registerActivitiesImplementations(new MyGreetingActivitiesImpl());

    factory.start();

    MyWorkflowWithInit withInitStub =
        client.newWorkflowStub(
            MyWorkflowWithInit.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("with-init")
                .setTaskQueue(TASK_QUEUE)
                .build());
    // Start with init workflow which is expected to succeed
    // As WorkflowInit will initialize peopleToGreet before signal handler is invoked
    WorkflowStub.fromTyped(withInitStub)
        .signalWithStart(
            "addGreeting",
            new Object[] {new Person("Michael", "Jordan", 55)},
            new Object[] {new Person("John", "Stockton", 57)});

    String result = WorkflowStub.fromTyped(withInitStub).getResult(String.class);
    System.out.println("Result: " + result);

    // Start without init, this execution is expected to fail as we set
    // NullPointerException as a workflow failure type
    // NPE is caused because we did not initialize peopleToGreet array
    MyWorkflowNoInit noInitStub =
        client.newWorkflowStub(
            MyWorkflowNoInit.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("without-init")
                .setTaskQueue(TASK_QUEUE)
                .build());
    WorkflowStub.fromTyped(noInitStub)
        .signalWithStart(
            "addGreeting",
            new Object[] {new Person("Michael", "Jordan", 55)},
            new Object[] {new Person("John", "Stockton", 57)});
    try {
      WorkflowStub.fromTyped(noInitStub).getResult(String.class);
    } catch (WorkflowFailedException e) {
      System.out.println("Expected workflow failure: " + e.getMessage());
    }

    System.exit(0);
  }
}
