package nachos.threads;

import java.util.LinkedList;
import nachos.machine.*;
import nachos.threads.*;

/**
 * A synchronized queue.
 */
public class SynchList2 {
    /**
     * Allocate a new synchronized queue.
     */
    public SynchList2() {
		list = new LinkedList<Object>();
		lock = new Lock();
    }
	
    /**
     * Add the specified object to the end of the queue.
     *
     * @param	o	the object to add. Must not be <tt>null</tt>.
     */
    public void add(Object o) {
		Lib.assertTrue(o != null);
		
		lock.acquire();
		list.add(o);
		lock.release();
    }
	
    /**
     * Remove an object from the front of the queue, returns null
	 * if the queue is empty.
     *
     * @return	the element removed from the front of the queue.
     */
    public Object removeFirst() {
		Object o;
		
		lock.acquire();
		if (list.isEmpty())
			o = null;
		else
			o = list.removeFirst();
		lock.release();
		
		return o;
    }
	
	/**
	 * Returns the number of items in the queue.
	 */
	public int size(){
		int s;
		
		lock.acquire();
		s = list.size();
		lock.release();
		
		return s;
	}
	
    private LinkedList<Object> list;
    private Lock lock;
}

