package testenemy;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonPlayer implements InterfacePlayer
{
	static int age = 0;
	static final CustomRNG rng = new CustomRNG(6147);
	
	@Override
	public void create(RobotController rc)
			throws GameActionException
	{
	
	}
	
	@Override
	public void run(RobotController rc)
			throws GameActionException
	{
		age++;
		rng.adaptSeed(rc);
		for (int t = 0; t <= 20; t++)
		{
			Direction dir = rng.nextDirection();
			if (rc.canBuildRobot(RobotType.SAGE, dir))
			{
				rc.buildRobot(RobotType.SAGE, dir);
			}
			if (age <= 300)
			{
				if (rc.canBuildRobot(RobotType.MINER, dir))
					rc.buildRobot(RobotType.MINER, dir);
			}
			else
			{
				if (rc.canBuildRobot(RobotType.SOLDIER, dir))
					rc.buildRobot(RobotType.SOLDIER, dir);
			}
		}
	}
}
