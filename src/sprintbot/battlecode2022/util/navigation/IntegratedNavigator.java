package sprintbot.battlecode2022.util.navigation;

import battlecode.common.*;
import sprintbot.battlecode2022.util.*;

public class IntegratedNavigator extends Navigator
{
	private Navigator dpNavigator;
	private int dpBytecodeCostUpperBound;
	private final BugNavigator bugNavigator = new BugNavigator(controller);
	private Navigator currentNavigator = null;
	private int stuckTurns;
	
	public IntegratedNavigator(RobotController controller)
	{
		super(controller);
		switch (controller.getType().visionRadiusSquared)
		{
			case 20:
				dpNavigator = new DPR20Navigator(controller);
				dpBytecodeCostUpperBound = 5000; // TODO: Adjust
				break;
			case 34:
				dpNavigator = new DPR34Navigator(controller);
				dpBytecodeCostUpperBound = 7000; // TODO: Adjust
				break;
			case 53:
				dpNavigator = new DPR53Navigator(controller);
				dpBytecodeCostUpperBound = 10000; // TODO: Adjust
				break;
			default:
				System.out.printf("Error - detected in IntegratedNavigator - received a vision range of %d which doesn't belong to {20,34,53}\n",
						controller.getType().visionRadiusSquared);
				break;
		}
	}
	
	private void pickNavigator()
	{
		/*
			Bytecode Costs:
			dp:5000 +
			bug:500
			When stuck, try using bug navigator instead
		*/
		int bytecodeLeft = Clock.getBytecodesLeft();
		if (bytecodeLeft > dpBytecodeCostUpperBound && stuckTurns <= 20)
			currentNavigator = dpNavigator;
		else
			currentNavigator = bugNavigator;
	}
	
	@Override
	public MoveResult move(MapLocation target_location) throws GameActionException
	{
		pickNavigator();
		//if (Constants.DEBUG)
		//	System.out.printf("Before Navigation: [Bytecode left: %d, Using %s]\n", Clock.getBytecodesLeft(), currentNavigator.getClass());
		MoveResult move_result = currentNavigator.move(target_location);
		//if (Constants.DEBUG)
		//	System.out.printf("After Navigation: [Bytecode left: %d, Move result: %s]\n", Clock.getBytecodesLeft(), move_result);
		switch (move_result)
		{
			case SUCCESS:
			case REACHED:
				stuckTurns = 0;
				break;
			default:
				stuckTurns++;
				break;
		}
		return move_result;
	}
}
