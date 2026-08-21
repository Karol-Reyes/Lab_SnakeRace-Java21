package co.eci.snake.concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

public final class PauseController {
  private final int participants;
  private final AtomicInteger pausedParticipants = new AtomicInteger();
  private boolean paused;

  public PauseController(int participants) {
    if (participants < 0) throw new IllegalArgumentException("participants must be >= 0");
    this.participants = participants;
  }

  public synchronized boolean pauseAndAwait(long timeoutMillis) {
    if (timeoutMillis <= 0) throw new IllegalArgumentException("timeoutMillis must be > 0");
    paused = true;
    notifyAll();
    long remainingNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    while (pausedParticipants.get() < participants) {
      if (remainingNanos <= 0) {
        return false;
      }
      try {
        long started = System.nanoTime();
        TimeUnit.NANOSECONDS.timedWait(this, remainingNanos);
        remainingNanos -= System.nanoTime() - started;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return true;
  }

  public synchronized void resume() {
    paused = false;
    notifyAll();
  }

  public synchronized void awaitIfPaused() {
    while (paused) {
      pausedParticipants.incrementAndGet();
      notifyAll();
      try {
        wait();
      } catch (InterruptedException e) {
        pausedParticipants.decrementAndGet();
        Thread.currentThread().interrupt();
        return;
      }
      pausedParticipants.decrementAndGet();
    }
  }
}