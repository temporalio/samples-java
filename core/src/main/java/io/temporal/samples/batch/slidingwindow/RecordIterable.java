package io.temporal.samples.batch.slidingwindow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.NotNull;

/** Iterable implementation that relies on RecordLoader activity. */
public class RecordIterable implements Iterable<SingleRecord> {

  /**
   * Iterator implementation that relies on RecordLoader activity.
   *
   * <p>This code assumes that RecordLoader.getRecords never returns a failure to the workflow. The
   * real production application might make a different design choice.
   */
  private class RecordIterator implements Iterator<SingleRecord> {

    /**
     * The last page of records loaded through RecordLoader activity. The activity returns an empty
     * page to indicate the end of iteration.
     */
    private List<SingleRecord> lastPage;

    /** The offset of the last loaded batch of records. */
    private int offset;

    /** Index into the last loaded page of the next record to return. */
    private int index;

    RecordIterator() {
      this.offset = initialOffset;
      if (initialOffset > maximumOffset) {
        this.lastPage = new ArrayList<>();
      } else {
        int size = Math.min(pageSize, maximumOffset - offset);
        this.lastPage = recordLoader.getRecords(size, offset);
      }
    }

    @Override
    public boolean hasNext() {
      return !lastPage.isEmpty();
    }

    @Override
    public SingleRecord next() {
      int size = lastPage.size();
      if (size == 0) {
        throw new NoSuchElementException();
      }
      SingleRecord result = lastPage.get(index++);
      if (size == index) {
        offset += index;
        index = 0;
        lastPage = recordLoader.getRecords(pageSize, offset);
      }
      return result;
    }
  }

  private final int initialOffset;

  private final int pageSize;

  private final int maximumOffset;

  private final RecordLoader recordLoader =
      Workflow.newActivityStub(
          RecordLoader.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  /**
   * @param pageSize size of a single page to load.
   * @param initialOffset the initial offset to load records from.
   * @param maximumOffset the maximum offset (exclusive).
   */
  public RecordIterable(int pageSize, int initialOffset, int maximumOffset) {
    this.pageSize = pageSize;
    this.initialOffset = initialOffset;
    this.maximumOffset = maximumOffset;
  }

  @NotNull
  @Override
  public Iterator<SingleRecord> iterator() {
    return new RecordIterator();
  }
}
