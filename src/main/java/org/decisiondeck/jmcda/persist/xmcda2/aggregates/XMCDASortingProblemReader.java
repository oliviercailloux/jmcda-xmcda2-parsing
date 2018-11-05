package org.decisiondeck.jmcda.persist.xmcda2.aggregates;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import org.apache.xmlbeans.XmlException;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.sorting.category.Categories;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.utils.collection.extensional_order.ExtentionalTotalOrder;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives.AlternativesParsingMethod;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAssignments;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACategories;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeType;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesAffectations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategories;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesProfiles;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;
import org.decisiondeck.jmcda.structure.sorting.assignment.AssignmentsToMultipleFiltering;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.AssignmentsWithCredibilitiesFiltering;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IAssignmentsWithCredibilitiesRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsUtils;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResults;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResultsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResultsWithCredibilities;
import org.decisiondeck.xmcda_oo.structure.sorting.SortingProblemUtils;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

/**
 * <p>
 * A class to read MCDA sorting problems defined in XMCDA documents, including the typical preferences informations
 * related to a sorting problem, namely thresholds and coalitions; and sorting results, namely assignments. Support is
 * given for assignments to single categories, assignments to multiple categories and assignments with credibilites.
 * This class supports the case of a single decision maker.
 * </p>
 * 
 * @see org.decisiondeck.jmcda.persist.xmcda2
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDASortingProblemReader extends XMCDAHelperWithVarious {

    private final XMCDAProblemReader m_problemReader;
    private ByteSource m_sourceCategories;
    private ByteSource m_sourceCategoriesProfiles;
    private ByteSource m_sourceProfiles;
    private ByteSource m_sourceProfilesEvaluations;
    private NavigableSet<Category> m_categories;
    private CatsAndProfs m_catsAndProfs;
    private Set<Alternative> m_profiles;
    private EvaluationsRead m_profilesEvaluationsView;
    private ByteSource m_sourceAssignments;
    private IAssignmentsToMultiple m_assignments;
    private IAssignmentsWithCredibilities m_assignmentsWithCredibilities;

    /**
     * Creates a new reader which will use the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDASortingProblemReader() {
	this(new XMCDAErrorsManager());
    }

    /**
     * Creates a new reader with a main source. The reader will use the default error management strategy
     * {@link ErrorManagement#THROW}.
     * 
     * @param mainSource
     *            not <code>null</code>.
     */
    public XMCDASortingProblemReader(ByteSource mainSource) {
	this(mainSource, new XMCDAErrorsManager());
    }

    /**
     * Creates a new reader with a main source, and delegating error management to the given error manager in case of
     * unexpected data read.
     * 
     * @param mainSource
     *            not <code>null</code>.
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDASortingProblemReader(ByteSource mainSource, XMCDAErrorsManager errorsManager) {
	super(errorsManager);
	checkNotNull(mainSource);
	m_problemReader = new XMCDAProblemReader(mainSource, errorsManager);
	init();
    }

    private void init() {
	m_sourceCategories = null;
	m_sourceCategoriesProfiles = null;
	m_sourceProfiles = null;
	m_sourceProfilesEvaluations = null;
	m_sourceAssignments = null;

	clearCache();
    }

    /**
     * Sets the dedicated source used to read the categories and associated profiles.
     * 
     * @param sourceCategoriesProfiles
     *            <code>null</code> for not set.
     */
    public void setSourceCategoriesProfiles(ByteSource sourceCategoriesProfiles) {
	m_sourceCategoriesProfiles = sourceCategoriesProfiles;
	clearCache();
    }

    /**
     * Retrieves the source dedicated to categories.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceCategories() {
	return m_sourceCategories;
    }

    /**
     * Retrieves the source dedicated to definition of the categories through bounding profiles.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceCategoriesProfiles() {
	return m_sourceCategoriesProfiles;
    }

    /**
     * Retrieves the source dedicated to profiles.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceProfiles() {
	return m_sourceProfiles;
    }

    /**
     * Retrieves the source dedicated to profiles evaluations.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceProfilesEvaluations() {
	return m_sourceProfilesEvaluations;
    }

    /**
     * <p>
     * Reads the categories from the dedicated source, or from the the main source if the dedicated source is not set,
     * or retrieves the results of the previous read if it ended successfully.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public NavigableSet<Category> readCategories() throws IOException, XmlException, InvalidInputException {
	if (m_categories != null) {
	    return Sets.unmodifiableNavigableSet(m_categories);
	}
	final XMCDA xmcda = getXMCDA(m_sourceCategories);
	if (xmcda == null) {
	    m_categories = ExtentionalTotalOrder.create();
	} else {
	    final List<XCategories> xCategoriesList = xmcda.getCategoriesList();
	    final XCategories xCategories = getUniqueOrZero(xCategoriesList);
	    if (xCategories == null) {
		m_categories = ExtentionalTotalOrder.create();
	    } else {
		final XMCDACategories xmcdaCategories = new XMCDACategories();
		m_categories = xmcdaCategories.read(xCategories);
	    }
	}
	return Sets.unmodifiableNavigableSet(m_categories);
    }

    /**
     * <p>
     * Reads the categories, together with their relations with the profiles defining them, from the dedicated source,
     * or from the the main source if the dedicated source is not set, or retrieves the results of the previous read if
     * it ended successfully.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public CatsAndProfs readCategoriesProfiles() throws IOException, XmlException, InvalidInputException {
	if (m_catsAndProfs != null) {
	    return Categories.getReadView(m_catsAndProfs);
	}
	final XMCDA xmcda = getXMCDA(m_sourceCategoriesProfiles);
	if (xmcda == null) {
	    m_catsAndProfs = Categories.newCatsAndProfs();
	} else {
	    final List<XCategoriesProfiles> xCategoriesProfilesList = xmcda.getCategoriesProfilesList();
	    final XCategoriesProfiles xCategoriesProfiles = getUniqueOrZero(xCategoriesProfilesList);
	    if (xCategoriesProfiles == null) {
		m_catsAndProfs = Categories.newCatsAndProfs();
	    } else {
		final XMCDACategories xmcdaCategories = new XMCDACategories();
		m_catsAndProfs = xmcdaCategories.read(xCategoriesProfiles);
	    }
	}
	return Categories.getReadView(m_catsAndProfs);
    }

    /**
     * <p>
     * Reads the profiles from the dedicated source, or from the the main source if the dedicated source is not set, or
     * retrieves the results of the previous read if it ended successfully. Which profiles are returned depend on the
     * chosen parsing method.
     * </p>
     * <p>
     * The returned set iteration order matches the order of the source.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #setAlternativesParsingMethod(AlternativesParsingMethod)
     */
    public Set<Alternative> readProfiles() throws IOException, XmlException, InvalidInputException {
	if (m_profiles != null) {
	    return m_profiles;
	}
	final XMCDA xmcda = getXMCDA(m_sourceProfiles);
	if (xmcda == null) {
	    m_profiles = Collections.emptySet();
	} else {
	    final List<XAlternatives> xAlternativesList = xmcda.getAlternativesList();
	    final AlternativesParsingMethod parsingMethod;
	    if (getAlternativesParsingMethod() == null) {
		if (xAlternativesList.size() <= 1) {
		    parsingMethod = AlternativesParsingMethod.TAKE_ALL;
		} else {
		    parsingMethod = AlternativesParsingMethod.SEEK_CONCEPT;
		}
	    } else {
		parsingMethod = getAlternativesParsingMethod();
	    }
	    m_profiles = XMCDAAlternatives.read(xAlternativesList, XAlternativeType.FICTIVE, parsingMethod);
	}
	return m_profiles;
    }

    /**
     * <p>
     * Reads the profiles from the profiles evaluations source, or from the the main source if the dedicated source is
     * not set, or retrieves the results of the previous read if it ended successfully. This method returns every
     * evaluations found in the relevant source. To restrict these evaluations to those concerning the profiles, the
     * {@link EvaluationsUtils} class may be useful.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public EvaluationsRead readProfilesEvaluations() throws IOException, XmlException, InvalidInputException {
	if (m_profilesEvaluationsView != null) {
	    return m_profilesEvaluationsView;
	}
	if (equal(getSourceAlternativesEvaluations(), getSourceProfilesEvaluations())
		&& getAlternativesParsingMethod() == AlternativesParsingMethod.TAKE_ALL) {
	    /** Let's read both alternatives and profiles evaluations at the same time, to avoid double reading. */
	    readEvaluationsIgnoreConcept();
	} else {
	    readProfilesEvaluationsReally();
	}
	return m_profilesEvaluationsView;
    }

    private EvaluationsRead readProfilesEvaluationsReally() throws IOException, XmlException, InvalidInputException {
	final XMCDA xmcda = getXMCDA(m_sourceProfilesEvaluations);
	if (xmcda == null) {
	    m_profilesEvaluationsView = EvaluationsUtils.newEvaluationMatrix();
	} else {
	    final List<XPerformanceTable> xPerformanceTableList = xmcda.getPerformanceTableList();
	    final XMCDAEvaluations xmcdaEvaluations = new XMCDAEvaluations();
	    if (getAlternativesParsingMethod() == AlternativesParsingMethod.SEEK_CONCEPT
		    || getAlternativesParsingMethod() == AlternativesParsingMethod.USE_MARKING) {
		xmcdaEvaluations.setConceptToRead(XAlternativeType.FICTIVE);
	    }
	    final Evaluations evaluations = xmcdaEvaluations.read(xPerformanceTableList);
	    m_profilesEvaluationsView = EvaluationsUtils.getFilteredView(evaluations,
		    Predicates.<Alternative> alwaysTrue(), null);
	}
	return m_profilesEvaluationsView;
    }

    /**
     * Sets the dedicated source used to read the categories.
     * 
     * @param source
     *            <code>null</code> for not set.
     */
    public void setSourceCategories(ByteSource source) {
	m_sourceCategories = source;
	clearCache();
    }

    /**
     * Sets the dedicated source used to read the profiles.
     * 
     * @param sourceProfiles
     *            <code>null</code> for not set.
     */
    public void setSourceProfiles(ByteSource sourceProfiles) {
	m_sourceProfiles = sourceProfiles;
	clearCache();
    }

    /**
     * Sets the dedicated source used to read the profiles evaluations.
     * 
     * @param source
     *            <code>null</code> for not set.
     */
    public void setSourceProfilesEvaluations(ByteSource source) {
	m_sourceProfilesEvaluations = source;
	clearCache();
    }

    /**
     * Creates a new reader delegating error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDASortingProblemReader(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
	m_problemReader = new XMCDAProblemReader(errorsManager);
	init();
    }

    /**
     * Creates a new reader with a main source. The reader will use the default error management strategy
     * {@link ErrorManagement#THROW}.
     * 
     * @param mainSource
     *            not <code>null</code>.
     */
    public XMCDASortingProblemReader(URL mainSource) {
	this(Resources.asByteSource(mainSource));
    }

    /**
     * Retrieves the source dedicated to alternatives.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceAlternatives() {
	return m_problemReader.getSourceAlternatives();
    }

    /**
     * Sets the dedicated source used to read alternatives.
     * 
     * @param sourceAlternatives
     *            <code>null</code> for not set.
     */
    public void setSourceAlternatives(ByteSource sourceAlternatives) {
	m_problemReader.setSourceAlternatives(sourceAlternatives);
	clearCache();
    }

    /**
     * Retrieves the source dedicated to alternatives evaluations.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceAlternativesEvaluations() {
	return m_problemReader.getSourceAlternativesEvaluations();
    }

    /**
     * Sets the dedicated source used to read the evaluations of the alternatives.
     * 
     * @param sourceAlternativesEvaluations
     *            <code>null</code> for not set.
     */
    public void setSourceAlternativesEvaluations(ByteSource sourceAlternativesEvaluations) {
	m_problemReader.setSourceAlternativesEvaluations(sourceAlternativesEvaluations);
	clearCache();
    }

    /**
     * Retrieves the source dedicated to coalitions.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceCoalitions() {
	return m_problemReader.getSourceCoalitions();
    }

    /**
     * Sets the dedicated source used to read the coalitions.
     * 
     * @param sourceCoalitions
     *            <code>null</code> for not set.
     */
    public void setSourceCoalitions(ByteSource sourceCoalitions) {
	m_problemReader.setSourceCoalitions(sourceCoalitions);
	clearCache();
    }

    /**
     * Retrieves the source dedicated to criteria.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceCriteria() {
	return m_problemReader.getSourceCriteria();
    }

    /**
     * Sets the dedicated source used to read the criteria.
     * 
     * @param sourceCriteria
     *            <code>null</code> for not set.
     */
    public void setSourceCriteria(ByteSource sourceCriteria) {
	m_problemReader.setSourceCriteria(sourceCriteria);
	clearCache();
    }

    /**
     * Retrieves the main source. This is used to read any type of object when the dedicated source is not set.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceMain() {
	return m_problemReader.getSourceMain();
    }

    /**
     * Sets the main source used to read all types of objects for which no dedicated source is set.
     * 
     * @param sourceMain
     *            <code>null</code> for not set.
     */
    public void setSourceMain(ByteSource sourceMain) {
	m_problemReader.setSourceMain(sourceMain);
	clearCache();
    }

    /**
     * Retrieves the parsing method used to read alternatives.
     * 
     * @return the parsing method.
     */
    public AlternativesParsingMethod getAlternativesParsingMethod() {
	return m_problemReader.getAlternativesParsingMethod();
    }

    /**
     * Sets the parsing method used to read alternatives and profiles.
     * 
     * @param alternativesParsingMethod
     *            not <code>null</code>.
     */
    public void setAlternativesParsingMethod(AlternativesParsingMethod alternativesParsingMethod) {
	m_problemReader.setAlternativesParsingMethod(alternativesParsingMethod);
    }

    /**
     * <p>
     * Reads the alternatives from the dedicated source, or from the the main source if the dedicated source is not set,
     * or retrieves the results of the previous read if it ended successfully. Which alternatives are returned depend on
     * the chosen parsing method.
     * </p>
     * <p>
     * The returned set iteration order matches the order of the source.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #setAlternativesParsingMethod(AlternativesParsingMethod)
     */
    public Set<Alternative> readAlternatives() throws IOException, XmlException, InvalidInputException {
	return m_problemReader.readAlternatives();
    }

    /**
     * <p>
     * Reads the coalitions from the dedicated source, or from the the main source if the dedicated source is not set,
     * or retrieves the results of the previous read if it ended successfully.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Coalitions readCoalitions() throws IOException, XmlException, InvalidInputException {
	return m_problemReader.readCoalitions();
    }

    /**
     * <p>
     * Reads the criteria from the dedicated source, or from the the main source if the dedicated source is not set, or
     * retrieves the results of the previous read if it ended successfully. This method also sets the scales and
     * thresholds in this object.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Set<Criterion> readCriteria() throws IOException, XmlException, InvalidInputException {
	return m_problemReader.readCriteria();
    }

    /**
     * <p>
     * Reads the scales from the dedicated source, or from the the main source if the dedicated source is not set, or
     * retrieves the results of the previous read if it ended successfully.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Map<Criterion, Interval> readScales() throws IOException, XmlException, InvalidInputException {
	return m_problemReader.readScales();
    }

    /**
     * <p>
     * Reads the thresholds from the dedicated source, or from the the main source if the dedicated source is not set,
     * or retrieves the results of the previous read if it ended successfully.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Thresholds readThresholds() throws IOException, XmlException, InvalidInputException {
	return m_problemReader.readThresholds();
    }

    /**
     * Retrieves the XMCDA document from the given source <em>or</em> from the main source if the given source is
     * <code>null</code>. Ensures that it contains an XMCDA document conforming to the XMCDA schema.
     * 
     * @param source
     *            may be <code>null</code>, in which case the main source in this object must be non <code>null</code>.
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the contents of the source happens while parsing the source, including if
     *             the given source does not contain a valid XMCDA document.
     */
    public XMCDA getXMCDA(ByteSource source) throws IOException, XmlException {
	return m_problemReader.getXMCDA(source);
    }

    /**
     * Retrieves the source dedicated to assignments.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceAssignments() {
	return m_sourceAssignments;
    }

    /**
     * <p>
     * Reads the assignments from the dedicated source, or from the the main source if the dedicated source is not set,
     * or retrieves the results of the previous read if it ended successfully, as a set of assignments of an alternative
     * to a single category.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public IAssignmentsRead readAssignments() throws IOException, XmlException, InvalidInputException {
	readAssignmentsToMultiple();
	return AssignmentsFactory.newAssignmentsFromMultiple(m_assignments);
    }

    /**
     * Sets the dedicated source used to read assignments.
     * 
     * @param source
     *            <code>null</code> for not set.
     */
    public void setSourceAssignments(ByteSource source) {
	m_sourceAssignments = source;
	clearCache();
    }

    /**
     * <p>
     * Reads the assignments from the dedicated source, or from the the main source if the dedicated source is not set,
     * or retrieves the results of the previous read if it ended successfully, as a set of assignments of an alternative
     * to multiple categories.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public IAssignmentsToMultipleRead readAssignmentsToMultiple() throws IOException, XmlException,
	    InvalidInputException {
	if (m_assignments != null) {
	    return new AssignmentsToMultipleFiltering(m_assignments, Predicates.<Alternative> alwaysTrue());
	}
	final XMCDA xmcda = getXMCDA(m_sourceAssignments);
	if (xmcda == null) {
	    m_assignments = AssignmentsFactory.newAssignmentsToMultiple();
	} else {
	    final List<XAlternativesAffectations> xAlternativesAffectationsList = xmcda
		    .getAlternativesAffectationsList();
	    final XAlternativesAffectations xAlternativesAffectations = getUniqueOrZero(xAlternativesAffectationsList);
	    if (xAlternativesAffectations == null) {
		m_assignments = AssignmentsFactory.newAssignmentsToMultiple();
	    } else {
		final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
		m_assignments = xmcdaAssignments.read(xAlternativesAffectations);
	    }
	}
	return new AssignmentsToMultipleFiltering(m_assignments, Predicates.<Alternative> alwaysTrue());
    }

    /**
     * <p>
     * Reads the assignments from the dedicated source, or from the the main source if the dedicated source is not set,
     * or retrieves the results of the previous read if it ended successfully, as a set of assignments of an alternative
     * to multiple categories with associated credibilities.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public IAssignmentsWithCredibilitiesRead readAssignmentsWithCredibilities() throws IOException, XmlException,
	    InvalidInputException {
	if (m_assignmentsWithCredibilities != null) {
	    return new AssignmentsWithCredibilitiesFiltering(m_assignmentsWithCredibilities);
	}
	final XMCDA xmcda = getXMCDA(m_sourceAssignments);
	if (xmcda == null) {
	    m_assignmentsWithCredibilities = AssignmentsFactory.newAssignmentsWithCredibilities();
	} else {
	    final List<XAlternativesAffectations> xAlternativesAffectationsList = xmcda
		    .getAlternativesAffectationsList();
	    final XAlternativesAffectations xAlternativesAffectations = getUniqueOrZero(xAlternativesAffectationsList);
	    if (xAlternativesAffectations == null) {
		m_assignmentsWithCredibilities = AssignmentsFactory.newAssignmentsWithCredibilities();
	    } else {
		final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
		m_assignmentsWithCredibilities = xmcdaAssignments.readWithCredibilities(xAlternativesAffectations);
	    }
	}
	return new AssignmentsWithCredibilitiesFiltering(m_assignmentsWithCredibilities);
    }

    /**
     * <p>
     * Retrieves the union of the evaluations read from the alternatives evaluations source and the evaluations read
     * from the profiles evaluations source, or from the the main source if the dedicated source is not set. The results
     * of possible previous successful reads are re-used. If the two relevant sources are equal, the source is read only
     * once. This method returns every evaluations found, with no distinction between REAL and FICTIVE alternatives.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * 
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public EvaluationsRead readEvaluationsIgnoreConcept() throws IOException, XmlException, InvalidInputException {
	checkState(getAlternativesParsingMethod() == null
		|| getAlternativesParsingMethod() == AlternativesParsingMethod.TAKE_ALL);
	final EvaluationsRead alternativesEvaluations = m_problemReader.readEvaluationsIgnoreConcept();
	final EvaluationsRead all;
	if (equal(getSourceAlternativesEvaluations(), getSourceProfilesEvaluations())) {
	    m_profilesEvaluationsView = EvaluationsUtils.getFilteredView(alternativesEvaluations,
		    Predicates.<Alternative> in(m_profiles), null);
	    all = alternativesEvaluations;
	} else {
	    final EvaluationsRead profilesEvaluations = readProfilesEvaluationsReally();
	    all = EvaluationsUtils.merge(alternativesEvaluations, profilesEvaluations);
	}

	return all;
    }

    /**
     * <p>
     * Retrieves an aggregator object that gather several object type. The various sources set in this object are read
     * from and the results gathered into the returned object. Results from previous successful reads are re-used
     * instead of read again. In particular, if informations from all sources have been read already, no read occurs.
     * </p>
     * <p>
     * The returned object is writable.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public ISortingData readSortingData() throws IOException, XmlException, InvalidInputException {
	return readSortingData(false);
    }

    public ISortingData readSortingData(boolean neverReadProfiles) throws IOException, XmlException,
	    InvalidInputException {
	final ISortingData data = ProblemFactory.newSortingData();

	final Set<Alternative> alternatives = readAlternatives();
	data.getAlternatives().addAll(alternatives);

	final Set<Alternative> profiles = readProfiles();
	final SetView<Alternative> both = Sets.intersection(data.getAlternatives(), profiles);
	if (both.size() >= 1) {
	    error("Found a profile that is also a real alternative: " + both.iterator().next() + ".");
	} else {
	    data.getProfiles().addAll(profiles);
	}

	final Set<Criterion> criteria = readCriteria();
	data.getCriteria().addAll(criteria);

	final Map<Criterion, Interval> scales = readScales();
	for (Criterion criterion : scales.keySet()) {
	    final Interval scale = scales.get(criterion);
	    data.setScale(criterion, scale);
	}

	final CatsAndProfs catsAndProfs = readCategoriesProfiles();
	final NavigableSet<Category> categories = readCategories();
	/** Avoid bad crash if the categories have undetermined order viz catsAndProfs. */
	// CategoryUtils.addTo(categories, catsAndProfs);
	if (!categories.isEmpty() && !catsAndProfs.getCategories().isEmpty()
		&& !Iterables.elementsEqual(categories, catsAndProfs.getCategories())) {
	    error("Categories: " + categories + " do not match categories with associated profiles: " + catsAndProfs
		    + " with category part: " + catsAndProfs.getCategories() + ".");
	}
	final CatsAndProfs effCats;
	if (catsAndProfs.isEmpty()) {
	    effCats = Categories.newCatsAndProfs(categories);
	} else {
	    effCats = catsAndProfs;
	}
	final SetView<Alternative> catsAreAlts = Sets.intersection(data.getAlternatives(), catsAndProfs.getProfiles());
	if (both.size() >= 1) {
	    error("Found a category profile that is also a real alternative: " + catsAreAlts.iterator().next() + ".");
	} else {
	    data.getCatsAndProfs().clear();
	    data.getCatsAndProfs().addAll(effCats);
	}

	if (!neverReadProfiles
		&& equal(getSourceAlternativesEvaluations(), getSourceProfilesEvaluations())
		&& (getAlternativesParsingMethod() == AlternativesParsingMethod.TAKE_ALL || getAlternativesParsingMethod() == null)) {
	    final EvaluationsRead evaluations = readEvaluationsIgnoreConcept();
	    final EvaluationsRead alternativesEvaluations = EvaluationsUtils.getFilteredView(evaluations,
		    Predicates.in(data.getAlternatives()), null);
	    data.setEvaluations(alternativesEvaluations);
	    final Set<Alternative> evaluated = evaluations.getRows();
	    final Set<Alternative> allowed = data.getAllAlternatives();
	    final SetView<Alternative> unexpected = Sets.difference(evaluated, allowed);
	    if (unexpected.size() >= 1) {
		error("Found evaluation corresponding to " + unexpected.iterator().next()
			+ ", neither a profile nor an alternative.");
	    }
	} else {
	    final EvaluationsRead evaluations = readAlternativesEvaluations();
	    final EvaluationsRead alternativesEvaluations = EvaluationsUtils.getFilteredView(evaluations,
		    Predicates.in(data.getAlternatives()), null);
	    data.setEvaluations(ensureNo(alternativesEvaluations, data.getProfiles()));
	}

	return data;
    }

    private EvaluationsRead ensureNo(EvaluationsRead evaluations, Set<Alternative> unwanted)
	    throws InvalidInputException {
	final EvaluationsRead restrictedEvaluations;
	final Set<Alternative> evaluatedAlternatives = evaluations.getRows();
	final SetView<Alternative> unexpected = Sets.intersection(evaluatedAlternatives, unwanted);
	if (unexpected.size() >= 1) {
	    error("Evaluation corresponds to an unexpected alternative: " + unexpected.iterator().next() + ".");
	    restrictedEvaluations = EvaluationsUtils.getFilteredView(evaluations,
		    Predicates.not(Predicates.in(unwanted)), null);
	} else {
	    restrictedEvaluations = evaluations;
	}
	return restrictedEvaluations;
    }

    /**
     * <p>
     * Reads the evaluations from the alternatives evaluations source, or from the the main source if the dedicated
     * source is not set, or retrieves the results of the previous read if it ended successfully. This method returns
     * every evaluations found in the relevant source. To restrict these evaluations to those concerning the real
     * alternatives, or to those concerning the profiles, the {@link EvaluationsUtils} class may be useful.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public EvaluationsRead readAlternativesEvaluations() throws IOException, XmlException, InvalidInputException {
	final EvaluationsRead evaluations;
	if (equal(getSourceAlternativesEvaluations(), getSourceProfilesEvaluations())
		&& getAlternativesParsingMethod() == AlternativesParsingMethod.TAKE_ALL) {
	    /** Let's read both alternatives and profiles evaluations at the same time, to avoid double reading. */
	    evaluations = readEvaluationsIgnoreConcept();
	} else {
	    evaluations = m_problemReader.readAlternativeEvaluations();
	}
	return evaluations;
    }

    /**
     * Tests whether two sources are equal when replacing a <code>null</code> source with the main source. If both
     * resulting sources are <code>null</code>, this method returns <code>true</code>. This method can be used to test
     * whether the effective sources that this reader will use are equal.
     * 
     * @param source1
     *            may be <code>null</code>.
     * @param source2
     *            may be <code>null</code>.
     * @return <code>true</code> iff both sources are equal from the point of view of this class.
     */
    public boolean equal(ByteSource source1, ByteSource source2) {
	return m_problemReader.equal(source1, source2);
    }

    /**
     * <p>
     * Retrieves an aggregator object that gather several object type. The various sources set in this object are read
     * from and the results gathered into the returned object. Results from previous successful reads are re-used
     * instead of read again. In particular, if informations from all sources have been read already, no read occurs.
     * </p>
     * <p>
     * The returned object is writable.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public ISortingPreferences readSortingPreferences() throws IOException, XmlException, InvalidInputException {
	final ISortingPreferences data = ProblemFactory.newSortingPreferences();

	SortingProblemUtils.copyDataToTarget(readSortingData(), data);

	final EvaluationsRead profilesEvaluations;
	if (equal(getSourceAlternativesEvaluations(), getSourceProfilesEvaluations())
		&& (getAlternativesParsingMethod() == null || getAlternativesParsingMethod() == AlternativesParsingMethod.TAKE_ALL)) {
	    final EvaluationsRead evaluations = readEvaluationsIgnoreConcept();
	    profilesEvaluations = EvaluationsUtils
		    .getFilteredView(evaluations, Predicates.in(data.getProfiles()), null);
	} else {
	    final EvaluationsRead evaluations = readProfilesEvaluations();
	    profilesEvaluations = ensureNo(evaluations, data.getAlternatives());
	}
	data.setProfilesEvaluations(profilesEvaluations);

	data.setCoalitions(readCoalitions());
	data.setThresholds(readThresholds());

	return data;
    }

    /**
     * <p>
     * Retrieves an aggregator object that gather several object type. The various sources set in this object are read
     * from and the results gathered into the returned object. Results from previous successful reads are re-used
     * instead of read again. In particular, if informations from all sources have been read already, no read occurs.
     * </p>
     * <p>
     * The returned object is writable.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public ISortingResults readSortingResults() throws IOException, XmlException, InvalidInputException {
	final ISortingResults data = SortingProblemUtils.newResults();

	final ISortingPreferences preferences = readSortingPreferences();
	SortingProblemUtils.copyPreferencesToTarget(preferences, data);

	final IAssignmentsRead source = readAssignments();
	try {
	    AssignmentsUtils.copyAssignmentsToOrderedTarget(source, data.getAssignments());
	} catch (InvalidInputException exc) {
	    error(exc.getMessage());
	}

	return data;
    }

    /**
     * <p>
     * Retrieves an aggregator object that gather several object type. The various sources set in this object are read
     * from and the results gathered into the returned object. Results from previous successful reads are re-used
     * instead of read again. In particular, if informations from all sources have been read already, no read occurs.
     * </p>
     * <p>
     * The returned object is writable.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public ISortingResultsToMultiple readSortingResultsToMultiple() throws IOException, XmlException,
	    InvalidInputException {
	final ISortingResultsToMultiple data = ProblemFactory.newSortingResultsToMultiple();

	final ISortingPreferences preferences = readSortingPreferences();
	SortingProblemUtils.copyPreferencesToTarget(preferences, data);

	final IAssignmentsToMultipleRead source = readAssignmentsToMultiple();
	try {
	    AssignmentsUtils.copyAssignmentsToMultipleToOrderedTarget(source, data.getAssignments());
	} catch (InvalidInputException exc) {
	    error(exc.getMessage());
	}

	return data;
    }

    /**
     * <p>
     * Retrieves an aggregator object that gather several object type. The various sources set in this object are read
     * from and the results gathered into the returned object. Results from previous successful reads are re-used
     * instead of read again. In particular, if informations from all sources have been read already, no read occurs.
     * </p>
     * <p>
     * The returned object is writable.
     * </p>
     * <p>
     * In case of unexpected data, an InvalidInputException is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @return not <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the xml correctness of the source happens while parsing the source,
     *             including if the given source does not contain a valid XMCDA document.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public ISortingResultsWithCredibilities readSortingResultsWithCredibilities() throws IOException, XmlException,
	    InvalidInputException {
	final ISortingResultsWithCredibilities data = SortingProblemUtils.newResultsWithCredibilities();

	final ISortingPreferences preferences = readSortingPreferences();
	SortingProblemUtils.copyPreferencesToTarget(preferences, data);

	final IAssignmentsWithCredibilitiesRead source = readAssignmentsWithCredibilities();
	try {
	    AssignmentsUtils.copyAssignmentsWithCredibilitiesToOrderedTarget(source, data.getAssignments());
	} catch (InvalidInputException exc) {
	    error(exc.getMessage());
	}

	return data;
    }

    /**
     * Clears the information cached in this class, resulting in the loss of any previously read information that had
     * been remembered by this class. After this method is called, using any read method results in an effective read of
     * the relevant source instead of a possible re-use of the cached data.
     */
    public void clearCache() {
	m_problemReader.clearCache();
	m_categories = null;
	m_catsAndProfs = null;
	m_profiles = null;
	m_profilesEvaluationsView = null;
	m_assignments = null;
	m_assignmentsWithCredibilities = null;
    }

    /**
     * Retrieves the XMCDA version of the documents read by this object.
     * 
     * @return <code>null</code> if not uniform or nothing read yet.
     */
    public String getSourceVersion() {
	return m_problemReader.getSourceVersion();
    }

}
