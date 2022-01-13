package sprintbot.battlecode2022.util.navigation;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import sprintbot.battlecode2022.util.Navigator;

import static battlecode.common.Direction.*;

/**
 * Costs 6000 bytecodes
 * To be optimized
 */
public class R4Navigator extends Navigator
{
	private final int VISION_RADIUS = 4;
	private final int LIM = 13;
	private final int SOURCE = 6;
	private final int[] DX = new int[]{-2, -1, -1, -1, 0, 0, 0, 0, 0, 1, 1, 1, 2};
	private final int[] DY = new int[]{0, -1, 0, 1, -2, -1, 0, 1, 2, -1, 0, 1, 0};
	private final int[][]
			NEIGHBOUR =
			new int[][]{{1, 2, 3},
					{0, 2, 4, 5, 6},
					{0, 1, 3, 5, 6, 7},
					{0, 2, 6, 7, 8},
					{1, 5, 9},
					{1, 2, 4, 6, 9, 10},
					{1, 2, 3, 5, 7, 9, 10, 11},
					{2, 3, 6, 8, 10, 11},
					{3, 7, 11},
					{4, 5, 6, 10, 12},
					{5, 6, 7, 9, 11, 12},
					{6, 7, 8, 10, 12},
					{9, 10, 11}};
	private final int[] EIGHT_ID = new int[]{7, 11, 10, 9, 5, 1, 2, 3};
	
	
	private final int X_BOUND;
	private final int Y_BOUND;
	private final int SWIPE_BOUND = 2;
	private final int INF_MARBLE = 100000;
	private final int MOVE_COOLDOWN;
	private final int UPPER_BOUND_TURN_PER_GRID;
	private final Direction[] EIGHT_DIRECTION = new Direction[]{NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST};
	private final int[] MARBLE_COST_LOOKUP;
	private final RobotController rc;
	private int[] marble_cost = new int[LIM], dist = new int[LIM];
	private Direction best_direction;
	
	public R4Navigator(RobotController controller)
	{
		super(controller);
		rc = controller;
		X_BOUND = rc.getMapWidth() - 1;    // [0, X_BOUND]
		Y_BOUND = rc.getMapHeight() - 1;
		MOVE_COOLDOWN = rc.getType().movementCooldown;
		UPPER_BOUND_TURN_PER_GRID = MOVE_COOLDOWN * 11;
		MARBLE_COST_LOOKUP =
				new int[]{(int) (1.0 * MOVE_COOLDOWN),
						(int) (1.1 * MOVE_COOLDOWN),
						(int) (1.2 * MOVE_COOLDOWN),
						(int) (1.3 * MOVE_COOLDOWN),
						(int) (1.4 * MOVE_COOLDOWN),
						(int) (1.5 * MOVE_COOLDOWN),
						(int) (1.6 * MOVE_COOLDOWN),
						(int) (1.7 * MOVE_COOLDOWN),
						(int) (1.8 * MOVE_COOLDOWN),
						(int) (1.9 * MOVE_COOLDOWN),
						(int) (2.0 * MOVE_COOLDOWN),
						(int) (2.1 * MOVE_COOLDOWN),
						(int) (2.2 * MOVE_COOLDOWN),
						(int) (2.3 * MOVE_COOLDOWN),
						(int) (2.4 * MOVE_COOLDOWN),
						(int) (2.5 * MOVE_COOLDOWN),
						(int) (2.6 * MOVE_COOLDOWN),
						(int) (2.7 * MOVE_COOLDOWN),
						(int) (2.8 * MOVE_COOLDOWN),
						(int) (2.9 * MOVE_COOLDOWN),
						(int) (3.0 * MOVE_COOLDOWN),
						(int) (3.1 * MOVE_COOLDOWN),
						(int) (3.2 * MOVE_COOLDOWN),
						(int) (3.3 * MOVE_COOLDOWN),
						(int) (3.4 * MOVE_COOLDOWN),
						(int) (3.5 * MOVE_COOLDOWN),
						(int) (3.6 * MOVE_COOLDOWN),
						(int) (3.7 * MOVE_COOLDOWN),
						(int) (3.8 * MOVE_COOLDOWN),
						(int) (3.9 * MOVE_COOLDOWN),
						(int) (4.0 * MOVE_COOLDOWN),
						(int) (4.1 * MOVE_COOLDOWN),
						(int) (4.2 * MOVE_COOLDOWN),
						(int) (4.3 * MOVE_COOLDOWN),
						(int) (4.4 * MOVE_COOLDOWN),
						(int) (4.5 * MOVE_COOLDOWN),
						(int) (4.6 * MOVE_COOLDOWN),
						(int) (4.7 * MOVE_COOLDOWN),
						(int) (4.8 * MOVE_COOLDOWN),
						(int) (4.9 * MOVE_COOLDOWN),
						(int) (5.0 * MOVE_COOLDOWN),
						(int) (5.1 * MOVE_COOLDOWN),
						(int) (5.2 * MOVE_COOLDOWN),
						(int) (5.3 * MOVE_COOLDOWN),
						(int) (5.4 * MOVE_COOLDOWN),
						(int) (5.5 * MOVE_COOLDOWN),
						(int) (5.6 * MOVE_COOLDOWN),
						(int) (5.7 * MOVE_COOLDOWN),
						(int) (5.8 * MOVE_COOLDOWN),
						(int) (5.9 * MOVE_COOLDOWN),
						(int) (6.0 * MOVE_COOLDOWN),
						(int) (6.1 * MOVE_COOLDOWN),
						(int) (6.2 * MOVE_COOLDOWN),
						(int) (6.3 * MOVE_COOLDOWN),
						(int) (6.4 * MOVE_COOLDOWN),
						(int) (6.5 * MOVE_COOLDOWN),
						(int) (6.6 * MOVE_COOLDOWN),
						(int) (6.7 * MOVE_COOLDOWN),
						(int) (6.8 * MOVE_COOLDOWN),
						(int) (6.9 * MOVE_COOLDOWN),
						(int) (7.0 * MOVE_COOLDOWN),
						(int) (7.1 * MOVE_COOLDOWN),
						(int) (7.2 * MOVE_COOLDOWN),
						(int) (7.3 * MOVE_COOLDOWN),
						(int) (7.4 * MOVE_COOLDOWN),
						(int) (7.5 * MOVE_COOLDOWN),
						(int) (7.6 * MOVE_COOLDOWN),
						(int) (7.7 * MOVE_COOLDOWN),
						(int) (7.8 * MOVE_COOLDOWN),
						(int) (7.9 * MOVE_COOLDOWN),
						(int) (8.0 * MOVE_COOLDOWN),
						(int) (8.1 * MOVE_COOLDOWN),
						(int) (8.2 * MOVE_COOLDOWN),
						(int) (8.3 * MOVE_COOLDOWN),
						(int) (8.4 * MOVE_COOLDOWN),
						(int) (8.5 * MOVE_COOLDOWN),
						(int) (8.6 * MOVE_COOLDOWN),
						(int) (8.7 * MOVE_COOLDOWN),
						(int) (8.8 * MOVE_COOLDOWN),
						(int) (8.9 * MOVE_COOLDOWN),
						(int) (9.0 * MOVE_COOLDOWN),
						(int) (9.1 * MOVE_COOLDOWN),
						(int) (9.2 * MOVE_COOLDOWN),
						(int) (9.3 * MOVE_COOLDOWN),
						(int) (9.4 * MOVE_COOLDOWN),
						(int) (9.5 * MOVE_COOLDOWN),
						(int) (9.6 * MOVE_COOLDOWN),
						(int) (9.7 * MOVE_COOLDOWN),
						(int) (9.8 * MOVE_COOLDOWN),
						(int) (9.9 * MOVE_COOLDOWN),
						(int) (10.0 * MOVE_COOLDOWN),
						(int) (10.1 * MOVE_COOLDOWN),
						(int) (10.2 * MOVE_COOLDOWN),
						(int) (10.3 * MOVE_COOLDOWN),
						(int) (10.4 * MOVE_COOLDOWN),
						(int) (10.5 * MOVE_COOLDOWN),
						(int) (10.6 * MOVE_COOLDOWN),
						(int) (10.7 * MOVE_COOLDOWN),
						(int) (10.8 * MOVE_COOLDOWN),
						(int) (10.9 * MOVE_COOLDOWN),
						(int) (11.0 * MOVE_COOLDOWN)};
	}
	
	private int estimateUpperBound(int x1, int y1, int x2, int y2)
	{
		return UPPER_BOUND_TURN_PER_GRID * Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
	}
	
	public MoveResult move(MapLocation target_loc) throws GameActionException
	{
		if (!rc.isMovementReady())
			return MoveResult.FAIL;
		if (target_loc == null)
			return MoveResult.IMPOSSIBLE;
		int tar_loc_x = target_loc.x;
		int tar_loc_y = target_loc.y;
		if (tar_loc_x < 0 || tar_loc_y < 0
				|| tar_loc_x > X_BOUND || tar_loc_y > Y_BOUND)
			return MoveResult.IMPOSSIBLE;
		MapLocation current_loc = rc.getLocation();
		if (current_loc == target_loc)
			return MoveResult.REACHED;
		//System.out.printf("Check Complete, left = %d\n", Clock.getBytecodesLeft());
		int cur_loc_x = current_loc.x;
		int cur_loc_y = current_loc.y;
		for (int t = LIM; --t >= 0; )
		{
			//System.out.printf("Initializing position %d, left = %d\n", t, Clock.getBytecodesLeft());
			int ax = cur_loc_x + DX[t];
			int ay = cur_loc_y + DY[t];
			try
			{
				marble_cost[t] = MARBLE_COST_LOOKUP[rc.senseRubble(new MapLocation(ax, ay))];
			} catch (GameActionException e)
			{
				marble_cost[t] = INF_MARBLE;
			}
			dist[t] = estimateUpperBound(ax, ay, tar_loc_x, tar_loc_y);
		}
		int can_move_direction_cnt = 0;
		for (int t = 8; --t >= 0; )
			if (!rc.canMove(EIGHT_DIRECTION[t]))
				marble_cost[EIGHT_ID[t]] = INF_MARBLE;
			else
				can_move_direction_cnt++;
		if (can_move_direction_cnt == 0)
			return MoveResult.FAIL;
		int final_relax = 0;
		//System.out.printf("Initialization Complete, left = %d\n", Clock.getBytecodesLeft());
		for (int swipe = SWIPE_BOUND; --swipe >= 0; )
			for (int t = LIM; --t >= 0; )
			{
				//System.out.printf("Swipe No. %d, Relaxing position %d, left = %d\n", swipe, t, Clock.getBytecodesLeft());
				int relax_dist = dist[t] + marble_cost[t];
				for (int v : NEIGHBOUR[t])
					if (relax_dist < dist[v])
					{
						dist[v] = relax_dist;
						if (v == SOURCE)
							final_relax = t;
					}
			}
		if (final_relax == 0)
			return MoveResult.FAIL;
		for (int t = 8; --t >= 0; )
			if (EIGHT_ID[t] == final_relax)
			{
				best_direction = EIGHT_DIRECTION[t];
				break;
			}
		try
		{
			rc.move(best_direction);
			//System.out.printf("Successful move, left = %d\n", Clock.getBytecodesLeft());
			return MoveResult.SUCCESS;
		} catch (GameActionException e)
		{
			return MoveResult.FAIL;
		}
	}
}
