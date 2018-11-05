package org.decisiondeck.jmcda.persist.xmcda2;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.apache.xmlbeans.XmlException;
import org.decision_deck.jmcda.structure.Criterion;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDASortingProblemReader;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class ReadOldVersionTest {
	@Test
	public void testAutoCorrectVersion() throws Exception {
		final ByteSource readerSupplier = Resources
				.asByteSource(getClass().getResource("SixRealCars v2.0.0 - Criteria.xml"));

		final XMCDASortingProblemReader reader = new XMCDASortingProblemReader();
		reader.setSourceMain(readerSupplier);

		assertEquals(SixRealCars.getInstance().getCriteria(), reader.readCriteria());
	}

	@Test
	public void testCorrectedVersion() throws Exception {
		final ByteSource supplier = Resources.asByteSource(getClass().getResource("SixRealCars v2.0.0 - Criteria.xml"));
		final ByteSource correctVersion = XMCDAReadUtils.getAsVersion(supplier, XMCDAReadUtils.DEFAULT_XMCDA_VERSION);

		final XMCDADoc xmcdaDoc = XMCDADoc.Factory.parse(correctVersion.openBufferedStream());
		final List<XCriteria> xCriteriaList = xmcdaDoc.getXMCDA().getCriteriaList();
		final XCriteria xCriteria = Iterables.getOnlyElement(xCriteriaList);

		final XMCDACriteria reader = new XMCDACriteria();
		final Set<Criterion> criteria = reader.read(xCriteria);

		assertEquals(SixRealCars.getInstance().getCriteria(), criteria);
	}

	@Test(expected = XmlException.class)
	public void testToIncorrectVersion() throws Exception {
		final ByteSource supplier = Resources.asByteSource(getClass().getResource("SixRealCars v2.0.0 - Criteria.xml"));
		final ByteSource otherVersion = XMCDAReadUtils.getAsVersion(supplier, "2.0.1");

		XMCDADoc.Factory.parse(otherVersion.openBufferedStream());
	}
}
