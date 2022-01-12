package watchtowerrush.battlecode2022.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class MainForceCommunicator extends Communicator
{
	/*
	 *  Main force attack strategy communication schema
	 *  Uses the integer at index No.2 (i.e. id = 1), 16 bits
	 */
	
	public static void update(MapLocation current_loc) throws GameActionException
	{
		int mask = current_loc.x | (current_loc.y << 8);
		controller.writeSharedArray(1, mask);
	}
	
	// TODO: Better dispatch point randomization
	// Select a dispatch point
	public static void updateDispatchPoint(MapLocation archon_loc) throws GameActionException
	{
		int x = archon_loc.x;
		int y = archon_loc.y;
		int dx = 6;
		int dy = 6;
		if (x - dx >= 0)
			x -= dx;
		else
			x += dx;
		if (y - dy >= 0)
			y -= dy;
		else
			y += dy;
		update(new MapLocation(x, y));
	}
	
	public static void read() throws GameActionException
	{
		int mask = controller.readSharedArray(1);
		int y_mask = mask >> 8;
		int x_mask = mask - (y_mask << 8);
		Cache.main_force_location = new MapLocation(x_mask, y_mask);
	}
}
