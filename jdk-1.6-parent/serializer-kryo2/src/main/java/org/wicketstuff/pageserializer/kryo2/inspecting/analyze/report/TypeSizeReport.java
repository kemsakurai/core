package org.wicketstuff.pageserializer.kryo2.inspecting.analyze.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.pageserializer.kryo2.inspecting.analyze.ISerializedObjectTree;
import org.wicketstuff.pageserializer.kryo2.inspecting.analyze.ISerializedObjectTreeProcessor;
import org.wicketstuff.pageserializer.kryo2.inspecting.analyze.reportbuilder.AttributeBuilder;
import org.wicketstuff.pageserializer.kryo2.inspecting.analyze.reportbuilder.Column;
import org.wicketstuff.pageserializer.kryo2.inspecting.analyze.reportbuilder.Report;

public class TypeSizeReport implements ISerializedObjectTreeProcessor
{

	private final static Logger LOG = LoggerFactory.getLogger(TypeSizeReport.class);

	static final Column emptyFirst = new Column("",
		new AttributeBuilder()
			.set(Column.Separator, "|")
			.build());
	static final Column label = new Column("Type",
		new AttributeBuilder().set(Column.FillAfter, '.')
			.set(Column.Separator, "...")
			.build());
	static final Column size = new Column("bytes", new AttributeBuilder().set(Column.Align.Right)
		.set(Column.FillBefore, '.')
		.set(Column.Separator, "|")
		.build());
	
	@Override
	public void process(ISerializedObjectTree tree)
	{
		if (LOG.isDebugEnabled())
		{
			Map<Class<?>, Counter> map = new HashMap<Class<?>, TypeSizeReport.Counter>();
			process(tree, map);
	
			List<Map.Entry<Class<?>, Counter>> sorted = new ArrayList<Map.Entry<Class<?>, Counter>>();
			sorted.addAll(map.entrySet());
			Collections.sort(sorted, new Comparator<Map.Entry<Class<?>, Counter>>()
			{
				@Override
				public int compare(Entry<Class<?>, Counter> o1, Entry<Class<?>, Counter> o2)
				{
					int s1 = o1.getValue().size;
					int s2 = o2.getValue().size;
					return s1 == s2 ? 0 : s1 > s2 ? -1 : 1;
				}
			});

// StringBuilder sb = new StringBuilder();

// sb.append("\n-----------------------------\n");

			Report report = new Report("\n");
			for (Map.Entry<Class<?>, Counter> e : sorted)
			{
				report.newRow()
					.set(label, 0, e.getKey().getName())
					.set(size, 0, "" + e.getValue().size);
			}
			String result = report.export(emptyFirst, label, size)
				.separateColumnNamesWith('-')
				.tableBorderWith('=')
				.asString();

			LOG.debug(result);
		}
	}

	private void process(ISerializedObjectTree tree, Map<Class<?>, Counter> map)
	{
		Counter counter = getOrCreate(map, tree.type());
		counter.increment(tree.size());

		for (ISerializedObjectTree child : tree.children())
		{
			process(child, map);
		}

	}

	private Counter getOrCreate(Map<Class<?>, Counter> map, Class<? extends Object> type)
	{
		Counter ret = map.get(type);
		if (ret == null)
		{
			ret = new Counter();
			map.put(type, ret);
		}
		return ret;
	}

	static class Counter
	{
		int size;

		void increment(int diff)
		{
			size = size + diff;
		}
	}

}