package org.decisiondeck.jmcda.persist.xmcda2;

import static org.junit.Assert.assertEquals;

import org.decision_deck.utils.ByteArraysSupplier;
import org.decision_deck.utils.StringUtils;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDASortingProblemWriter;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class XMCDAWriterTest {
    private static final Logger s_logger = LoggerFactory.getLogger(XMCDAWriterTest.class);

    @Test
    public void testWriteData() throws Exception {
	final ISortingData data = SixRealCars.getInstance().getAsSortingPreferences70();
	final ByteArraysSupplier byteArraysSupplier = StringUtils.newByteArraysSupplier();
	final XMCDASortingProblemWriter writer = new XMCDASortingProblemWriter(byteArraysSupplier);
	/**
	 * This make the result closer to what eclipse formatter yields, but anyway it's not identical, so we have to
	 * avoid eclipse formatter.
	 */
	// writer.getSaveOptions().setSavePrettyPrintOffset(0);
	// writer.getSaveOptions().setSavePrettyPrintIndent(4);
	writer.writeData(data);

	final String written = byteArraysSupplier.getWrittenStrings(Charsets.UTF_8).iterator().next();
	s_logger.info("Written data.");
	final String expected = Resources.toString(getClass().getResource("SixRealCars - Expected written data.xml"),
		Charsets.UTF_8);
	assertEquals(expected, written);
    }

    @Test
    public void testWritePreferences() throws Exception {
	final ISortingPreferences data = SixRealCars.getInstance().getAsSortingPreferences70();
	// final OutputSupplier<FileOutputStream> out = Files.newOutputStreamSupplier(new File("out.xml"));
	final ByteArraysSupplier out = StringUtils.newByteArraysSupplier();
	final XMCDASortingProblemWriter writer = new XMCDASortingProblemWriter(out);
	writer.writePreferences(data);

	final String written = out.getWrittenStrings(Charsets.UTF_8).iterator().next();
	final String expected = Resources.toString(
		getClass().getResource("SixRealCars - Expected written preferences.xml"), Charsets.UTF_8);
	assertEquals(expected, written);
    }
}
