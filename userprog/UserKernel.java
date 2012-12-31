package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.LinkedList;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    
	/** The linked list of our free pages */
	private static final LinkedList<Integer> freePages = initializeList(Machine.processor().getNumPhysPages());
	private static final LinkedList<Integer> initializeList(int numPages){
		LinkedList<Integer> ret = new LinkedList<Integer>();
		for (int i=0; i<numPages; i++) {
			ret.offer(i);
		}
		return ret;
	}
	
	/** The lock for our LinkedList */
	private static Lock listLock;
	
	/** getPages
	 * If there are enough free pages,
	 * return an int array of physical page 
	 * numbers for the process to have.
	 * If not enough, return null (handle elsewhere)
	 */
	public static int[] getPages(int numPages){
		listLock.acquire();
		if (numPages>freePages.size()) {
			listLock.release();
			return null;
		}
		int[] ret = new int[numPages];
		for (int i = 0; i<numPages; i++) {
			ret[i]=freePages.poll();
		}
		listLock.release();
		return ret;
	}
	
	/** freeUpPages
	 * Takes an int or an int array 
	 * of physical pages numbers and
	 * allocates this/these as free.
	 */
	public static void freeUpPages(int toFree){
		listLock.acquire();
		freePages.offer(toFree);
		listLock.release();
	}
	
	/**
     * Allocate a new user kernel.
     */
    public UserKernel() {
		super();
    }
	
    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
		super.initialize(args);
		
		console = new SynchConsole(Machine.console());
		
		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() { exceptionHandler(); }
	    });
		listLock = new Lock();
    }
	
    /**
     * Test the console device.
     */	
    public void selfTest() {
		super.selfTest();
		
		System.out.println("Testing the console device. Typed characters");
		System.out.println("will be echoed until q is typed.");
		
		char c;
		
		do {
			c = (char) console.readByte(true);
			console.writeByte(c);
		}
		while (c != 'q');
		
		System.out.println("");
    }
	
    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
		if (!(KThread.currentThread() instanceof UThread))
			return null;
		
		return ((UThread) KThread.currentThread()).process;
    }
	
    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
		Lib.assertTrue(KThread.currentThread() instanceof UThread);
		
		UserProcess process = ((UThread) KThread.currentThread()).process;
		int cause = Machine.processor().readRegister(Processor.regCause);
		process.handleException(cause);
    }
	
    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
		super.run();
		
		UserProcess process = UserProcess.newUserProcess();
		
		String shellProgram = Machine.getShellProgramName();	
		Lib.assertTrue(process.execute(shellProgram, new String[] { }));
		
		KThread.currentThread().finish();
    }
	
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
		super.terminate();
    }
	
    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;
	
    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
	
}
