package org.decisiondeck.jmcda.persist.xmcda2;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.MatrixesMC;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.utils.PredicateUtils;
import org.decision_deck.utils.matrix.Matrixes;
import org.decision_deck.utils.matrix.SparseMatrixD;
import org.decision_deck.utils.matrix.SparseMatrixDRead;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeReference;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesComparisons;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValue;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValues;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Methods for reading alternative matrixes from XMCDA fragments, and writing matrixes of alternatives to XMCDA.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDAAlternativesMatrix extends XMCDAHelperWithVarious {
    private final Set<Alternative> m_alternatives = Sets.newLinkedHashSet();
    private final Set<Criterion> m_criteria = Sets.newLinkedHashSet();

    /**
     * <p>
     * Retrieves the alternative matrixes corresponding to the given XMCDA information, by criteria.
     * </p>
     * <p>
     * The returned map has no empty matrixes as values <em>if no unexpected data was read</em>. If unexpected data was
     * read an empty matrix may have been created.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xAlternativesComparisons
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Map<Criterion, SparseAlternativesMatrixFuzzy> readAlternativesFuzzyMatrixesByCriteria(
	    XAlternativesComparisons xAlternativesComparisons) throws InvalidInputException {
	checkNotNull(xAlternativesComparisons);
	final Map<Criterion, SparseAlternativesMatrixFuzzy> matrixes = Maps.newHashMap();

	final XAlternativesComparisons.Pairs xPairs = xAlternativesComparisons.getPairs();
	final List<XAlternativesComparisons.Pairs.Pair> xPairList = xPairs.getPairList();
	for (final XAlternativesComparisons.Pairs.Pair xPair : xPairList) {
	    final Alternative initial = getInitial(xPair);
	    if (initial == null) {
		continue;
	    }
	    final Alternative terminal = getTerminal(xPair);
	    if (terminal == null) {
		continue;
	    }
	    final List<XValues> xValuesList = xPair.getValuesList();
	    for (final XValues xValues : xValuesList) {
		final List<XValue> xValueList = xValues.getValueList();
		for (final XValue xValue : xValueList) {
		    final String id = xValue.getId();
		    if (id == null || id.isEmpty()) {
			error("Found a value without an id.");
			continue;
		    }
		    final Criterion criterion = new Criterion(id);
		    final Double value = readDouble(xValue);
		    if (value == null) {
			continue;
		    }
		    if (!matrixes.containsKey(criterion)) {
			matrixes.put(criterion, MatrixesMC.newAlternativesFuzzy());
		    }
		    final SparseAlternativesMatrixFuzzy matrix = matrixes.get(criterion);
		    final Double entry = matrix.getEntry(initial, terminal);
		    if (entry != null) {
			error("More than one value found for " + initial + ", " + terminal + ", " + criterion + ".");
			break;
		    }
		    try {
			matrix.put(initial, terminal, value.doubleValue());
		    } catch (IllegalArgumentException exc) {
			error("Invalid value found at " + initial + ", " + terminal + ", " + criterion + ": " + value
				+ ".");
		    }
		}
	    }
	}
	return matrixes;
    }

    /**
     * Retrieves the XMCDA equivalent of the given matrix. The matrix may be empty: this produces a valid tag anyway.
     * 
     * @param matrix
     *            not <code>null</code>, may be incomplete.
     * @return not <code>null</code>.
     */
    public XAlternativesComparisons write(SparseMatrixDRead<Alternative, Alternative> matrix) {
	checkNotNull(matrix);
	final XAlternativesComparisons xAlternativesComparisons = XMCDA.Factory.newInstance()
		.addNewAlternativesComparisons();
	final Set<Alternative> rowsSorted = new TreeSet<Alternative>();
	rowsSorted.addAll(matrix.getRows());
	final Set<Alternative> colsSorted = new TreeSet<Alternative>();
	colsSorted.addAll(matrix.getColumns());
	final XAlternativesComparisons.Pairs pairs = xAlternativesComparisons.addNewPairs();
	s_logger.info("Pairs: {}", pairs);
	for (final Alternative initial : rowsSorted) {
	    s_logger.info("Initial: {}", initial);
	    final XAlternativeReference xInitial = XAlternativeReference.Factory.newInstance();
	    xInitial.setAlternativeID(initial.getId());
	    for (final Alternative terminal : colsSorted) {
		final Double entry = matrix.getEntry(initial, terminal);
		if (entry == null) {
		    continue;
		}
		final XAlternativeReference xTerminal = XAlternativeReference.Factory.newInstance();
		xTerminal.setAlternativeID(terminal.getId());
		final XAlternativesComparisons.Pairs.Pair xPair = pairs.addNewPair();
		xPair.setInitial(xInitial);
		xPair.setTerminal(xTerminal);
		xPair.addNewValue().setReal((float) entry.doubleValue());
	    }
	}
	return xAlternativesComparisons;
    }

    private static final Logger s_logger = LoggerFactory.getLogger(XMCDAAlternativesMatrix.class);

    /**
     * Retrieves the XMCDA equivalent of the given matrix.
     * 
     * @param matrixes
     *            not <code>null</code>, no <code>null</code> key or values. The matrixes in values may be incomplete.
     *            It is not required that they contain homogeneous sets of rows or columns.
     * @return not <code>null</code>.
     */
    public XAlternativesComparisons write(Map<Criterion, ? extends SparseMatrixDRead<Alternative, Alternative>> matrixes) {
	checkNotNull(matrixes);
	final XAlternativesComparisons xAlternativesComparisons = XMCDA.Factory.newInstance()
		.addNewAlternativesComparisons();
	final Set<Criterion> critsSorted = new TreeSet<Criterion>();
	critsSorted.addAll(matrixes.keySet());
	final Set<Alternative> rowsSorted = new TreeSet<Alternative>();
	final Set<Alternative> colsSorted = new TreeSet<Alternative>();
	for (Criterion crit : critsSorted) {
	    rowsSorted.addAll(matrixes.get(crit).getRows());
	    colsSorted.addAll(matrixes.get(crit).getColumns());
	}
	final XAlternativesComparisons.Pairs pairs = xAlternativesComparisons.addNewPairs();
	for (final Alternative initial : rowsSorted) {
	    final XAlternativeReference xInitial = XAlternativeReference.Factory.newInstance();
	    xInitial.setAlternativeID(initial.getId());
	    for (final Alternative terminal : colsSorted) {
		final XAlternativeReference xTerminal = XAlternativeReference.Factory.newInstance();
		xTerminal.setAlternativeID(terminal.getId());
		final XAlternativesComparisons.Pairs.Pair xPair = pairs.addNewPair();
		xPair.setInitial(xInitial);
		xPair.setTerminal(xTerminal);
		final XValues xValues = xPair.addNewValues();
		for (Criterion criterion : critsSorted) {
		    final Double entry = matrixes.get(criterion).getEntry(initial, terminal);
		    if (entry == null) {
			continue;
		    }
		    final XValue xValue = xValues.addNewValue();
		    xValue.setId(criterion.getId());
		    xValue.setReal((float) entry.doubleValue());
		}
	    }
	}
	return xAlternativesComparisons;
    }

    /**
     * <p>
     * Retrieves an alternative matrix corresponding to the given XMCDA information.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xAlternativesComparisons
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public SparseMatrixD<Alternative, Alternative> readAlternativesFloatMatrix(
	    XAlternativesComparisons xAlternativesComparisons) throws InvalidInputException {
	checkNotNull(xAlternativesComparisons);
	final SparseMatrixD<Alternative, Alternative> altMat = Matrixes.newSparseD();
	readInternal(xAlternativesComparisons, Predicates.<Double> alwaysTrue(), altMat);
	return altMat;
    }

    private void readInternal(XAlternativesComparisons xAlternativesComparisons,
 Predicate<Double> pValidator,
	    SparseMatrixD<Alternative, Alternative> out)
	    throws InvalidInputException {
	final XAlternativesComparisons.Pairs xPairs = xAlternativesComparisons.getPairs();
	final List<XAlternativesComparisons.Pairs.Pair> xPairList = xPairs.getPairList();
	for (final XAlternativesComparisons.Pairs.Pair xPair : xPairList) {
	    final Alternative altInit = getInitial(xPair);
	    if (altInit == null) {
		continue;
	    }
	    final Alternative altTerminal = getTerminal(xPair);
	    if (altTerminal == null) {
		continue;
	    }
	    final List<XValue> xValues = xPair.getValueList();
	    final XValue xValue = getUnique(xValues, xPair.toString());
	    if (xValue == null) {
		continue;
	    }
	    final Double value = readDouble(xValue);
	    if (value == null) {
		continue;
	    }
	    final Double entry = out.getEntry(altInit, altTerminal);
	    if (entry != null) {
		error("More than one value found for pair " + altInit + ", " + altTerminal + ".");
		continue;
	    }
	    if (!pValidator.apply(value)) {
		throw new InvalidInputException("Invalid value found at " + altInit + ", " + altTerminal + ": " + value
			+ ".");
	    }
	    out.put(altInit, altTerminal, value.doubleValue());
	}
    }

    private Alternative getTerminal(final XAlternativesComparisons.Pairs.Pair xPair) throws InvalidInputException {
	final XAlternativeReference xTerminal = xPair.getTerminal();
	if (xTerminal == null) {
	    error("Found a pair without terminal alternative.");
	    return null;
	}
	if (!xTerminal.isSetAlternativeID()) {
	    error("Found a pair without terminal alternative id.");
	    return null;
	}
	final String id = xTerminal.getAlternativeID();
	if (id == null || id.isEmpty()) {
	    error("Found a pair without terminal alternative id.");
	    return null;
	}
	final Alternative alternative = new Alternative(id);
	return alternative;
    }

    /**
     * <p>
     * Retrieves an alternative matrix corresponding to the given XMCDA information.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xAlternativesComparisons
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public SparseAlternativesMatrixFuzzy readAlternativesFuzzyMatrix(XAlternativesComparisons xAlternativesComparisons)
	    throws InvalidInputException {
	checkNotNull(xAlternativesComparisons);
	final SparseAlternativesMatrixFuzzy altMat = MatrixesMC.newAlternativesFuzzy();
	readInternal(xAlternativesComparisons, PredicateUtils.inBetween(0d, 1d), altMat);
	return altMat;
    }

    /**
     * Retrieves a writeable view of the set of alternatives stored in this object.
     * 
     * @return not <code>null</code>.
     */
    public Set<Alternative> getAlternatives() {
	return m_alternatives;
    }

    /**
     * Sets the alternatives stored in this object as the given ones. No reference is held to the given set.
     * 
     * @param alternatives
     *            not <code>null</code>, may be empty.
     */
    public void setAlternatives(Set<Alternative> alternatives) {
	checkNotNull(alternatives);
	m_alternatives.clear();
	m_alternatives.addAll(alternatives);
    }

    private Alternative getInitial(final XAlternativesComparisons.Pairs.Pair xPair) throws InvalidInputException {
	final XAlternativeReference xInitial = xPair.getInitial();
	if (xInitial == null) {
	    error("Found a pair without initial alternative.");
	    return null;
	}
	if (!xInitial.isSetAlternativeID()) {
	    error("Found a pair without initial alternative id.");
	    return null;
	}
	final String id = xInitial.getAlternativeID();
	if (id == null || id.isEmpty()) {
	    error("Found a pair without initial alternative id.");
	    return null;
	}
	final Alternative alternative = new Alternative(id);
	return alternative;
    }

    /**
     * Retrieves a writeable view of the set of criteria stored in this object.
     * 
     * @return not <code>null</code>.
     */
    public Set<Criterion> getCriteria() {
	return m_criteria;
    }

    /**
     * Sets the criteria stored in this object as the given ones. No reference is held to the given set.
     * 
     * @param criteria
     *            not <code>null</code>, may be empty.
     */
    public void setCriteria(Set<Criterion> criteria) {
	checkNotNull(criteria);
	m_criteria.clear();
	m_criteria.addAll(criteria);
    }

    /**
     * Creates a new object which will use the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDAAlternativesMatrix() {
	super();
    }

    /**
     * Creates a new object delegating error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDAAlternativesMatrix(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
    }

}
