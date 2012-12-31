package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Lib;
import java.util.Arrays;

public class Boat
{
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
  	begin(1, 2, b);

	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
	begin(3, 3, b);
    }
    
    static BoatGrader bg;
    private static Lock actionLock; //This is necessary for a child or adult to take any action.
    private static Condition adultC; //This will queue adults that haven't arrived at the destination.
	//Note that adults at the destination are no longer significant to our solution.
	
    private static Condition kidCD; //This will queue kids that have arrived at the destination.
	//Note these children may need to row back home.
	
    private static Condition kidCH; //This will queue kids that haven't arrived at the destination.
    private static boolean[] crossed; //This keeps track of location index by id
	//*Note: This array is only accessed by a person to check their own location
	
    private static int totalAdults; //Only used to differentiate indicies in crossed
    private static boolean boatCrossed;
    private static boolean timeToRideTheWalrus; //This means there is a child waiting to pilot the boat.
	//Note, a child is NOT allowed (in our solution) to row to the destination alone, therefore this case
	// takes priority over literally everything aside from geography.
	
    private static Semaphore endGame; //This will be used to signal "begin()" to return.

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	
	crossed = new boolean[adults+children];
	totalAdults = adults;
	Arrays.fill(crossed, false);
	actionLock = new Lock();
	adultC = new Condition(actionLock);
	kidCD = new Condition(actionLock);
	kidCH = new Condition(actionLock);
	boatCrossed = false;
	timeToRideTheWalrus = false;
	endGame = new Semaphore(0);

	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	for(int i=0; i<adults;i++) {
	    final int id = i;
	    new KThread(new Runnable() {public void run() {AdultItinerary(id);}}).fork(); //Start new adult threads with proper ID's
	}
	final boolean[] endGameb = {false};
	for(int i=0; i<children;i++) {
	    final int id = i+adults; //Offset the ID for children
	    new KThread(new Runnable() {public void run() {ChildItinerary(id,endGameb);}}).fork(); //Start new kid threads with proper ID's
	}
	
	endGame.P(); //Wait until the last arriver on the destination calls V()
	
	System.out.println("GAME OVER");
    }
    
	//Returns how many kids are currently on your island
    private static int howManyKidsHere(boolean myIsland){
		Lib.assertTrue(actionLock.isHeldByCurrentThread());
		int ret=0;
		for(int i = totalAdults; i < crossed.length; i++) { //i starts at totalAdults so we only count kids
			if(crossed[i] == myIsland) {
			ret++;
			}
		}
		return ret;
    }

	//Returns how many people are currently on your island
    private static int howManyHere(boolean myIsland){
		Lib.assertTrue(actionLock.isHeldByCurrentThread());
		int ret=0;
		for(int i = 0; i < crossed.length; i++) {
			if(crossed[i] == myIsland) {
			ret++;
			}
		}
		return ret;
    }

    static void AdultItinerary(final int id)
    {
	while(!crossed[id]) {
	    actionLock.acquire();
		//This adult sleeps if there is already a kid in the boat, the boat is not on his island, or there are >1 kids here (let the kids go first)
	    while(timeToRideTheWalrus || boatCrossed || howManyKidsHere(false) > 1) {
		adultC.sleep();
	    }
		//Otherwise this adult rows to the destination
	    bg.AdultRowToMolokai();
	    crossed[id]=true;
	    boatCrossed=true;
	    kidCD.wake(); //wake a kid at the destination to row home
	    actionLock.release();
	}
    }

    static void ChildItinerary(final int id, final boolean[] endGameb)
    {
	while(!endGameb[0]){ //this is only used so threads terminate at the end of "begin()"
	    actionLock.acquire();
	    if(crossed[id]){ //if this kid is on the destination island
			//sleep if the boat is at home
			while(!boatCrossed && !endGameb[0]) {
				kidCD.sleep();
			}
			if(endGameb[0]){break;} //this is only used so threads terminate at the end of "begin()"
			//bring the boat home
			bg.ChildRowToOahu();
			crossed[id]=false;
			boatCrossed=false;
			kidCH.wake(); //wake a kid at home to potentially go to the destination
			adultC.wake(); //wake an adult at home to potentially go to the destination
	    }else{	//if this kid is on the home island
			//sleep if there is not a child waiting on the boat and (the boat is away or he is the only kid here)
			while(!timeToRideTheWalrus && !endGameb[0] && (boatCrossed || howManyKidsHere(false) < 2)) {
				kidCH.sleep();
			}
			if(endGameb[0]){break;} //this is only used so threads terminate at the end of "begin()"
			if(timeToRideTheWalrus){ //if there is a kid waiting on the boat to pilot this kid to the destination...
				//this kid rides.
				bg.ChildRideToMolokai();
				crossed[id]=true;
				boatCrossed = howManyHere(false)!=0;
				if(!boatCrossed){endGameb[0]=true; endGame.V();}
				timeToRideTheWalrus=false;
				kidCD.wake(); //wake a kid at the destination to row home
			}else{ //otherwise this kid becomes the pilot
				bg.ChildRowToMolokai();
				crossed[id]=true;
				timeToRideTheWalrus=true; //Now this kid is waiting in the boat
				kidCH.wake(); //Wakes a kid at home to ride with him
			}
		}
	    actionLock.release();
	}
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai(); //Whhhaaaaaaat???
	bg.ChildRideToMolokai();
    }
    
}
