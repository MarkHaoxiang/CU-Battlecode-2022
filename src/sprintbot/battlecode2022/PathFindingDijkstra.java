package sprintbot.battlecode2022;

import battlecode.common.*;

import java.util.Random;

/**
 * Navigation Module
 * This consumes way to many bytecodes (costs ~ 7 rounds to compute)
 * TODO: make actual efficient ones
 */
public class PathFindingDijkstra
{
	private static RobotController controller;
	private static Random rng;
	private static int move_cooldown_base;
	private static int vision_radius;
	private static int linear_vision_radius;
	private static int total_possible_size;
	private static MapLocation current_location;
	private static final Direction[] DIRECTIONS =
			{Direction.NORTH, Direction.NORTHEAST,
					Direction.EAST, Direction.SOUTHEAST,
					Direction.SOUTH, Direction.SOUTHWEST,
					Direction.WEST, Direction.NORTHWEST};
	
	public static void init(RobotController rc)
	{
		PathFindingDijkstra.controller = rc;
		rng = new Random(rc.getID());
		switch (rc.getType())
		{
			case ARCHON:
			case WATCHTOWER:
				move_cooldown_base = 24;
				vision_radius = 34;
				total_possible_size = 110;
				break;
			case LABORATORY:
				move_cooldown_base = 24;
				vision_radius = 53;
				total_possible_size = 178;
				break;
			case SOLDIER:
				vision_radius = 20;
				move_cooldown_base = 16;
				total_possible_size = 70;
				break;
			case SAGE:
				vision_radius = 20;
				move_cooldown_base = 25;
				total_possible_size = 70;
				break;
			default:
				move_cooldown_base = 20;
				vision_radius = 20;
				total_possible_size = 70;
				break;
		}
		linear_vision_radius =
				(int) Math.sqrt(vision_radius);
	}
	
	private static double estimatedCost(MapLocation a,
	                                    MapLocation b)
	{
		if (a == null || b == null) return 99999;
		int dx = Math.abs(a.x - b.x);
		int dy = Math.abs(a.y - b.y);
		return 400.0 * Math.max(dx, dy) + 1.0 * (dx + dy);
	}
	
	private static double turnCost(MapLocation x)
			throws GameActionException
	{
		return move_cooldown_base * (1.0 + controller.senseRubble(x) / 10.0);
	}
	
	private static class DS // heap
	{
		int size;
		int[] q = new int[total_possible_size];
		double[] v = new double[total_possible_size];
		
		DS()
		{
			size = 0;
		}
		
		int getHead()
		{
			return q[1];
		}
		
		void pushup(int x)
		{
			if (x == 1)
				return;
			int f = x >> 1;
			if (v[q[x]] < v[q[f]])
			{
				int tmp = q[f];
				q[f] = q[x];
				q[x] = tmp;
				pushup(f);
			}
		}
		
		void push(int x, double val)
		{
			v[x] = val;
			q[++size] = x;
			pushup(x);
		}
		
		boolean empty()
		{
			return size == 0;
		}
		
		void pushdown(int x)
		{
			int ls = x << 1;
			if (ls > size)
				return;
			int rs = x << 1 | 1;
			int t;
			if (v[q[ls]] > v[q[rs]] && rs <= size)
				t = rs;
			else
				t = ls;
			if (v[q[t]] < v[q[x]])
			{
				int tmp = q[x];
				q[x] = q[t];
				q[t] = tmp;
				pushdown(t);
			}
		}
		
		void popHead()
		{
			q[1] = q[size--];
			pushdown(1);
		}
	}
	
	public static int move(MapLocation target_location)
			throws
			GameActionException
	{
		if (controller.getMovementCooldownTurns() > 0)
			return 0;
		current_location = controller.getLocation();
		if (target_location == null || !controller.onTheMap(current_location.add(current_location.directionTo(target_location))))
			return 2;
		if (target_location.equals(current_location))
			return 3;
		int tot_visible = 0;
		MapLocation[] visible = new MapLocation[133];
		int[][] XYToID = new int[linear_vision_radius * 2 + 1][linear_vision_radius * 2 + 1];
		double[] dist = new double[total_possible_size];
		int[] totpath = new int[total_possible_size];
		boolean[] isin = new boolean[total_possible_size];
		double[][] path_weight = new double[total_possible_size][9];
		int[][] path_node = new int[total_possible_size][9];
		MapLocation new_location;
		for (int dx = -linear_vision_radius; dx <= linear_vision_radius; dx++)
			for (int dy = -linear_vision_radius; dy <= linear_vision_radius; dy++)
			{
				new_location = current_location.translate(dx, dy);
				if (controller.canSenseLocation(new_location))
				{
					visible[++tot_visible] = new_location;
					XYToID[dx + linear_vision_radius][dy + linear_vision_radius] = tot_visible;
				}
			}
		int fixX = -current_location.x + linear_vision_radius;
		int fixY = -current_location.y + linear_vision_radius;
		int current_location_id = XYToID[linear_vision_radius][linear_vision_radius];
		DS ds = new DS();
		for (int i = 1; i <= tot_visible; i++)
		{
			dist[i] = estimatedCost(visible[i], target_location);
			double myCost = turnCost(visible[i]);
			for (Direction dir : DIRECTIONS)
			{
				MapLocation N = visible[i].add(dir);
				int nx = N.x + fixX, ny = N.y + fixY;
				if (nx >= 0 && ny >= 0 && nx < XYToID.length && ny < XYToID[0].length && XYToID[nx][ny] > 0)
				{
					path_weight[i][++totpath[i]] = myCost;
					path_node[i][totpath[i]] = XYToID[nx][ny];
				}
			}
			ds.push(i, dist[i]);
			isin[i] = true;
		}
		
		MapLocation best_next_location = null;
		// Dijkstra
		while (!ds.empty())
		{
			int u = ds.getHead();
			for (int i = 1; i <= totpath[u]; i++)
			{
				if (path_weight[u][i] + dist[u] < dist[path_node[u][i]])
				{
					dist[path_node[u][i]] = path_weight[u][i] + dist[u];
					if (path_node[u][i] == current_location_id)
						best_next_location = visible[u];
					if (!isin[path_node[u][i]])
						ds.push(path_node[u][i], dist[path_node[u][i]]);
				}
			}
			ds.popHead();
			isin[u] = false;
		}
		
		if (best_next_location == null)
		{
			//stuck
			return 0;
		}
		Direction best_next_direction = current_location.directionTo(best_next_location);
		if (controller.canMove(best_next_direction))
		{
			controller.move(best_next_direction);
			return 1;
		}
		//stuck
		return 0;
	}
}
