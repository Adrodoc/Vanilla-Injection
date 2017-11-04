package de.adrodoc55.minecraft.structure;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;

import com.evilco.mc.nbt.stream.NbtOutputStream;
import com.evilco.mc.nbt.tag.ITag;
import com.evilco.mc.nbt.tag.TagCompound;
import com.evilco.mc.nbt.tag.TagDouble;
import com.evilco.mc.nbt.tag.TagInteger;
import com.evilco.mc.nbt.tag.TagList;
import com.evilco.mc.nbt.tag.TagString;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import de.adrodoc55.minecraft.coordinate.Coordinate3D;
import de.adrodoc55.minecraft.coordinate.Coordinate3I;

/**
 * A {@link Structure} is used to build the contents of a
 * <a href="https://minecraft.gamepedia.com/Structure_block_file_format">structure file</a>. The
 * <a href="https://minecraft-de.gamepedia.com/NBT-Format">NBT</a> is generated by a call to
 * {@link #toNbt()};
 *
 * @author Adrodoc55
 */
public class Structure {
  static List<ITag> toNbt(Coordinate3D coordinate) {
    return Arrays.asList(//
        new TagDouble("", coordinate.x), //
        new TagDouble("", coordinate.y), //
        new TagDouble("", coordinate.z) //
    );
  }

  static List<ITag> toNbt(Coordinate3I coordinate) {
    return Arrays.asList(//
        new TagInteger("", coordinate.x), //
        new TagInteger("", coordinate.y), //
        new TagInteger("", coordinate.z) //
    );
  }

  /**
   * The {@link Block}s of this {@link Structure}.
   */
  private final Map<Coordinate3I, Block> blocks = new HashMap<>();
  /**
   * The {@link Entity entities} of this {@link Structure}.
   */
  private final List<Entity> entities = new ArrayList<>();
  /**
   * The version of this {@link Structure}.
   */
  private int dataVersion;
  /**
   * The author of this {@link Structure}.
   */
  private String author;
  /**
   * The {@link BlockState} used to fill the {@link Structure}, where there are no {@link #blocks}.
   */
  private @Nullable BlockState background;

  public Structure(int dataVersion, String author) {
    this(dataVersion, author, null);
  }

  public Structure(int dataVersion, String author, @Nullable BlockState background) {
    setDataVersion(dataVersion);
    setAuthor(author);
    setBackground(background);
  }

  /**
   * @return the value of {@link #dataVersion}
   */
  public int getDataVersion() {
    return dataVersion;
  }

  /**
   * @param dataVersion the new value for {@link #dataVersion}
   */
  public void setDataVersion(int dataVersion) {
    this.dataVersion = dataVersion;
  }

  /**
   * @return the value of {@link #author}
   */
  public String getAuthor() {
    return author;
  }

  /**
   * @param author the new value for {@link #author}
   */
  public void setAuthor(String author) {
    this.author = checkNotNull(author, "author == null!");
  }

  /**
   * @return the value of {@link #background}
   */
  public @Nullable BlockState getBackground() {
    return background;
  }

  /**
   * @param background the new value for {@link #background}
   */
  public void setBackground(@Nullable BlockState background) {
    this.background = background;
  }

  /**
   * Adds the specified {@link Block}s to this {@link Structure}. If there is already a
   * {@link Block} at one of the coordinates this method throws an {@link IllegalArgumentException}.
   * To avoid this use {@link #replaceBlocks(Collection)}.
   *
   * @param blocks
   * @throws IllegalArgumentException if there is already a {@link Block} at one of the coordinates
   */
  public void addBlocks(Collection<? extends Block> blocks) throws IllegalArgumentException {
    for (Block block : blocks) {
      addBlock(block);
    }
  }

  /**
   * Adds a {@link Block} to this {@link Structure}. If there is already a {@link Block} at the same
   * coordinate this method throws an {@link IllegalArgumentException}. To avoid this use
   * {@link #replaceBlock(Block)}.
   *
   * @param block
   * @throws IllegalArgumentException if there is already a {@link Block} at the same coordinate
   */
  public void addBlock(Block block) throws IllegalArgumentException {
    Coordinate3I coordinate = block.getCoordinate();
    if (blocks.containsKey(coordinate)) {
      throw new IllegalArgumentException(
          "There is already a block associated with the coordinate " + coordinate);
    }
    replaceBlock(block);
  }

  /**
   * Adds the specified {@link Block}s to this {@link Structure}. Existing {@link Block}s at the
   * same coordinates are replaced.
   *
   * @param blocks
   * @see #replaceBlock(Block)
   */
  public void replaceBlocks(Collection<? extends Block> blocks) {
    for (Block block : blocks) {
      replaceBlock(block);
    }
  }

  /**
   * Adds a {@link Block} to this {@link Structure}. If there is already a {@link Block} at the same
   * coordinate it is replaced.
   *
   * @param block
   * @see #addBlock(Block)
   */
  public void replaceBlock(Block block) {
    blocks.put(block.getCoordinate(), block);
  }

  /**
   * Add {@link Entity entities} to {@link #entities}.
   *
   * @param entities
   */
  public void addEntities(Collection<? extends Entity> entities) {
    this.entities.addAll(entities);
  }

  /**
   * Add an {@link Entity} to {@link #entities}.
   *
   * @param entity
   */
  public void addEntity(Entity entity) {
    checkNotNull(entity, "entity == null!");
    entities.add(entity);
  }

  /**
   * Calculates the size required to fit in all {@link #blocks} and {@link #entities}.
   *
   * @return the required size
   */
  public Coordinate3I getSize() {
    return Stream.concat(//
        blocks.keySet().stream(), //
        entities.stream()//
            .map(Entity::getCoordinate)//
            .map(Coordinate3D::floor)//
    ).reduce(Coordinate3I.getBinaryOperator(Math::max))//
        .map(c -> c.plus(new Coordinate3I(1, 1, 1)))//
        .orElse(new Coordinate3I());
  }

  /**
   * Write the <a href="https://minecraft-de.gamepedia.com/NBT-Format">NBT</a> obtained by calling
   * {@link #toNbt()} to the specified {@link File}.
   *
   * @param file the {@link File} to write to
   * @throws IOException if an I/O error has occurred
   */
  public void writeTo(File file) throws IOException {
    Files.createParentDirs(file);
    TagCompound nbt = toNbt();
    try (NbtOutputStream out =
        new NbtOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
      out.write(nbt);
    }
  }

  /**
   * Returns a <a href="https://minecraft-de.gamepedia.com/NBT-Format">NBT</a> representation of
   * this {@link Structure}.
   *
   * @return a new {@link TagCompound}
   */
  public TagCompound toNbt() {
    TagCompound result = new TagCompound("");
    result.setTag(new TagInteger("DataVersion", dataVersion));
    result.setTag(new TagString("author", author));
    Coordinate3I size = getSize();
    List<Block> blocks = new ArrayList<>(this.blocks.values());
    if (background != null) {
      for (int x = 0; x < size.getX(); x++) {
        for (int y = 0; y < size.getY(); y++) {
          for (int z = 0; z < size.getZ(); z++) {
            Coordinate3I coordinate = new Coordinate3I(x, y, z);
            if (!this.blocks.containsKey(coordinate)) {
              blocks.add(new SimpleBlock(background, coordinate));
            }
          }
        }
      }
    }
    result.setTag(new TagList("size", toNbt(size)));
    Palette palette = new Palette();
    result.setTag(palette.toNbt(blocks));
    result.setTag(palette.toNbt());
    result.setTag(new TagList("entities", Lists.transform(entities, this::toNbt)));
    return result;
  }

  private TagCompound toNbt(Entity entity) {
    TagCompound result = new TagCompound("");
    result.setTag(new TagList("pos", toNbt(entity.getCoordinate())));
    result.setTag(new TagList("blockPos", toNbt(entity.getCoordinate().floor())));
    TagCompound nbt = entity.getNbt();
    if (nbt != null) {
      result.setTag(nbt);
    }
    return result;
  }

  @Override
  public String toString() {
    return "Structure [blocks=" + blocks + ", entities=" + entities + ", dataVersion=" + dataVersion
        + ", author=" + author + ", background=" + background + "]";
  }
}
