package watchtowerrush;

import battlecode.common.*;
import watchtowerrush.battlecode2022.*;
import watchtowerrush.battlecode2022.util.*;
import watchtowerrush.battlecode2022.util.navigation.BugNavigator;

public abstract class RunnableBot
{
	
	private final RobotController controller;
	protected Navigator navigator;
	//private Communicator communicator;
	
	// Constructor
	
	public RunnableBot(RobotController controller) throws GameActionException
	{
		this.controller = controller;
		this.navigator = new BugNavigator(controller);
		Cache.init(controller);
		Communicator.init(controller);
		// Init for strategy, setup before!
		this.init();
	}
	
	// Methods
	public RobotController getRobotController()
	{
		return controller;
	}
	
	// To Implement
	
	public void init() throws GameActionException
	{
	}
	
	public abstract void turn() throws GameActionException;
	
	
}