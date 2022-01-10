package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;
import sprintbot.battlecode2022.util.navigation.GreedyNavigator;

import java.util.Arrays;
import java.util.Comparator;

public class Soldier extends RunnableBot
{
	// Move towards metal strategy (or it could be scouting strategy, or etc., but basically it's a strategy to move)
	private MoveStrategy current_moving_strategy;
	private final DefaultMoveStrategy default_moving_strategy = new DefaultMoveStrategy();
	// Same here
	private AttackStrategy current_attacking_strategy;
	private final DefaultAttackStrategy default_attacking_strategy = new DefaultAttackStrategy();
	private RobotInfo[] enemies;
	
	public Soldier(RobotController rc) throws GameActionException
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
	
	interface AttackStrategy
	{
		boolean attack() throws GameActionException;
	}
	
	class DefaultMoveStrategy implements MoveStrategy
	{
		private MapLocation move_target = null;
		private final int GIVE_UP_THRESHOLD_TURN = 1;
		/* Number of turns to give up moving if repeatedly stuck
		   Note that it is necessary because it might get surrounded by robots
		   It is also probably related to bytecode limit, but we don't account for that now */
		
		@Override
		public boolean move() throws GameActionException
		{
			// Move towards the closest enemy if found
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
				Navigator.MoveResult move_result = navigator.move(move_target);
				if (move_result == Navigator.MoveResult.IMPOSSIBLE ||
						move_result == Navigator.MoveResult.REACHED)
					return false;
				// TODO: Fix bug here where location might not be uploaded
				// Report enemy location [COMMUNICATE]
				MatrixCommunicator.update(Communicator.Event.SOLDIER, move_target);
			}
			else // If no enemies nearby, either moves towards the closest reported location or chooses a random location
			{
				Navigator.MoveResult move_result = navigator.move(move_target);
				// Only changes the target if moving fails
				int tot_attempts = 0;
				while (move_result == Navigator.MoveResult.IMPOSSIBLE ||
						move_result == Navigator.MoveResult.REACHED)
				{
					// TODO: Fix bug here where location might not be fetched
					// Update reported location to get the desired target [COMMUNICATE]
					MatrixCommunicator.read(Communicator.Event.SOLDIER);
					move_target = Communicator.getClosestFromCompressedLocationArray(Cache.metal_compressed_locations,
							Cache.controller.getLocation());
					// If nothing is available then choose a random location
					if (move_target == null)
						move_target = navigator.randomLocation();
					move_result = navigator.move(move_target);
					tot_attempts++;
					if (tot_attempts >= GIVE_UP_THRESHOLD_TURN) // Give up on further attempts
						return false;
				}
			}
			return true;
		}
	}
	
	class DefaultAttackStrategy implements AttackStrategy
	{
		@Override
		public boolean attack() throws GameActionException
		{
			// TODO: Better implementation like prioritize
			// Reuse the enemies[] info fetched in observeNearbyEnemies()
			if (enemies.length > 0)
			{
				MapLocation toAttack = enemies[0].location;
				if (Cache.controller.canAttack(toAttack))
					Cache.controller.attack(toAttack);
				// TODO: Upload all enemy locations here if bytecodes permit
				MatrixCommunicator.update(Communicator.Event.SOLDIER, enemies[0].location);
			}
			return true;
		}
	}
	
	private void observeNearbyEnemies()
	{
		// Fetch enemies[] info. Also used later
		int radius = Cache.controller.getType().actionRadiusSquared;
		Team opponent = Cache.controller.getTeam().opponent();
		enemies = Cache.controller.senseNearbyRobots(radius, opponent);
	}
	
	@Override
	public void turn() throws GameActionException
	{
		Cache.update();

		observeNearbyEnemies();
		
		current_attacking_strategy = default_attacking_strategy;
		current_attacking_strategy.attack();
		
		current_moving_strategy = default_moving_strategy;
		current_moving_strategy.move();
	}
}