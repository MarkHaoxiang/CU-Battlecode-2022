package watchtowerrush.battlecode2022;

import battlecode.common.*;
import testenemy.CustomRNG;
import watchtowerrush.RunnableBot;
import watchtowerrush.battlecode2022.util.*;

import javax.naming.OperationNotSupportedException;
import java.util.*;


public class Miner extends RunnableBot
{
	public Miner(RobotController rc) throws GameActionException
	{
		super(rc);
	}
	
	@Override
	public void init()
			throws GameActionException
	{
		super.init();
	}
	
	static final Random rng = new Random(6147);
	
	/**
	 * Array containing all the possible movement directions.
	 */
	static final Direction[] directions = {
			Direction.NORTH,
			Direction.NORTHEAST,
			Direction.EAST,
			Direction.SOUTHEAST,
			Direction.SOUTH,
			Direction.SOUTHWEST,
			Direction.WEST,
			Direction.NORTHWEST,
	};
	
	@Override
	public void turn()
			throws GameActionException
	{
		RobotController rc = getRobotController();
		MapLocation me = rc.getLocation();
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
				// Notice that the Miner's action cooldown is very low.
				// You can mine multiple times per turn!
				while (rc.canMineGold(mineLocation))
				{
					rc.mineGold(mineLocation);
				}
				while (rc.canMineLead(mineLocation))
				{
					rc.mineLead(mineLocation);
				}
			}
		}
		
		// Also try to move randomly.
		MapLocation[] all_loc = Cache.controller.senseNearbyLocationsWithLead(Cache.controller.getType().visionRadiusSquared);
		if (all_loc.length == 0)
		{
			Direction dir = directions[rng.nextInt(8)];
			if (rc.canMove(dir))
			{
				rc.move(dir);
			}
		}
		else
		{
			int best = 0;
			MapLocation cur_loc = Cache.controller.getLocation();
			for (int i = 1; i < all_loc.length; i++)
				if (all_loc[i].distanceSquaredTo(cur_loc) < all_loc[best].distanceSquaredTo(cur_loc))
					best = i;
			Direction dir = cur_loc.directionTo(all_loc[best]);
			if (rc.canMove(dir))
				rc.move(dir);
		}
	}
}
