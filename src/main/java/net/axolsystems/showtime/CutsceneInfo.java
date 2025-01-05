package net.axolsystems.showtime;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class CutsceneInfo {
    private final String name;
    private final List<CameraStep> steps;

    public CutsceneInfo(String name) {
        this.name = name;
        this.steps = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<CameraStep> getSteps() {
        return steps;
    }

    public void addStep(Location location, int duration) {
        steps.add(new CameraStep(location, duration));
    }

    public static class CameraStep {
        private final Location location;
        private final int duration;

        public CameraStep(Location location, int duration) {
            this.location = location;
            this.duration = duration;
        }

        public Location getLocation() {
            return location;
        }

        public int getDuration() {
            return duration;
        }
    }
}
