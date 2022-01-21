package sprintbot.battlecode2022.util;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;

// Data such as turn number, turns alive, local environment etc

public class Cache
{
	public static RobotController controller;
	public static Team OUR_TEAM;
	public static Team OPPONENT_TEAM;
	
	public static int MAP_WIDTH;
	public static int MAP_HEIGHT;

	public static MapLocation MY_SPAWN_LOCATION = null;
	public static int MY_ACTION_RADIUS;
	public static int MY_VISION_RADIUS;
	
	// the end of data is marked with -1
	public static int[] opponent_archon_compressed_locations = new int[64];
	public static int[] metal_compressed_locations = new int[64];
	public static int[] opponent_soldier_compressed_locations = new int[64];

	public static MapLocation main_force_target;

	public static AnomalyScheduleEntry ANOMALIES[];
	public static AnomalyScheduleEntry next_anomaly = null;
	private static int current_anomaly_index = 0;

	public static int age = 0;

	public static RobotInfo[] friendly_soldiers = null; // watchtower, sages, and soldiers
	public static RobotInfo[] friendly_villagers = null; // miners, builders
	public static RobotInfo[] friendly_buildings = null; // labs, archon
	public static RobotInfo[] opponent_soldiers = null;
	public static RobotInfo[] opponent_villagers = null;
	public static RobotInfo[] opponent_buildings = null;

	public static double opponent_total_damage = 0;
	public static int opponent_total_health = 0;
	public static double our_total_damage = 0;
	public static int our_total_health = 0;
	public static boolean can_see_archon = false;
	public static int lowest_health_soldier = 50;
	public static int injured = 0;
	public static MapLocation archon_location = null;

	public static MapLocation[] lead_spots = null;
	public static int lead_amount = 0;

	public static void init(RobotController controller)
	{
		Cache.controller = controller;
		MY_ACTION_RADIUS = controller.getType().actionRadiusSquared;
		MY_VISION_RADIUS = controller.getType().visionRadiusSquared;
		OUR_TEAM = controller.getTeam();
		OPPONENT_TEAM = OUR_TEAM.opponent();
		MAP_WIDTH = controller.getMapWidth();
		MAP_HEIGHT = controller.getMapHeight();
		opponent_archon_compressed_locations[0] = -1;
		metal_compressed_locations[0] = -1;
		opponent_soldier_compressed_locations[0] = -1;
		MY_SPAWN_LOCATION = controller.getLocation();
		ANOMALIES = controller.getAnomalySchedule();
		//Arrays.sort(ANOMALIES, Comparator.comparingInt(anomalyScheduleEntry -> anomalyScheduleEntry.roundNumber));
		if (ANOMALIES.length > 0) {
			next_anomaly = ANOMALIES[0];
		}
	}

	// Scouting
	public static void update() throws GameActionException
	{

		// Variable update
		age += 1;

		// Scouting routine
		unit_scout_routine();
		unit_lead_routine();

		// Anomaly
		if (next_anomaly != null && next_anomaly.roundNumber < controller.getRoundNum()) {
			current_anomaly_index ++;
			if (current_anomaly_index < ANOMALIES.length) {
				next_anomaly = ANOMALIES[current_anomaly_index];
			}
			else {
				next_anomaly = null;
			}
		}

	}

	public static void unit_scout_routine() throws GameActionException {

		turn_report_reset = false;

		RobotInfo[] units = controller.senseNearbyRobots();
		int fs,fv,fb,os,ov,ob;
		fs = fv = os = ov = fb = ob = 0;

		// Units
		opponent_total_damage = 0.0;
		opponent_total_health = 0;
		our_total_damage = 0.0;
		our_total_health = 0;
		can_see_archon = false;
		lowest_health_soldier = 0;
		injured = 0;
		archon_location = null;

		if (controller.getType() == RobotType.SOLDIER || controller.getType() == RobotType.SAGE) {
			our_total_damage += (double)controller.getType().damage / (double)controller.getType().actionCooldown * 10.0;
			our_total_health += controller.getHealth();
		}

		for (RobotInfo unit : units) {
			if (unit.getTeam() == OUR_TEAM) {
				switch (unit.getType()) {
					case SOLDIER:
					case SAGE:
					case WATCHTOWER:
						our_total_damage += (double)unit.getType().damage / (double)unit.getType().actionCooldown * 10.0;
						our_total_health += unit.getHealth();
						if (unit.getHealth() < unit.getType().health) {
							injured += 1;
						}
						if (unit.getHealth() < lowest_health_soldier) {
							lowest_health_soldier = unit.getHealth();
						}
						fs ++;
						break;
					case MINER:
					case BUILDER:
						fv ++;
						break;
					case ARCHON:
						can_see_archon = true;
						if (archon_location == null || unit.getLocation().distanceSquaredTo(controller.getLocation())<archon_location.distanceSquaredTo(controller.getLocation())) {
							archon_location = unit.getLocation();
						}
					case LABORATORY:
						fb ++;
				}
			}
			else {
				report_unit(unit);
				switch (unit.getType()) {
					case SOLDIER:
					case SAGE:
						os ++;
						opponent_total_damage +=(double)unit.getType().damage / (double)unit.getType().actionCooldown * 10.0;
						opponent_total_health += unit.getHealth();
						break;
					case WATCHTOWER:
						os ++;
						if (unit.mode == RobotMode.TURRET) {
							opponent_total_damage += (double)unit.getType().damage / (double)unit.getType().actionCooldown * 10.0;
							opponent_total_health += unit.getHealth();
						}
						break;
					case MINER:
					case BUILDER:
						ov ++;
						break;
					case ARCHON:
					case LABORATORY:
						ob ++;
				}
			}
		}

		friendly_soldiers = new RobotInfo[fs];
		friendly_villagers = new RobotInfo[fv];
		friendly_buildings = new RobotInfo[fb];
		opponent_soldiers = new RobotInfo[os];
		opponent_villagers = new RobotInfo[ov];
		opponent_buildings = new RobotInfo[ob];

		fs = fv = fb = os = ov = ob = 0;

		for (RobotInfo unit : units) {
			if (unit.getTeam() == OUR_TEAM) {
				switch (unit.getType()) {
					case SOLDIER:
					case SAGE:
					case WATCHTOWER:
						friendly_soldiers[fs] = unit;
						fs ++;
						break;
					case MINER:
					case BUILDER:
						friendly_villagers[fv] = unit;
						fv ++;
						break;
					case LABORATORY:
					case ARCHON:
						friendly_buildings[fb] = unit;
						fb ++;
				}
			}
			else {
				switch (unit.getType()) {
					case SOLDIER:
					case SAGE:
					case WATCHTOWER:
						opponent_soldiers[os] = unit;
						os ++;
						break;
					case MINER:
					case BUILDER:
						opponent_villagers[ov] = unit;
						ov ++;
						break;
					case LABORATORY:
					case ARCHON:
						opponent_buildings[ob] = unit;
						ob ++;
				}
			}
		}
	}


	private static void unit_lead_routine() throws GameActionException {

		//int start_bytecode = Clock.getBytecodeNum();
		int REPORT_LEAD_THRESHOLD = Math.min(150,Communicator.X_STEP * Communicator.Y_STEP * 10);
		boolean has_report = false;

		lead_amount = 0;
		int[] has_lead = new int[Communicator.NUM_OF_COMPRESSED_LOCATIONS];
		lead_spots = controller.senseNearbyLocationsWithLead(MY_VISION_RADIUS);

		for (int i = lead_spots.length; --i >= 0;) {
			MapLocation spot = lead_spots[i];
			int l = controller.senseLead(spot);
			lead_amount += l;

			/*
			New framework - communicate only when spot has a large amount of lead to call for help
			 */


			if (l > 1) {
				has_lead[Communicator.compressLocation(spot)] += l - 1;
				if (has_lead[Communicator.compressLocation(spot)] >= REPORT_LEAD_THRESHOLD) {
					has_report = true;
				}
			}

			/* Old lead communication framework - all lead spots count. Depreciated.
			int compressed = Communicator.compressLocation(spot);
			if (has_lead[compressed] != 0) {
				continue;
			}
			MatrixCommunicator.update(Communicator.Event.METAL,spot);
			has_lead[compressed] = 1;
			 */
		}

		MapLocation my_location = controller.getLocation();
		if (has_report) {
			System.out.println(my_location);
			int vision_range = (int)Math.sqrt(controller.getType().visionRadiusSquared);
			for (int x = Math.max(0,my_location.x - vision_range); x < Math.min(MAP_WIDTH-1,my_location.x + vision_range); x += Communicator.X_STEP) {
				for (int y = Math.max(my_location.y - vision_range,0); y < Math.min(MAP_HEIGHT-1,my_location.y + vision_range); y += Communicator.Y_STEP) {
					int i = Communicator.compressLocation(new MapLocation(x,y));
					if (has_lead[i] >= REPORT_LEAD_THRESHOLD) {
						MatrixCommunicator.update(Communicator.Event.METAL,i,true);
					}
				}
			}
		}
		int my_compressed = Communicator.compressLocation(my_location);
		if (has_lead[my_compressed] < REPORT_LEAD_THRESHOLD) {
			MatrixCommunicator.update(Communicator.Event.METAL,my_compressed,false);
		}

	}


	// Bytecode saving measure
	private static boolean turn_report_reset = false;
	private static boolean[] reported = null;

	/**
	 *
	 * @param robot - enemy robot to report
	 * @throws GameActionException - Bug in matrix
	 */
	private static void report_unit (RobotInfo robot) throws GameActionException {

		if (!turn_report_reset) {
			reported = new boolean[Communicator.NUM_OF_COMPRESSED_LOCATIONS];
			turn_report_reset = true;
		}

		int compressed_location = Communicator.compressLocation(robot.getLocation());

		if (robot.getTeam() == OUR_TEAM) {
			return;
		}
		switch (robot.getType()) {
			case SOLDIER:
			case SAGE:
			case WATCHTOWER:
				if (!reported[compressed_location]) {
					reported[compressed_location] = true;
					MatrixCommunicator.update(Communicator.Event.SOLDIER, robot.getLocation());
				}
				break;
			case ARCHON:
				MatrixCommunicator.update(Communicator.Event.ARCHON,robot.getLocation());
				break;
			default:
				// Not yet implemented
				break;
		}
	}

}