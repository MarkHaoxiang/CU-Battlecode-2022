package testenemy;

import battlecode.common.*;

public class SagePlayer implements InterfacePlayer
{
	static CustomRNG rng;
	static Direction dirPrimary;
	static int failedMoveAttemptCnt = 0;
	static int age = 0;
	
	@Override
	public void create(RobotController rc)
			throws GameActionException
	{
		rng = new CustomRNG(rc);
		dirPrimary = rng.nextDirection();
		PathFindingOld.init(rc);
	}
	
	@Override
	public void run(RobotController rc)
			throws GameActionException
	{
		age++;
		rng.adaptSeed(rc);
		int radius = rc.getType().actionRadiusSquared;
		Team opponent = rc.getTeam().opponent();
		RobotInfo[]
				enemies =
				rc.senseNearbyRobots(radius, opponent);
		if (enemies.length > 0)
		{
			MapLocation toAttack = enemies[0].location;
			if (rc.canAttack(toAttack))
			{
				rc.attack(toAttack);
			}
			int X = toAttack.x;
			int Y = toAttack.y;
			rc.writeSharedArray(5, X);
			rc.writeSharedArray(6, Y);
		}
		else
		{
			int X = rc.readSharedArray(5);
			int Y = rc.readSharedArray(6);
			if (PathFindingOld.move(new MapLocation(X, Y)) == 3)
			{
				X = rng.nextInt(rc.getMapWidth());
				Y = rng.nextInt(rc.getMapHeight());
				rc.writeSharedArray(5, X);
				rc.writeSharedArray(6, Y);
			}
			return;
		}
		
		if (rng.trueWithProbability(0.8))
			dirPrimary = rng.nextDirection();
		if (rc.canMove(dirPrimary))
		{
			rc.move(dirPrimary);
			failedMoveAttemptCnt = 0;
		}
		else
		{
			failedMoveAttemptCnt++;
			if (failedMoveAttemptCnt > 5)
				dirPrimary = rng.nextDirection();
		}
	}
}
