package sprintbot.battlecode2022;

		import battlecode.common.*;
		import sprintbot.RunnableBot;
		import sprintbot.battlecode2022.util.*;

public class Soldier extends RunnableBot
{
	// Move towards metal strategy (or it could be scouting strategy, or etc., but basically it's a strategy to move)
	private MoveStrategy current_moving_strategy;
	private final SearchMoveStrategy search_moving_strategy = new SearchMoveStrategy();
	private final RetreatMoveStrategy retreat_moving_strategy = new RetreatMoveStrategy();
	private final FightMoveStrategy fight_moving_strategy = new FightMoveStrategy();
	// Same here
	private AttackStrategy current_attacking_strategy;
	private final DefaultAttackStrategy default_attacking_strategy = new DefaultAttackStrategy();

	public Soldier(RobotController rc) throws GameActionException
	{
		super(rc);
	}

	@Override
	public void init() throws GameActionException
	{
		super.init();
		MatrixCommunicator.read(Communicator.Event.ARCHON);
		MatrixCommunicator.read(Communicator.Event.SOLDIER);
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


	// Macro
	class SearchMoveStrategy implements MoveStrategy
	{
		private MapLocation move_target;
		private boolean is_random = false;
		private final int GIVE_UP_THRESHOLD_TURN = 1;
		private final int IGNORE_SOLDIER_THRESHOLD = 60;

		@Override
		public boolean move() throws GameActionException
		{

			// Go for nearby soldiers first
			MapLocation my_location = getRobotController().getLocation();
			MapLocation potential_target = Communicator.getClosestFromCompressedLocationArray(Cache.opponent_soldier_compressed_locations,
					Cache.controller.getLocation());

			if (potential_target != null
					&& potential_target != my_location) {
				move_target = potential_target;
				is_random = false;
			}

			// And then opponent archons
			if (is_random || move_target == null) {
				potential_target = Communicator.getClosestFromCompressedLocationArray(Cache.opponent_archon_compressed_locations,
						Cache.controller.getLocation());
				if (potential_target != null) {
					is_random = false;
					move_target = potential_target;
				}
			}


			// If nothing is available then choose a random location
			if (move_target == null)
			{
				is_random = true;
				move_target = new MapLocation(Cache.MAP_WIDTH / 2, Cache.MAP_HEIGHT / 2);
				if (move_target.distanceSquaredTo(Cache.MY_SPAWN_LOCATION) > 35)
				{
					double k = 35 * 1.0 / move_target.distanceSquaredTo(Cache.MY_SPAWN_LOCATION);
					int dx = (int) (k * (move_target.x - Cache.MY_SPAWN_LOCATION.x));
					int dy = (int) (k * (move_target.y - Cache.MY_SPAWN_LOCATION.y));
					move_target = new MapLocation(Cache.MY_SPAWN_LOCATION.x + dx, Cache.MY_SPAWN_LOCATION.y + dy);
				}
			}


			Navigator.MoveResult move_result = navigator.move(move_target);


			switch (move_result) {
				case SUCCESS:
					getRobotController().setIndicatorString("SUCCESS");
					return true;
				case REACHED:
					// Nothing here, go somewhere else
					MatrixCommunicator.update(Communicator.Event.SOLDIER,my_location,false);
					MatrixCommunicator.update(Communicator.Event.ARCHON,my_location,false);
					move_target = null;
					return false;
				case IMPOSSIBLE:
					getRobotController().setIndicatorString("IMPOSSIBLE");
					if (Navigator.travelDistance(my_location,move_target) <= 4) {
						MatrixCommunicator.update(Communicator.Event.SOLDIER,move_target,false);
					}
					move_target = navigator.randomLocation();
					is_random = true;
					if (navigator.move(move_target) == Navigator.MoveResult.SUCCESS) {
						return true;
					};
					return false;
				case FAIL:
					getRobotController().setIndicatorString("FAIL");
				default:
					return false;
			}
		}
	}

	class RetreatMoveStrategy implements MoveStrategy
	{

		int HP_THRESHOLD = 16;
		@Override
		public boolean move() throws GameActionException
		{
			if (shouldRun()) {

				RobotController controller = getRobotController();
				Direction direction = null;
				Integer closest = Integer.MAX_VALUE;

				for (RobotInfo robot : Cache.opponent_soldiers) {
					int attack_radius = robot.getType().actionRadiusSquared;
					int distance = attack_radius-controller.getLocation().distanceSquaredTo(robot.getLocation());
					if (closest > distance) {
						closest = distance;
						direction = controller.getLocation().directionTo(robot.getLocation()).opposite();
					}
				}
				// Greedy move away
				if (Cache.can_see_archon)
				{
					for (RobotInfo robot : Cache.friendly_buildings)
					{
						if (robot.getType() == RobotType.ARCHON && robot.getLocation().isWithinDistanceSquared(controller.getLocation(), 8))
						{
							return false;
						}
					}
				}
				if (navigator.move(Cache.MY_SPAWN_LOCATION) == Navigator.MoveResult.SUCCESS) return true;
			}
			return false;
		}

		public boolean shouldRun () throws GameActionException
		{

			int health = getRobotController().getHealth();
			if (health == RobotType.SOLDIER.health) {
				return false;
			}
			if (!Cache.can_see_archon) {
				RobotController controller = getRobotController();
				//if (controller.getHealth() < HP_THRESHOLD || (controller.getHealth() <= Cache.lowest_health_soldier && Cache.can_see_archon)) {
				if (health < HP_THRESHOLD) {
					return true;
				}
			}
			else {
				if (Cache.opponent_soldiers.length > 0 && (getRobotController().getHealth() > HP_THRESHOLD || willWeWin())) {
					return false;
				}
				if (health > 15 && Cache.injured > 3) {
					return false;
				}
				MatrixCommunicator.update(Communicator.Event.SOLDIER,getRobotController().getLocation(),false);
				return true;
			}
			return false;
		}

		public boolean willWeWin () {
			if (Cache.opponent_soldiers.length == 0) {
				return true;
			}
			if (1.0 * Cache.our_total_health / Cache.opponent_total_damage >= 1.0 * Cache.opponent_total_health / Cache.our_total_damage * 1.2) {
				return true;
			}
			return false;
		}
	}

	// Micro
	class FightMoveStrategy implements MoveStrategy {

		private MapLocation move_target;

		@Override
		public boolean move() throws GameActionException {

			// TODO: Add last seen location memory
			RobotController controller = getRobotController();
			MapLocation my_location = controller.getLocation();

			// Not attacking soldier
			if (Cache.opponent_soldiers.length == 0) {
				// Prioritize buildings
				if (Cache.opponent_buildings.length > 0) {
					if (!my_location.isWithinDistanceSquared(Cache.opponent_buildings[0].getLocation(),
							RobotType.SOLDIER.actionRadiusSquared)) {
						move_target = Cache.opponent_buildings[0].getLocation();
						Navigator.MoveResult move_result = navigator.move(move_target);
						if (move_result == Navigator.MoveResult.SUCCESS) {
							return true;
						}
						return false;
					}
				}
				else if (Cache.opponent_villagers.length > 0) {
					move_target = Cache.opponent_villagers[0].getLocation();
					if (move_target.distanceSquaredTo(my_location) > 2) {
						Navigator.MoveResult move_result = navigator.move(move_target);
						if (move_result == Navigator.MoveResult.SUCCESS) {
							return true;
						}
					}
					return false;
				}
			}


			// Should we chase close to dead opponents?

			// Fighting
			// Assume we can win, code to determine should not be here
			// Heuristic time
			double best_score = -9999.0;
			MapLocation best_location = null;

			for (MapLocation location : navigator.adjacentLocationWithCenter(my_location)) {
				if (location.equals(my_location) || controller.canMove(my_location.directionTo(location))) {
					// Consider other unit types?
					double score;
					double expected_damage_from_opponents = 0;
					double expected_damage_from_opponents_move = 0;
					boolean in_range = false;
					boolean in_vision = false;
					for (RobotInfo robot : Cache.opponent_soldiers) {
						//int start = Clock.getBytecodeNum();
						MapLocation robot_location = robot.getLocation();
						in_vision = robot_location.isWithinDistanceSquared(location,RobotType.SOLDIER.visionRadiusSquared);

						/*
						MapLocation potential_robot_location_a = robot_location.add(robot_location.directionTo(location));
						MapLocation potential_robot_location_b = robot_location.add(robot_location.directionTo(location).rotateLeft());
						MapLocation potential_robot_location_c = robot_location.add(robot_location.directionTo(location).rotateRight());
						*/
						int rubble_default, rubble_a, rubble_b, rubble_c;
						rubble_default = rubble_b = rubble_c = rubble_a = 10000;
						/*
						if (controller.onTheMap(potential_robot_location_a) && location.isWithinDistanceSquared(potential_robot_location_a,RobotType.SOLDIER.actionRadiusSquared)) {
							rubble_a = controller.senseRubble(potential_robot_location_a);
						}
						if (controller.onTheMap(potential_robot_location_b) && location.isWithinDistanceSquared(potential_robot_location_b,RobotType.SOLDIER.actionRadiusSquared)) {
							rubble_b = controller.senseRubble(potential_robot_location_b);
						}
						if (controller.onTheMap(potential_robot_location_c) && location.isWithinDistanceSquared(potential_robot_location_c,RobotType.SOLDIER.actionRadiusSquared)) {
							rubble_c = controller.senseRubble(potential_robot_location_c);
						}
						 */
						if (robot_location.isWithinDistanceSquared(location,robot.getType().actionRadiusSquared)) {
							rubble_default = controller.senseRubble(robot_location);
							expected_damage_from_opponents += robot.getType().damage / ((1.0+rubble_default/10.0) * robot.getType().actionCooldown / 10.0);
						}

						//double min_rubble = Math.min(Math.min(rubble_c,Math.min(rubble_a,rubble_b)),rubble_default);
						//if (min_rubble < 1000) expected_damage_from_opponents_move += robot.getType().damage / ((1.0+ min_rubble /10.0) * robot.getType().actionCooldown / 10.0);
						if (robot_location.isWithinDistanceSquared(location,RobotType.SOLDIER.actionRadiusSquared)) {
							in_range = true;
						}
						//System.out.println(Clock.getBytecodeNum()-start);
					}

					expected_damage_from_opponents = expected_damage_from_opponents / (double)(Cache.friendly_soldiers.length + 1);
					// My expected damage output

					double rubble = controller.senseRubble(location);
					double damage = controller.getType().damage;
					double base_cooldown = controller.getType().actionCooldown;
					double expected_damage = damage / ((1.0+rubble/10.0) * base_cooldown / 10.0);

					if (!in_vision) {
						continue;
					}
					controller.setIndicatorString(Boolean.toString(retreat_moving_strategy.willWeWin()));
					// Be aggressive, prioritize our own damage
					score = 0;
					double bravery = -0.25 + controller.getHealth() / 100.0;
					expected_damage = (1-bravery) * expected_damage;
					if (controller.isActionReady() && in_range) {
						if (retreat_moving_strategy.willWeWin()
								&& Cache.friendly_soldiers.length > Cache.opponent_soldiers.length + 1
						) {
							score = expected_damage * 1.2 - expected_damage_from_opponents * 0.8;
						}
						else {
							score = expected_damage * 1.1 - expected_damage_from_opponents * 0.9;
						}
					}
					else if (controller.isActionReady() && !in_range) {
						score = expected_damage * 0.6 - expected_damage_from_opponents;
					}
					else {
						score = expected_damage - expected_damage_from_opponents;
					}
					if (score > best_score) {
						best_score = score;
						best_location = location;
					}
				}
			}

			if (best_location != null) {
				Navigator.MoveResult move_result = navigator.move(best_location);
				controller.setIndicatorString(best_location.toString());
				if (move_result == Navigator.MoveResult.SUCCESS) {
					return true;
				}
			}
			return false;
		}
	}

	class DefaultAttackStrategy implements AttackStrategy
	{
		@Override
		public boolean attack() throws GameActionException
		{
			// TODO: Maybe consider potential damage to a robot? Or damage output of opponent>
			// Currently prioritizes lowest health soldiers XX Now changes to score-based
			int best_score = -9999999;
			MapLocation lowest_location = null;
			int dmg = getRobotController().getType().getDamage(0);

			for (RobotInfo robot : Cache.opponent_soldiers) {
				if (getRobotController().canAttack(robot.getLocation())) {
					int score = 0;
					score = (40 - (robot.health + dmg - 1) / dmg) * 300;
					score -= robot.getLocation().distanceSquaredTo(getRobotController().getLocation()) * 2;
					score -= getRobotController().senseRubble(robot.getLocation()) / 4;
					if (score > best_score)
					{
						lowest_location = robot.location;
						best_score = score;
					}
				}
			}
			if (lowest_location != null) {
				Cache.controller.attack(lowest_location);
				return true;
			}

			// Check the 8 edge cases
			MapLocation my_location = getRobotController().getLocation();
			for (MapLocation newly_scouted : new MapLocation[] {
					new MapLocation(my_location.x-3,my_location.y-2),
					new MapLocation(my_location.x-2,my_location.y-3),
					new MapLocation(my_location.x-2,my_location.y+2),
					new MapLocation(my_location.x-3,my_location.y+3),
					new MapLocation(my_location.x+2,my_location.y-2),
					new MapLocation(my_location.x+3,my_location.y-3),
					new MapLocation(my_location.x+2,my_location.y+2),
					new MapLocation(my_location.x+3,my_location.y+3),
			}) {
				if (navigator.inMap(newly_scouted) && getRobotController().canSenseLocation(newly_scouted)) {
					RobotInfo robot = getRobotController().senseRobotAtLocation(newly_scouted);
					if (robot != null && robot.getTeam() != Cache.OUR_TEAM
							&& (robot.getType() == RobotType.SOLDIER
							|| robot.getType() == RobotType.SAGE
							|| robot.getType() == RobotType.WATCHTOWER)) {
					}
				}
			}

			for (RobotInfo robot : Cache.opponent_buildings) {
				if (getRobotController().canAttack(robot.getLocation())) {
					lowest_location = robot.location;
					break;
				}
			}
			if (lowest_location != null) {
				Cache.controller.attack(lowest_location);
				return true;
			}

			int lowest = 999999;
			for (RobotInfo robot : Cache.opponent_villagers) {
				if (robot.health < lowest && getRobotController().canAttack(robot.getLocation())) {
					lowest = robot.health;
					lowest_location = robot.location;
				}
			}
			if (lowest_location != null) {
				Cache.controller.attack(lowest_location);
				return true;
			}
			return false;
		}
	}


	@Override
	public void turn() throws GameActionException
	{
		Cache.update();

		getRobotController().writeSharedArray(CommandCommunicator.SOLDIER_INDEX,
				getRobotController().readSharedArray(CommandCommunicator.SOLDIER_INDEX)+1);

		current_attacking_strategy = default_attacking_strategy;

		if (retreat_moving_strategy.shouldRun()) {
			current_moving_strategy = retreat_moving_strategy;
			getRobotController().setIndicatorString("Run");
		}
		else if (Cache.opponent_soldiers.length + Cache.opponent_villagers.length + Cache.opponent_buildings.length > 0) {
			current_moving_strategy = fight_moving_strategy;
			getRobotController().setIndicatorString("Fight");
		}
		else {
			if ((Cache.age & 1) == 1) {
				MatrixCommunicator.read(Communicator.Event.ARCHON);
			}
			else {
				MatrixCommunicator.read(Communicator.Event.SOLDIER);
			}
			getRobotController().setIndicatorString("Search");
			current_moving_strategy = search_moving_strategy;
		}

		if (getRobotController().isActionReady() && Cache.opponent_soldiers.length > 0) {
			current_attacking_strategy.attack();
		}

		if (getRobotController().isMovementReady()) {
			current_moving_strategy.move();
		}

		if (getRobotController().isActionReady()) {
			current_attacking_strategy.attack();
		}

		// TODO: Precalculate areas with extra bytecode?

	}

}