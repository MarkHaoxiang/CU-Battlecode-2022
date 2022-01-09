package testenemy;

import battlecode.common.Direction;
import battlecode.common.RobotController;

import java.util.Random;

public class CustomRNG extends Random
{
	public CustomRNG(long seed)
	{
		super(seed);
	}
	
	public CustomRNG(RobotController rc)
	{
		super(rc.getID());
	}
	
	Direction nextDirection()
	{
		return Factory.getDirectionFromSubscript(super.nextInt(
				8));
	}
	
	void adaptSeed(RobotController rc)
	{
		super.setSeed(super.nextLong() ^ rc.getID());
	}
	
	boolean trueWithProbability(double prob)
	{
		double eps = 1e-5;
		if (Math.abs(prob) < eps) prob = 0;
		if (Math.abs(prob - 1) < eps) prob = 1;
		if (prob < 0 || prob > 1)
			throw new IllegalArgumentException("Received " +
					"illegal probability " + prob);
		long acceptRange = (long) (prob * Long.MAX_VALUE);
		return super.nextLong() <= acceptRange;
	}
}