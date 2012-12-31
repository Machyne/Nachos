package nachos.network;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.LinkedList;

/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */
public class NetProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public NetProcess() {
		super();
		
		freePorts = new LinkedList<Integer>();
		for (int i=0; i<NetMessage.portLimit; i++) {
			freePorts.offer(i);
		}
		
		plock = new Lock();
    }
	
	/**
	 * Creates a connection.
	 * then stores the connection file in 
	 * the file table
	 * then returns the file descriptor
	 */
	private int handleConnect(int host, int port){
		if(port < 0 || port >= NetMessage.portLimit){
			return -1;
		}
		int i = 0;
		for (; i < FILE_TABLE_SIZE; i++) {
			if(fileTable[i] == null) {
				break;
			}
		}
		if (i == FILE_TABLE_SIZE) {
			return -1;
		}
		
		int myPort = getPort();
		
		final int I = i;
		Runnable closer = new Runnable(){ public void run(){fileTable[I]=null;}};
		Connection theFile = new Connection(myPort, Machine.networkLink().getLinkAddress(), port, host, closer);
		
		theFile.open();
		
		fileTable[i] = theFile;
		return i;		
	}
	
	/**
	 * Checks the PO if there is a request
	 * for connection, if there isn't returns
	 * 1 if there is creates a connections and
	 * sends and ACK then returns the file 
	 * descriptor
	 */
	private int handleAccept(int port){
		if(port < 0 || port >= NetMessage.portLimit){
			return -1;
		}
		int i = 0;
		for (; i < FILE_TABLE_SIZE; i++) {
			if(fileTable[i] == null) {
				break;
			}
		}
		if (i == FILE_TABLE_SIZE) {
			return -1;
		}
		Connection theFile;
		NetMessage nm;
		try {
			nm = NetKernel.postOffice.receive(port);
		}
		catch (Exception e) {
			return -1;
		}
		if (!nm.getFlags()[3] || nm==null) {
			return -1;
		}

		final int I = i;
		Runnable closer = new Runnable(){ public void run(){fileTable[I]=null;}};
		theFile = new Connection(nm,closer);

		theFile.recievedSyn();
		
		fileTable[i] = theFile;
		return i;
	}
	
	/** Lock for allocating ports */
	private Lock plock;
	
	/** Queue of open ports */
	private final LinkedList<Integer> freePorts;
	
	/**
	 * Returns an unused port.
	 */
	private int getPort(){
		int ret;
		plock.acquire();
		ret = freePorts.poll();
		plock.release();
		return ret;
	}
	
	/**
	 * Takes in a released port and adds to the unused list.
	 */
	private void releasePort(int port){
		plock.acquire();
		freePorts.offer(port);
		plock.release();
		return;
	}
	
	
    private static final int
	syscallConnect = 11,
	syscallAccept = 12;
    
    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>11</td><td><tt>int  connect(int host, int port);</tt></td></tr>
     * <tr><td>12</td><td><tt>int  accept(int port);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
			case syscallConnect:
				return handleConnect(a0,a1);
			case syscallAccept:
				return handleAccept(a0);
				
			default:
				return super.handleSyscall(syscall, a0, a1, a2, a3);
		}
    }
	
	
	
}
