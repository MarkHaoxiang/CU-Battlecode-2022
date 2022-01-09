package testenemy;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public interface InterfacePlayer
{
	void create(RobotController rc) throws
			GameActionException;
	void run(RobotController rc) throws
			GameActionException;
}
