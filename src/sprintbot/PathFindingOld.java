package sprintbot.battlecode2022;

import battlecode.common.*;

import java.util.*;

public class PathFindingOld
{
	
	private static RobotController controller;
	private static Random random;
	private static final Direction[] DIRECTIONS =
			{Direction.NORTH, Direction.NORTHEAST,
					Direction.EAST, Direction.SOUTHEAST,
					Direction.SOUTH, Direction.SOUTHWEST,
					Direction.WEST, Direction.NORTHWEST};
	private static int MAP_HEIGHT;
	private static int MAP_WIDTH;
	private static final int BFS_BYTECODE_BOUND = 7000;
	
	public static void init(RobotController controller)
	{
		PathFindingOld.controller = controller;
		random = new Random(controller.getID());
		MAP_HEIGHT = controller.getMapHeight();
		MAP_WIDTH = controller.getMapWidth();
	}
	
	// Multiple navigation algorithm
	static boolean isBugging = false;
	static boolean clockwise = true;
	static int count = 0;
	static MapLocation prevTarget;
	static MapLocation startBugLocation;
	static MapLocation obstacle;
	static MapLocation CURRENT_LOCATION;
	static double gradient;
	static int stuckTurns;
	
	/**
	 * Usage: first init(rc) then move(target location)
	 * Returns:
	 * 0 means the controller is in cool down or robot
	 * is stuck
	 * 1 means moved successfully
	 * 2 means that target is out of the map or is null
	 * 3 means that we've already reached the target
	 */
	public static int move(MapLocation targetLoc)
			throws GameActionException
	{
		// Is it ready
		if (controller.getMovementCooldownTurns() > 0)
			return 0;
		CURRENT_LOCATION = controller.getLocation();
		// Is target out of the map
		if (targetLoc == null ||
				!controller.onTheMap(CURRENT_LOCATION.add(
						CURRENT_LOCATION.directionTo(
								targetLoc)))) return 2;
		// At destination
		if (targetLoc.equals(CURRENT_LOCATION))
		{
			isBugging = false;
			return 3;
		}
		// Target change, stop bugging
		if (targetLoc != prevTarget)
		{
			isBugging = false;
		}
		prevTarget = targetLoc;
		
		if (!isBugging)
		{
			int res = greedyMove(targetLoc);
			// Can move
			if (res == 1 || res == 2)
			{
				return res;
			}
			// Stuck, start bugging
			isBugging = true;
			clockwise = true;
			count = 0;
			startBugLocation = CURRENT_LOCATION;
			gradient =
					calculateGradient(CURRENT_LOCATION,
							targetLoc);
			obstacle =
					controller.adjacentLocation(
							CURRENT_LOCATION.directionTo(
									targetLoc));
			stuckTurns = 0;
			return move(targetLoc);
		}
		else
		{
			// Robot trapped
			if (startBugLocation.equals(CURRENT_LOCATION))
			{
				count += 1;
				if (count >= 3)
				{
					return 0;
				}
			}
			Direction
					obstacleDirection =
					CURRENT_LOCATION.directionTo(
							obstacle);
			Direction targetDirection = obstacleDirection;
			// Edge Case: Obstacle gone
			if (naiveMove(obstacleDirection))
			{
				isBugging = false;
				return 1;
			}
			if (clockwise)
			{
				targetDirection =
						targetDirection.rotateRight();
			}
			else
			{
				targetDirection =
						targetDirection.rotateLeft();
			}
			while (!naiveMove(targetDirection))
			{
				if (clockwise)
				{
					targetDirection =
							targetDirection.rotateRight();
				}
				else
				{
					targetDirection =
							targetDirection.rotateLeft();
				}
				//If on the edge of the map, switch bug directions
				//Or, there is no way past
				if (!controller.onTheMap(controller.adjacentLocation(
						targetDirection)))
				{
					if (clockwise)
					{
						clockwise = false;
						targetDirection =
								targetDirection.rotateLeft();
						return move(targetLoc);
					}
					else
					{
						stuckTurns += 1;
						return 0;
					}
				}
				if (targetDirection == obstacleDirection)
				{
					stuckTurns += 1;
					return 0;
				}
			}
			if (clockwise)
			{
				obstacle =
						controller.adjacentLocation(
								targetDirection.rotateLeft());
			}
			else
			{
				obstacle =
						controller.adjacentLocation(
								targetDirection.rotateRight());
			}
			MapLocation
					moveLoc =
					controller.adjacentLocation(
							targetDirection);
			//Check if it's passing the original line closer to the target
			if (CURRENT_LOCATION.distanceSquaredTo(
					targetLoc) <
					startBugLocation.distanceSquaredTo(
							targetLoc))
			{
				if (calculateGradient(CURRENT_LOCATION,
						targetLoc) > gradient &&
						calculateGradient(moveLoc,
								targetLoc) <= gradient)
				{
					isBugging = false;
				}
				else if (calculateGradient(moveLoc,
						targetLoc) >= gradient)
				{
					isBugging = false;
				}
			}
			if (naiveMove(targetDirection))
			{
				return 1;
			}
			return 0;
		}
	}
	
	private static class BFS
	{
		private final MapLocation[] queue;
		private final int[] turn_penalty;
		private int queue_head, queue_tail; //[queue_head,queue_tail] i.e. size = queue_tail - queue_head + 1
		private final int[] traceback_subscript;
		private final HashSet<MapLocation> visited_set;
		private int step;
		private final MapLocation target_location;
		
		/**
		 * constructor BFS(step, target_location, current_location) does BFS for "step" steps
		 * and tries to find the closest it can get relative to
		 * the target location
		 */
		BFS(int step, MapLocation target_location, MapLocation current_location)
		{
			this.step = step;
			this.target_location = target_location;
			this.queue = new MapLocation[(step * 2 + 1) * (step * 2 + 1) * 10];
			this.turn_penalty = new int[this.queue.length];
			this.traceback_subscript = new int[this.queue.length];
			this.queue[0] = current_location;
			this.queue_head = 0;
			this.queue_tail = 0;
			this.turn_penalty[0] = 0;
			visited_set = new HashSet<>();
			visited_set.add(current_location);
			RobotInfo[] barriers = controller.senseNearbyRobots(2);
			for (int i = 0; i < barriers.length; i++)
				visited_set.add(barriers[i].getLocation());
			//eliminate nearest 8 "clogged" possibilities
		}
		
		int getTurnPenalty(MapLocation o) throws GameActionException
		{
			int M = controller.senseRubble(o);
			return M / 10;
		}
		
		void updateSearchedLocationsByOneStep() throws GameActionException
		{
			int old_queue_tail = queue_tail;
			while (queue_head <= old_queue_tail)
			{
				if (turn_penalty[queue_head] > 0)
				{
					queue[++queue_tail] = queue[queue_head];
					traceback_subscript[queue_tail] = traceback_subscript[queue_head];
					turn_penalty[queue_tail] = turn_penalty[queue_head] - 1;
				}
				else
				{
					MapLocation u = queue[queue_head];
					for (int i = 0; i < DIRECTIONS.length; i++)
					{
						Direction dir = DIRECTIONS[i];
						MapLocation v = u.add(dir);
						if (!visited_set.contains(v) && controller.canSenseLocation(v))
						{
							queue[++queue_tail] = v;
							traceback_subscript[queue_tail] = queue_head;
							turn_penalty[queue_tail] = getTurnPenalty(v);
							visited_set.add(v);
						}
					}
				}
				queue_head++;
			}
		}
		
		/**
		 * Returns center if not plausible
		 * else returns best direction
		 */
		Direction tracebackForBestDirection()
		{
			int best_location_sub = queue_head;
			int best_cost = travelDistance(queue[queue_head], target_location) + turn_penalty[queue_head];
			for (int i = queue_head + 1; i <= queue_tail; i++)
			{
				int current_cost = travelDistance(queue[i], target_location) + turn_penalty[i];
				if (current_cost < best_cost)
				{
					best_cost = current_cost;
					best_location_sub = i;
				}
			}
			if (queue_head > queue_tail || best_location_sub == 0)
				return Direction.CENTER;
			while (traceback_subscript[best_location_sub] > 0)
				best_location_sub = traceback_subscript[best_location_sub];
			return queue[0].directionTo(queue[best_location_sub]);
		}
		
		int tryMoveInTracebackedDirection(Direction direction) throws GameActionException
		{
			if (direction == Direction.CENTER)
			{
				return 0;
			}
			else
			{
				if (controller.canMove(direction))
				{
					controller.move(direction);
					return 1;
				}
				return 0;
			}
		}
		
		int tryMove() throws GameActionException
		{
			while (step-- > 0)
				updateSearchedLocationsByOneStep();
			return tryMoveInTracebackedDirection(tracebackForBestDirection());
		}
	}
	
	// Greedily from 3 naive options
	public static int greedyMove(MapLocation targetLoc)
			throws GameActionException
	{
		
		if (targetLoc == null) return 0;
		// TODO: Set bytecode limit and bound it with step
		/*if (Clock.getBytecodesLeft() >= BFS_BYTECODE_BOUND)
		{
			System.out.println(">" + Clock.getBytecodesLeft());
			int step = 2;
			BFS bfs = new BFS(step, targetLoc, CURRENT_LOCATION);
			int ret = bfs.tryMove();
			System.out.println("<" + Clock.getBytecodesLeft());
			return ret;
		}*/
		// Potential choices
		MapLocation
				a =
				CURRENT_LOCATION.add(CURRENT_LOCATION.directionTo(
						targetLoc));
		MapLocation
				b =
				CURRENT_LOCATION.add(CURRENT_LOCATION.directionTo(
						targetLoc).rotateRight());
		MapLocation
				c =
				CURRENT_LOCATION.add(CURRENT_LOCATION.directionTo(
						targetLoc).rotateLeft());
		MapLocation[] choices = new MapLocation[3];
		//Bytecode efficient insertion sort
		if (controller.canSenseLocation(a))
		{
			choices[0] = a;
		}
		if (controller.canSenseLocation(b))
		{
			double
					costA =
					controller.senseRubble(a) / 2.0;
			double
					costB =
					controller.senseRubble(b);
			if (costB < costA)
			{
				choices[0] = b;
				choices[1] = a;
			}
			else
			{
				choices[1] = b;
			}
			if (controller.canSenseLocation(c))
			{
				double
						costC =
						controller.senseRubble(c);
				if (costC < Math.min(costA, costB))
				{
					choices[2] = choices[1];
					choices[1] = choices[0];
					choices[0] = c;
				}
				else if (costC < costA || costC < costB)
				{
					choices[2] = choices[1];
					choices[1] = c;
				}
				else
				{
					choices[2] = c;
				}
			}
		}
		else if (controller.canSenseLocation(c))
		{
			if (2.0 * controller.senseRubble(c) <
					controller.senseRubble(a))
			{
				choices[0] = c;
				choices[1] = a;
			}
		}
		
		// Move
		for (int i = 0; i <= 2; i++)
		{
			if (choices[i] == null)
				return 0;
			if (naiveMove(choices[i]))
				return 1;
		}
		return 0;
	}
	// Naive movement | error checks
	
	/* Finds a random valid direction.
	returns null if no valid direction */
	public static Direction randomValidDirection()
	{
		return toMovePreferredDirection(DIRECTIONS[random.nextInt(
				8)], 4);
	}
	
	/* Greedily returns the closest valid direction to preferredDirection within the directionFlexibilityDelta value (2 means allow for 2 clockwise 45 deg in both directions)
	Returns null if no valid direction with specification
	directionFlexibilityDelta: max value 4 */
	public static Direction toMovePreferredDirection(
			Direction preferredDirection,
			int directionFlexibilityDelta)
	{
		
		if (controller.getMovementCooldownTurns() > 0 ||
				preferredDirection == null) return null;
		
		if (controller.canMove(preferredDirection))
		{
			return preferredDirection;
		}
		
		Direction left = preferredDirection;
		Direction right = preferredDirection;
		for (int i = 1; i <= directionFlexibilityDelta; ++i)
		{
			right = right.rotateRight();
			left = left.rotateLeft();
			if (controller.canMove(right)) return right;
			if (controller.canMove(left)) return left;
		}
		return null;
	}
	
	public static Boolean naiveMove(Direction dir)
			throws GameActionException
	{
		if (dir != null && controller.canMove(dir))
		{
			controller.move(dir);
			return true;
		}
		return false;
	}
	
	public static Boolean naiveMove(MapLocation loc)
			throws GameActionException
	{
		if (loc == null) return false;
		return naiveMove(controller.getLocation()
				.directionTo(loc));
	}
	
	// Util
	public static Integer travelDistance(MapLocation a,
	                                     MapLocation b)
	{
		if (a == null || b == null) return 99999;
		return Math.max(Math.abs(a.x - b.x),
				Math.abs(a.y - b.y));
	}
	
	public static boolean inMap(MapLocation a)
	{
		if (a == null) return false;
		return a.x < MAP_WIDTH && a.y < MAP_HEIGHT;
	}
	
	public static int[] relative(MapLocation from,
	                             MapLocation to)
	{
		return new int[]{to.x - from.x, to.y - from.y};
	}
	
	private static double calculateGradient(
			MapLocation start, MapLocation end)
	{
		if (start == null || end == null) return -2;
		if (end.x - start.x == 0)
		{
			return -1;
		}
		//Rise over run
		return 1.0 * (end.y - start.y) / (end.x - start.x);
	}
	
	//TODO: Make random take into account map areas
	public static MapLocation randomLocation()
	{
		MapLocation
				res =
				new MapLocation((int) (Math.random() * 64 -
						32) + CURRENT_LOCATION.x,
						(int) (Math.random() * 64 - 32) +
								CURRENT_LOCATION.y);
		int i = 0;
		while (!inMap(res) && i <= 100)
		{
			res =
					new MapLocation((int) (Math.random() *
							64 - 32) +
							CURRENT_LOCATION.x,
							(int) (Math.random() * 64 -
									32) +
									CURRENT_LOCATION.y);
			++i;
		}
		return res;
	}
}
