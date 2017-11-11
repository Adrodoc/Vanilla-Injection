package de.adrodoc55.minecraft.placement;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.primitives.Ints.max;
import static java.math.RoundingMode.CEILING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.math.DoubleMath;

import de.adrodoc55.minecraft.coordinate.Axis3;
import de.adrodoc55.minecraft.coordinate.Coordinate3I;
import de.adrodoc55.minecraft.coordinate.Direction3;
import de.adrodoc55.minecraft.coordinate.Orientation3;

/**
 * @author Adrodoc55
 * @see #place(List, Coordinate3I, Coordinate3I, Orientation3, CommandBlockFactory)
 */
public class CommandBlockPlacer {
  /**
   * Places the {@link Command}s in {@code chain} between {@code min} and {@code max} according to
   * the specified {@link Orientation3}. The {@link CommandBlockFactory} is used to create the
   * resulting command blocks.
   *
   * @param chain the chain containing all {@link Command}s that should be placed
   * @param min the minimal {@link Coordinate3I} (inclusive)
   * @param max the maximal {@link Coordinate3I} (exclusive)
   * @param orientation the {@link Orientation3}
   * @param factory
   * @return the command blocks generated by {@code factory} using the coordinates of the placement
   * @throws NotEnoughSpaceException if not all {@link Command}s of {@code chain} could be placed in
   *         the cubiod between {@code min} and {@code max}
   */
  public static <C extends Command, CB> Collection<CB> place(List<? extends C> chain,
      Coordinate3I min, Coordinate3I max, Orientation3 orientation,
      CommandBlockFactory<C, CB> factory) throws NotEnoughSpaceException {
    checkNotNull(factory, "factory == null!");
    List<CommandBlock<C>> blocks = place(chain, min, max, orientation);
    return transformResult(factory, blocks);
  }

  private static <CB, C extends Command> Collection<CB> transformResult(
      CommandBlockFactory<C, CB> factory, List<CommandBlock<C>> blocks) {
    List<CB> result = new ArrayList<>();
    int i = 0;
    for (CommandBlock<C> block : blocks) {
      result.add(
          factory.create(i++, block.getCommand(), block.getCoordinate(), block.getDirection()));
    }
    return result;
  }

  private static <C extends Command> List<CommandBlock<C>> place(List<? extends C> chain,
      Coordinate3I min, Coordinate3I max, Orientation3 orientation) throws NotEnoughSpaceException {
    checkNotNull(chain, "chain == null!");
    checkNotNull(min, "min == null!");
    checkNotNull(max, "max == null!");
    checkNotNull(orientation, "orientation == null!");
    checkArgument(min.x < max.x, "min.x >= max.x!");
    checkArgument(min.z < max.y, "min.y >= max.y!");
    checkArgument(min.z < max.y, "min.z >= max.z!");

    int deltaX = max.x - min.x;
    int deltaY = max.y - min.y;
    int deltaZ = max.z - min.z;

    // Initialized to minimal side length of a cube that can hold all commands
    int sideLength = DoubleMath.roundToInt(Math.cbrt(chain.size()), CEILING);
    while (true) {
      // -1 because the corners of ChainPlacer.place are inclusive
      Coordinate3I estimatedMax = Coordinate3I.min(max, min.plus(new Coordinate3I(sideLength - 1)));
      List<Coordinate3I> curve = getSpaceFillingCurve(min, estimatedMax, orientation);
      try {
        return ChainPlacer.place(chain, curve);
      } catch (NotEnoughSpaceException ex) {
        if (sideLength > max(deltaX, deltaY, deltaZ)) {
          throw ex;
        }
        sideLength++;
      }
    }
  }

  /**
   * Returns a {@link List} of {@link Coordinate3I}s that completely fill the cubiod between
   * {@code corner1} and {@code corner2} as a space filling curve with the specified
   * {@link Orientation3}.<br>
   * Furthermore the {@link List} has the following properties:
   * <ul>
   * <li>The distance between two successive coordinates in the returned list is always 1.</li>
   * <li>The list contains only distinct coordinates. No two coordinates in the list are equal.</li>
   * <li>The first coordinate is in one corner of the cuboid and the last one is in the diagonally
   * opposite corner (Not neccessarily {@code corner1} or {@code corner2}).</li>
   * </ul>
   * There are no limitations for {@code corner1} and {@code corner2}, they can be positive or
   * negative and it does not matter which one is bigger.
   *
   * @param corner1 one corner of the cuboid (inclusive)
   * @param corner2 the diagonally opposite corner of the cuboid (inclusive)
   * @param orientation
   * @return a space filling curve for the cuboid between {@code corner1} and {@code corner2} with
   *         the specified {@code orientation}
   */
  public static List<Coordinate3I> getSpaceFillingCurve(Coordinate3I corner1, Coordinate3I corner2,
      Orientation3 orientation) {
    final Coordinate3I min = Coordinate3I.min(corner1, corner2);
    final Coordinate3I max = Coordinate3I.max(corner1, corner2);

    final Direction3 tDirection = orientation.getTertiary();
    final Direction3 sDirection = orientation.getSecondary();
    final Direction3 pDirection = orientation.getPrimary();
    final Axis3 tAxis = tDirection.getAxis();
    final Axis3 sAxis = sDirection.getAxis();
    final Axis3 pAxis = pDirection.getAxis();
    final int minT = min.get(tAxis);
    final int maxT = max.get(tAxis);
    final int minS = min.get(sAxis);
    final int maxS = max.get(sAxis);
    final int minP = min.get(pAxis);
    final int maxP = max.get(pAxis);

    List<Coordinate3I> result = new ArrayList<>();
    boolean backwardsSecondary = false;
    boolean backwardsPrimary = false;
    boolean incrementT = tDirection.isPositive();
    for (int t = incrementT ? minT : maxT; minT <= t && t <= maxT; t += incrementT ? 1 : -1) {
      boolean incrementS = sDirection.isPositive() ^ backwardsSecondary;
      for (int s = incrementS ? minS : maxS; minS <= s && s <= maxS; s += incrementS ? 1 : -1) {
        boolean incrementP = pDirection.isPositive() ^ backwardsPrimary;
        for (int p = incrementP ? minP : maxP; minP <= p && p <= maxP; p += incrementP ? 1 : -1) {
          result.add(new Coordinate3I()//
              .plus(p, pAxis)// Primary
              .plus(s, sAxis)// Secondary
              .plus(t, tAxis)// Tertiary
          );
        }
        backwardsPrimary = !backwardsPrimary;
      }
      backwardsSecondary = !backwardsSecondary;
    }
    return result;
  }
}
