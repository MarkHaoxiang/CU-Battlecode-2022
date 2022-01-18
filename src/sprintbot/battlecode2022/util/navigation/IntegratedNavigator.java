package sprintbot.battlecode2022.util.navigation;

import battlecode.common.*;
import sprintbot.battlecode2022.util.*;

public class IntegratedNavigator extends Navigator
{
	private Navigator dpWorseNavigator, dpBetterNavigator;
	private int dpWorseBytecodeCostUpperBound, dpBetterBytecodeCostUpperBound;
	private final BugNavigator bugNavigator = new BugNavigator(controller);
	private Navigator currentNavigator = null;
	private int stuckTurns;
	
	public IntegratedNavigator(RobotController controller)
	{
		super(controller);
		switch (controller.getType().visionRadiusSquared)
		{
			case 20:
				dpWorseNavigator = new DPR16Navigator(controller);
				dpWorseBytecodeCostUpperBound = 3500;
				dpBetterNavigator = new DPR20Navigator(controller);
				dpBetterBytecodeCostUpperBound = 5000;
				break;
			case 34:
				dpWorseNavigator = new DPR20Navigator(controller);
				dpWorseBytecodeCostUpperBound = 5000;
				dpBetterNavigator = new DPR34Navigator(controller);
				dpBetterBytecodeCostUpperBound = 7000; // TODO: Adjust
				break;
			case 53:
				dpWorseNavigator = new DPR34Navigator(controller);
				dpWorseBytecodeCostUpperBound = 7000;
				dpBetterNavigator = new DPR53Navigator(controller);
				dpBetterBytecodeCostUpperBound = 9000; // TODO: Adjust
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
		if (bytecodeLeft > dpBetterBytecodeCostUpperBound && stuckTurns <= 20)
			currentNavigator = dpBetterNavigator;
		else if (bytecodeLeft > dpWorseBytecodeCostUpperBound && stuckTurns <= 20)
			currentNavigator = dpWorseNavigator;
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
