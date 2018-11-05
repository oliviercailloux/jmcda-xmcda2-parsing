package org.decisiondeck.jmcda.persist.xmcda2.aggregates;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
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
import org.decisiondeck.jmcda.persist.xmcda2.X2Concept;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAssignments;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACategories;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesAffectations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategories;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesProfiles;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaSet;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelper;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IAssignmentsWithCredibilitiesRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.assignments.ISortingAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class X2SimpleReader extends XMCDAHelper {

    /**
     * Not <code>null</code>.
     */
    private final XMCDA m_source;
    private XMCDACriteria m_xmcdaCriteria;
    private CatsAndProfs m_catsAndProfs;
    private EvaluationsRead m_profilesEvaluations;
    private XMCDACategories m_xmcdaCategories;
    private XMCDAAlternatives m_xmcdaAlternatives;
    private boolean m_hasReadCategoriesProfiles;
    private boolean m_hasReadCategories;
    private EvaluationsRead m_unmarkedEvaluations;
    private IAssignmentsToMultiple m_assignments;
    private boolean m_hasReadAssignmentsWithCredibilities;
    private EvaluationsRead m_alternativesEvaluationsReal;
    private Map<DecisionMaker, IAssignmentsWithCredibilities> m_allAssignmentsWithCredibilities;
    private Map<DecisionMaker, Coalitions> m_allCoalitions;
    private Map<DecisionMaker, IAssignmentsToMultiple> m_allAssignments;
    private boolean m_hasReadAssignments;

    public X2SimpleReader(XMCDA source) {
	checkNotNull(source);
	m_source = source;
	m_xmcdaCriteria = null;
	m_xmcdaCategories = null;
	m_catsAndProfs = null;
	m_alternativesEvaluationsReal = null;
	m_profilesEvaluations = null;
	m_xmcdaAlternatives = null;
	m_hasReadCategoriesProfiles = false;
	m_hasReadCategories = false;
	m_unmarkedEvaluations = null;
	m_assignments = null;
	m_hasReadAssignments = false;
	m_allAssignments = null;
	m_allAssignmentsWithCredibilities = null;
	m_allCoalitions = null;
    }

    public X2SimpleReader(XMCDADoc source) {
	this(source.getXMCDA());
    }

    public ISortingPreferences readSortingPreferences() throws InvalidInputException {
	readAlternatives();
	final Set<Alternative> alternativesOnly = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.REAL);
	final Set<Alternative> profiles = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.FICTIVE);
	final Set<Alternative> alternativesUnmarked = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.UNMARKED);

	final SetView<Alternative> alternatives = Sets.union(alternativesOnly, alternativesUnmarked);
	final SetView<Alternative> both = Sets.intersection(alternatives, profiles);
	assert both.isEmpty() : "Found a profile that is also a real alternative: " + both.iterator().next() + ".";

	final Set<Criterion> criteria = readCriteria();
	final Map<Criterion, Interval> scales = getReadScales();
	final Thresholds thresholds = getReadThresholds();

	readCategoriesProfilesAndCategories();
	final CatsAndProfs effCats = ensureNo(m_catsAndProfs, alternatives);

	readAlternativesEvaluationsReal();
	readProfilesEvaluationsReal();
	readUnmarkedEvaluations();

	final EvaluationsRead alternativesEvaluationsNoProfiles = ensureNo(m_alternativesEvaluationsReal, profiles);
	final EvaluationsRead profilesEvaluationsNoAlternatives = ensureNo(m_profilesEvaluations, alternatives);
	final EvaluationsRead unmarkedEvaluationsOfProfiles = EvaluationsUtils.getFilteredView(m_unmarkedEvaluations,
		Predicates.in(profiles), null);
	final EvaluationsRead unmarkedEvaluationsOfAlternatives = EvaluationsUtils.getFilteredView(
		m_unmarkedEvaluations, Predicates.not(Predicates.in(profiles)), null);

	final Coalitions coalitions = readCoalitions();

	final ISortingPreferences read = ProblemFactory.newSortingPreferences(
		EvaluationsUtils.merge(alternativesEvaluationsNoProfiles, unmarkedEvaluationsOfAlternatives), scales,
		effCats, EvaluationsUtils.merge(profilesEvaluationsNoAlternatives, unmarkedEvaluationsOfProfiles),
		thresholds, coalitions);
	read.getAlternatives().addAll(alternatives);
	read.getAlternatives().addAll(alternativesEvaluationsNoProfiles.getRows());
	read.getAlternatives().addAll(unmarkedEvaluationsOfAlternatives.getRows());
	read.getProfiles().addAll(profiles);
	read.getProfiles().addAll(profilesEvaluationsNoAlternatives.getRows());
	if (criteria != null) {
	    read.getCriteria().addAll(criteria);
	}
	assert read.getProfiles().containsAll(unmarkedEvaluationsOfProfiles.getRows());
	return read;
    }

    public Coalitions readCoalitions() throws InvalidInputException {
	final List<XCriteriaSet> xCriteriaSetList = m_source.getCriteriaSetList();
	final XCriteriaSet xCriteriaSet = getUniqueOrZero(xCriteriaSetList);
	if (xCriteriaSet == null) {
	    return null;
	}
	final Coalitions coalitions;
	final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
	coalitions = xmcdaCriteria.readCoalitions(xCriteriaSet);
	return coalitions;
    }

    public Map<DecisionMaker, Coalitions> readGroupCoalitions() throws InvalidInputException {
	if (m_allCoalitions == null) {
	    final List<XCriteriaSet> xCriteriaSetList = m_source.getCriteriaSetList();
	    if (xCriteriaSetList.size() == 0) {
		m_allCoalitions = null;
	    } else {
		final XMCDACriteria reader = new XMCDACriteria();
		m_allCoalitions = reader.readAllCoalitions(xCriteriaSetList);
	    }
	}
	return m_allCoalitions;
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

    public ISortingData readSortingData() throws InvalidInputException {
	readAlternatives();
	final Set<Alternative> alternativesOnly = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.REAL);
	final Set<Alternative> profiles = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.FICTIVE);
	final Set<Alternative> alternativesUnmarked = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.UNMARKED);

	final SetView<Alternative> alternatives = Sets.union(alternativesOnly, alternativesUnmarked);
	final SetView<Alternative> both = Sets.intersection(alternatives, profiles);
	assert both.isEmpty() : "Found a profile that is also a real alternative: " + both.iterator().next() + ".";

	final Set<Criterion> criteria = readCriteria();

	readCategoriesProfilesAndCategories();
	final CatsAndProfs effCats = ensureNo(m_catsAndProfs, alternatives);

	readAlternativesEvaluationsReal();
	readUnmarkedEvaluations();
	final EvaluationsRead alternativesEvaluationsNoProfiles = ensureNo(m_alternativesEvaluationsReal, profiles);
	final EvaluationsRead unmarkedEvaluationsNoProfiles = EvaluationsUtils.getFilteredView(m_unmarkedEvaluations,
		Predicates.not(Predicates.in(profiles)), null);

	final ISortingData read = ProblemFactory.newSortingData(
		EvaluationsUtils.merge(alternativesEvaluationsNoProfiles, unmarkedEvaluationsNoProfiles),
		getReadScales(), effCats);

	read.getAlternatives().addAll(alternatives);
	read.getAlternatives().addAll(alternativesEvaluationsNoProfiles.getRows());
	read.getAlternatives().addAll(unmarkedEvaluationsNoProfiles.getRows());
	read.getProfiles().addAll(profiles);
	if (criteria != null) {
	    read.getCriteria().addAll(criteria);
	}
	return read;
    }

    private void readAlternatives() throws InvalidInputException {
	if (m_xmcdaAlternatives == null) {
	    final List<XAlternatives> xAlternativesList = m_source.getAlternativesList();
	    m_xmcdaAlternatives = new XMCDAAlternatives();
	    m_xmcdaAlternatives.readAll(xAlternativesList);
	}
    }

    private CatsAndProfs ensureNo(final CatsAndProfs catsAndProfs, final Set<Alternative> alternatives)
	    throws InvalidInputException {
	final CatsAndProfs effCats;
	if (catsAndProfs == null) {
	    effCats = null;
	} else {
	    final SetView<Alternative> catsAreAlts = Sets.intersection(alternatives, catsAndProfs.getProfiles());
	    if (catsAreAlts.size() >= 1) {
		error("Found a category profile that is also a real alternative: " + catsAreAlts.iterator().next()
			+ ".");
		effCats = null;
	    } else {
		effCats = catsAndProfs;
	    }
	}
	return effCats;
    }

    /**
     * Retrieves information about categories and profiles. If one of the relevant tag is found, information is
     * returned. If no relevant tag is found, returns <code>null</code>.
     * 
     * @return <code>null</code> iff no appropriate tag is found.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public CatsAndProfs readCategoriesProfilesAndCategories() throws InvalidInputException {
	boolean hasReadCatsAndProfs = false;
	if (!hasReadCatsAndProfs) {
	    hasReadCatsAndProfs = true;
	    final Set<Category> categories = readCategories();
	    final CatsAndProfs catsAndProfs = readCategoriesProfiles();
	    if (categories == null && catsAndProfs == null) {
		m_catsAndProfs = null;
	    } else {
		final boolean hasCategories = categories != null && !categories.isEmpty();
		if (catsAndProfs == null || catsAndProfs.isEmpty()) {
		    if (!hasCategories) {
			m_catsAndProfs = Categories.newCatsAndProfs();
		    } else {
			m_catsAndProfs = Categories.newCatsAndProfs(categories);
		    }
		} else {
		    if (hasCategories && !Iterables.elementsEqual(categories, catsAndProfs.getCategories())) {
			error("Categories: " + categories + " do not match categories with associated profiles: "
				+ catsAndProfs + " with category part: " + catsAndProfs.getCategories() + ".");
			m_catsAndProfs = null;
		    } else {
			m_catsAndProfs = catsAndProfs;
		    }
		}
	    }
	}
	return m_catsAndProfs;
    }

    /**
     * @return <code>null</code> iff the appropriate tag is not found.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public NavigableSet<Category> readCategories() throws InvalidInputException {
	if (!m_hasReadCategories) {
	    m_hasReadCategories = true;
	    final List<XCategories> xCategoriesList = m_source.getCategoriesList();
	    final XCategories xCategories = getUniqueOrZero(xCategoriesList);
	    if (xCategories == null) {
		m_xmcdaCategories = null;
	    } else {
		if (m_xmcdaCategories == null) {
		    m_xmcdaCategories = new XMCDACategories();
		}
		m_xmcdaCategories.read(xCategories);
	    }
	}
	return m_xmcdaCategories == null ? null : ExtentionalTotalOrder.create(m_xmcdaCategories.getCategories());
    }

    /**
     * @return <code>null</code> iff the appropriate tag is not found.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public CatsAndProfs readCategoriesProfiles() throws InvalidInputException {
	if (!m_hasReadCategoriesProfiles) {
	    m_hasReadCategoriesProfiles = true;
	    final List<XCategoriesProfiles> xCategoriesProfilesList = m_source.getCategoriesProfilesList();
	    final XCategoriesProfiles xCategoriesProfiles = getUniqueOrZero(xCategoriesProfilesList);
	    if (xCategoriesProfiles == null) {
		m_xmcdaCategories = null;
	    } else {
		if (m_xmcdaCategories == null) {
		    m_xmcdaCategories = new XMCDACategories();
		}
		m_xmcdaCategories.read(xCategoriesProfiles);
	    }
	}
	return m_xmcdaCategories == null ? null : m_xmcdaCategories.getCatsAndProfs();
    }

    public Set<Criterion> readCriteria() throws InvalidInputException {
	if (m_xmcdaCriteria == null) {
	    final List<XCriteria> xCriteriaList = m_source.getCriteriaList();
	    final XCriteria xCriteria = getUniqueOrZero(xCriteriaList);
	    if (xCriteria == null) {
		m_xmcdaCriteria = null;
		return null;
	    }
	    m_xmcdaCriteria = new XMCDACriteria();
	    m_xmcdaCriteria.read(xCriteria);
	}
	return m_xmcdaCriteria.getCriteria();
    }

    public Thresholds getReadThresholds() {
	return m_xmcdaCriteria == null ? null : m_xmcdaCriteria.getThresholds();
    }

    public ISortingAssignmentsToMultiple readSortingAssignmentsToMultiple() throws InvalidInputException {
	readAlternatives();
	final Set<Alternative> alternativesOnly = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.REAL);
	final Set<Alternative> profiles = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.FICTIVE);
	final Set<Alternative> alternativesUnmarked = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.UNMARKED);

	final SetView<Alternative> alternatives = Sets.union(alternativesOnly, alternativesUnmarked);
	final SetView<Alternative> both = Sets.intersection(alternatives, profiles);
	assert both.isEmpty() : "Found a profile that is also a real alternative: " + both.iterator().next() + ".";

	final Set<Criterion> criteria = readCriteria();

	readCategoriesProfilesAndCategories();
	if (m_catsAndProfs == null) {
	    error("No cats and profs found.");
	    return null;
	}
	final CatsAndProfs effCats = ensureNo(m_catsAndProfs, alternatives);

	readAlternativesEvaluationsReal();
	readUnmarkedEvaluations();
	final EvaluationsRead alternativesEvaluationsNoProfiles = ensureNo(m_alternativesEvaluationsReal, profiles);
	final EvaluationsRead unmarkedEvaluationsNoProfiles = EvaluationsUtils.getFilteredView(m_unmarkedEvaluations,
		Predicates.not(Predicates.in(profiles)), null);

	readAssignments();

	final ISortingAssignmentsToMultiple read = ProblemFactory.newSortingAssignmentsToMultiple(
		EvaluationsUtils.merge(alternativesEvaluationsNoProfiles, unmarkedEvaluationsNoProfiles),
		getReadScales(), effCats, m_assignments);

	read.getAlternatives().addAll(alternatives);
	read.getAlternatives().addAll(alternativesEvaluationsNoProfiles.getRows());
	read.getAlternatives().addAll(unmarkedEvaluationsNoProfiles.getRows());
	read.getProfiles().addAll(profiles);
	read.getCriteria().addAll(criteria);
	return read;
    }

    private void readUnmarkedEvaluations() throws InvalidInputException {
	if (m_unmarkedEvaluations == null) {
	    final List<XPerformanceTable> xPerformanceTableList = m_source.getPerformanceTableList();
	    final XMCDAEvaluations xmcdaEvaluations = new XMCDAEvaluations();
	    xmcdaEvaluations.setConceptToRead(X2Concept.UNMARKED);
	    m_unmarkedEvaluations = xmcdaEvaluations.read(xPerformanceTableList);
	}
    }

    private void readAlternativesEvaluationsReal() throws InvalidInputException {
	final List<XPerformanceTable> xPerformanceTableList = m_source.getPerformanceTableList();
	if (m_alternativesEvaluationsReal == null) {
	    final XMCDAEvaluations xmcdaEvaluations = new XMCDAEvaluations();
	    xmcdaEvaluations.setConceptToRead(X2Concept.REAL);
	    m_alternativesEvaluationsReal = xmcdaEvaluations.read(xPerformanceTableList);
	}
    }

    public Evaluations readProfilesEvaluations() throws InvalidInputException {
	readAlternatives();
	final Set<Alternative> alternativesOnly = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.REAL);
	final Set<Alternative> profiles = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.FICTIVE);
	final Set<Alternative> alternativesUnmarked = m_xmcdaAlternatives.getMarkedAlternatives(X2Concept.UNMARKED);
	readAlternativesEvaluationsReal();
	readProfilesEvaluationsReal();
	readUnmarkedEvaluations();

	final SetView<Alternative> alternatives = Sets.union(alternativesOnly, alternativesUnmarked);
	final EvaluationsRead profilesEvaluationsNoAlternatives = ensureNo(m_profilesEvaluations, alternatives);
	final EvaluationsRead unmarkedEvaluationsOfProfiles = EvaluationsUtils.getFilteredView(m_unmarkedEvaluations,
		Predicates.in(profiles), null);

	return EvaluationsUtils.merge(profilesEvaluationsNoAlternatives, unmarkedEvaluationsOfProfiles);
    }

    public IAssignmentsToMultiple readAssignments() throws InvalidInputException {
	if (!m_hasReadAssignments) {
	    m_hasReadAssignments = true;
	    final List<XAlternativesAffectations> xAlternativesAffectationsList = m_source
		    .getAlternativesAffectationsList();
	    final XAlternativesAffectations xAlternativesAffectations = getUniqueOrZero(xAlternativesAffectationsList);
	    if (xAlternativesAffectations == null) {
		m_assignments = null;
	    } else {
		final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
		m_assignments = xmcdaAssignments.read(xAlternativesAffectations);
	    }
	}
	return m_assignments;
    }

    public Map<Criterion, Interval> getReadScales() {
	return m_xmcdaCriteria == null ? null : m_xmcdaCriteria.getScales();
    }

    public Map<DecisionMaker, IAssignmentsWithCredibilities> readGroupAssignmentsWithCredibilities()
	    throws InvalidInputException {
	return readGroupAssignmentsWithCredibilitiesConstrained(null);
    }

    private void readProfilesEvaluationsReal() throws InvalidInputException {
	final List<XPerformanceTable> xPerformanceTableList = m_source.getPerformanceTableList();
	if (m_profilesEvaluations == null) {
	    final XMCDAEvaluations xmcdaEvaluations = new XMCDAEvaluations();
	    xmcdaEvaluations.setConceptToRead(X2Concept.FICTIVE);
	    m_profilesEvaluations = xmcdaEvaluations.read(xPerformanceTableList);
	}
    }

    private Map<DecisionMaker, IAssignmentsWithCredibilities> readGroupAssignmentsWithCredibilitiesConstrained(
	    Set<Category> knownCategories)
	    throws InvalidInputException {
	if (m_hasReadAssignmentsWithCredibilities) {
	    for (IAssignmentsWithCredibilitiesRead assignments : m_allAssignmentsWithCredibilities.values()) {
		if (knownCategories != null) {
		    final Set<Category> unknown = Sets.difference(assignments.getCategories(), knownCategories)
			    .immutableCopy();
		    if (unknown.size() >= 1) {
			throw new InvalidInputException("Unknown categories " + unknown + ".");
		    }
		}
	    }
	} else {
	    m_hasReadAssignmentsWithCredibilities = true;
	    final List<XAlternativesAffectations> xAlternativesAffectationsList = m_source
		    .getAlternativesAffectationsList();
	    // m_assignments = null;
	    if (xAlternativesAffectationsList.size() == 0) {
		m_allAssignmentsWithCredibilities = null;
	    } else {
		final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
		if (knownCategories != null) {
		    xmcdaAssignments.setCategories(knownCategories);
		}
		m_allAssignmentsWithCredibilities = xmcdaAssignments
			.readAllWithCredibilities(xAlternativesAffectationsList);
	    }
	}
	return m_allAssignmentsWithCredibilities;
    }

    public Map<DecisionMaker, IOrderedAssignmentsToMultiple> readGroupOrderedAssignments(Set<Category> categories)
	    throws InvalidInputException {
	checkNotNull(categories);
	final Map<DecisionMaker, IAssignmentsToMultiple> group = readGroupAssignmentsConstrained(categories);
	final Map<DecisionMaker, IOrderedAssignmentsToMultiple> allOrdered = Maps.newLinkedHashMap();
	final Set<DecisionMaker> dms = group.keySet();
	for (DecisionMaker dm : dms) {
	    final IAssignmentsToMultiple unordered = group.get(dm);
	    final IOrderedAssignmentsToMultiple ordered = AssignmentsFactory.newOrderedAssignmentsToMultiple(unordered, ExtentionalTotalOrder.create(categories));
	    allOrdered.put(dm, ordered);
	}
	return allOrdered;
    }

    public Map<DecisionMaker, IAssignmentsToMultiple> readGroupAssignments(Set<Category> knownCategories)
            throws InvalidInputException {
        checkNotNull(knownCategories);
        return readGroupAssignmentsConstrained(knownCategories);
    }

    public Map<DecisionMaker, IAssignmentsToMultiple> readGroupAssignments() throws InvalidInputException {
        return readGroupAssignmentsConstrained(null);
    }

    private Map<DecisionMaker, IAssignmentsToMultiple> readGroupAssignmentsConstrained(Set<Category> knownCategories)
	    throws InvalidInputException {
	if (m_hasReadAssignments) {
	    for (IAssignmentsToMultipleRead assignments : m_allAssignments.values()) {
		if (knownCategories != null) {
		    final Set<Category> unknown = Sets.difference(assignments.getCategories(), knownCategories)
			    .immutableCopy();
		    if (unknown.size() >= 1) {
			throw new InvalidInputException("Unknown categories " + unknown + ".");
		    }
		}
	    }
	} else {
	    m_hasReadAssignments = true;
	    final List<XAlternativesAffectations> xAlternativesAffectationsList = m_source
		    .getAlternativesAffectationsList();
	    if (xAlternativesAffectationsList.size() == 0) {
		m_assignments = null;
		m_allAssignments = null;
	    } else {
		final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
		m_assignments = null;
		if (knownCategories != null) {
		    xmcdaAssignments.setCategories(knownCategories);
		}
		m_allAssignments = xmcdaAssignments.readAll(xAlternativesAffectationsList);
	    }
	}
	return m_allAssignments;
    }

}
