package org.decisiondeck.jmcda.persist.xmcda2;

import static org.junit.Assert.assertEquals;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDAProblemReader;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDASortingProblemReader;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternative;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class XMCDAReaderTest {
    private static final Logger s_logger = LoggerFactory.getLogger(XMCDAReaderTest.class);

    @Test(expected = InvalidInputException.class)
    public void testReadDuplicateId() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("Criteria - Duplicate.xml"));
	final XMCDAProblemReader reader = new XMCDAProblemReader();
	reader.setSourceMain(readerSupplier);

	try {
	    reader.readCriteria();
	} catch (InvalidInputException exc) {
	    s_logger.info("Expected exception message: '{}'.", exc.getMessage());
	    throw exc;
	}
    }

    @Test
    public void testGetTagName() throws Exception {
	assertEquals("alternatives", XMCDAReadUtils.getTagName(XAlternatives.class));
	assertEquals("alternative", XMCDAReadUtils.getTagName(XAlternative.class));
    }

    @Test
    public void testDeserResidences() throws Exception {
	final XMCDASortingProblemReader reader = new XMCDASortingProblemReader(
		Resources.asByteSource(getClass().getResource("Housing data.xml")));
	final ISortingData data = reader.readSortingData();
	assertEquals("Incorrect number of alternatives.", 12, data.getAllAlternatives().size());
	assertEquals("Incorrect number of criteria.", 8, data.getCriteria().size());
	assertEquals("Incorrect number of categories.", 3, data.getCatsAndProfs().getCategories().size());
	final Alternative profileDown = data.getCatsAndProfs().getProfileDown("Medium");
	assertEquals("Incorrect profile.", "0", profileDown.getId());
	// assertTrue("Incorrect profile.",
	// simpleDeser.getSituation().getAlternativeNames().get(profileDown).equals("Bad to medium profile"));
	assertEquals("Incorrect number of evaluations.", 80, data.getAlternativesEvaluations().getValueCount());
	assertEquals("Incorrect evaluation.", 552.77d,
		data.getAlternativesEvaluations().getEntry(new Alternative("15"), new Criterion("2")).doubleValue(),
		1e-4d);
    }
}
