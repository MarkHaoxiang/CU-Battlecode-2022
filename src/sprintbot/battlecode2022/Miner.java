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
	private final SearchMoveStrategy search_moving_strategy = new SearchMoveStrategy();
	private MoveStrategy current_moving_strategy = search_moving_strategy;
	// Same here
	private MineStrategy current_mining_strategy;
	private final DefaultMineStrategy default_mining_strategy = new DefaultMineStrategy();
	private final RunStrategy run_strategy = new RunStrategy();

	int income = 0;

	enum MinerState {
		SEARCHING,
		MINING,
		RUNNING
	};

	protected MinerState state = MinerState.SEARCHING;
	public static int LEAD_MINE_THRESHOLD = 1; // Do not mine if <= 1
	
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

	@Override
	public void turn() throws GameActionException
	{

		Cache.update();

		/*
		if (Navigator.travelDistance(Cache.MY_SPAWN_LOCATION, getRobotController().getLocation()) >= 8 &&
				(Cache.opponent_soldiers.length > Cache.friendly_soldiers.length
				|| Cache.friendly_soldiers.length == 0 && Cache.opponent_buildings.length > 0)
		) {
			LEAD_MINE_THRESHOLD = 0;
		}
		else {
			LEAD_MINE_THRESHOLD = 1;
		}
		*/


		// Update info for archons
		income = 0;
		getRobotController().writeSharedArray(CommandCommunicator.TOTAL_FARMER_INDEX,
				getRobotController().readSharedArray(CommandCommunicator.TOTAL_FARMER_INDEX)+1);

		// Local variables save bytecode -> save to Cache saves more?
		current_mining_strategy = default_mining_strategy;
		current_mining_strategy.mine();


		if (state == MinerState.RUNNING || run_strategy.should_run()) {
			current_moving_strategy.close();
			current_moving_strategy = run_strategy;
		}
		else if (state == MinerState.SEARCHING) {
			current_moving_strategy.close();
			current_moving_strategy = search_moving_strategy;
		}

		if (Constants.DEBUG) {
			getRobotController().setIndicatorString(current_moving_strategy.toString());
		}

		if (state == MinerState.SEARCHING) {
			getRobotController().writeSharedArray(CommandCommunicator.IDLE_FARMER_INDEX,
					getRobotController().readSharedArray(CommandCommunicator.IDLE_FARMER_INDEX)+1);
		}

		current_moving_strategy.move();

		getRobotController().writeSharedArray(CommandCommunicator.INCOME_INDEX,
				getRobotController().readSharedArray(CommandCommunicator.INCOME_INDEX)+income);

	}
	
	// Strategy
	
	interface MoveStrategy
	{
		boolean move() throws GameActionException;
		void close() throws GameActionException;
	}
	
	interface MineStrategy
	{
		boolean mine() throws GameActionException;
	}


	// Find a good patch of metal to mine
	class SearchMoveStrategy implements MoveStrategy
	{
		private MapLocation move_target = null;
		private final int GIVE_UP_THRESHOLD_TURN = 3;
		/* Number of turns to give up moving if repeatedly stuck
		   Note that it is necessary because it might get surrounded by robots
		   It is also probably related to bytecode limit, but we don't account for that now */

		public SearchMoveStrategy () {

			// Select a direction to scout
				// Did archon assign a location?
			if (assigned_location != null && assigned_location != getRobotController().getLocation()) {
				move_target = assigned_location;
				assigned_location = null;
			}
			else if (move_target == null) {
				move_target = navigator.randomLocation();
			}
		}

		@Override
		public void close() throws GameActionException {
			return;
		}

		@Override
		public boolean move() throws GameActionException
		{
			MapLocation closest = null;
			MapLocation my_location = getRobotController().getLocation();
			int closest_distance = 9999;

			if (Cache.lead_spots.length > 0) {
				for (MapLocation lead_spot : Cache.lead_spots) {
					int d = Navigator.travelDistance(my_location,lead_spot);
					int lead_amt = getRobotController().senseLead(lead_spot);
					if (lead_amt <= LEAD_MINE_THRESHOLD) {
						// Not enough lead
						continue;
					}
					if (lead_amt < 8 && Cache.friendly_villagers.length >= 1) {
						// Don't even bother, someone will mine it eventually
						continue;
					}
					if (lead_amt < 20) {
						boolean someone_closer = false;
						for (RobotInfo robot : Cache.friendly_villagers) {
							if (robot.getType() == RobotType.MINER
									&& Navigator.travelDistance(robot.getLocation(),my_location) < d) {
								// Don't bother, someone else is in a better position to mine
								someone_closer = true;
							}
						}
						if (someone_closer) continue;
					}
					if (MatrixCommunicator.read(Communicator.Event.FRIENDLY_MINER,lead_spot) && lead_amt < 10) {
						// Friendly miner already allocated
						continue;
					}
					// Mining location found!
					if (d < closest_distance) {
						closest_distance = d;
						closest = lead_spot;
					}
					move_target = closest;
					state = MinerState.MINING;
				}
			}

			// No free lead nearby
			// TODO: Check nearby communicator map
			if (closest == null
					&& move_target != null
					&& getRobotController().canSenseLocation(move_target)) {
				int lead = getRobotController().senseLead(move_target);
				if (lead >= 1 && lead <= 2) {
					// Someone else mined it
					move_target = null;
				}
			}
			if (move_target == null) {
				move_target = navigator.randomLocation();
				/*
				int tries = 0;
				while (MatrixCommunicator.read(Communicator.Event.FRIENDLY_MINER,move_target) && tries < 5) {
					move_target = navigator.randomLocation();
					tries += 1;
				}
				 */
			}

			int distance = move_target.distanceSquaredTo(my_location);
			Navigator.MoveResult move_result = Navigator.MoveResult.REACHED;
			if (closest == null || distance > 2 || distance < 2 && getRobotController().senseRubble(move_target) < getRobotController().senseRubble(my_location)) {
				move_result = navigator.move(move_target);
			}

			switch (move_result) {
				case FAIL:
					return false;
				case IMPOSSIBLE:
					move_target = navigator.randomLocation();
					navigator.move(move_target);
					return true;
				case REACHED:
					if (closest != null
							&& !MatrixCommunicator.read(Communicator.Event.FRIENDLY_MINER,closest)) {
						// Maybe adjacent square miner is already camping this location
						//getRobotController().setIndicatorString(Integer.toString(Cache.friendly_villagers.length));
						//TODO: Needs improvement
						for (RobotInfo robot : Cache.friendly_villagers) {
							if (robot.getType() == RobotType.MINER
									&& Navigator.travelDistance(robot.getLocation(),my_location) <= 3
									&& MatrixCommunicator.read(Communicator.Event.FRIENDLY_MINER,robot.getLocation())) {
								return true;
							}
						}
						if (Cache.lead_spots.length <= 4) {
							return true;
						}
						state = MinerState.MINING;
						current_moving_strategy.close();
						current_moving_strategy = new CampMoveStrategy(closest);
					}
					else if (closest == null) {
						move_target = navigator.randomLocation();
						navigator.move(move_target);
					}
					return true;
				case SUCCESS:
				default:
					return true;
			}
		}
	}

	// We found the patch. Mine it. Claim it if mostly mined out.
	class CampMoveStrategy implements MoveStrategy {

		private MapLocation move_target = null;
		private MapLocation[] mine_locations;
		private MapLocation start_location;

		public CampMoveStrategy(MapLocation location) throws GameActionException {
			mine_locations = Cache.lead_spots;
			start_location = location;
			labelMining();
		}

		@Override
		public void close() throws GameActionException {
			MatrixCommunicator.update(Communicator.Event.FRIENDLY_MINER,start_location,false);
		}

		@Override
		public boolean move() throws GameActionException {

			if (Constants.DEBUG) {
				String temp = MatrixCommunicator.read(Communicator.Event.FRIENDLY_MINER,getRobotController().getLocation()).toString();
				temp = temp + start_location.toString();
				getRobotController().setIndicatorString(temp);
			}

			MapLocation closest = null;
			MapLocation my_location = getRobotController().getLocation();
			int closest_distance = 9999;
			move_target = start_location;

			// Better place to mine
			if (Cache.lead_spots.length > mine_locations.length && Cache.lead_amount > 50
					&&
					(Communicator.compressLocation(my_location) == Communicator.compressLocation(start_location)
							|| !MatrixCommunicator.read(Communicator.Event.FRIENDLY_MINER,my_location)))
			{
				close();
				mine_locations = Cache.lead_spots;
				start_location = getRobotController().getLocation();
				labelMining();
			}


			//int mine_potential = 0;
			if (mine_locations.length > 0) {
				for (MapLocation lead_spot : mine_locations) {
					if (!getRobotController().canSenseLocation(lead_spot)) continue;
 					int lead_amount = getRobotController().senseLead(lead_spot);
					//mine_potential += lead_amount;
					if (lead_amount <= LEAD_MINE_THRESHOLD) {
						// Not enough lead
						continue;
					}
					if (MatrixCommunicator.read(Communicator.Event.FRIENDLY_MINER,lead_spot)) {
						// Friendly miner already allocated
						continue;
					}
					int d = Navigator.travelDistance(my_location,lead_spot);
					// Mining location found!
					if (d < closest_distance) {
						closest_distance = d;
						closest = lead_spot;
					}
				}

				if (closest != null) {
					move_target = closest;
				}
				else if (mine_locations.length <= 1) {
					close();
					state = MinerState.SEARCHING;
				}
				Navigator.MoveResult move_result = navigator.move(move_target);
				switch (move_result) {
					case FAIL:
						return false;
					case SUCCESS:
					default:
						return true;
				}
			}
			else {
				return false;
			}
		}

		private void labelMining() throws GameActionException {
			MatrixCommunicator.update(Communicator.Event.FRIENDLY_MINER,start_location);
		}

	}

	class RunStrategy implements MoveStrategy
	{
		private final int HP_THRESHOLD = 15;

		@Override
		public void close() throws GameActionException {
			return;
		}

		public boolean should_run () {
			RobotController controller = getRobotController();
			if (Cache.opponent_soldiers.length > Cache.friendly_soldiers.length
					|| controller.getHealth() < HP_THRESHOLD
					|| Cache.opponent_total_damage > controller.getHealth()
					|| Cache.lead_amount < 20 && Cache.opponent_soldiers.length > 0
			) {
				return true;
			}
			return false;
		}

		@Override
		public boolean move() throws GameActionException {
			search_moving_strategy.move_target = null;
			RobotController controller = getRobotController();
			Direction direction = null;
			Integer closest = Integer.MAX_VALUE;
			if (should_run()) {
				state = MinerState.RUNNING;

				/*
				for (RobotInfo robot : Cache.opponent_soldiers) {
					int attack_radius = robot.getType().actionRadiusSquared;
					int distance = attack_radius-controller.getLocation().distanceSquaredTo(robot.getLocation());
					if (closest > distance) {
						closest = distance;
						direction = controller.getLocation().directionTo(robot.getLocation()).opposite();
					}
				}
				*/
				if (Cache.injured >= 3 && Cache.can_see_archon && controller.senseLead(controller.getLocation()) == 0) {
					controller.disintegrate();
					return true;
				}
				if (navigator.move(Cache.MY_SPAWN_LOCATION) == Navigator.MoveResult.SUCCESS) return true;

			}

			/*
			if (direction != null) {
				if (navigator.move(direction) == Navigator.MoveResult.SUCCESS) {
					return true;
				}
				else {
					if (navigator.move(Cache.MY_SPAWN_LOCATION) == Navigator.MoveResult.SUCCESS) return true;
				}
			}
			*/

			if (state == MinerState.RUNNING) {
				state = MinerState.SEARCHING;
			}
			return false;
		}
	}

	class DefaultMineStrategy implements MineStrategy
	{
		@Override
		public boolean mine() throws GameActionException
		{
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
					while (Cache.controller.canMineLead(mineLocation)
							&& ((Cache.controller.senseLead(mineLocation) > LEAD_MINE_THRESHOLD)
							|| (Cache.controller.senseLead(mineLocation) > 0 && Cache.opponent_soldiers.length > Cache.friendly_soldiers.length + 1))) {
						Cache.controller.mineLead(mineLocation);
						income += 1;
					}
				}
			return true;
		}
	}
}
