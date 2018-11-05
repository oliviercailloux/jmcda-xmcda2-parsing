package org.decisiondeck.jmcda.persist.xmcda2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlCursor.TokenType;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.SortingMode;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives.AlternativesParsingMethod;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.X2SimpleReader;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDAGroupSortingProblemReader;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDASortingProblemReader;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.decisiondeck.jmcda.structure.sorting.problem.group_preferences.IGroupSortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.group_results.IGroupSortingResults;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResults;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResultsToMultiple;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class XMCDASortingProblemReaderTest {
    private static final Logger s_logger = LoggerFactory.getLogger(XMCDASortingProblemReaderTest.class);

    String getAsXmlStringWithOuterText(XmlTokenSource xmlSource) {
	final String xmlText = xmlSource.xmlText(new XmlOptions().setSaveOuter());
	s_logger.info("Outer: {}.", xmlText);
	return xmlText;
    }

    String getAsFullString(XmlTokenSource x) throws IOException {
	final ByteArrayOutputStream stream = new ByteArrayOutputStream();
	x.save(stream, new XmlOptions().setSaveOuter());
	final String str = stream.toString(Charsets.UTF_8.name());
	// final String str = x.xmlText(new XmlOptions().setSaveOuter());
	s_logger.info("As full string: {}.", str);
	return str;
    }

    @Test(expected = InvalidInputException.class)
    public void testReadDuplicateId() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("Criteria - Duplicate.xml"));
	final XMCDAGroupSortingProblemReader reader = new XMCDAGroupSortingProblemReader();
	reader.setSourceMain(readerSupplier);

	try {
	    reader.readCriteria();
	} catch (InvalidInputException exc) {
	    s_logger.info("Expected duplicate criterion id exception message: '{}'.", exc.getMessage());
	    throw exc;
	}
    }

    void readContent(XCriteria xC) {
	final XmlCursor cursor = xC.newCursor();
	s_logger.info("Name: {}.", cursor.getName());
	while (cursor.hasNextToken()) {
	    final TokenType type = cursor.toNextToken();
	    s_logger.info("Type: {}, name: {}.", type, cursor.getName());
	}
    }

    @Test(expected = InvalidInputException.class)
    public void testUnknownDm() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("Coalitions - Unknown DM.xml"));
	final XMCDAGroupSortingProblemReader reader = new XMCDAGroupSortingProblemReader();
	reader.setSourceMain(readerSupplier);
	try {
	    reader.readGroupResults();
	} catch (InvalidInputException exc) {
	    s_logger.info("Expected unknown dm exception message: '{}'.", exc.getMessage());
	    throw exc;
	}
    }

    @Test(expected = InvalidInputException.class)
    public void testCarsAssignments75BothToSingle() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("SixRealCars - Assignments both, threshold 75.xml"));
	final XMCDASortingProblemReader reader = new XMCDASortingProblemReader();
	reader.setSourceMain(readerSupplier);
	reader.setAlternativesParsingMethod(AlternativesParsingMethod.SEEK_CONCEPT);
	try {
	    reader.readSortingResults();
	} catch (InvalidInputException exc) {
	    s_logger.info("Expected invalid single assignments exception message: '{}'.", exc.getMessage());
	    throw exc;
	}
    }

    @Test
    public void testEvaluations() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("SixRealCars - Mixed performances.xml"));
	final X2SimpleReader reader = new X2SimpleReader(new XMCDAReadUtils().getXMCDA(readerSupplier));
	final ISortingPreferences results = reader.readSortingPreferences();

	final SixRealCars data = SixRealCars.getInstance();

	final EvaluationsRead alternativesEvaluations = results.getAlternativesEvaluations();
	assertEquals(data.getAlternativesEvaluations(), alternativesEvaluations);

	final EvaluationsRead profilesEvaluations = results.getProfilesEvaluations();
	assertEquals(data.getProfilesEvaluations(), profilesEvaluations);
	assertFalse(data.getProfilesEvaluations().equals(alternativesEvaluations));
    }

    @Test
    public void testCarsGroups() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("SixRealCars with criteriaSet.xml"));
	final XMCDAGroupSortingProblemReader sreader = new XMCDAGroupSortingProblemReader();
	sreader.setSourceMain(readerSupplier);
	sreader.setAlternativesParsingMethod(AlternativesParsingMethod.SEEK_CONCEPT);
	// final X2SimpleReader reader = new X2SimpleReader(new XMCDAReadUtils().getXMCDA(readerSupplier));
	// reader.readSortingPreferences()
	final IGroupSortingPreferences results = sreader.readGroupResults();

	final SixRealCars data = SixRealCars.getInstance();

	assertEquals(data.getAlternatives(), results.getAlternatives());
	assertEquals(data.getProfiles(), results.getProfiles());
	assertEquals(data.getCriteria(), results.getCriteria());
	assertEquals(data.getScales(), results.getScales());
	assertEquals(data.getCatsAndProfs(), results.getCatsAndProfs());
	/** Mhh this is broken. */
	// final IRdEvaluations alternativesEvaluations = results.getAlternativesEvaluations();
	// assertEquals(data.getAlternativesEvaluations(), alternativesEvaluations);

	assertTrue(data.getWeights().approxEquals(results.getSharedCoalitions().getWeights(), 1e-6));
	assertEquals(data.getThresholds(), results.getSharedThresholds());
	final EvaluationsRead profilesEvaluations = results.getSharedProfilesEvaluations();
	assertTrue(profilesEvaluations.isEmpty());
    }

    @Test
    public void testCars() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("SixRealCars with criteriaSet.xml"));
	final X2SimpleReader reader = new X2SimpleReader(new XMCDAReadUtils().getXMCDA(readerSupplier));

	final Set<Criterion> criteria = reader.readCriteria();

	final SixRealCars data = SixRealCars.getInstance();
	final Set<Criterion> expectedCriteria = data.getCriteria();
	assertEquals(expectedCriteria, criteria);

	final Map<Criterion, Interval> expectedScales = data.getScales();
	assertEquals(expectedScales, reader.getReadScales());

	final ISortingPreferences results = reader.readSortingPreferences();
	assertEquals(data.getAlternatives(), results.getAlternatives());
	assertEquals(data.getProfiles(), results.getProfiles());
	assertEquals(data.getCriteria(), results.getCriteria());
	assertEquals(data.getScales(), results.getScales());
	assertEquals(data.getCatsAndProfs(), results.getCatsAndProfs());
	final EvaluationsRead alternativesEvaluations = results.getAlternativesEvaluations();
	assertEquals(data.getAlternativesEvaluations(), alternativesEvaluations);

	assertTrue(data.getWeights().approxEquals(results.getCoalitions().getWeights(), 1e-6));
	assertEquals(data.getThresholds(), results.getThresholds());
	final EvaluationsRead profilesEvaluations = results.getProfilesEvaluations();
	assertTrue(profilesEvaluations.isEmpty());
    }

    @Test
    public void testCarsAssignments75Optimistic() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("SixRealCars - Assignments optimistic, threshold 75.xml"));
	final XMCDASortingProblemReader reader = new XMCDASortingProblemReader();
	reader.setSourceMain(readerSupplier);
	reader.setAlternativesParsingMethod(AlternativesParsingMethod.SEEK_CONCEPT);
	final ISortingResults results = reader.readSortingResults();

	final SixRealCars data = SixRealCars.getInstance();

	assertEquals(data.getAssignments75(SortingMode.OPTIMISTIC), results.getAssignments());
    }

    @Test
    public void testCarsGroupAssignments75() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("SixRealCars - Group assignments, threshold 75.xml"));
	final XMCDAGroupSortingProblemReader reader = new XMCDAGroupSortingProblemReader();
	reader.setSourceMain(readerSupplier);
	reader.setAlternativesParsingMethod(AlternativesParsingMethod.SEEK_CONCEPT);
	final IGroupSortingResults results = reader.readGroupResults();

	final SixRealCars data = SixRealCars.getInstance();

	final DecisionMaker optimistic = new DecisionMaker("optimistic");
	final DecisionMaker pessimistic = new DecisionMaker("pessimistic");
	final Set<DecisionMaker> expectedDms = Sets.newLinkedHashSet();
	expectedDms.add(optimistic);
	expectedDms.add(pessimistic);
	assertTrue(Iterables.elementsEqual(expectedDms, results.getDms()));

	assertEquals(data.getAssignments75(SortingMode.OPTIMISTIC), results.getAssignments(optimistic));
	assertEquals(data.getAssignments75(SortingMode.PESSIMISTIC), results.getAssignments(pessimistic));
    }

    @Test
    public void testCarsAssignments75Both() throws Exception {
	final ByteSource readerSupplier = Resources.asByteSource(getClass()
		.getResource("SixRealCars - Assignments both, threshold 75.xml"));
	final XMCDASortingProblemReader reader = new XMCDASortingProblemReader();
	reader.setSourceMain(readerSupplier);
	reader.setAlternativesParsingMethod(AlternativesParsingMethod.SEEK_CONCEPT);
	final ISortingResultsToMultiple results = reader.readSortingResultsToMultiple();

	final SixRealCars data = SixRealCars.getInstance();

	assertEquals(data.getAssignments75(SortingMode.BOTH), results.getAssignments());
    }
}
