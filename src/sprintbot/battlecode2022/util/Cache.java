package sprintbot.battlecode2022.util;

import battlecode.common.*;

import javax.naming.OperationNotSupportedException;

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
	public static int[] opponent_archon_compressed_locations = new int[Communicator.NUM_OF_COMPRESSED_LOCATIONS];
	public static int[] metal_compressed_locations = new int[Communicator.NUM_OF_COMPRESSED_LOCATIONS];
	public static int[] opponent_soldier_compressed_locations = new int[Communicator.NUM_OF_COMPRESSED_LOCATIONS];

	public static int age = 0;

	public static RobotInfo[] friendly_soldiers = null; // watchtower, sages, and soldiers
	public static RobotInfo[] friendly_villagers = null; // miners, builders
	public static RobotInfo[] friendly_buildings = null; // labs, archon
	public static RobotInfo[] opponent_soldiers = null;
	public static RobotInfo[] opponent_villagers = null;
	public static RobotInfo[] opponent_buildings = null;

	public static MapLocation[] lead_spots = null;

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
	}

	// Scouting
	public static void update() throws GameActionException
	{

		// Variable update
		age += 1;

		// Scouting routine
		unit_scout_routine();
		unit_lead_routine();

	}

	private static void unit_scout_routine() throws GameActionException {

		RobotInfo[] units = controller.senseNearbyRobots();
		int fs,fv,fb,os,ov,ob;
		fs = fv = os = ov = fb = ob = 0;

		// Units
		for (RobotInfo unit : units) {
			if (unit.getTeam() == OUR_TEAM) {
				switch (unit.getType()) {
					case SOLDIER:
					case SAGE:
					case WATCHTOWER:
						fs ++;
					case MINER:
					case BUILDER:
						fv ++;
					case ARCHON:
					case LABORATORY:
						fb ++;
				}
			}
			else {
				report_unit(unit);
				switch (unit.getType()) {
					case SOLDIER:
					case SAGE:
					case WATCHTOWER:
						os ++;
					case MINER:
					case BUILDER:
						ov ++;
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

		for (int i = 0; i < units.length; i ++) {
			RobotInfo unit = units[i];
			if (unit.getTeam() == OUR_TEAM) {
				switch (unit.getType()) {
					case SOLDIER:
					case SAGE:
					case WATCHTOWER:
						friendly_soldiers[fs] = unit;
						fs ++;
					case MINER:
					case BUILDER:
						friendly_villagers[fv] = unit;
						fv ++;
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
					case MINER:
					case BUILDER:
						opponent_villagers[ov] = unit;
						ov ++;
					case LABORATORY:
					case ARCHON:
						opponent_buildings[ob] = unit;
						ob ++;
				}
			}
		}
	}

	private static void unit_lead_routine() throws GameActionException {

		int[] has_lead = new int[Communicator.NUM_OF_COMPRESSED_LOCATIONS];

		lead_spots = controller.senseNearbyLocationsWithLead(MY_VISION_RADIUS);
		for (MapLocation spot : lead_spots) {
			int compressed = Communicator.compressLocation(spot);
			if (has_lead[compressed] != 0) {
				continue;
			}
			MatrixCommunicator.update(Communicator.Event.METAL,spot);
			has_lead[compressed] = 1;
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