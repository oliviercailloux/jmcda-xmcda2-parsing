package org.decisiondeck.jmcda.persist.xmcda2.aggregates;

import static com.google.common.base.Preconditions.checkNotNull;

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
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.CoalitionsUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives.AlternativesParsingMethod;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAssignments;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDADecisionMakers;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeType;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesAffectations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaSet;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodParameters;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;
import org.decisiondeck.jmcda.structure.sorting.assignment.AssignmentsToMultipleFiltering;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignments;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignments;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.AssignmentsWithCredibilitiesFiltering;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IAssignmentsWithCredibilitiesRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilitiesRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsUtils;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.group_preferences.IGroupSortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.group_results.IGroupSortingResults;
import org.decisiondeck.jmcda.structure.sorting.problem.group_results.IGroupSortingResultsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.problem.group_results.IGroupSortingResultsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.decisiondeck.xmcda_oo.structure.sorting.SortingProblemUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

/**
 * TODO document properly.
 * <p>
 * A class to read MCDA group sorting problems defined in XMCDA documents,
 * including the typical preferences informations related to a sorting problem,
 * namely thresholds and coalitions; and sorting results, namely assignments.
 * Support is given for assignments to single categories, assignments to
 * multiple categories and assignments with credibilities. This class supports
 * the case of a group of decision makers.
 * </p>
 *
 * @see org.decisiondeck.jmcda.persist.xmcda2
 * @author Olivier Cailloux
 *
 */
public class XMCDAGroupSortingProblemReader extends XMCDAHelperWithVarious {
	private Map<DecisionMaker, IAssignmentsToMultiple> m_allAssignments;
	private Map<DecisionMaker, IAssignmentsWithCredibilities> m_allAssignmentsWithCredibilities;
	private Map<DecisionMaker, Coalitions> m_allCoalitions;
	private Map<DecisionMaker, Evaluations> m_allEvaluations;
	private Set<DecisionMaker> m_dms;
	private final XMCDASortingProblemReader m_problemReader;
	private ByteSource m_sourceDms;

	/**
	 * Creates a new reader which will use the default error management strategy
	 * {@link ErrorManagement#THROW}.
	 */
	public XMCDAGroupSortingProblemReader() {
		this(new XMCDAErrorsManager());
	}

	/**
	 * Creates a new reader with a main source. The reader will use the default
	 * error management strategy {@link ErrorManagement#THROW}.
	 *
	 * @param mainSource
	 *            not <code>null</code>.
	 */
	public XMCDAGroupSortingProblemReader(ByteSource mainSource) {
		this(mainSource, new XMCDAErrorsManager());
	}

	/**
	 * Creates a new reader with a main source, and delegating error management
	 * to the given error manager in case of unexpected data read.
	 *
	 * @param mainSource
	 *            not <code>null</code>.
	 * @param errorsManager
	 *            not <code>null</code>.
	 */
	public XMCDAGroupSortingProblemReader(ByteSource mainSource, XMCDAErrorsManager errorsManager) {
		super(errorsManager);
		checkNotNull(mainSource);
		m_problemReader = new XMCDASortingProblemReader(mainSource, errorsManager);
		init();
	}

	/**
	 * Creates a new reader with a main source. The reader will use the default
	 * error management strategy {@link ErrorManagement#THROW}.
	 *
	 * @param mainSource
	 *            not <code>null</code>.
	 */
	public XMCDAGroupSortingProblemReader(URL mainSource) {
		this(Resources.asByteSource(mainSource));
	}

	/**
	 * Creates a new reader delegating error management to the given error
	 * manager in case of unexpected data read.
	 *
	 * @param errorsManager
	 *            not <code>null</code>.
	 */
	public XMCDAGroupSortingProblemReader(XMCDAErrorsManager errorsManager) {
		super(errorsManager);
		m_problemReader = new XMCDASortingProblemReader(errorsManager);
		init();
	}

	/**
	 * Clears the information cached in this class, resulting in the loss of any
	 * previously read information that had been remembered by this class. After
	 * this method is called, using any read method results in an effective read
	 * of the relevant source instead of a possible re-use of the cached data.
	 */
	public void clearCache() {
		m_problemReader.clearCache();
		m_allCoalitions = null;
		m_allAssignments = null;
		m_allEvaluations = null;
	}

	/**
	 * Tests whether two sources are equal when replacing a <code>null</code>
	 * source with the main source. If both resulting sources are
	 * <code>null</code>, this method returns <code>true</code>. This method can
	 * be used to test whether the effective sources that this reader will use
	 * are equal.
	 *
	 * @param source1
	 *            may be <code>null</code>.
	 * @param source2
	 *            may be <code>null</code>.
	 * @return <code>true</code> iff both sources are equal from the point of
	 *         view of this class.
	 */
	public boolean equal(ByteSource source1, ByteSource source2) {
		return m_problemReader.equal(source1, source2);
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
	 * Retrieves the source dedicated to alternatives.
	 *
	 * @return <code>null</code> if not set.
	 */
	public ByteSource getSourceAlternatives() {
		return m_problemReader.getSourceAlternatives();
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
	 * Retrieves the source dedicated to assignments.
	 *
	 * @return <code>null</code> if not set.
	 */
	public ByteSource getSourceAssignments() {
		return m_problemReader.getSourceAssignments();
	}

	/**
	 * Retrieves the source dedicated to categories.
	 *
	 * @return <code>null</code> if not set.
	 */
	public ByteSource getSourceCategories() {
		return m_problemReader.getSourceCategories();
	}

	/**
	 * Retrieves the source dedicated to definition of the categories through
	 * bounding profiles.
	 *
	 * @return <code>null</code> if not set.
	 */
	public ByteSource getSourceCategoriesProfiles() {
		return m_problemReader.getSourceCategoriesProfiles();
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
	 * Retrieves the source dedicated to criteria.
	 *
	 * @return <code>null</code> if not set.
	 */
	public ByteSource getSourceCriteria() {
		return m_problemReader.getSourceCriteria();
	}

	/**
	 * Retrieves the source dedicated to the decision makers.
	 *
	 * @return <code>null</code> if not set.
	 */
	public ByteSource getSourceDms() {
		return m_sourceDms;
	}

	/**
	 * Retrieves the main source. This is used to read any type of object when
	 * the dedicated source is not set.
	 *
	 * @return <code>null</code> if not set.
	 */
	public ByteSource getSourceMain() {
		return m_problemReader.getSourceMain();
	}

	/**
	 * Retrieves the source dedicated to profiles.
	 *
	 * @return <code>null</code> if not set.
	 */
	public ByteSource getSourceProfiles() {
		return m_problemReader.getSourceProfiles();
	}

	/**
	 * Retrieves the source dedicated to profiles evaluations.
	 *
	 * @return <code>null</code> if not set.
	 */
	public ByteSource getSourceProfilesEvaluations() {
		return m_problemReader.getSourceProfilesEvaluations();
	}

	/**
	 * Retrieves the XMCDA version of the documents read by this object.
	 *
	 * @return <code>null</code> if not uniform or nothing read yet.
	 */
	public String getSourceVersion() {
		return m_problemReader.getSourceVersion();
	}

	public XMCDA getXMCDA(ByteSource source) throws IOException, XmlException {
		return m_problemReader.getXMCDA(source);
	}

	/**
	 * <p>
	 * Reads the assignments per decision maker from the dedicated source, or
	 * from the the main source if the dedicated source is not set, or retrieves
	 * the results of the previous read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Map<DecisionMaker, IAssignmentsToMultipleRead> readAllAssignmentsToMultiple()
			throws IOException, XmlException, InvalidInputException {
		if (m_allAssignments != null) {
			return Collections.unmodifiableMap(Maps.transformValues(m_allAssignments,
					new Function<IAssignmentsToMultiple, IAssignmentsToMultipleRead>() {
						@Override
						public IAssignmentsToMultipleRead apply(IAssignmentsToMultiple input) {
							return new AssignmentsToMultipleFiltering(input, Predicates.<Alternative> alwaysTrue());
						}
					}));
		}
		final XMCDA xmcda = getXMCDA(getSourceAssignments());
		if (xmcda == null) {
			m_allAssignments = Collections.emptyMap();
		} else {
			final List<XAlternativesAffectations> xAlternativesAffectationsList = xmcda
					.getAlternativesAffectationsList();
			final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
			m_allAssignments = xmcdaAssignments.readAll(xAlternativesAffectationsList);
		}
		return Collections.unmodifiableMap(Maps.transformValues(m_allAssignments,
				new Function<IAssignmentsToMultiple, IAssignmentsToMultipleRead>() {
					@Override
					public IAssignmentsToMultipleRead apply(IAssignmentsToMultiple input) {
						return new AssignmentsToMultipleFiltering(input, Predicates.<Alternative> alwaysTrue());
					}
				}));
	}

	public Map<DecisionMaker, IAssignmentsWithCredibilitiesRead> readAllAssignmentsWithCredibilities()
			throws IOException, XmlException, InvalidInputException {
		if (m_allAssignmentsWithCredibilities != null) {
			return Collections.unmodifiableMap(Maps.transformValues(m_allAssignmentsWithCredibilities,
					new Function<IAssignmentsWithCredibilities, IAssignmentsWithCredibilitiesRead>() {
						@Override
						public IAssignmentsWithCredibilitiesRead apply(IAssignmentsWithCredibilities input) {
							return new AssignmentsWithCredibilitiesFiltering(input,
									Predicates.<Alternative> alwaysTrue());
						}
					}));
		}
		final XMCDA xmcda = getXMCDA(getSourceAssignments());
		if (xmcda == null) {
			m_allAssignmentsWithCredibilities = Collections.emptyMap();
		} else {
			final List<XAlternativesAffectations> xAlternativesAffectationsList = xmcda
					.getAlternativesAffectationsList();
			final XMCDAAssignments xmcdaAssignments = new XMCDAAssignments();
			m_allAssignmentsWithCredibilities = xmcdaAssignments
					.readAllWithCredibilities(xAlternativesAffectationsList);
		}
		return Collections.unmodifiableMap(Maps.transformValues(m_allAssignmentsWithCredibilities,
				new Function<IAssignmentsWithCredibilities, IAssignmentsWithCredibilitiesRead>() {
					@Override
					public IAssignmentsWithCredibilitiesRead apply(IAssignmentsWithCredibilities input) {
						return new AssignmentsWithCredibilitiesFiltering(input, Predicates.<Alternative> alwaysTrue());
					}
				}));
	}

	/**
	 * <p>
	 * Reads the coalitions per decision maker from the dedicated source, or
	 * from the the main source if the dedicated source is not set, or retrieves
	 * the results of the previous read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Map<DecisionMaker, Coalitions> readAllCoalitions() throws IOException, XmlException, InvalidInputException {
		if (m_allCoalitions != null) {
			return Collections
					.unmodifiableMap(Maps.transformValues(m_allCoalitions, new Function<Coalitions, Coalitions>() {
						@Override
						public Coalitions apply(Coalitions input) {
							return CoalitionsUtils.asReadView(input);
						}
					}));
		}

		final XMCDA xmcda = getXMCDA(getSourceCoalitions());
		if (xmcda == null) {
			m_allCoalitions = Collections.emptyMap();
		} else {
			final List<XCriteriaSet> xCriteriaSetList = xmcda.getCriteriaSetList();
			final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
			if (xmcdaCriteria.mightBeCoalitionsPerDecisionMaker(xCriteriaSetList)) {
				m_allCoalitions = xmcdaCriteria.readAllCoalitions(xCriteriaSetList);
			} else {
				m_allCoalitions = Collections.emptyMap();
			}
		}

		return Collections
				.unmodifiableMap(Maps.transformValues(m_allCoalitions, new Function<Coalitions, Coalitions>() {
					@Override
					public Coalitions apply(Coalitions input) {
						return CoalitionsUtils.asReadView(input);
					}
				}));
	}

	/**
	 * <p>
	 * Reads the assignments from the dedicated source, or from the the main
	 * source if the dedicated source is not set, or retrieves the results of
	 * the previous read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 * <p>
	 * This method also reads other data as is required to transform the
	 * original assignments read into the requested type. If no differentiated
	 * assignments information is found, equal assignments for each decision
	 * makers in the set of read decison makers are returned from the set of
	 * shared assignments.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Map<DecisionMaker, IOrderedAssignmentsRead> readAllOrderedAssignments()
			throws IOException, XmlException, InvalidInputException {
		readAllAssignmentsToMultiple();
		final Map<DecisionMaker, IOrderedAssignmentsRead> allOrderedAssignments = Maps.newLinkedHashMap();
		if (m_allAssignments.isEmpty()) {
			final IAssignmentsRead source = m_problemReader.readAssignments();
			final IOrderedAssignments ordered = AssignmentsFactory.newOrderedAssignments();
			ordered.setCategories(readCategories());
			try {
				AssignmentsUtils.copyAssignmentsToOrderedTarget(source, ordered);
			} catch (InvalidInputException exc) {
				error(exc.getMessage());
			}
			for (DecisionMaker dm : readDms()) {
				allOrderedAssignments.put(dm, ordered);
			}
		} else {
			for (DecisionMaker dm : m_allAssignments.keySet()) {
				final IAssignmentsToMultiple assignmentsToMultiple = m_allAssignments.get(dm);
				final IOrderedAssignments ordered = AssignmentsFactory.newOrderedAssignments();
				ordered.setCategories(readCategories());
				try {
					final IAssignments assignmentsSingle = AssignmentsFactory
							.newAssignmentsFromMultiple(assignmentsToMultiple);
					AssignmentsUtils.copyAssignmentsToOrderedTarget(assignmentsSingle, ordered);
				} catch (InvalidInputException exc) {
					error(exc.getMessage());
				}
				allOrderedAssignments.put(dm, ordered);
			}
		}
		return allOrderedAssignments;
	}

	/**
	 * TODO comment from other, similar, method.
	 */
	public Map<DecisionMaker, IOrderedAssignmentsToMultipleRead> readAllOrderedAssignmentsToMultiple()
			throws IOException, XmlException, InvalidInputException {
		readAllAssignmentsToMultiple();
		final Map<DecisionMaker, IOrderedAssignmentsToMultipleRead> allOrderedAssignments = Maps.newLinkedHashMap();
		if (m_allAssignments.isEmpty()) {
			final IAssignmentsToMultipleRead source = m_problemReader.readAssignmentsToMultiple();
			final IOrderedAssignmentsToMultiple ordered = AssignmentsFactory.newOrderedAssignmentsToMultiple();
			ordered.setCategories(readCategories());
			try {
				AssignmentsUtils.copyAssignmentsToMultipleToOrderedTarget(source, ordered);
			} catch (InvalidInputException exc) {
				error(exc.getMessage());
			}
			for (DecisionMaker dm : readDms()) {
				allOrderedAssignments.put(dm, ordered);
			}
		} else {
			for (DecisionMaker dm : m_allAssignments.keySet()) {
				final IAssignmentsToMultiple assignmentsToMultiple = m_allAssignments.get(dm);
				final IOrderedAssignmentsToMultiple ordered = AssignmentsFactory.newOrderedAssignmentsToMultiple();
				ordered.setCategories(readCategories());
				try {
					AssignmentsUtils.copyAssignmentsToMultipleToOrderedTarget(assignmentsToMultiple, ordered);
				} catch (InvalidInputException exc) {
					error(exc.getMessage());
				}
				allOrderedAssignments.put(dm, ordered);
			}
		}
		return allOrderedAssignments;
	}

	/**
	 * TODO comment from other, similar, method.
	 */
	public Map<DecisionMaker, IOrderedAssignmentsWithCredibilities> readAllOrderedAssignmentsWithCredibilities()
			throws IOException, XmlException, InvalidInputException {
		readAllAssignmentsWithCredibilities();
		final Map<DecisionMaker, IOrderedAssignmentsWithCredibilities> allOrderedAssignments = Maps.newLinkedHashMap();
		if (m_allAssignmentsWithCredibilities.isEmpty()) {
			final IAssignmentsWithCredibilitiesRead source = m_problemReader.readAssignmentsWithCredibilities();
			final IOrderedAssignmentsWithCredibilities ordered = AssignmentsFactory
					.newOrderedAssignmentsWithCredibilities();
			ordered.setCategories(readCategories());
			try {
				AssignmentsUtils.copyAssignmentsWithCredibilitiesToOrderedTarget(source, ordered);
			} catch (InvalidInputException exc) {
				error(exc.getMessage());
			}
			for (DecisionMaker dm : readDms()) {
				allOrderedAssignments.put(dm, ordered);
			}
		} else {
			for (DecisionMaker dm : m_allAssignmentsWithCredibilities.keySet()) {
				final IAssignmentsWithCredibilities assignmentsWithCredibilities = m_allAssignmentsWithCredibilities
						.get(dm);
				final IOrderedAssignmentsWithCredibilities ordered = AssignmentsFactory
						.newOrderedAssignmentsWithCredibilities();
				ordered.setCategories(readCategories());
				try {
					AssignmentsUtils.copyAssignmentsWithCredibilitiesToOrderedTarget(assignmentsWithCredibilities,
							ordered);
				} catch (InvalidInputException exc) {
					error(exc.getMessage());
				}
				allOrderedAssignments.put(dm, ordered);
			}
		}
		return allOrderedAssignments;
	}

	/**
	 * <p>
	 * Reads the profiles evaluations per decision maker from the dedicated
	 * source, or from the the main source if the dedicated source is not set,
	 * or retrieves the results of the previous read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Map<DecisionMaker, EvaluationsRead> readAllProfilesEvaluations()
			throws IOException, XmlException, InvalidInputException {
		if (m_allEvaluations != null) {
			return Collections.unmodifiableMap(
					Maps.transformValues(m_allEvaluations, new Function<Evaluations, EvaluationsRead>() {
						@Override
						public EvaluationsRead apply(Evaluations input) {
							return EvaluationsUtils.getFilteredView(input, null, null);
						}
					}));
		}

		final XMCDA xmcda = getXMCDA(getSourceProfilesEvaluations());
		if (xmcda == null) {
			m_allEvaluations = Collections.emptyMap();
		} else {
			final List<XPerformanceTable> xPerformanceTableList = xmcda.getPerformanceTableList();
			final XMCDAEvaluations xmcdaEvaluations = new XMCDAEvaluations();
			/** TODO think about the difference between these two states. */
			if (m_problemReader.getAlternativesParsingMethod() == AlternativesParsingMethod.SEEK_CONCEPT
					|| getAlternativesParsingMethod() == AlternativesParsingMethod.USE_MARKING) {
				xmcdaEvaluations.setConceptToRead(XAlternativeType.FICTIVE);
			}
			if (xmcdaEvaluations.hasNames(xPerformanceTableList)) {
				m_allEvaluations = xmcdaEvaluations.readPerDecisionMaker(xPerformanceTableList);
			} else {
				m_allEvaluations = Collections.emptyMap();
			}
		}

		return Collections
				.unmodifiableMap(Maps.transformValues(m_allEvaluations, new Function<Evaluations, EvaluationsRead>() {
					@Override
					public EvaluationsRead apply(Evaluations input) {
						return EvaluationsUtils.getFilteredView(input, null, null);
					}
				}));
	}

	/**
	 * <p>
	 * Reads the alternatives from the dedicated source, or from the the main
	 * source if the dedicated source is not set, or retrieves the results of
	 * the previous read if it ended successfully.
	 * </p>
	 * <p>
	 * The returned set iteration order matches the order of the source.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Set<Alternative> readAlternatives() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readAlternatives();
	}

	/**
	 * <p>
	 * Reads the alternatives evaluations from the dedicated source, or from the
	 * the main source if the dedicated source is not set, or retrieves the
	 * results of the previous read if it ended successfully. This method
	 * returns every evaluations found in the relevant source. To restrict these
	 * evaluations to those concerning the real alternatives, or to those
	 * concerning the profiles, the {@link EvaluationsUtils} class may be
	 * useful.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public EvaluationsRead readAlternativesEvaluations() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readAlternativesEvaluations();
	}

	/**
	 * <p>
	 * Reads the categories from the dedicated source, or from the the main
	 * source if the dedicated source is not set, or retrieves the results of
	 * the previous read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public NavigableSet<Category> readCategories() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readCategories();
	}

	/**
	 * <p>
	 * Reads the categories, together with their relations with the profiles
	 * defining them, from the dedicated source, or from the the main source if
	 * the dedicated source is not set, or retrieves the results of the previous
	 * read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public CatsAndProfs readCategoriesProfiles() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readCategoriesProfiles();
	}

	/**
	 * <p>
	 * Reads the coalitions from the dedicated source, or from the the main
	 * source if the dedicated source is not set, or retrieves the results of
	 * the previous read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Coalitions readCoalitions() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readCoalitions();
	}

	/**
	 * <p>
	 * Reads the criteria from the dedicated source, or from the the main source
	 * if the dedicated source is not set, or retrieves the results of the
	 * previous read if it ended successfully. This method also sets the scales
	 * and thresholds in this object.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Set<Criterion> readCriteria() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readCriteria();
	}

	/**
	 * <p>
	 * Reads the decision makers from the dedicated source, or from the the main
	 * source if the dedicated source is not set, or retrieves the results of
	 * the previous read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Set<DecisionMaker> readDms() throws IOException, XmlException, InvalidInputException {
		if (m_dms != null) {
			return Collections.unmodifiableSet(m_dms);
		}
		final XMCDA xmcda = getXMCDA(m_sourceDms);
		if (xmcda == null) {
			m_dms = Collections.emptySet();
		} else {
			final List<XMethodParameters> xMethodParametersList = xmcda.getMethodParametersList();
			final XMethodParameters xMethodParameters = getUniqueOrZero(xMethodParametersList);
			if (xMethodParameters == null) {
				m_dms = Collections.emptySet();
			} else {
				m_dms = new XMCDADecisionMakers().read(xMethodParameters);
			}
		}
		return Collections.unmodifiableSet(m_dms);
	}

	/**
	 * <p>
	 * Reads the evaluations from the dedicated source, or from the the main
	 * source if the dedicated source is not set, or retrieves the results of
	 * the previous read if it ended successfully. This method returns every
	 * evaluations found in the relevant source, with no distinction between
	 * REAL and FICTIVE alternatives.
	 * </p>
	 * <p>
	 * This method may not be used if the dedicated source for alternatives
	 * evaluations and the dedicated source for profiles evaluations are both
	 * set and are not equal, because in such a situation which source to use is
	 * undetermined. In positive terms, at least one dedicated source must be
	 * <code>null</code> or they must be equal. If both dedicated sources are
	 * <code>null</code>, this method uses the main source. If it is
	 * <code>null</code> as well, an empty object is returned.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public EvaluationsRead readEvaluationsIgnoreConcept() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readEvaluationsIgnoreConcept();
	}

	public void readGroupPreferencesTo(final IGroupSortingPreferences preferences)
			throws IOException, XmlException, InvalidInputException {
		SortingProblemUtils.copyDataToTarget(m_problemReader.readSortingData(false), preferences);

		final Set<DecisionMaker> dms = readDms();
		preferences.getDms().addAll(dms);

		final Coalitions sharedCoalitions = readCoalitions();
		final Map<DecisionMaker, Coalitions> allCoalitions = readAllCoalitions();
		final Set<DecisionMaker> havingCoalitions = allCoalitions.keySet();
		final SetView<DecisionMaker> unknownCoalitions = Sets.difference(havingCoalitions, preferences.getDms());
		if (unknownCoalitions.size() >= 1) {
			error("Unknown " + unknownCoalitions.iterator().next() + " having coalitions.");
		}
		if (sharedCoalitions.isEmpty()) {
			for (DecisionMaker dm : allCoalitions.keySet()) {
				final Coalitions coalitions = allCoalitions.get(dm);
				preferences.setCoalitions(dm, coalitions);
			}
		} else {
			final SetView<DecisionMaker> withoutCoalitions = Sets.difference(preferences.getDms(), havingCoalitions);
			if (withoutCoalitions.size() >= 1 && havingCoalitions.size() >= 1) {
				error("Has no coalitions: " + withoutCoalitions.iterator().next()
						+ " although shared coalitions were found.");
			}
			for (DecisionMaker dm : allCoalitions.keySet()) {
				final Coalitions coalitions = allCoalitions.get(dm);
				if (!coalitions.equals(sharedCoalitions)) {
					error("Found coalitions for " + dm + " that are not equal to the shared coalitions.");
				}
			}
			preferences.setSharedCoalitions(sharedCoalitions);
		}

		final Map<DecisionMaker, EvaluationsRead> allProfilesEvaluations = readAllProfilesEvaluations();

		if (allProfilesEvaluations.isEmpty()) {
			final EvaluationsRead profilesEvaluations = readProfilesEvaluations();
			preferences.setSharedProfilesEvaluations(profilesEvaluations);
		} else {
			for (DecisionMaker dm : allProfilesEvaluations.keySet()) {
				final EvaluationsRead profilesEvaluations = allProfilesEvaluations.get(dm);
				preferences.setProfilesEvaluations(dm, profilesEvaluations);
			}
		}

		/** Individual thresholds parsing not implemented. */
		final Thresholds sharedThresholds = readThresholds();
		preferences.setSharedThresholds(sharedThresholds);
	}

	/**
	 * <p>
	 * Retrieves an aggregator object that gather several object type. The
	 * various sources set in this object are read from and gathered into the
	 * returned type. Results from successful reads are re-used instead of read
	 * again. In particular, if informations from all sources have been read
	 * already, no read occurs.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public IGroupSortingResults readGroupResults() throws XmlException, IOException, InvalidInputException {
		final IGroupSortingResults results = SortingProblemUtils.newGroupResults();
		readGroupPreferencesTo(results);

		final Map<DecisionMaker, ? extends IOrderedAssignmentsRead> allAssignments = readAllOrderedAssignments();
		for (DecisionMaker dm : allAssignments.keySet()) {
			final IOrderedAssignmentsRead assignments = allAssignments.get(dm);
			results.getDms().add(dm);
			final IOrderedAssignments target = results.getAssignments(dm);
			AssignmentsUtils.copyOrderedAssignmentsToTarget(assignments, target);
		}

		return results;
	}

	/**
	 * TODO comment from other similar method.
	 */
	public IGroupSortingResultsToMultiple readGroupResultsToMultiple()
			throws XmlException, IOException, InvalidInputException {
		/**
		 * TODO should reflect readGroupResults, dont read preferences from
		 * problem reader.
		 */
		final IGroupSortingResultsToMultiple results = ProblemFactory.newGroupSortingResultsToMultiple();

		SortingProblemUtils.copyDataToTarget(m_problemReader.readSortingData(), results);
		final ISortingPreferences individualData = m_problemReader.readSortingPreferences();

		final Set<DecisionMaker> dms = readDms();
		results.getDms().addAll(dms);

		final Map<DecisionMaker, IOrderedAssignmentsToMultipleRead> allAssignments = readAllOrderedAssignmentsToMultiple();
		for (DecisionMaker dm : allAssignments.keySet()) {
			final IOrderedAssignmentsToMultipleRead assignments = allAssignments.get(dm);
			results.getDms().add(dm);
			final IOrderedAssignmentsToMultiple target = results.getAssignments(dm);
			AssignmentsUtils.copyOrderedAssignmentsToMultipleToTarget(assignments, target);
		}

		final Coalitions sharedCoalitions = readCoalitions();
		final Map<DecisionMaker, Coalitions> allCoalitions = readAllCoalitions();
		final Set<DecisionMaker> havingCoalitions = allCoalitions.keySet();
		final SetView<DecisionMaker> unknownCoalitions = Sets.difference(havingCoalitions, results.getDms());
		if (unknownCoalitions.size() >= 1) {
			error("Unknown " + unknownCoalitions.iterator().next() + " having coalitions.");
		}
		if (sharedCoalitions.isEmpty()) {
			for (DecisionMaker dm : allCoalitions.keySet()) {
				final Coalitions coalitions = allCoalitions.get(dm);
				results.setCoalitions(dm, coalitions);
			}
		} else {
			final SetView<DecisionMaker> withoutCoalitions = Sets.difference(results.getDms(), havingCoalitions);
			if (withoutCoalitions.size() >= 1 && havingCoalitions.size() >= 1) {
				error("Has no coalitions: " + withoutCoalitions.iterator().next()
						+ " although shared coalitions were found.");
			}
			for (DecisionMaker dm : allCoalitions.keySet()) {
				final Coalitions coalitions = allCoalitions.get(dm);
				if (!coalitions.equals(sharedCoalitions)) {
					error("Found coalitions for " + dm + " that are not equal to the shared coalitions.");
				}
			}
			results.setSharedCoalitions(sharedCoalitions);
		}

		final EvaluationsRead sharedProfilesEvaluations = individualData.getProfilesEvaluations();
		final Map<DecisionMaker, EvaluationsRead> allProfilesEvaluations = readAllProfilesEvaluations();

		if (sharedProfilesEvaluations.isEmpty()) {
			for (DecisionMaker dm : allProfilesEvaluations.keySet()) {
				final EvaluationsRead profilesEvaluations = allProfilesEvaluations.get(dm);
				results.setProfilesEvaluations(dm, profilesEvaluations);
			}
		} else {
			for (DecisionMaker dm : allProfilesEvaluations.keySet()) {
				final EvaluationsRead profilesEvaluations = allProfilesEvaluations.get(dm);
				if (!profilesEvaluations.equals(sharedProfilesEvaluations)) {
					error("Found profiles evaluations for " + dm
							+ " that are not equal to the shared profiles evaluations.");
				}
			}
			results.setSharedProfilesEvaluations(sharedProfilesEvaluations);
		}

		/** Individual thresholds parsing not implemented. */
		final Thresholds sharedThresholds = readThresholds();
		results.setSharedThresholds(sharedThresholds);

		return results;
	}

	/**
	 * TODO comment from other similar method.
	 */
	public IGroupSortingResultsWithCredibilities readGroupResultsWithCredibilities()
			throws XmlException, IOException, InvalidInputException {
		/**
		 * TODO should reflect readGroupResults, dont read preferences from
		 * problem reader.
		 */
		final IGroupSortingResultsWithCredibilities results = ProblemFactory.newGroupSortingResultsWithCredibilities();

		SortingProblemUtils.copyDataToTarget(m_problemReader.readSortingData(), results);
		final ISortingPreferences individualData = m_problemReader.readSortingPreferences();

		final Set<DecisionMaker> dms = readDms();
		results.getDms().addAll(dms);

		final Map<DecisionMaker, IOrderedAssignmentsWithCredibilities> allAssignments = readAllOrderedAssignmentsWithCredibilities();
		for (DecisionMaker dm : allAssignments.keySet()) {
			final IOrderedAssignmentsWithCredibilitiesRead assignments = allAssignments.get(dm);
			results.getDms().add(dm);
			final IOrderedAssignmentsWithCredibilities target = results.getAssignments(dm);
			AssignmentsUtils.copyOrderedAssignmentsWithCredibilitiesToTarget(assignments, target);
		}

		final Coalitions sharedCoalitions = readCoalitions();
		final Map<DecisionMaker, Coalitions> allCoalitions = readAllCoalitions();
		final Set<DecisionMaker> havingCoalitions = allCoalitions.keySet();
		final SetView<DecisionMaker> unknownCoalitions = Sets.difference(havingCoalitions, results.getDms());
		if (unknownCoalitions.size() >= 1) {
			error("Unknown " + unknownCoalitions.iterator().next() + " having coalitions.");
		}
		if (sharedCoalitions.isEmpty()) {
			for (DecisionMaker dm : allCoalitions.keySet()) {
				final Coalitions coalitions = allCoalitions.get(dm);
				results.setCoalitions(dm, coalitions);
			}
		} else {
			final SetView<DecisionMaker> withoutCoalitions = Sets.difference(results.getDms(), havingCoalitions);
			if (withoutCoalitions.size() >= 1 && havingCoalitions.size() >= 1) {
				error("Has no coalitions: " + withoutCoalitions.iterator().next()
						+ " although shared coalitions were found.");
			}
			for (DecisionMaker dm : allCoalitions.keySet()) {
				final Coalitions coalitions = allCoalitions.get(dm);
				if (!coalitions.equals(sharedCoalitions)) {
					error("Found coalitions for " + dm + " that are not equal to the shared coalitions.");
				}
			}
			results.setSharedCoalitions(sharedCoalitions);
		}

		final EvaluationsRead sharedProfilesEvaluations = individualData.getProfilesEvaluations();
		final Map<DecisionMaker, EvaluationsRead> allProfilesEvaluations = readAllProfilesEvaluations();

		if (sharedProfilesEvaluations.isEmpty()) {
			for (DecisionMaker dm : allProfilesEvaluations.keySet()) {
				final EvaluationsRead profilesEvaluations = allProfilesEvaluations.get(dm);
				results.setProfilesEvaluations(dm, profilesEvaluations);
			}
		} else {
			for (DecisionMaker dm : allProfilesEvaluations.keySet()) {
				final EvaluationsRead profilesEvaluations = allProfilesEvaluations.get(dm);
				if (!profilesEvaluations.equals(sharedProfilesEvaluations)) {
					error("Found profiles evaluations for " + dm
							+ " that are not equal to the shared profiles evaluations.");
				}
			}
			results.setSharedProfilesEvaluations(sharedProfilesEvaluations);
		}

		/** Individual thresholds parsing not implemented. */
		final Thresholds sharedThresholds = readThresholds();
		results.setSharedThresholds(sharedThresholds);

		return results;
	}

	/**
	 * <p>
	 * Reads the profiles from the dedicated source, or from the the main source
	 * if the dedicated source is not set, or retrieves the results of the
	 * previous read if it ended successfully.
	 * </p>
	 * <p>
	 * The returned set iteration order matches the order of the source.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Set<Alternative> readProfiles() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readProfiles();
	}

	/**
	 * <p>
	 * Reads the profiles evaluations from the dedicated source, or from the the
	 * main source if the dedicated source is not set, or retrieves the results
	 * of the previous read if it ended successfully. This method returns every
	 * evaluations found in the relevant source. To restrict these evaluations
	 * to those concerning the profiles, the {@link EvaluationsUtils} class may
	 * be useful.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public EvaluationsRead readProfilesEvaluations() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readProfilesEvaluations();
	}

	/**
	 * <p>
	 * Reads the scales from the dedicated source, or from the the main source
	 * if the dedicated source is not set, or retrieves the results of the
	 * previous read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Map<Criterion, Interval> readScales() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readScales();
	}

	/**
	 * <p>
	 * Reads the thresholds from the dedicated source, or from the the main
	 * source if the dedicated source is not set, or retrieves the results of
	 * the previous read if it ended successfully.
	 * </p>
	 * <p>
	 * In case of unexpected data, an InvalidInputException is thrown if this
	 * object follows the {@link ErrorManagement#THROW} strategy, otherwise, non
	 * conforming informations will be skipped.
	 * </p>
	 *
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             reader, or while parsing the source.
	 * @throws XmlException
	 *             if an exception related to the xml correctness of the source
	 *             happens while parsing the source, including if the given
	 *             source does not contain a valid XMCDA document.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Thresholds readThresholds() throws IOException, XmlException, InvalidInputException {
		return m_problemReader.readThresholds();
	}

	/**
	 * Sets the parsing method used to read alternatives.
	 *
	 * @param alternativesParsingMethod
	 *            not <code>null</code>.
	 */
	public void setAlternativesParsingMethod(AlternativesParsingMethod alternativesParsingMethod) {
		m_problemReader.setAlternativesParsingMethod(alternativesParsingMethod);
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
	 * Sets the dedicated source used to read the evaluations of the
	 * alternatives.
	 *
	 * @param sourceAlternativesEvaluations
	 *            <code>null</code> for not set.
	 */
	public void setSourceAlternativesEvaluations(ByteSource sourceAlternativesEvaluations) {
		m_problemReader.setSourceAlternativesEvaluations(sourceAlternativesEvaluations);
		clearCache();
	}

	/**
	 * Sets the dedicated source used to read assignments.
	 *
	 * @param source
	 *            <code>null</code> for not set.
	 */
	public void setSourceAssignments(ByteSource source) {
		m_problemReader.setSourceAssignments(source);
		clearCache();
	}

	/**
	 * Sets the dedicated source used to read the categories.
	 *
	 * @param source
	 *            <code>null</code> for not set.
	 */
	public void setSourceCategories(ByteSource source) {
		m_problemReader.setSourceCategories(source);
		clearCache();
	}

	/**
	 * Sets the dedicated source used to read the categories and associated
	 * profiles.
	 *
	 * @param sourceCategoriesProfiles
	 *            <code>null</code> for not set.
	 */
	public void setSourceCategoriesProfiles(ByteSource sourceCategoriesProfiles) {
		m_problemReader.setSourceCategoriesProfiles(sourceCategoriesProfiles);
		clearCache();
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
	 * Sets the dedicated source used to read the decision makers.
	 *
	 * @param source
	 *            <code>null</code> for not set.
	 */
	public void setSourceDms(ByteSource source) {
		m_sourceDms = source;
		clearCache();
	}

	/**
	 * Sets the main source used to read all types of objects for which no
	 * dedicated source is set.
	 *
	 * @param sourceMain
	 *            <code>null</code> for not set.
	 */
	public void setSourceMain(ByteSource sourceMain) {
		m_problemReader.setSourceMain(sourceMain);
		clearCache();
	}

	/**
	 * Sets the dedicated source used to read the profiles.
	 *
	 * @param sourceProfiles
	 *            <code>null</code> for not set.
	 */
	public void setSourceProfiles(ByteSource sourceProfiles) {
		m_problemReader.setSourceProfiles(sourceProfiles);
		clearCache();
	}

	/**
	 * Sets the dedicated source used to read the profiles evaluations.
	 *
	 * @param source
	 *            <code>null</code> for not set.
	 */
	public void setSourceProfilesEvaluations(ByteSource source) {
		m_problemReader.setSourceProfilesEvaluations(source);
		clearCache();
	}

	private void init() {
		m_sourceDms = null;

		clearCache();
	}
}
