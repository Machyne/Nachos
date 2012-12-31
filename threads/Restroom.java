package nachos.threads;

import nachos.machine.*;
import java.util.Random;

/**
 * This is designed to solve the restroom problem (seperate is not equal)
 * The idea is if men are occupying the restroom, women may not, and vice versa.
 * Anyone may enter an empty room. A sign is hung on the outside to signify if
 * men, women, or no one is/are currently occupying the restroom.
 * -Midterm Take Home Problem 4, Matt Cotter
 */
public class Restroom {
	static short doorSign = 0; //0 will be empty, 1 will be men, 2 will be women.
	static int peopleInRoom = 0; //the number of people in the restroom
	static Lock lock = new Lock();
	static Condition menWait = new Condition(lock); //men waiting will be here
	static Condition womenWait = new Condition(lock); //women waiting will be here

	static void womanArrives() {
		System.out.println("woman arrives");
		lock.acquire();

		while(doorSign==1){ //if there are men in the room
			womenWait.sleep(); //sleep
		}

		doorSign=2; //set the sign to say there are women in the room
		peopleInRoom++; //add a person to the count in the room
		System.out.println("woman enters");

		lock.release();
	}

	static void manArrives() {
		System.out.println("man arrives");
		lock.acquire();

		while(doorSign==2){ //if there are women in the room
			menWait.sleep(); //sleep
		}

		doorSign=1; //set the sign to say there are men in the room
		peopleInRoom++; //add a person to the count in the room
		System.out.println("man enters");

		lock.release();
	}

	static void womanLeaves() {
		System.out.println("woman leaves");
		lock.acquire();

		//decrement the count of people in the room
		if(--peopleInRoom == 0){ //if there are now no people in the room
			doorSign=0; //set the sign to say the room is empty
			menWait.wakeAll(); //wake all men
		}
		
		lock.release();
	}

	static void manLeaves() {
		System.out.println("man leaves");
		lock.acquire();

		//decrement the count of people in the room
		if(--peopleInRoom == 0){ //if there are now no people in the room
			doorSign=0; //set the sign to say the room is empty
			womenWait.wakeAll(); //wake all women
		}

		lock.release();
	}
   
	//This is the provided test code
	public static void selfTest() {
		System.out.println("\n***running Restroom self-test***\n");
		final Random rgen = new Random();
		// 20 people show up at the door with sex chosen randomly for each.
		// women take a random number of clock ticks up to 1,000 inside the 
		// restroom, men take a random number up to 500, to make it more interesting.
		for (int i=0; i<20; i++) {
			if (rgen.nextInt(2)==0) {
				new KThread(new Runnable() { 
						public void run() {
							womanArrives();
							ThreadedKernel.alarm.waitUntil(rgen.nextInt(1000));
							womanLeaves();
						}
					}).setName(""+i).fork();
			}
			else {
				new KThread(new Runnable() { 
						public void run() {
							manArrives();
							ThreadedKernel.alarm.waitUntil(rgen.nextInt(500));
							manLeaves();
						}
					}).setName(""+i).fork();
			}
		}

		// one last man (21st) person waits 2000 clock ticks to ensure that this main 
		// thread will be interrupted and so this guy will arrive at the bathroom last
		// so that all of the other threads can finish before the program exits
		ThreadedKernel.alarm.waitUntil(2000);
		manArrives();
		ThreadedKernel.alarm.waitUntil(rgen.nextInt(500));
		manLeaves();
	}
}