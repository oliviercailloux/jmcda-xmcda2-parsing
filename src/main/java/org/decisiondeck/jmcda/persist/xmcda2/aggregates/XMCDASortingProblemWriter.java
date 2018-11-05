package org.decisiondeck.jmcda.persist.xmcda2.aggregates;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.xmlbeans.XmlOptions;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.thresholds.ThresholdsUtils;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decisiondeck.jmcda.persist.xmcda2.X2Concept;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAssignments;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACategories;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDADecisionMakers;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeType;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesAffectations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategories;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesProfiles;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaSet;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodParameters;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAWriteUtils;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilitiesRead;
import org.decisiondeck.jmcda.structure.sorting.problem.assignments.ISortingAssignments;
import org.decisiondeck.jmcda.structure.sorting.problem.assignments.ISortingAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.problem.assignments.ISortingAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;
import org.decisiondeck.jmcda.structure.sorting.problem.group_assignments.IGroupSortingAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.problem.group_assignments.IGroupSortingAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.problem.group_data.IGroupSortingData;
import org.decisiondeck.jmcda.structure.sorting.problem.group_preferences.IGroupSortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.group_results.IGroupSortingResults;
import org.decisiondeck.jmcda.structure.sorting.problem.group_results.IGroupSortingResultsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.problem.group_results.IGroupSortingResultsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResults;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResultsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResultsWithCredibilities;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSink;

/**
 * <p>
 * A class to write MCDA sorting problems to XMCDA documents, including the typical preferences informations related to
 * a sorting problem, namely thresholds and coalitions; and sorting results, namely assignments. Support is given for
 * assignments to single categories, assignments to multiple categories and assignments with credibilites. This class
 * supports the case of a single decision maker.
 * </p>
 * <p>
 * Although this class is meant to write characters rather than bytes, it uses output streams rather than writers. This
 * is because the writer hides the encoding used, which disables the possibility to correctly write the encoding used in
 * the XML header. Objects of this class use the UTF-8 encoding by default. See {@link #getSaveOptions()}.
 * </p>
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDASortingProblemWriter {

    private final XMCDAWriteUtils m_writeUtils = new XMCDAWriteUtils();

    private ByteSink m_destinationCategories;
    private ByteSink m_destinationCategoriesProfiles;
    private ByteSink m_destinationProfiles;
    private ByteSink m_destinationProfilesEvaluations;
    private ByteSink m_destinationAssignments;
    private ByteSink m_destinationAlternatives;
    private ByteSink m_destinationAlternativesEvaluations;
    private ByteSink m_destinationCoalitions;
    private ByteSink m_destinationCriteria;
    private ByteSink m_destinationMain;
    private boolean m_writeIfEmpty;

    /**
     * Creates a new writer.
     */
    public XMCDASortingProblemWriter() {
	init();
    }

    /**
     * Creates a new writer with a main destination.
     * 
     * @param mainDestination
     *            not <code>null</code>.
     */
    public XMCDASortingProblemWriter(ByteSink mainDestination) {
	checkNotNull(mainDestination);
	init();
	m_destinationMain = mainDestination;
    }

    private void init() {
	m_destinationAlternatives = null;
	m_destinationAlternativesEvaluations = null;
	m_destinationAssignments = null;
	m_destinationCategories = null;
	m_destinationCategoriesProfiles = null;
	m_destinationCoalitions = null;
	m_destinationCriteria = null;
	m_destinationMain = null;
	m_destinationProfiles = null;
	m_destinationProfilesEvaluations = null;

	m_writeIfEmpty = false;

	m_writeUtils.getSaveOptions().setSavePrettyPrint();
	m_writeUtils.getSaveOptions().setCharacterEncoding(Charsets.UTF_8.name());
    }

    /**
     * Retrieves a writable view of the options used to save XML streams. Default options are to use pretty print and to
     * use the UTF-8 encoding.
     * 
     * @return not <code>null</code>.
     */
    public XmlOptions getSaveOptions() {
	return m_writeUtils.getSaveOptions();
    }

    public void writeProblemData(IProblemData data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendProblemData(data, null, xmcda);

	write(doc, m_destinationMain);
    }

    public void append(CatsAndProfs catsAndProfs, XMCDA xmcda) {
	if (m_writeIfEmpty || !catsAndProfs.isEmpty()) {
	    final XMCDACategories xmcdaCategories = new XMCDACategories();
	    final XCategories xCategories = xmcdaCategories.write(catsAndProfs.getCategories());
	    m_writeUtils.appendTo(xCategories, xmcda);
	    final XCategoriesProfiles xCatsAndProfs = xmcdaCategories.write(catsAndProfs);
	    m_writeUtils.appendTo(xCatsAndProfs, xmcda);
	}
    }

    public void append(EvaluationsRead evaluations, X2Concept concept, Set<Alternative> alternativesOrder,
	    Set<Criterion> criteriaOrder, XMCDA xmcda) {
	checkArgument(alternativesOrder == null || alternativesOrder.containsAll(evaluations.getRows()));
	checkArgument(criteriaOrder == null || criteriaOrder.containsAll(evaluations.getColumns()));

	if (!evaluations.isEmpty()) {
	    final XMCDAEvaluations xmcdaEvaluations = new XMCDAEvaluations();
	    xmcdaEvaluations.setAlternativesOrder(alternativesOrder);
	    xmcdaEvaluations.setCriteriaOrder(criteriaOrder);
	    xmcdaEvaluations.setConceptToWrite(concept);
	    final XPerformanceTable xEvaluations = xmcdaEvaluations.write(evaluations);
	    m_writeUtils.appendTo(xEvaluations, xmcda);
	}
    }

    public void append(Set<Criterion> criteria, Map<Criterion, Interval> scales,
	    Thresholds thresholds, XMCDA xmcda) {
	if (m_writeIfEmpty || criteria.size() >= 1) {
	    final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
	    if (scales != null) {
		final Set<Criterion> keySet = scales.keySet();
		for (Criterion criterion : keySet) {
		    checkArgument(scales.get(criterion).getStepSize() == null, "Incorrect scale for " + criterion
			    + ", is discrete.");
		}
		xmcdaCriteria.setScales(scales);
	    }
	    if (thresholds != null) {
		xmcdaCriteria.setPreferenceThresholds(thresholds.getPreferenceThresholds());
		xmcdaCriteria.setIndifferenceThresholds(thresholds.getIndifferenceThresholds());
		xmcdaCriteria.setVetoThresholds(thresholds.getVetoThresholds());
	    }
	    final XCriteria xCriteria = xmcdaCriteria.write(criteria);
	    m_writeUtils.appendTo(xCriteria, xmcda);
	}
    }

    public void append(Set<Alternative> alternatives, XAlternativeType.Enum type, XMCDA xmcda) {
	if (m_writeIfEmpty || alternatives.size() >= 1) {
	    final XMCDAAlternatives xmcdaAlternatives = new XMCDAAlternatives();
	    final XAlternatives xAlternatives = xmcdaAlternatives.writeAlternatives(alternatives, type);
	    m_writeUtils.appendTo(xAlternatives, xmcda);
	}
    }

    /**
     * Sets the dedicated destination used to write the categories and associated profiles.
     * 
     * @param destinationCategoriesProfiles
     *            <code>null</code> for not set.
     */
    public void setDestinationCategoriesProfiles(ByteSink destinationCategoriesProfiles) {
	m_destinationCategoriesProfiles = destinationCategoriesProfiles;
    }

    /**
     * Retrieves the destination dedicated to categories.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationCategories() {
	return m_destinationCategories;
    }

    /**
     * Retrieves the destination dedicated to definition of the categories through bounding profiles.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationCategoriesProfiles() {
	return m_destinationCategoriesProfiles;
    }

    /**
     * Retrieves the destination dedicated to profiles.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationProfiles() {
	return m_destinationProfiles;
    }

    /**
     * Retrieves the destination dedicated to profiles evaluations.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationProfilesEvaluations() {
	return m_destinationProfilesEvaluations;
    }

    /**
     * Sets the dedicated destination used to write the categories.
     * 
     * @param destination
     *            <code>null</code> for not set.
     */
    public void setDestinationCategories(ByteSink destination) {
	m_destinationCategories = destination;
    }

    /**
     * Sets the dedicated destination used to write the profiles.
     * 
     * @param destinationProfiles
     *            <code>null</code> for not set.
     */
    public void setDestinationProfiles(ByteSink destinationProfiles) {
	m_destinationProfiles = destinationProfiles;
    }

    /**
     * Sets the dedicated destination used to write the profiles evaluations.
     * 
     * @param destination
     *            <code>null</code> for not set.
     */
    public void setDestinationProfilesEvaluations(ByteSink destination) {
	m_destinationProfilesEvaluations = destination;
    }

    /**
     * Retrieves the destination dedicated to alternatives.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationAlternatives() {
	return m_destinationAlternatives;
    }

    /**
     * Sets the dedicated destination used to write alternatives.
     * 
     * @param destinationAlternatives
     *            <code>null</code> for not set.
     */
    public void setDestinationAlternatives(ByteSink destinationAlternatives) {
	m_destinationAlternatives = destinationAlternatives;
    }

    /**
     * Retrieves the destination dedicated to alternatives evaluations.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationAlternativesEvaluations() {
	return m_destinationAlternativesEvaluations;
    }

    /**
     * Sets the dedicated destination used to write the evaluations of the alternatives.
     * 
     * @param destinationAlternativesEvaluations
     *            <code>null</code> for not set.
     */
    public void setDestinationAlternativesEvaluations(
	    ByteSink destinationAlternativesEvaluations) {
	m_destinationAlternativesEvaluations = destinationAlternativesEvaluations;
    }

    /**
     * Retrieves the destination dedicated to coalitions.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationCoalitions() {
	return m_destinationCoalitions;
    }

    /**
     * Sets the dedicated destination used to write the coalitions.
     * 
     * @param destinationCoalitions
     *            <code>null</code> for not set.
     */
    public void setDestinationCoalitions(ByteSink destinationCoalitions) {
	m_destinationCoalitions = destinationCoalitions;
    }

    /**
     * Retrieves the destination dedicated to criteria.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationCriteria() {
	return m_destinationCriteria;
    }

    /**
     * Sets the dedicated destination used to write the criteria.
     * 
     * @param destinationCriteria
     *            <code>null</code> for not set.
     */
    public void setDestinationCriteria(ByteSink destinationCriteria) {
	m_destinationCriteria = destinationCriteria;
    }

    /**
     * Retrieves the main destination. This is used to write any type of object when the dedicated destination is not
     * set.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationMain() {
	return m_destinationMain;
    }

    /**
     * Sets the main destination used to write all types of objects for which no dedicated destination is set.
     * 
     * @param destinationMain
     *            <code>null</code> for not set.
     */
    public void setDestinationMain(ByteSink destinationMain) {
	m_destinationMain = destinationMain;
    }

    /**
     * Writes the given XMCDA document to the given destination <em>or</em> to the main destination if the given
     * destination is <code>null</code>. The document must be valid, except if this object is specifically set to not
     * validate documents.
     * 
     * @param doc
     *            not <code>null</code>, must conform to the XMCDA schema.
     * @param destination
     *            may be <code>null</code>, in which case the main destination in this object must be non
     *            <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given writer, or while writing to the
     *             destination.
     * @see #setValidate(boolean)
     */
    public void write(XMCDADoc doc, ByteSink destination) throws IOException {
	checkArgument(destination != null || m_destinationMain != null);
	checkNotNull(doc);
	if (m_writeUtils.doesValidate()) {
	    assert (doc.validate());
	}

	String versionToWrite = XMCDAReadUtils.DEFAULT_XMCDA_VERSION;
	final ByteSink realDestination = destination == null ? m_destinationMain
		: destination;
	m_writeUtils.write(doc, realDestination, versionToWrite);
    }

    /**
     * Retrieves the information whether this object only accepts to write valid documents. The default is
     * <code>true</code>.
     * 
     * @return <code>true</code> if this object validates documents before writing them.
     */
    public boolean doesValidate() {
	return m_writeUtils.doesValidate();
    }

    /**
     * Enables or disables the check for validation before writing any document. The default is <code>true</code>, thus
     * this object validates each document before writing them. It is not recommanded to disable validation but it can
     * be useful for debug.
     * 
     * @param validate
     *            <code>false</code> to allow writing invalid documents.
     */
    public void setValidate(boolean validate) {
	m_writeUtils.setValidate(validate);
    }

    /**
     * Retrieves the destination dedicated to assignments.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSink getDestinationAssignments() {
	return m_destinationAssignments;
    }

    /**
     * Sets the dedicated destination used to write assignments.
     * 
     * @param destination
     *            <code>null</code> for not set.
     */
    public void setDestinationAssignments(ByteSink destination) {
	m_destinationAssignments = destination;
    }

    public void writePreferences(ISortingPreferences data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendPreferences(data, xmcda);

	write(doc, m_destinationMain);
    }

    public void appendPreferences(ISortingPreferences data, XMCDA xmcda) {
	appendData(data, data.getThresholds(), xmcda);

	final Set<Criterion> criteriaOrder = data.getCriteria();
	append(data.getCoalitions(), criteriaOrder, xmcda);

	final Set<Alternative> profilesOrder = data.getProfiles();
	append(data.getProfilesEvaluations(), X2Concept.FICTIVE, profilesOrder, criteriaOrder, xmcda);
    }

    public void writeAssignments(ISortingAssignments data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendData(data, null, xmcda);

	final Set<Alternative> alternativesOrder = data.getAlternatives();
	append(data.getAssignments(), alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void appendWithCredibilities(IOrderedAssignmentsWithCredibilitiesRead assignments,
	    Set<Alternative> alternativesOrder, XMCDA xmcda) {
	if (m_writeIfEmpty || assignments.getAlternatives().size() >= 1) {
	    final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
	    xmcdaAssignments.setAlternativesOrder(alternativesOrder);
	    final XAlternativesAffectations xAssignments = xmcdaAssignments.write(assignments);
	    m_writeUtils.appendTo(xAssignments, xmcda);
	}
    }

    public void append(Coalitions coalitions, Set<Criterion> criteriaOrder, XMCDA xmcda) {
	if (m_writeIfEmpty || !coalitions.isEmpty()) {
	    final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
	    xmcdaCriteria.setCriteriaOrder(criteriaOrder);
	    final XCriteriaSet xCriteriaSet = xmcdaCriteria.writeCoalitions(coalitions);
	    m_writeUtils.appendTo(xCriteriaSet, xmcda);
	}
    }

    public void append(IOrderedAssignmentsToMultipleRead assignments, Set<Alternative> alternativesOrder, XMCDA xmcda) {
	if (m_writeIfEmpty || assignments.getAlternatives().size() >= 1) {
	    final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
	    xmcdaAssignments.setAlternativesOrder(alternativesOrder);
	    final XAlternativesAffectations xAssignments = xmcdaAssignments.write(assignments);
	    m_writeUtils.appendTo(xAssignments, xmcda);
	}
    }

    public void writeAssignments(ISortingAssignmentsToMultiple data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendData(data, null, xmcda);

	final Set<Alternative> alternativesOrder = data.getAlternatives();
	append(data.getAssignments(), alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void writeResults(ISortingResults data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendPreferences(data, xmcda);

	final Set<Alternative> alternativesOrder = data.getAlternatives();
	append(data.getAssignments(), alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void writeResults(ISortingResultsToMultiple data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendPreferences(data, xmcda);

	final Set<Alternative> alternativesOrder = data.getAlternatives();
	append(data.getAssignments(), alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void writeResultsWithCredibilities(ISortingResultsWithCredibilities data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendPreferences(data, xmcda);

	final Set<Alternative> alternativesOrder = data.getAlternatives();
	appendWithCredibilities(data.getAssignments(), alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void writeAssignmentsWithCredibilities(ISortingAssignmentsWithCredibilities data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendData(data, null, xmcda);

	final Set<Alternative> alternativesOrder = data.getAlternatives();
	appendWithCredibilities(data.getAssignments(), alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void writeGroupPreferences(IGroupSortingPreferences data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendGroupPreferences(data, xmcda);

	write(doc, m_destinationMain);
    }

    public void appendGroupPreferences(IGroupSortingPreferences data, XMCDA xmcda) {
	checkNotNull(data);
	final Thresholds thresholds;
	if (!data.getSharedThresholds().isEmpty()) {
	    thresholds = data.getSharedThresholds();
	} else {
	    final Map<DecisionMaker, Thresholds> allThresholds = data.getThresholds();
	    final boolean empty = Iterables.all(allThresholds.values(), ThresholdsUtils.getPredicateIsEmpty());
	    if (!empty) {
		throw new UnsupportedOperationException("Writing individual thresholds is unsupported.");
	    }
	    thresholds = null;
	}
	appendGroupData(data, thresholds, xmcda);

	final Set<DecisionMaker> dmsOrder = data.getDms();
	final Set<Criterion> criteriaOrder = data.getCriteria();

	if (!data.getSharedCoalitions().isEmpty()) {
	    append(data.getSharedCoalitions(), criteriaOrder, xmcda);
	} else {
	    append(data.getCoalitions(), dmsOrder, criteriaOrder, xmcda);
	}

	final Set<Alternative> profilesOrder = data.getProfiles();
	if (!data.getSharedProfilesEvaluations().isEmpty()) {
	    append(data.getSharedProfilesEvaluations(), X2Concept.FICTIVE, profilesOrder, criteriaOrder, xmcda);
	} else {
	    final Map<DecisionMaker, EvaluationsRead> profilesEvaluations = data.getProfilesEvaluations();
	    final Map<DecisionMaker, EvaluationsRead> noEmpty = Maps.filterEntries(profilesEvaluations,
		    new Predicate<Entry<DecisionMaker, EvaluationsRead>>() {
			@Override
			public boolean apply(Entry<DecisionMaker, EvaluationsRead> input) {
			    return (input.getValue().getValueCount() >= 1);
			}
		    });
	    append(noEmpty, dmsOrder, profilesOrder, criteriaOrder, xmcda);
	}
    }

    public void appendGroupData(IGroupSortingData data, Thresholds thresholds, XMCDA xmcda) {
	appendData(data, thresholds, xmcda);
	append(data.getDms(), xmcda);
    }

    public void appendData(ISortingData data, Thresholds thresholds, XMCDA xmcda) {
	appendProblemData(data, thresholds, xmcda);

	append(data.getProfiles(), XAlternativeType.FICTIVE, xmcda);

	append(data.getCatsAndProfs(), xmcda);
    }

    public void appendProblemData(IProblemData data, Thresholds thresholds, XMCDA xmcda) {
	append(data.getAlternatives(), XAlternativeType.REAL, xmcda);

	append(data.getCriteria(), data.getScales(), thresholds, xmcda);

	final Set<Alternative> alternativesOrder = data.getAlternatives();
	final Set<Criterion> criteriaOrder = data.getCriteria();
	append(data.getAlternativesEvaluations(), X2Concept.REAL, alternativesOrder, criteriaOrder, xmcda);
    }

    public void append(Map<DecisionMaker, EvaluationsRead> profilesEvaluations, Set<DecisionMaker> dmsOrder,
	    Set<Alternative> profilesOrder, Set<Criterion> criteriaOrder, XMCDA xmcda) {
	/** NB no empty evaluations in values! */
	if (m_writeIfEmpty || profilesEvaluations.size() >= 1) {
	    final XMCDAEvaluations xmcdaEvaluations = new XMCDAEvaluations();
	    xmcdaEvaluations.setConceptToWrite(X2Concept.FICTIVE);
	    xmcdaEvaluations.setDmsOrder(dmsOrder);
	    xmcdaEvaluations.setAlternativesOrder(profilesOrder);
	    xmcdaEvaluations.setCriteriaOrder(criteriaOrder);

	    final Collection<XPerformanceTable> xEvaluations = xmcdaEvaluations.write(profilesEvaluations);
	    m_writeUtils.appendTo(xEvaluations, xmcda);
	}
    }

    public void append(Map<DecisionMaker, Coalitions> coalitions, Set<DecisionMaker> dmsOrder,
	    Set<Criterion> criteriaOrder, XMCDA xmcda) {
	if (m_writeIfEmpty || coalitions.size() >= 1) {
	    final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
	    xmcdaCriteria.setDmsOrder(dmsOrder);
	    xmcdaCriteria.setCriteriaOrder(criteriaOrder);
	    final List<XCriteriaSet> xCoalitions = xmcdaCriteria.write(coalitions);
	    m_writeUtils.appendTo(xCoalitions, xmcda);
	}
    }

    public void append(Set<DecisionMaker> dms, XMCDA xmcda) {
	if (m_writeIfEmpty || dms.size() >= 1) {
	    final XMCDADecisionMakers xmcdaDecisionMakers = new XMCDADecisionMakers();
	    final XMethodParameters xDms = xmcdaDecisionMakers.write(dms);
	    m_writeUtils.appendTo(xDms, xmcda);
	}
    }

    public void appendAssignments(Map<DecisionMaker, ? extends IOrderedAssignmentsToMultipleRead> assignments,
	    Set<DecisionMaker> dmsOrder, Set<Alternative> alternativesOrder, XMCDA xmcda) {
	if (m_writeIfEmpty || assignments.size() >= 1) {
	    final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
	    xmcdaAssignments.setDmsOrder(dmsOrder);
	    xmcdaAssignments.setAlternativesOrder(alternativesOrder);
	    final Collection<XAlternativesAffectations> xAssignments = xmcdaAssignments.writeAll(assignments);
	    m_writeUtils.appendTo(xAssignments, xmcda);
	}
    }

    public void writeGroupAssignments(IGroupSortingAssignmentsToMultipleRead data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendGroupData(data, null, xmcda);

	final Set<DecisionMaker> dmsOrder = data.getDms();
	final Set<Alternative> alternativesOrder = data.getAlternatives();
	appendAssignments(data.getAssignments(), dmsOrder, alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void writeGroupAssignmentsWithCredibilities(IGroupSortingAssignmentsWithCredibilities data)
	    throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendGroupData(data, null, xmcda);

	final Set<DecisionMaker> dmsOrder = data.getDms();
	final Set<Alternative> alternativesOrder = data.getAlternatives();
	appendAssignmentsWithCredibilities(data.getAssignments(), dmsOrder, alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void appendAssignmentsWithCredibilities(
	    Map<DecisionMaker, ? extends IOrderedAssignmentsWithCredibilitiesRead> assignments,
	    Set<DecisionMaker> dmsOrder, Set<Alternative> alternativesOrder, XMCDA xmcda) {
	if (m_writeIfEmpty || assignments.size() >= 1) {
	    final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
	    xmcdaAssignments.setDmsOrder(dmsOrder);
	    xmcdaAssignments.setAlternativesOrder(alternativesOrder);
	    final Collection<XAlternativesAffectations> xAssignments = xmcdaAssignments
		    .writeAllWithCredibilities(assignments);
	    m_writeUtils.appendTo(xAssignments, xmcda);
	}
    }

    public void writeGroupResults(IGroupSortingResults data) throws IOException {
	checkNotNull(data);
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendGroupPreferences(data, xmcda);

	final Set<DecisionMaker> dmsOrder = data.getDms();
	final Set<Alternative> alternativesOrder = data.getAlternatives();
	appendAssignments(data.getAssignments(), dmsOrder, alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void writeGroupResults(IGroupSortingResultsToMultiple data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendGroupPreferences(data, xmcda);

	final Set<DecisionMaker> dmsOrder = data.getDms();
	final Set<Alternative> alternativesOrder = data.getAlternatives();
	appendAssignments(data.getAssignments(), dmsOrder, alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void writeGroupResultsWithCredibilities(IGroupSortingResultsWithCredibilities data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendGroupPreferences(data, xmcda);

	final Set<DecisionMaker> dmsOrder = data.getDms();
	final Set<Alternative> alternativesOrder = data.getAlternatives();
	appendAssignmentsWithCredibilities(data.getAssignments(), dmsOrder, alternativesOrder, xmcda);

	write(doc, m_destinationMain);
    }

    public void writeData(ISortingData data) throws IOException {
	final XMCDADoc doc = XMCDADoc.Factory.newInstance();
	final XMCDA xmcda = doc.addNewXMCDA();

	appendData(data, null, xmcda);

	write(doc, m_destinationMain);
    }

}
