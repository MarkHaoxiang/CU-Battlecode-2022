package testenemy;

import battlecode.common.*;

public class MinerPlayer implements InterfacePlayer
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
	}
	
	@Override
	public void run(RobotController rc)
			throws GameActionException
	{
		age++;
		rng.adaptSeed(rc);
		int minedTime = 0;
		MapLocation me = rc.getLocation();
		for (int dx = -1; dx <= 1; dx++)
			for (int dy = -1; dy <= 1; dy++)
			{
				MapLocation
						mineLocation =
						new MapLocation(me.x + dx,
								me.y + dy);
				while (rc.canMineGold(mineLocation))
				{
					rc.mineGold(mineLocation);
					minedTime++;
				}
				while (rc.canMineLead(mineLocation))
				{
					rc.mineLead(mineLocation);
					minedTime++;
				}
			}
		
		int radius = rc.getType().actionRadiusSquared;
		Team opponent = rc.getTeam().opponent();
		RobotInfo[]
				enemies =
				rc.senseNearbyRobots(radius, opponent);
		if (enemies.length > 0)
		{
			MapLocation toAttack = enemies[0].location;
			int X = toAttack.x;
			int Y = toAttack.y;
			rc.writeSharedArray(5, X);
			rc.writeSharedArray(6, Y);
		}
		
		if (rc.canMove(dirPrimary) && minedTime <= 1)
		{
			rc.move(dirPrimary);
			failedMoveAttemptCnt = 0;
		}
		else
		{
			failedMoveAttemptCnt++;
			if (failedMoveAttemptCnt > 5 && minedTime <= 1)
				dirPrimary = rng.nextDirection();
		}
	}
}
