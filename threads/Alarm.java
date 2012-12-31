package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
	//We are taking advantage of how often this gets called :)
	for(int i = 0; i < waitQueue.size(); i++) {
	    Waitaphore waitaphore = waitQueue.get(i);
	    if(waitaphore.wakeTime <= Machine.timer().getTime()) {
		waitaphore.semaphore.V();
		waitQueue.remove(i--);
	    }
	}

	//This is what this method is supposed to do
	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	long wakeTime = Machine.timer().getTime() + x;
	Semaphore semaphore = new Semaphore(0);
	Waitaphore waitaphore = new Waitaphore(semaphore, wakeTime);
	waitQueue.add(waitaphore);
	semaphore.P();
    }

    private LinkedList<Waitaphore> waitQueue = new LinkedList<Waitaphore>();

    private static class Waitaphore{
	public Semaphore semaphore;
	public long wakeTime;
	public Waitaphore(Semaphore semaphore, long wakeTime){
	    this.semaphore=semaphore;
	    this.wakeTime=wakeTime;
	}
    }

    private static class PingTest implements Runnable {
	PingTest(int which) {
	    this.which = which;
	}
	
	public void run() {
	    System.out.println("*** thread " + which + " current time "
			       + Machine.timer().getTime());
	    ThreadedKernel.alarm.waitUntil(1000000);
	     System.out.println("*** thread " + which + " after wait  "
			       + Machine.timer().getTime());
	}

	private int which;
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
	new KThread(new PingTest(1)).setName("forked thread").fork();
	new PingTest(0).run();
    }
}
