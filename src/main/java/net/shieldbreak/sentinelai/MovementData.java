package net.shieldbreak.sentinelai;


import java.util.Objects;

class MovementData {
    private final long timeSinceStarted;
    private final double x;
    private final double y;
    private final double z;
    private final float pitch;
    private final float yaw;

    public MovementData(long timeSinceStarted, double x, double y, double z, float pitch, float yaw) {
        this.timeSinceStarted = timeSinceStarted;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public long getTimeSinceStarted() {
        return timeSinceStarted;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MovementData other = (MovementData) obj;

        return Float.compare(other.pitch, pitch) == 0 && Float.compare(other.yaw, yaw) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, pitch, yaw);
    }
}