package sprintbot.battlecode2022.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class MatrixCommunicator extends Communicator {
	
	/* Unit Schema
	 * Starts 7th integer in shared memory.
	 * 14 bits boolean array per compressed location.
	 * 0 - 0: Opponent Archon
	 * 1 - 1: Metal & no miner around
	 * 2 - 2: Opponent Soldier / Sage
	 * */
	
	private static final int offset = 6 ;
	private static final int BITS_PER_LOCATION = 14; // Every location has 14 bits to store states
	// Can be expanded to store fewer states but more options
	
	
	/**
	 * Updates an event given a MapLocation to shared memory.
	 * @param event    event type
	 * @param location MapLocation
	 * @throws GameActionException -
	 */
	public static void update(Event event, MapLocation location) throws GameActionException {
		int compressed_location = Communicator.compressLocation(location);
		update(event, compressed_location);
	}

	/**
	 * Updates an event to true or false given a MapLocation to shared memory.
	 * @param event    event type
	 * @param location MapLocation
	 * @param state    update to true or false
	 * @throws GameActionException -
	 */
	public static void update(Event event, MapLocation location, boolean state) throws GameActionException {
		int compressed_location = Communicator.compressLocation(location);
		update(event, compressed_location, state);
	}
	
	/**
	 * Updates an event given a compressed location to shared memory.
	 * @param event               event type
	 * @param compressed_location compressed location
	 * @throws GameActionException -
	 */
	public static void update(Event event, int compressed_location) throws GameActionException {
		int event_bit_id = Communicator.eventNum(event);
		int bit_id = event_bit_id + BITS_PER_LOCATION * compressed_location; // the bit to change
		int id = offset + bit_id / BITS_PER_INTEGER; // integer id in shared memory
		if (Constants.DEBUG && id >= 64) {
			System.out.println("Who wrote a bug?");
			System.out.println(compressed_location);
			System.out.println(event_bit_id);
			System.out.println(id);
		}
		int value = controller.readSharedArray(id);
		int relative_bit_id = BITS_PER_INTEGER - bit_id % BITS_PER_INTEGER - 1; // count from LSB
		int new_value = value | (1 << relative_bit_id);
		if (new_value > 65535) {
			System.out.println("Who wrote a bug? Matrix update");
			System.out.println(new_value);
		}
		if (new_value != value) {
			controller.writeSharedArray(id, new_value);
		}
	}

	/**
	 * Updates an event to true or false given a compressed location to shared memory.
	 * @param event               event type
	 * @param compressed_location compressed location
	 * @param state               update to true or false
	 * @throws GameActionException -
	 */
	public static void update(Event event, int compressed_location, boolean state) throws GameActionException {
		int event_bit_id = Communicator.eventNum(event);
		int bit_id = event_bit_id + BITS_PER_LOCATION * compressed_location; // the bit to change
		int id = offset + bit_id / BITS_PER_INTEGER; // integer id in shared memory
		if (Constants.DEBUG && id >= 64) {
			System.out.println("Who wrote a bug?");
			System.out.println(compressed_location);
			System.out.println(event_bit_id);
			System.out.println(id);
		}
		int value = controller.readSharedArray(id);
		int relative_bit_id = BITS_PER_INTEGER - bit_id % BITS_PER_INTEGER - 1; // count from LSB
		int new_value;
		if (state) {
			new_value = value | (1 << relative_bit_id);
		}
		else {
			new_value = value & ~(1 << relative_bit_id);
		}
		if (new_value != value) {
			controller.writeSharedArray(id, new_value);
		}
	}
	
	
	/**
	 * Finds compressed locations where an event happens and writes to Cache.
	 * @param event event type
	 * @throws GameActionException -
	 */
	public static void read(Event event) throws GameActionException {
		int event_bit = Communicator.eventNum(event);
		int[] compressed_locations = new int[NUM_OF_COMPRESSED_LOCATIONS];
		int cnt = 0;
		for (int compressed_location = 0; compressed_location < NUM_OF_COMPRESSED_LOCATIONS; compressed_location++) {
			int bit_id = event_bit + BITS_PER_LOCATION * compressed_location;
			int id = offset + bit_id / BITS_PER_INTEGER;
			int value = controller.readSharedArray(id);
			int relative_bit_id = BITS_PER_INTEGER - bit_id % BITS_PER_INTEGER - 1;
			int bit = (value >> relative_bit_id) & 1;
			if (bit == 1) {
				compressed_locations[cnt++] = compressed_location;
			}
		}
		if (cnt < NUM_OF_COMPRESSED_LOCATIONS) {
			compressed_locations[cnt] = -1; // mark the end
		}
		
		switch (event) {
			case ARCHON:
				Cache.opponent_archon_compressed_locations = compressed_locations;
				break;
			case METAL:
				Cache.metal_compressed_locations = compressed_locations;
				break;
			case SOLDIER:
				Cache.opponent_soldier_compressed_locations = compressed_locations;
		}
	}
	
}