package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    public final Lock waitLock = new Lock();
    public final Condition speakkLine = new Condition(waitLock);
    public final Condition listenLine = new Condition(waitLock);
    
    private final int speakPrime=2;
    private final int listenPrime=3;

    public int[] word = {0,0,1};
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
	waitLock.acquire();
	this.word[2]*=speakPrime;
	while(this.word[1] != 0 || this.word[2]%listenPrime != 0){
	    speakkLine.sleep();
	}
	//System.out.println("Speak "+this.word[1]);
	//System.out.println(this.word[2]);
	this.word[0] = word;
	this.word[1] = 1;
	this.word[2] /= listenPrime;
	listenLine.wake();
	waitLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
	int word = 0;
	waitLock.acquire();
	this.word[2]*=listenPrime;
	while(this.word[1] == 0 || this.word[2]%speakPrime != 0){
	    listenLine.sleep();
	}
	//System.out.println("Listen "+this.word[1]);
	//System.out.println(this.word[2]);
	word = this.word[0];
	this.word[1] = 0;
	this.word[2] /= speakPrime;
	speakkLine.wake();
	waitLock.release();
	return word;
    }



    /**********************************************/

    private static class Speaker implements Runnable {
	Speaker(Communicator comm, int which) {
	    this.comm = comm;
	    this.which = which;
	}
	
	public void run() {
	    for(int i=0;i<4;i++){
		comm.speak(i+100*which);
		System.out.println(which + " spoke " + (100*which+i));
	    }
	}

	private Communicator comm;
	private int which;
    }

    private static class Listener implements Runnable {
	Listener(Communicator comm, int which) {
	    this.comm = comm;
	    this.which = which;
	}
	
	public void run() {
	    for(int i=0;i<4;i++){
		int heard = comm.listen();
		System.out.println(which + " heard " + heard);
	    }
	}

	private Communicator comm;
	private int which;
    }

    /**
     * Test if this module is working.
     */
    public static void selfTest() {
	Communicator comm = new Communicator();
	
	new KThread(new Speaker(comm,1)).setName("prod").fork();
	new KThread(new Speaker(comm,2)).setName("prod").fork();
	new KThread(new Speaker(comm,3)).setName("prod").fork();
	//new KThread(new Listener(comm,1)).setName("prod").fork();
	//new KThread(new Listener(comm,2)).setName("prod").fork();
	new Listener(comm,3).run();
    }
}
