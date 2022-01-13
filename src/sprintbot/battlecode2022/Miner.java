package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

import java.util.*;


public class Miner extends RunnableBot
{
	// Archon assigned role (unused for now, do something fun like farming with it)
	CommandCommunicator.RobotRole assigned_role = CommandCommunicator.RobotRole.MINER;
	MapLocation assigned_location = null;

	// Move towards metal strategy (or it could be scouting strategy, or etc., but basically it's a strategy to move)
	private MoveStrategy current_moving_strategy;
	private final DefaultMoveStrategy default_moving_strategy = new DefaultMoveStrategy();
	// Same here
	private MineStrategy current_mining_strategy;
	private final DefaultMineStrategy default_mining_strategy = new DefaultMineStrategy();
	private final RunStrategy run_strategy = new RunStrategy();
	
	public Miner(RobotController rc) throws GameActionException
	{
		super(rc);
	}
	
	@Override
	public void init() throws GameActionException
	{
		super.init();
		CommandCommunicator.SpawnOrder order = CommandCommunicator.getSpawnRole();
		assigned_role = order.role;
		assigned_location = order.loc;
		Cache.MY_SPAWN_LOCATION = getRobotController().getLocation();
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
		boolean is_random = false; // whether move_target is random or not
		private final int GIVE_UP_THRESHOLD_TURN = 1;
		/* Number of turns to give up moving if repeatedly stuck
		   Note that it is necessary because it might get surrounded by robots
		   It is also probably related to bytecode limit, but we don't account for that now */
		private static final int HP_THRESHOLD = 10;

		@Override
		public boolean move() throws GameActionException
		{
			// Re-add the metal location to matrix when HP is too low
			if (move_target != null && navigator.inMap(move_target) && Cache.controller.getHealth() < HP_THRESHOLD) {
				MatrixCommunicator.update(Communicator.Event.METAL, move_target);
			}

			// Move towards the closest lead in vision if found
			MapLocation[] lead_spots = Cache.controller.senseNearbyLocationsWithLead(RobotType.MINER.visionRadiusSquared);
			boolean should_mine = false;
			MapLocation lead_target = null;
			if (lead_spots.length > 0)
			{
				int min_v = Integer.MAX_VALUE;
				for (MapLocation lead_spot: lead_spots) {
					int v = Navigator.travelDistance(Cache.controller.getLocation(), lead_spot);
					for (MapLocation loc : navigator.adjacentLocationWithCenter(lead_spot)) {
						if (Cache.controller.canSenseLocation(loc)) {
							RobotInfo robot = Cache.controller.senseRobotAtLocation(loc);
							if (robot != null
									&& robot.getTeam().isPlayer()
									&& robot.getType() == RobotType.MINER) {
								v = Integer.MAX_VALUE;
							}
						}
					}
					if (v < min_v) {
						min_v = v;
						lead_target = lead_spot;
					}
				}
				if (min_v < Integer.MAX_VALUE) {
					should_mine = true;
				}
			}

			if (should_mine) {
				// not report the lead location, but save the location in this miner's instance state
				move_target = lead_target;
				is_random = false;

				Navigator.MoveResult move_result = navigator.move(lead_target);
				if (move_result == Navigator.MoveResult.IMPOSSIBLE ||
						move_result == Navigator.MoveResult.REACHED)
					return false;
			}

			else // If no leads nearby or shouldn't mine nearby lead, either moves towards the closest reported location and erases that location from matrix or chooses a random location
			{
				Navigator.MoveResult move_result = navigator.move(move_target);
				// Only changes the target if moving fails
				int tot_attempts = 0;

				while (move_result == Navigator.MoveResult.IMPOSSIBLE ||
						move_result == Navigator.MoveResult.REACHED)
				{
					tot_attempts++;
					if (tot_attempts > GIVE_UP_THRESHOLD_TURN) // Give up on further attempts
						return false;

					MatrixCommunicator.read(Communicator.Event.METAL);
					move_target = Communicator.getClosestFromCompressedLocationArray(Cache.metal_compressed_locations,
							Cache.controller.getLocation());
					is_random = false;

					// If nothing is available then choose a random location
					if (move_target == null) {
						move_target = navigator.randomLocation();
						is_random = true;
					}

					move_result = navigator.move(move_target);
				}

				if (!is_random && move_target != null) {
					// erase the location from the matrix
					MatrixCommunicator.update(Communicator.Event.METAL, move_target, false);
				}
			}
			return true;
		}
	}

	class RunStrategy implements MoveStrategy
	{
		@Override
		public boolean move() throws GameActionException {
			RobotController controller = getRobotController();
			RobotInfo[] robots = controller.senseNearbyRobots();

			Direction direction = null;

			Integer closest = Integer.MAX_VALUE;
			for (RobotInfo robot : robots) {
				if (robot.getTeam() != Cache.OUR_TEAM &&
						(robot.getType() == RobotType.SOLDIER || robot.getType() == RobotType.SAGE || robot.getType() == RobotType.WATCHTOWER)) {
					int attack_radius = robot.getType().actionRadiusSquared;
					int distance = attack_radius-controller.getLocation().distanceSquaredTo(robot.getLocation());
					if (closest > distance) {
						closest = distance;
						direction = controller.getLocation().directionTo(robot.getLocation()).opposite();
					}
				}
			}

			if (direction != null) {
				if (navigator.move(direction) == Navigator.MoveResult.SUCCESS) {
					return true;
				}
				else {
					if (navigator.move(Cache.MY_SPAWN_LOCATION) == Navigator.MoveResult.SUCCESS) return true;
				}
			}
			return false;
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
					while (Cache.controller.canMineLead(mineLocation) && Cache.controller.senseLead(mineLocation) > 2) {
						Cache.controller.mineLead(mineLocation);
					}
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
		
		current_moving_strategy = run_strategy;
		//current_moving_strategy = less_gathering_move_strategy;
		if (!current_moving_strategy.move());
			current_moving_strategy = default_moving_strategy;
			current_moving_strategy.move();
	}
}
