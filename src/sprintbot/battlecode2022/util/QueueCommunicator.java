package sprintbot.battlecode2022.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;


public class QueueCommunicator extends Communicator{
	
	/* Unit Schema
	 * Queue-based communication.
	 * Uses index=2~63 in shared memory.
	 * Each message consists of 2 bits for event type and 6 bits for compressed location.
	 * Can hold a maximum of 120 messages simultaneously.
	 * */
	
	// can add one more event
	/* Event encoding (2 bits)
	 * 0: Opponent Archon
	 * 1: Metal with no miners around
	 * 2: Opponent Soldier or Sage
	 */
	
	private static final int BITS_PER_LOCATION = 6; // bits to represent a compressed location; 8*8 = 2^6
	private static final int BITS_PER_MESSAGE = 8;
	
	// queue parameters
	private static final int p = 100;
	private static final int offset = 2;
	private static int tail = 0; // end of queue content (open end, tail not included)
	private static int length = 0; // length of queue content
	
	
	/**
	 * Encodes a message into 8 bits.
	 * @param event            type of event
	 * @param compressed_loc   location of the event
	 * @return                 integer representation of the message
	 */
	private static int encode(Event event, int compressed_loc) {
		int event_num = Communicator.eventNum(event);
		return (event_num << BITS_PER_LOCATION) + compressed_loc;
	}
	
	/**
	 * Writes a message to shared memory.
	 * @param event          event type
	 * @param compressed_loc compressed location of the event
	 * @throws GameActionException -
	 */
	public static void push(Event event, int compressed_loc) throws GameActionException {
		int log = encode(event, compressed_loc);
		if (length == p) { // no more space
			return;
		}
		int id = offset + tail / 2;
		int num = controller.readSharedArray(id);
		if (length % 2 == 1) {
			log = (num - num % (1 << BITS_PER_MESSAGE)) + log;
		}
		else {
			log  = (log << BITS_PER_MESSAGE) + (num % (1 << BITS_PER_MESSAGE));
		}
		controller.writeSharedArray(id, log);
		tail = (tail + 1) % p; // wrap around the end
		length++;
	}
	
	/**
	 * Decodes a message and stores in Cache.
	 * @param log an encoded message
	 */
	private static void decode(int log) {
		int event_num = log >> BITS_PER_LOCATION;
		Event event = Communicator.events[event_num];
		int compressed_location = log % (1 << BITS_PER_LOCATION);
		
		int[] cache_array = Communicator.getCacheArray(event);
		for (int i = 0; i < NUM_OF_COMPRESSED_LOCATIONS; i++) {
			if (cache_array[i] == -1) {
				cache_array[i] = compressed_location;
				if (i < NUM_OF_COMPRESSED_LOCATIONS - 1) {
					cache_array[i + 1] = -1;
				}
				return;
			}
		}
	}
	
	/**
	 * Pops, decodes, and writes the first message in shared memory to Cache.
	 * @throws GameActionException -
	 */
	public static void pop() throws GameActionException {
		if (length == 0) { // pop empty queue
			return;
		}
		int head = (tail - length + p) % p;
		int id = offset + head / 2;
		int log = controller.readSharedArray(id);
		if (head % 2 == 1) {
			log %= (1 << BITS_PER_MESSAGE);
		}
		else {
			log = (log >> BITS_PER_MESSAGE);
		}
		length--;
		decode(log);
	}
}