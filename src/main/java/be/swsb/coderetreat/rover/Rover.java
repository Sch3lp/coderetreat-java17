package be.swsb.coderetreat.rover;

import be.swsb.coderetreat.vector.Vector;

import java.util.*;
import java.util.function.Function;

import static be.swsb.coderetreat.rover.Orientation.NORTH;
import static java.util.Collections.emptyList;

public class Rover {
    private final Vector position;
    private final Orientation orientation;
    private final List<String> errors;
    private final Function<Rover, Optional<String>> obstacleScanner;
    private final Obstacle obstacle;

    private Rover(Vector position, Orientation orientation, List<String> errors, Function<Rover, Optional<String>> scanner, Obstacle obstacle) {
        this.position = position;
        this.orientation = orientation;
        this.errors = List.copyOf(errors);
        this.obstacleScanner = scanner;
        this.obstacle = obstacle;
    }

    public static Rover initialRover(Vector position, Orientation orientation) {
        return new Rover(position, orientation, emptyList(), r -> Optional.empty(), null);
    }

    public static Rover defaultRover() {
        return new Rover(new Vector(0,0), NORTH, emptyList(), r -> Optional.empty(), null);
    }

    public static Rover aDefaultRoverWithScanner(Function<Rover, Optional<String>> scanner) {
        return new Rover(new Vector(0,0), NORTH, emptyList(), scanner, null);
    }

    private static Rover error(Rover rover, String error) {
        final var errors = new ArrayList<>(rover.errors);
        errors.add(error);
        return new Rover(rover.position, rover.orientation, errors, rover.obstacleScanner, null);
    }

    private static Rover turn(Rover rover, Orientation orientation) {
        return new Rover(rover.position, orientation, rover.errors, rover.obstacleScanner, null);
    }

    private static Rover move(Rover rover, Vector position) {
        return new Rover(position, rover.orientation, rover.errors, rover.obstacleScanner, null);
    }

    private static Rover stop(Rover rover, String obstacle) {
        return new Rover(rover.position, rover.orientation, rover.errors, rover.obstacleScanner, Obstacle.of(obstacle));
    }

    public Rover receive(String commands) {
        return Arrays.stream(commands.split(","))
                .reduce(this,
                        (rover, cmd) -> rover.receive(Command.parseToCommand(cmd)),
                        (r, r2) -> r2);
    }

    public String report() {
        final var messages = new ArrayList<>(errors);
        Obstacle.asString(obstacle).map(messages::add);
        return String.join("\n", messages);
    }

    public Optional<String> scan() {
        return obstacleScanner.apply(this);
    }

    private Rover receive(Command command) {
        return command.execute(this);
    }

    @Override
    public String toString() {
        return "Rover{" +
                "position=" + position +
                ", orientation=" + orientation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rover rover = (Rover) o;
        return Objects.equals(position, rover.position) && orientation == rover.orientation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, orientation);
    }

    sealed static abstract class Command {

        abstract Rover execute(Rover rover);

        final static class UnknownCommand extends Command {
            private final String error;
            UnknownCommand(String unknownCmd) {
                this.error = "Could not parse [" + unknownCmd + "] as a known command";
            }
            Rover execute(Rover rover) {
                return error(rover, error);
            }
        }

        final static class TurnRight extends Command {
            Rover execute(Rover rover) {
                return turn(rover, rover.orientation.turnRight());
            }
        }

        final static class TurnLeft extends Command {
            Rover execute(Rover rover) {
                return turn(rover, rover.orientation.turnLeft());
            }
        }

        final static class ForwardsCommand extends Command {
            Rover execute(Rover rover) {
                return rover.scan().map(scanResult -> stop(rover, scanResult))
                        .orElse(move(rover, rover.position.plus(asVector(rover.orientation))));
            }
        }

        final static class BackwardsCommand extends Command {
            Rover execute(Rover rover) {
                return move(rover, rover.position.plus(asVector(rover.orientation).reversed()));
            }
        }

        protected Vector asVector(Orientation orientation) {
            return switch (orientation) {
                case NORTH -> new Vector(0,1);
                case EAST -> new Vector(1,0);
                case SOUTH -> new Vector(0,-1);
                case WEST -> new Vector(-1,0);
            };
        }

        static Command parseToCommand(String cmd) {
            return switch (cmd.toLowerCase()) {
                case "r" -> new TurnRight();
                case "l" -> new TurnLeft();
                case "f" -> new ForwardsCommand();
                case "b" -> new BackwardsCommand();
                default -> new UnknownCommand(cmd);
            };
        }
    }
}
