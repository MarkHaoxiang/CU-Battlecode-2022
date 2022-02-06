package sprintbot;

import battlecode.common.*;
import sprintbot.battlecode2022.*;
import sprintbot.battlecode2022.util.Constants;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer
{
	public static RobotController controller;
	
	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * It is like the main function for your robot. If this method returns, the robot dies!
	 *
	 * @param rc The RobotController object. You use it to perform actions from this robot, and to get
	 *           information on its current status. Essentially your portal to interacting with the world.
	 **/
	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException
	{


		RobotPlayer.controller = rc;
		
		RunnableBot bot;
		switch (controller.getType())
		{
			case ARCHON:
				bot = new Archon(controller);
				break;
			case BUILDER:
				bot = new Builder(controller);
				break;
			case LABORATORY:
				bot = new Laboratory(controller);
				break;
			case MINER:
				bot = new Miner(controller);
				break;
			case SAGE:
				bot = new Sage(controller);
				break;
			case SOLDIER:
				bot = new Soldier(controller);
				break;
			case WATCHTOWER:
				bot = new Watchtower(controller);
				break;
			default:
				throw new IllegalStateException("NOT A VALID BOT");
		}
		
		while (true)
		{

			// This code runs during the entire lifespan of the robot, which is why it is in an infinite
			// loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
			// loop, we call Clock.yield(), signifying that we've done everything we want to do.
			
			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
			try
			{
				bot.turn();
			} catch (GameActionException e)
			{
				// Oh no! It looks like we did something illegal in the Battlecode world. You should
				// handle GameActionExceptions judiciously, in case unexpected events occur in the game
				// world. Remember, uncaught exceptions cause your robot to explode!
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
				
			} catch (Exception e)
			{
				// Oh no! It looks like our code tried to do something bad. This isn't a
				// GameActionException, so it's more likely to be a bug in our code.
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			} finally
			{
				// Signify we've done everything we want to do, thereby ending our turn.
				// This will make our code wait until the next turn, and then perform this loop again.
				Clock.yield();
			}
			// End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
		}
		
		// Your code should never reach here (unless it's intentional)! Self-destruction imminent...
	}
}