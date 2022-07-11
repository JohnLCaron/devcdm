package dev.ucdm.grib.grib2.table;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import dev.ucdm.grib.grib2.table.WmoCodeFlagTables.WmoTable;
import dev.ucdm.grib.grib2.table.WmoCodeFlagTables.WmoTable.WmoEntry;

import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class WmoFlagTable implements Grib2FlagTableInterface {
  private final WmoTable wmoTable;
  private final ImmutableListMultimap<Integer, WmoEntry> multimap;

  WmoFlagTable(WmoTable wmoTable) {
    Preconditions.checkNotNull(wmoTable);
    Preconditions.checkArgument(wmoTable.getType() == WmoCodeFlagTables.TableType.flag);
    this.wmoTable = wmoTable;
    ImmutableListMultimap.Builder<Integer, WmoEntry> builder = ImmutableListMultimap.builder();
    for (WmoEntry wmoEntry : wmoTable.getEntries()) {
      builder.put(wmoEntry.getNumber(), wmoEntry);
    }
    this.multimap = builder.build();
  }

  @Override
  public String getName() {
    return wmoTable.getName();
  }

  @Override
  public String getShortName() {
    return wmoTable.getName();
  }

  @Override
  public List<Entry> getEntries() {
    ImmutableList.Builder<Entry> builder = ImmutableList.builder();
    for (Map.Entry<Integer, Collection<WmoEntry>> entry : multimap.asMap().entrySet()) {
      builder.add(new WmoFlagEntry(entry.getKey(), entry.getValue()));
    }
    return builder.build();
  }

  @Nullable
  @Override
  public Entry getEntry(int code) {
    ImmutableList<WmoEntry> entries = multimap.get(code);
    if (entries == null)
      return null;
    return new WmoFlagEntry(code, entries);
  }

  private static class WmoFlagEntry implements Grib2FlagTableInterface.Entry {
    private final int code;
    private final Collection<WmoEntry> entries;

    public WmoFlagEntry(int code, Collection<WmoEntry> entries) {
      this.code = code;
      this.entries = entries;
    }

    @Override
    public int getCode() {
      return code;
    }

    @Override
    public List<Integer> getValues() {
      return entries.stream().map(WmoEntry::getValue).collect(ImmutableList.toImmutableList());
    }

    @Nullable
    @Override
    public String getName(int value) {
      return entries.stream().filter(e -> e.getValue() == value).findFirst().map(WmoEntry::getName).orElse(null);
    }
  }

}
