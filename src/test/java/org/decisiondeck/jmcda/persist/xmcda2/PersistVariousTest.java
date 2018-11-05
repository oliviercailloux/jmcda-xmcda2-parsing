package org.decisiondeck.jmcda.persist.xmcda2;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.decision_deck.utils.ByteArraysSupplier;
import org.decision_deck.utils.StringUtils;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDASortingProblemReader;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDASortingProblemWriter;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResults;
import org.decisiondeck.xmcda_oo.structure.sorting.SortingProblemUtils;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;

public class PersistVariousTest {
	@Test
	public void testEmpty() throws Exception {
		final ByteArraysSupplier destination = StringUtils.newByteArraysSupplier();
		new XMCDASortingProblemWriter(destination).writeResults(SortingProblemUtils.newResults());
		final ByteArrayOutputStream written = Iterables.getOnlyElement(destination.getArrays());
		final ByteSource source = ByteSource.wrap(written.toByteArray());
		final ISortingResults read = new XMCDASortingProblemReader(source).readSortingResults();
		assertTrue(read.getAllAlternatives().isEmpty());
		assertTrue(read.getCriteria().isEmpty());
		assertTrue(read.getAssignments().getAlternatives().isEmpty());
		assertTrue(read.getCatsAndProfs().isEmpty());
		assertTrue(read.getCoalitions().isEmpty());
	}
}
