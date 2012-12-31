package nachos.threads;

/**
 * This is an implementation of Semaphore using only BinSem.java and user level operations.
 * -Midterm Take Home Problem 2, Matt Cotter
 */
public class Semaphore2 {
    /**
     * Allocate a new Semaphore2.
     *
     * @param	initialValue	the initial value of this Semaphore2.
     */
    public Semaphore2(int initialValue) {
	value = initialValue;
	lock = new BinSem(1); //This is used to ensure mutual exclusion
	queue = new BinSem( value==0?0:1 ); //Initialize to 0 if the value is 0, otherwise initialize to 1.
	//Note - the value of the queue should Always be equal to 0 if the value is 0, otherwise 1.
	waiting = 0; //We start with no one waiting.
    }

    /**
     * Atomically wait for this Semaphore2 to become non-zero and decrement it.
     */
    public void P() {
	lock.P(); //"Aquire lock"

	if (value < 2) { // If the value less than 2
	    if(value == 0){ waiting++; } //Increment the waiters if this thread will have to wait.
	    else{ value=0; } //If this thread will not have to wait, decrement the value
	    lock.V(); // Release the lock - queue.P() may cause the current thread to block.
	    queue.P(); // call P() on the queue
	    lock.P(); //ReAquire lock when waking up.
	}
	else { //Otherwise
	    value--; // Just decrement the value.
	}

	lock.V(); //"Release lock"
    }

    /**
     * Atomically increment this Semaphore2 and wake up at most one other thread
     * sleeping on this Semaphore2.
     */
    public void V() {
	lock.P(); //"Aquire lock"
	
	if (waiting == 0) { //If no one is waiting
	    //Increment the value
	    if(0 == value++){ //If the old value was 0
		queue.V(); //Increase the queue value (to 1)
	    }
	}
	else { //Otherwise
	    waiting--; //Decrement the number of waiters
	    queue.V(); // and wake someone up.
	    //if waiting != 0, value must be 0, so queue must also be 0 - thus V is safe.
	}

	lock.V(); //"Release lock"
    }

    // This is the same test from Semaphore.java
    private static class PingTest implements Runnable {
	PingTest(Semaphore2 ping, Semaphore2 pong) {
	    this.ping = ping;
	    this.pong = pong;
	}
	
	public void run() {
	    for (int i=0; i<10; i++) {
		System.out.println("Ping P");
		ping.P();
		System.out.println("Pong V");
		pong.V();
	    }
	}

	private Semaphore2 ping;
	private Semaphore2 pong;
    }

    /**
     * Test if this module is working.
     */
    public static void selfTest() {
	Semaphore2 ping = new Semaphore2(0);
	Semaphore2 pong = new Semaphore2(0);
	System.out.println("Sem2Test");
	new KThread(new PingTest(ping, pong)).setName("ping").fork();

	for (int i=0; i<10; i++) {
	    System.out.println("Ping V");
	    ping.V();
	    System.out.println("Pong P");
	    pong.P();
	}
    }

    private int value, waiting;
    private BinSem lock, queue;
}

