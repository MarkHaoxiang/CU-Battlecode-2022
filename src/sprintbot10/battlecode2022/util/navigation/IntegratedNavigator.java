package sprintbot10.battlecode2022.util.navigation;

import battlecode.common.*;
import sprintbot10.battlecode2022.util.*;

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
				dpBytecodeCostUpperBound =5500; // TODO: Adjust
				break;
			case 34:
				dpNavigator = new DPR34Navigator(controller);
				dpBytecodeCostUpperBound = 7500; // TODO: Adjust
				break;
			case 53:
				dpNavigator = new DPR53Navigator(controller);
				dpBytecodeCostUpperBound = 10500; // TODO: Adjust
				break;
			default:
				System.out.printf("Error - detected in IntegratedNavigator - received a vision range of %d which doesn't belong to {20,34,53}\n",
						controller.getType().visionRadiusSquared);
				break;
		}
	}

	private void pickNavigator(MapLocation target_location) throws GameActionException
	{
		/*
			Bytecode Costs:
			dp:5000 +
			bug:500
			When stuck, try using bug navigator instead
		*/

		//  DP Naivgator doesn't work well when robots nearby
		MapLocation my_location = controller.getLocation();
		Direction dir = controller.getLocation().directionTo(target_location);
		for (MapLocation location : new MapLocation[] {
				my_location.add(dir),
				my_location.add(dir.rotateRight()),
				my_location.add(dir.rotateLeft())
		}) {
			if (controller.onTheMap(location) && controller.senseRobotAtLocation(location) == null) {
				currentNavigator = bugNavigator;
				return;
			}
		}

		int bytecodeLeft = Clock.getBytecodesLeft();
		if (bytecodeLeft > dpBytecodeCostUpperBound && stuckTurns <= 20)
			currentNavigator = dpNavigator;
		else
			currentNavigator = bugNavigator;
	}

	@Override
	public MoveResult move(MapLocation target_location) throws GameActionException
	{

		MapLocation current_location = controller.getLocation();

		// Is target out of the map
		if (target_location == null ||
				!controller.onTheMap(current_location.add(
						current_location.directionTo(
								target_location)))) return MoveResult.IMPOSSIBLE;

		// At destination
		if (target_location.equals(current_location)) {
			return MoveResult.REACHED;
		}

		// Our miners have priority, don't disturb them
		// Buildings are walls
		if (current_location.distanceSquaredTo(target_location) <= 2
				&& controller.canSenseLocation(target_location)) {
			RobotInfo robot = controller.senseRobotAtLocation(target_location);
			if (robot != null
					&& robot.getTeam() == Cache.OUR_TEAM
					&& robot.getType() == RobotType.MINER) {
				return MoveResult.IMPOSSIBLE;
			}
			if (robot != null
					&& robot.getType().isBuilding()
					&& robot.getMode().canMove == false) {
				return MoveResult.IMPOSSIBLE;
			}
		}


		// Is it ready
		if (!controller.isMovementReady())
			return MoveResult.FAIL;

		if (Constants.DEBUG) {
			controller.setIndicatorLine(controller.getLocation(),target_location,255,255,0);
		}

		pickNavigator(target_location);

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
