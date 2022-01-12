package watchtowerrush.battlecode2022;

import battlecode.common.*;
import watchtowerrush.RunnableBot;
import watchtowerrush.battlecode2022.util.*;

import java.util.Map;

public class Watchtower extends RunnableBot
{
	int nattackTurn = 0;
	int type = 1;
	
	public Watchtower(RobotController rc) throws GameActionException
	{
		super(rc);
	}
	
	
	@Override
	public void init() throws GameActionException
	{
		super.init();
	}
	
	@Override
	public void turn() throws GameActionException
	{
		Cache.update();
		int radius = Cache.controller.getType().actionRadiusSquared;
		Team opponent = Cache.controller.getTeam().opponent();
		RobotInfo[] enemies = Cache.controller.senseNearbyRobots(radius, opponent);
		if (enemies.length > 0 && type == 0 && Cache.controller.isTransformReady())
		{
			Cache.controller.transform();
			type ^= 1;
			nattackTurn = -10000;
		}
		if (type == 0)
		{
			int mask = Cache.controller.readSharedArray(2);
			int y = mask >> 8;
			int x = mask - (y << 8);
			MapLocation move_target = new MapLocation(x, y);
			navigator.move(move_target);
			nattackTurn = -10000;
		}
		if (!Cache.controller.isActionReady()) return;
		if (enemies.length == 0)
		{
			nattackTurn++;
			if (nattackTurn >= 10)
			{
				if (Cache.controller.isTransformReady())
				{
					Cache.controller.transform();
					type ^= 1;
				}
			}
			return;
		}
		else
			nattackTurn = 0;
		int best_target = 0;
		for (int i = 1; i < enemies.length; i++)
			if (enemies[i].health < enemies[best_target].health || enemies[i].getType() == RobotType.ARCHON)
				best_target = i;
		MapLocation toAttack = enemies[best_target].location;
		if (Cache.controller.canAttack(toAttack))
		{
			Cache.controller.attack(toAttack);
			int x = toAttack.x;
			int y = toAttack.y;
			Cache.controller.writeSharedArray(2,x | (y << 8));
			nattackTurn = 0;
		}
	}
}
