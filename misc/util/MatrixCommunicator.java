package sprintbot.battlecode2022.util;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class MatrixCommunicator extends Communicator {

	/* Unit Schema
	 * Starts 7th integer in shared memory.
	 * 11 bits boolean array per compressed location.
	 *
	 * 0 - 0: Opponent Archon
	 * 1 - 1: Metal exists
	 * 2 - 2: Opponent Soldier / Sage / Watchtower
	 * 3 - 3: Friendly miner exists
	 * 4 - 4: Our Archon
	 *
	 * */

	private static final int offset = 6 ;
	private static final int BITS_PER_LOCATION = 11; // Every location has 11 bits to store states
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
		update(event,compressed_location,state);
	}

	/**
	 * Updates an event given a compressed location to shared memory.
	 * @param event               event type
	 * @param compressed_location compressed location
	 */
	public static void update(Event event, int compressed_location) throws GameActionException {
		update(event,compressed_location,true);
	}

	/**
	 * Updates an event to true or false given a compressed location to shared memory.
	 * @param event               event type
	 * @param compressed_location compressed location
	 * @param state               update to true or false
	 */
	public static void update(Event event, int compressed_location, boolean state) throws GameActionException {
		int event_bit_id = Communicator.eventNum(event);
		int bit_id = event_bit_id + BITS_PER_LOCATION * compressed_location; // the bit to change
		int id = offset + bit_id / BITS_PER_INTEGER; // integer id in shared memory
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
		int[] compressed_locations = new int[NUM_OF_COMPRESSED_LOCATIONS];
		int cnt = 0;
		int bit_id = Communicator.eventNum(event) + BITS_PER_LOCATION * NUM_OF_COMPRESSED_LOCATIONS;
		for (int compressed_location = NUM_OF_COMPRESSED_LOCATIONS; --compressed_location >= 0;) {
			bit_id -= BITS_PER_LOCATION;
			if (((controller.readSharedArray(offset + bit_id / BITS_PER_INTEGER) >> (BITS_PER_INTEGER - bit_id % BITS_PER_INTEGER - 1)) & 1) == 1) {
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
				break;
			case FRIENDLY_ARCHON:
				Cache.friendly_archon_compressed_locations = compressed_locations;
				break;
		}
	}

	public static Boolean read (Event event, MapLocation location) throws GameActionException {
		return read(event,Communicator.compressLocation(location));
	}

	public static Boolean read(Event event, int compressed_location) throws GameActionException {
		int event_bit_id = Communicator.eventNum(event);
		int bit_id = event_bit_id + BITS_PER_LOCATION * compressed_location; // the bit to change
		int id = offset + bit_id / BITS_PER_INTEGER; // integer id in shared memory
		int value = controller.readSharedArray(id);
		int relative_bit_id = BITS_PER_INTEGER - bit_id % BITS_PER_INTEGER - 1; // count from LSB
		int bit = (value >> relative_bit_id) & 1;
		return bit == 1;
	}

}