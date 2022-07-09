package dev.cdm.grib.grib2.table;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class WmoFlagTable implements Grib2FlagTableInterface {
  private final WmoCodeFlagTables.WmoTable wmoTable;
  private final ImmutableListMultimap<Integer, WmoCodeFlagTables.WmoTable.WmoEntry> multimap;

  WmoFlagTable(WmoCodeFlagTables.WmoTable wmoTable) {
    Preconditions.checkNotNull(wmoTable);
    Preconditions.checkArgument(wmoTable.getType() == WmoCodeFlagTables.TableType.flag);
    this.wmoTable = wmoTable;
    ImmutableListMultimap.Builder<Integer, WmoCodeFlagTables.WmoTable.WmoEntry> builder = ImmutableListMultimap.builder();
    for (WmoCodeFlagTables.WmoTable.WmoEntry wmoEntry : wmoTable.getEntries()) {
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
    for (Map.Entry<Integer, Collection<WmoCodeFlagTables.WmoTable.WmoEntry>> entry : multimap.asMap().entrySet()) {
      builder.add(new WmoFlagEntry(entry.getKey(), entry.getValue()));
    }
    return builder.build();
  }

  @Nullable
  @Override
  public Entry getEntry(int code) {
    ImmutableList<WmoCodeFlagTables.WmoTable.WmoEntry> entries = multimap.get(code);
    if (entries == null)
      return null;
    return new WmoFlagEntry(code, entries);
  }

  private static class WmoFlagEntry implements Grib2FlagTableInterface.Entry {
    private final int code;
    private final Collection<WmoCodeFlagTables.WmoTable.WmoEntry> entries;

    public WmoFlagEntry(int code, Collection<WmoCodeFlagTables.WmoTable.WmoEntry> entries) {
      this.code = code;
      this.entries = entries;
    }

    @Override
    public int getCode() {
      return code;
    }

    @Override
    public List<Integer> getValues() {
      return entries.stream().map(WmoCodeFlagTables.WmoTable.WmoEntry::getValue).collect(ImmutableList.toImmutableList());
    }

    @Nullable
    @Override
    public String getName(int value) {
      return entries.stream().filter(e -> e.getValue() == value).findFirst().map(WmoCodeFlagTables.WmoTable.WmoEntry::getName).orElse(null);
    }
  }

}
