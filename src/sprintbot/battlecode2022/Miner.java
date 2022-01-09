package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

import javax.naming.OperationNotSupportedException;
import java.util.*;


public class Miner extends RunnableBot
{
	// Move towards metal strategy (or it could be scouting strategy, or etc., but basically it's a strategy to move)
	private MoveStrategy current_moving_strategy;
	private final DefaultMoveStrategy default_moving_strategy = new DefaultMoveStrategy();
	// Same here
	private MineStrategy current_mining_strategy;
	private final DefaultMineStrategy default_mining_strategy = new DefaultMineStrategy();
	
	public Miner(RobotController rc) throws GameActionException
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
	
	interface MineStrategy
	{
		boolean mine() throws GameActionException;
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
			// Move towards the closest lead if found
			MapLocation[] lead_spots = Cache.controller.senseNearbyLocationsWithLead(RobotType.MINER.visionRadiusSquared);
			
			if (lead_spots.length > 0)
			{
				Arrays.sort(lead_spots, new Comparator<MapLocation>()
				{
					@Override
					public int compare(MapLocation a, MapLocation b)
					{
						return Navigator.travelDistance(Cache.controller.getLocation(), a).
								compareTo(Navigator.travelDistance(Cache.controller.getLocation(), b));
					}
				});
				move_target = lead_spots[0];
				Navigator.MoveResult move_result = navigator.move(move_target);
				if (move_result == Navigator.MoveResult.IMPOSSIBLE ||
						move_result == Navigator.MoveResult.REACHED)
					return false;
				// TODO: Fix bug here where location might not be uploaded
				// Report lead location [COMMUNICATE]
				MatrixCommunicator.update(Communicator.Event.METAL, move_target);
			}
			else // If no leads nearby, either moves towards the closest reported location or chooses a random location
			{
				Navigator.MoveResult move_result = navigator.move(move_target);
				// Only changes the target if moving fails
				int tot_attempts = 0;
				while (move_result == Navigator.MoveResult.IMPOSSIBLE ||
						move_result == Navigator.MoveResult.REACHED)
				{
					// TODO: Fix bug here where location might not be fetched
					// Update reported location to get the desired target [COMMUNICATE]
					MatrixCommunicator.read(Communicator.Event.METAL);
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
	
	class DefaultMineStrategy implements MineStrategy
	{
		@Override
		public boolean mine() throws GameActionException
		{
			// TODO: Use better strategy?, like farming? Prioritize mining grids that are close to enemy workers?
			// Try to mine on squares around us. (Still uses the default mining method)
			MapLocation me = Cache.controller.getLocation();
			for (int dx = -1; dx <= 1; dx++)
				for (int dy = -1; dy <= 1; dy++)
				{
					MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
					// Notice that the Miner's action cooldown is very low.
					// You can mine multiple times per turn!
					while (Cache.controller.canMineGold(mineLocation))
						Cache.controller.mineGold(mineLocation);
					while (Cache.controller.canMineLead(mineLocation))
						Cache.controller.mineLead(mineLocation);
				}
			return true;
		}
	}
	
	private void observeNearbyEnemies() throws GameActionException
	{
		int radius = Cache.controller.getType().actionRadiusSquared;
		Team opponent = Cache.controller.getTeam().opponent();
		RobotInfo[]
				enemies =
				Cache.controller.senseNearbyRobots(radius, opponent);
		// TODO: Make full use of enemy information here
		if (enemies.length > 0)
		{
			MapLocation toAttack = enemies[0].location;
			// Upload enemy locations, here only the first one is uploaded for easier debugging
			QueueCommunicator.push(Communicator.Event.SOLDIER, Communicator.compressLocation(toAttack));
		}
	}
	
	@Override
	public void turn() throws GameActionException
	{
		
		Cache.update();
		
		// Local variables save bytecode -> save to Cache saves more?
		
		observeNearbyEnemies();
		
		current_mining_strategy = default_mining_strategy;
		current_mining_strategy.mine();
		
		current_moving_strategy = default_moving_strategy;
		current_moving_strategy.move();
	}
}
