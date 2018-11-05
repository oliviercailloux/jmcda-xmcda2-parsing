package org.decisiondeck.jmcda.persist.xmcda2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.utils.matrix.Matrixes;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.utils.ExportSettings;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeOnCriteriaPerformances;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeType;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValue;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Methods for reading and writing evaluations informations (called performance
 * tables in XMCDA terms) from and to XMCDA fragments.
 *
 * @author Olivier Cailloux
 *
 */
public class XMCDAEvaluations extends XMCDAHelperWithVarious {

	private X2Concept m_conceptToRead;

	private X2Concept m_conceptToWrite;

	private final ExportSettings m_exportSettings = new ExportSettings();

	/**
	 * Creates a new object which will use the default error management strategy
	 * {@link ErrorManagement#THROW}.
	 */
	public XMCDAEvaluations() {
		super();
		m_conceptToWrite = null;
		m_conceptToRead = null;
	}

	/**
	 * Creates a new object delegating error management to the given error
	 * manager in case of unexpected data read.
	 * 
	 * @param errorsManager
	 *            not <code>null</code>.
	 */
	public XMCDAEvaluations(XMCDAErrorsManager errorsManager) {
		super(errorsManager);
		m_conceptToWrite = null;
		m_conceptToRead = null;
	}

	public X2Concept getConceptToWrite() {
		return m_conceptToWrite;
	}

	/**
	 * <p>
	 * Tests whether the given XMCDA fragment possibly contains evaluations
	 * informations per decision maker. If the concept to read is set, reads
	 * only the performance tables that are marked appropriately.
	 * </p>
	 * 
	 * @param xPerformanceTables
	 *            not <code>null</code>.
	 * @return <code>true</code> iff the given collection is not empty and its
	 *         elements each have their name set.
	 * @see #setConceptToRead
	 */
	public boolean hasNames(Collection<XPerformanceTable> xPerformanceTables) {
		checkNotNull(xPerformanceTables);
		if (xPerformanceTables.isEmpty()) {
			return false;
		}
		for (XPerformanceTable xPerformanceTable : xPerformanceTables) {
			final String concept = xPerformanceTable.getMcdaConcept();
			if (!readable(concept)) {
				continue;
			}
			final String dmId = xPerformanceTable.getName();
			if (dmId == null || dmId.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * <p>
	 * Retrieves all the evaluations contained into the given XMCDA fragments.
	 * If the concept to read is set to a non <code>null</code> value, reads
	 * only the performance tables that are marked appropriately.
	 * </p>
	 * <p>
	 * In case of unexpected data, an exception is thrown if this object follows
	 * the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
	 * informations will be skipped.
	 * </p>
	 * 
	 * 
	 * @param xPerformanceTables
	 *            not <code>null</code>.
	 * @return not <code>null</code>.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 * @see #setConceptToRead
	 */
	public Evaluations read(Collection<XPerformanceTable> xPerformanceTables) throws InvalidInputException {
		checkNotNull(xPerformanceTables);
		Evaluations allEvaluations = EvaluationsUtils.newEvaluationMatrix();
		for (XPerformanceTable xPerformanceTable : xPerformanceTables) {
			final String concept = xPerformanceTable.getMcdaConcept();
			if (readable(concept)) {
				final Evaluations evaluations = read(xPerformanceTable);
				final Set<Alternative> duplicates = Matrixes.getDiscordingRows(allEvaluations, evaluations);
				if (!duplicates.isEmpty()) {
					error("Found distinct duplicated alternatives: " + duplicates
							+ ", ignoring all enclosing evaluations.");
					continue;
				}
				allEvaluations = EvaluationsUtils.merge(allEvaluations, evaluations);
			}
		}
		return allEvaluations;
	}

	/**
	 * <p>
	 * Retrieves the evaluations contained into the given XMCDA fragment.
	 * </p>
	 * <p>
	 * In case of unexpected data, an exception is thrown if this object follows
	 * the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
	 * informations will be skipped.
	 * </p>
	 * 
	 * 
	 * @param xPerformanceTable
	 *            not <code>null</code>.
	 * @return not <code>null</code>.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Evaluations read(XPerformanceTable xPerformanceTable) throws InvalidInputException {
		checkNotNull(xPerformanceTable);
		final Evaluations evaluations = EvaluationsUtils.newEvaluationMatrix();
		final List<XAlternativeOnCriteriaPerformances> xAlternativePerformancesList = xPerformanceTable
				.getAlternativePerformancesList();
		for (final XAlternativeOnCriteriaPerformances xAlternativePerformances : xAlternativePerformancesList) {
			if (!xAlternativePerformances.isSetAlternativeID()) {
				continue;
			}
			final String alternativeId = xAlternativePerformances.getAlternativeID();
			final Alternative alternative = new Alternative(alternativeId);
			final List<XAlternativeOnCriteriaPerformances.Performance> xPerformanceList = xAlternativePerformances
					.getPerformanceList();
			for (final XAlternativeOnCriteriaPerformances.Performance xAlternativePerformance : xPerformanceList) {
				if (!xAlternativePerformance.isSetCriterionID() || !xAlternativePerformance.isSetValue()) {
					continue;
				}
				final String criterionId = xAlternativePerformance.getCriterionID();
				final Criterion criterion = new Criterion(criterionId);
				if (evaluations.getEntry(alternative, criterion) != null) {
					error("Duplicate evaluation for " + alternative + ", " + criterion + " at "
							+ xAlternativePerformance + ".");
					continue;
				}
				final XValue xValue = xAlternativePerformance.getValue();
				final Double evaluation = readDouble(xValue);
				if (evaluation == null) {
					continue;
				}
				evaluations.put(alternative, criterion, evaluation.doubleValue());
			}
		}
		return evaluations;
	}

	/**
	 * <p>
	 * Retrieves the evaluations, per decision maker, contained into the given
	 * XMCDA fragments. Iteration order of the returned map matches that of the
	 * given collection. If the concept to read is set, reads only the
	 * performance tables that are marked appropriately.
	 * </p>
	 * <p>
	 * In case of unexpected data, an exception is thrown if this object follows
	 * the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
	 * informations will be skipped.
	 * </p>
	 * 
	 * 
	 * @param xPerformanceTables
	 *            not <code>null</code>.
	 * @return not <code>null</code>.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 * @see #setConceptToRead
	 */
	public Map<DecisionMaker, Evaluations> readPerDecisionMaker(Collection<XPerformanceTable> xPerformanceTables)
			throws InvalidInputException {
		final Map<DecisionMaker, Evaluations> allEvaluations = Maps.newLinkedHashMap();
		for (XPerformanceTable xPerformanceTable : xPerformanceTables) {
			final String concept = xPerformanceTable.getMcdaConcept();
			if (!readable(concept)) {
				continue;
			}
			final String name = xPerformanceTable.getName();
			if (name == null || name.isEmpty()) {
				error("Expected decision maker name at " + xPerformanceTable + ".");
				continue;
			}
			final Evaluations evaluations = read(xPerformanceTable);
			allEvaluations.put(new DecisionMaker(name), evaluations);
		}
		return allEvaluations;
	}

	public void setAlternativesOrder(Collection<Alternative> alternativesOrder) {
		m_exportSettings.setAlternativesOrder(alternativesOrder);
	}

	public void setConceptToRead(X2Concept conceptToRead) {
		m_conceptToRead = conceptToRead;
	}

	public void setConceptToRead(XAlternativeType.Enum conceptToRead) {
		m_conceptToRead = convert(conceptToRead);
	}

	public void setConceptToWrite(X2Concept conceptToWrite) {
		m_conceptToWrite = conceptToWrite;
	}

	public void setCriteriaOrder(Collection<Criterion> criteriaOrder) {
		m_exportSettings.setCriteriaOrder(criteriaOrder);
	}

	public void setDmsOrder(Collection<DecisionMaker> dmsOrder) {
		m_exportSettings.setDmsOrder(dmsOrder);
	}

	/**
	 * Retrieves the given evaluations informations as an XMCDA fragment.
	 * 
	 * @param evaluations
	 *            not <code>null</code>, not empty.
	 * @return not <code>null</code>.
	 */
	public XPerformanceTable write(EvaluationsRead evaluations) {
		checkNotNull(evaluations);
		checkArgument(evaluations.getValueCount() >= 1);
		final Set<Alternative> interOrderAlternatives = m_exportSettings.interOrderAlternatives(evaluations.getRows());
		checkArgument(interOrderAlternatives.size() >= 1);
		final Set<Criterion> interOrderCriteria = m_exportSettings.interOrderCriteria(evaluations.getColumns());
		checkArgument(interOrderCriteria.size() >= 1);

		final XPerformanceTable xPerformanceTable = XMCDA.Factory.newInstance().addNewPerformanceTable();
		if (m_conceptToWrite != null) {
			xPerformanceTable.setMcdaConcept(m_conceptToWrite.toString().toUpperCase(Locale.ENGLISH));
		}
		for (final Alternative alternative : interOrderAlternatives) {
			final XAlternativeOnCriteriaPerformances xAlternativePerformances = xPerformanceTable
					.addNewAlternativePerformances();
			xAlternativePerformances.setAlternativeID(alternative.getId());
			for (final Criterion criteria : interOrderCriteria) {
				final Double entry = evaluations.getEntry(alternative, criteria);
				if (entry == null) {
					continue;
				}
				final double value = entry.doubleValue();
				final XAlternativeOnCriteriaPerformances.Performance xAlternativePerformance = xAlternativePerformances
						.addNewPerformance();
				xAlternativePerformance.setCriterionID(criteria.getId());
				xAlternativePerformance.addNewValue().setReal((float) value);
			}
		}
		return xPerformanceTable;
	}

	/**
	 * Retrieves the given evaluations informations as a collection of XMCDA
	 * fragment, per decision maker.
	 * 
	 * @param allEvaluations
	 *            not <code>null</code>, no <code>null</code> entries, no empty
	 *            entries.
	 * @return not <code>null</code>.
	 */
	public Collection<XPerformanceTable> write(Map<DecisionMaker, EvaluationsRead> allEvaluations) {
		checkNotNull(allEvaluations);
		final Set<XPerformanceTable> xPerformanceTables = Sets.newLinkedHashSet();
		for (DecisionMaker dm : m_exportSettings.interOrderDms(allEvaluations.keySet())) {
			final EvaluationsRead evaluations = allEvaluations.get(dm);
			final XPerformanceTable xPerformanceTable = write(evaluations);
			xPerformanceTable.setName(dm.getId());
			xPerformanceTables.add(xPerformanceTable);
		}
		return xPerformanceTables;
	}

	private X2Concept convert(XAlternativeType.Enum concept) {
		if (concept == null) {
			return null;
		}
		switch (concept.intValue()) {
		case XAlternativeType.INT_REAL:
			return X2Concept.REAL;
		case XAlternativeType.INT_FICTIVE:
			return X2Concept.FICTIVE;
		default:
			throw new IllegalStateException();
		}
	}

	private boolean readable(String concept) {
		if (m_conceptToRead == null) {
			return true;
		}
		return m_conceptToRead.matches(concept);
	}
}
