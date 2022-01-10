package sprintbot.battlecode2022.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import sprintbot.RobotPlayer;

import java.util.Map;

public class Communicator
{
	
	protected static RobotController controller;
	protected static int MAP_WIDTH;
	protected static int MAP_HEIGHT;
	
	protected static final int COMPRESSED_SIZE = 8; // compress map to 8*8
	protected static final int NUM_OF_COMPRESSED_LOCATIONS = 64;
	protected static int X_STEP; // the number of x units compressed to 1
	protected static int Y_STEP;
	protected static int CENTRE_X; // the x coordinate of the centre of the region formed by (x,y) where x<X_step && y<Y_step (SOUTHWEST)
	protected static int CENTRE_Y; // the y coordinate of the centre of the region formed by (x,y) where x<X_step && y<Y_step (SOUTHWEST)
	
	protected static final int BITS_PER_INTEGER = 16;
	
	public enum Event
	{
		ARCHON, // Opponent Archon
		METAL, // Metal & no Miner around
		SOLDIER // Opponent Soldier or Sage
	}
	
	public static final Event[] events = {
			Event.ARCHON,
			Event.METAL,
			Event.SOLDIER
	};
	
	public static void init(RobotController controller)
	{
		Communicator.controller = controller;
		MAP_WIDTH = controller.getMapWidth();
		MAP_HEIGHT = controller.getMapHeight();
		X_STEP = (int) Math.ceil((double)MAP_WIDTH / COMPRESSED_SIZE);
		Y_STEP = (int) Math.ceil((double)MAP_HEIGHT / COMPRESSED_SIZE);
		CENTRE_X = (X_STEP - 1) / 2;
		CENTRE_Y = (Y_STEP - 1) / 2;
	}
	
	protected static int eventNum(Event event)
	{
		int event_num;
		switch (event)
		{
			case ARCHON:
				event_num = 0;
				break;
			case METAL:
				event_num = 1;
				break;
			case SOLDIER:
				event_num = 2;
				break;
			default: // won't happen
				throw new IllegalStateException("Unexpected Event: " + event);
		}
		return event_num;
	}
	
	protected static int[] getCacheArray(Event event)
	{
		int[] cache_array;
		switch (event)
		{
			case ARCHON:
				cache_array = Cache.opponent_archon_compressed_locations;
				break;
			case METAL:
				cache_array = Cache.metal_compressed_locations;
				break;
			case SOLDIER:
				cache_array = Cache.opponent_soldier_compressed_locations;
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + event);
		}
		return cache_array;
	}
	
	public static void update(Event event, MapLocation location) throws GameActionException
	{
		switch (Cache.controller.getRoundNum() % 5)
		{
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
		}
	}
	/**
	 * Compresses a MapLocation to a number between 0~63 (x-major order).
	 *
	 * @param loc a valid location on map
	 * @return compressed location
	 */
	public static int compressLocation(MapLocation loc)
	{
		int x = loc.x;
		int y = loc.y;
		return x / X_STEP + (y / Y_STEP) * COMPRESSED_SIZE;
	}
	
	/**
	 * Unzips a compressed location and returns the CENTRE of that region
	 * by default it tries to return the SOUTHWESTern centre if there are multiple centres
	 *
	 * @param compressed_loc the compressed location, an int
	 * @return the MapLocation object of that centre
	 */
	private static MapLocation unzipCompressedLocation(int compressed_loc)
	{
		int x = compressed_loc % COMPRESSED_SIZE * X_STEP + CENTRE_X;
		int y = compressed_loc / COMPRESSED_SIZE * Y_STEP + CENTRE_Y;
		return new MapLocation(x, y);
	}
	
	/**
	 * Unzips and then choose the closest compressed location from an array
	 *
	 * @param compressed_locations the array of compressed locations
	 * @param current_location     the relative position
	 * @return the MapLocation object of that closest centre, and null if no valid locations are found
	 */
	public static MapLocation getClosestFromCompressedLocationArray(int[] compressed_locations, MapLocation current_location)
	{
		MapLocation best_location = null;
		int best_distance = 1000; // MAX INT
		for (int i = 0; i < compressed_locations.length; i++)
		{
			if (compressed_locations[i] == -1)
				break;
			MapLocation try_location = unzipCompressedLocation(compressed_locations[i]);
			int try_distance = Navigator.travelDistance(current_location, try_location);
			if (try_distance < best_distance)
			{
				best_distance = try_distance;
				best_location = try_location;
			}
		}
		return best_location;
	}

}