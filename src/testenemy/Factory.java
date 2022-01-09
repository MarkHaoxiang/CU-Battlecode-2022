package testenemy;

import battlecode.common.Direction;

public class Factory
{
	static int getDirectionSubscript(Direction o)
	{
		switch (o)
		{
			case NORTH:
				return 0;
			case NORTHEAST:
				return 1;
			case EAST:
				return 2;
			case SOUTHEAST:
				return 3;
			case SOUTH:
				return 4;
			case SOUTHWEST:
				return 5;
			case WEST:
				return 6;
			case NORTHWEST:
				return 7;
			default:
				return -1;
		}
	}
	
	static Direction getDirectionFromSubscript(int o)
	{
		switch (o)
		{
			case 0:
				return Direction.NORTH;
			case 1:
				return Direction.NORTHEAST;
			case 2:
				return Direction.EAST;
			case 3:
				return Direction.SOUTHEAST;
			case 4:
				return Direction.SOUTH;
			case 5:
				return Direction.SOUTHWEST;
			case 6:
				return Direction.WEST;
			case 7:
				return Direction.NORTHWEST;
			default:
				return null;
		}
	}
}
