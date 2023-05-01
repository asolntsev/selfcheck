package selfcheck.doc;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

@ParametersAreNonnullByDefault
class ThreadNamer implements ThreadFactory {
  private static final AtomicLong counter = new AtomicLong();
  private final String name;

  ThreadNamer(String name) {
    this.name = name;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    return new Thread(runnable, name + counter.incrementAndGet());
  }
}
