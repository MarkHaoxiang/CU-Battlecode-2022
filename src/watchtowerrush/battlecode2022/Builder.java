package watchtowerrush.battlecode2022;

import battlecode.common.*;
import watchtowerrush.RunnableBot;
import watchtowerrush.battlecode2022.util.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class Builder extends RunnableBot
{
	// Move towards metal strategy (or it could be scouting strategy, or etc., but basically it's a strategy to move)
	private final DefaultMoveStrategy default_moving_strategy = new DefaultMoveStrategy();
	private MoveStrategy current_moving_strategy;
	private Direction enemy_dir;
	// Same here
	private final DefaultBuildStrategy default_building_strategy = new DefaultBuildStrategy();
	private BuildStrategy current_building_strategy;
	private RobotInfo[] enemies;
	
	public Builder(RobotController rc) throws GameActionException
	{
		super(rc);
	}
	
	@Override
	public void init() throws GameActionException
	{
		super.init();
	}
	
	// Strategy
	
	interface MoveStrategy
	{
		boolean move() throws GameActionException;
	}
	
	interface BuildStrategy
	{
		boolean build() throws GameActionException;
	}
	
	class DefaultMoveStrategy implements MoveStrategy
	{
		private MapLocation move_target = navigator.randomLocation();
		private final int GIVE_UP_THRESHOLD_TURN = 2;
		/* Number of turns to give up moving if repeatedly stuck
		   Note that it is necessary because it might get surrounded by robots
		   It is also probably related to bytecode limit, but we don't account for that now */
		
		@Override
		public boolean move() throws GameActionException
		{
			// Move away from the closest enemy if found
			// Reuse the enemies[] info fetched in observeNearbyEnemies()
			
			if (enemies.length > 0)
			{
				Arrays.sort(enemies, new Comparator<RobotInfo>()
				{
					@Override
					public int compare(RobotInfo a, RobotInfo b)
					{
						return Navigator.travelDistance(Cache.controller.getLocation(), a.getLocation()).
								compareTo(Navigator.travelDistance(Cache.controller.getLocation(), b.getLocation()));
					}
				});
				move_target = enemies[0].getLocation();
				enemy_dir = Cache.controller.getLocation().directionTo(move_target);
				Direction move_dir = enemy_dir.opposite();
				if (Cache.controller.canMove(move_dir))
				{
					Cache.controller.move(move_dir);
					return true;
				}
				return false;
			}
			else // If no enemies nearby, either moves towards the closest reported location or chooses a random location
			{
				final Direction[] directions = {
						Direction.NORTH,
						Direction.EAST,
						Direction.SOUTH,
						Direction.WEST,
				};
				Random rng = new Random(Cache.controller.getID() ^ Cache.controller.getRoundNum());
				enemy_dir = directions[rng.nextInt(4)];
				Navigator.MoveResult move_result = navigator.move(move_target);
				// Only changes the target if moving fails
				int tot_attempts = 0;
				while (move_result == Navigator.MoveResult.IMPOSSIBLE ||
						move_result == Navigator.MoveResult.REACHED || move_result == Navigator.MoveResult.FAIL)
				{
					move_target = navigator.randomLocation();
					move_result = navigator.move(move_target);
					if (move_result == Navigator.MoveResult.SUCCESS)
						return true;
					tot_attempts++;
					if (tot_attempts >= GIVE_UP_THRESHOLD_TURN) // Give up on further attempts
						return false;
				}
			}
			return true;
		}
	}
	
	class DefaultBuildStrategy implements BuildStrategy
	{
		@Override
		public boolean build() throws GameActionException
		{
			if (Cache.controller.getActionCooldownTurns() > 0)
				return false;
			MapLocation[]
					all_loc =
					Cache.controller.getAllLocationsWithinRadiusSquared(Cache.controller.getLocation(),
							Cache.controller.getType().actionRadiusSquared);
			for (MapLocation loc : all_loc)
				if (Cache.controller.canRepair(loc))
					Cache.controller.repair(loc);
			final Direction[] directions = {
					Direction.NORTH,
					Direction.EAST,
					Direction.SOUTH,
					Direction.WEST,
			};
			for (Direction dir : directions)
				if (Cache.controller.canBuildRobot(RobotType.WATCHTOWER, dir))
				{
					Cache.controller.buildRobot(RobotType.WATCHTOWER, dir);
					return true;
				}
			return false;
		}
	}
	
	private void observeNearbyEnemies() throws GameActionException
	{
		// Upload enemy locations
		// Fetch enemies[] info. Also used later
		// TODO: Squeeze for bytecode
		int radius = Cache.controller.getType().visionRadiusSquared;
		Team opponent = Cache.controller.getTeam().opponent();
		enemies = Cache.controller.senseNearbyRobots(radius, opponent);
		for (RobotInfo enemy : enemies)
			switch (enemy.getType())
			{
				case SOLDIER:
					MatrixCommunicator.update(Communicator.Event.SOLDIER, enemy.location);
					break;
				case ARCHON:
					MatrixCommunicator.update(Communicator.Event.ARCHON, enemy.location);
					int x = enemy.location.x;
					int y = enemy.location.y;
					Cache.controller.writeSharedArray(2,x | (y << 8));
					break;
				default:
					break;
			}
	}
	
	@Override
	public void turn() throws GameActionException
	{
		Cache.update();
		
		observeNearbyEnemies();
		
		current_moving_strategy = default_moving_strategy;
		current_moving_strategy.move();
		
		current_building_strategy = default_building_strategy;
		current_building_strategy.build();
	}
}
