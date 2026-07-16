package me.katoro.autedium.grindfulness.pinbook;

// pure bearing/distance math for the pin book hud. no MC imports — plain JUnit territory.
public final class PinMath {
	// index 0 = dead ahead, going clockwise (mc yaw increases clockwise from +Z/south)
	public static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	private PinMath() {}

	// mc yaw convention: 0 faces +Z, facing vector = (-sin(yaw), cos(yaw)) in (x, z)
	public static int octant(double yawDeg, double dx, double dz) {
		double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
		double rel = wrapDegrees(targetYaw - yawDeg);
		return Math.floorMod((int) Math.round(rel / 45.0), 8);
	}

	public static String arrow(double yawDeg, double dx, double dz) {
		return ARROWS[octant(yawDeg, dx, dz)];
	}

	// wrap into [-180, 180)
	public static double wrapDegrees(double deg) {
		double d = deg % 360.0;
		if (d >= 180.0) d -= 360.0;
		if (d < -180.0) d += 360.0;
		return d;
	}

	public static int distanceBlocks(double dx, double dy, double dz) {
		return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
	}
}
