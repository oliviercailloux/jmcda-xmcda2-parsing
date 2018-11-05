package org.decisiondeck.jmcda.persist.xmcda2.aggregates;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlException;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.thresholds.ThresholdsUtils;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.CoalitionsUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives.AlternativesParsingMethod;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeType;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaSet;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;

import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;

/**
 * <p>
 * A class to read classical MCDA problems defined in XMCDA documents. This class permits to read the following objects:
 * alternatives, criteria and criteria scales, evaluations of the alternatives over the criteria, coalitions (thus
 * weights and majority threshold), preference, indifference, and veto thresholds. This class supports the case of a
 * single decision maker.
 * </p>
 * 
 * @see org.decisiondeck.jmcda.persist.xmcda2
 * @author Olivier Cailloux
 * 
 */
public class XMCDAProblemReader extends XMCDAHelperWithVarious {

    /**
     * Clears the information cached in this class, resulting in the loss of any previously read information that had
     * been remembered by this class. After this method is called, using any read method results in an effective read of
     * the relevant source instead of a possible re-use of the cached data.
     */
    public void clearCache() {
	m_sourceVersion = null;

	m_alternatives = null;
	m_alternativesEvaluations = null;
	m_coalitions = null;
	m_criteria = null;
	m_scales = null;
	m_thresholds = null;
    }

    /**
     * Retrieves the XMCDA version of the documents read by this object.
     * 
     * @return <code>null</code> if not uniform or nothing read yet.
     */
    public String getSourceVersion() {
	return m_sourceVersion;
    }

    private ByteSource m_sourceAlternatives;

    /**
     * Retrieves the source dedicated to alternatives.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceAlternatives() {
	return m_sourceAlternatives;
    }

    /**
     * Sets the dedicated source used to read alternatives.
     * 
     * @param sourceAlternatives
     *            <code>null</code> for not set.
     */
    public void setSourceAlternatives(ByteSource sourceAlternatives) {
	m_sourceAlternatives = sourceAlternatives;
	clearCache();
    }

    /**
     * Retrieves the source dedicated to alternatives evaluations.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceAlternativesEvaluations() {
	return m_sourceAlternativesEvaluations;
    }

    /**
     * Sets the dedicated source used to read the evaluations.
     * 
     * @param sourceAlternativesEvaluations
     *            <code>null</code> for not set.
     */
    public void setSourceAlternativesEvaluations(ByteSource sourceAlternativesEvaluations) {
	m_sourceAlternativesEvaluations = sourceAlternativesEvaluations;
	clearCache();
    }

    /**
     * Retrieves the source dedicated to coalitions.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceCoalitions() {
	return m_sourceCoalitions;
    }

    /**
     * Sets the dedicated source used to read the coalitions.
     * 
     * @param sourceCoalitions
     *            <code>null</code> for not set.
     */
    public void setSourceCoalitions(ByteSource sourceCoalitions) {
	m_sourceCoalitions = sourceCoalitions;
	clearCache();
    }

    /**
     * Retrieves the source dedicated to criteria.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceCriteria() {
	return m_sourceCriteria;
    }

    /**
     * Sets the dedicated source used to read the criteria.
     * 
     * @param sourceCriteria
     *            <code>null</code> for not set.
     */
    public void setSourceCriteria(ByteSource sourceCriteria) {
	m_sourceCriteria = sourceCriteria;
	clearCache();
    }

    /**
     * Retrieves the main source. This is used to read any type of object when the dedicated source is not set.
     * 
     * @return <code>null</code> if not set.
     */
    public ByteSource getSourceMain() {
	return m_sourceMain;
    }

    /**
     * Sets the main source used to read all types of objects for which no dedicated source is set.
     * 
     * @param sourceMain
     *            <code>null</code> for not set.
     */
    public void setSourceMain(ByteSource sourceMain) {
	m_sourceMain = sourceMain;
	clearCache();
    }

    /**
     * Retrieves the parsing method used to read alternatives.
     * 
     * @return the parsing method.
     */
    public AlternativesParsingMethod getAlternativesParsingMethod() {
	return m_alternativesParsingMethod;
    }

    /**
     * Sets the parsing method used to read alternatives.
     * 
     * @param alternativesParsingMethod
     *            not <code>null</code>.
     */
    public void setAlternativesParsingMethod(AlternativesParsingMethod alternativesParsingMethod) {
	checkNotNull(alternativesParsingMethod);
	m_alternativesParsingMethod = alternativesParsingMethod;
    }

    /**
     * Retrieves the XMCDA document from the given source <em>or</em> from the main source if the given source is
     * <code>null</code>. Ensures that it contains an XMCDA document conforming to the XMCDA schema.
     * 
     * @param source
     *            may be <code>null</code>, in which case the main source in this object must be non <code>null</code>.
     * @return <code>null</code> iff the given source and the main source are <code>null</code>.
     * @throws IOException
     *             if an exception happens while opening or closing the given reader, or while parsing the source.
     * @throws XmlException
     *             if an exception related to the contents of the source happens while parsing the source, including if
     *             the given source does not contain a valid XMCDA document.
     */
    public XMCDA getXMCDA(ByteSource source) throws IOException, XmlException {
	if (source == null && m_sourceMain == null) {
	    return null;
	}
	final ByteSource realSource;
	if (source == null) {
	    realSource = m_sourceMain;
	} else {
	    realSource = source;
	}

	final XMCDAReadUtils xmcdaUtils = new XMCDAReadUtils();
	final XMCDA xmcda = xmcdaUtils.getXMCDA(realSource);

	final String lastVersionRead = xmcdaUtils.getLastVersionRead();
	if (m_sourceVersion == null) {
	    m_sourceVersion = lastVersionRead;
	} else {
	    if (!m_sourceVersion.equals(lastVersionRead)) {
		m_sourceVersion = null;
	    }
	}

	return xmcda;
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
	if (m_alternatives != null) {
	    return Collections.unmodifiableSet(m_alternatives);
	}
	final XMCDA xmcda = getXMCDA(m_sourceAlternatives);
	if (xmcda == null) {
	    m_alternatives = Collections.emptySet();
	} else {
	    final List<XAlternatives> xAlternativesList = xmcda.getAlternativesList();
	    final AlternativesParsingMethod parsingMethod;
	    if (m_alternativesParsingMethod == null) {
		if (xAlternativesList.size() <= 1) {
		    parsingMethod = AlternativesParsingMethod.TAKE_ALL;
		} else {
		    parsingMethod = AlternativesParsingMethod.SEEK_CONCEPT;
		}
	    } else {
		parsingMethod = m_alternativesParsingMethod;
	    }
	    m_alternatives = XMCDAAlternatives.read(xAlternativesList, XAlternativeType.REAL, parsingMethod);
	}
	return Collections.unmodifiableSet(m_alternatives);
    }

    private ByteSource m_sourceAlternativesEvaluations;
    private ByteSource m_sourceCoalitions;
    private ByteSource m_sourceCriteria;
    private ByteSource m_sourceMain;
    /**
     * <code>null</code> for "try to be clever". TODO: add a parsing method to define that state.
     */
    private AlternativesParsingMethod m_alternativesParsingMethod;
    private Set<Alternative> m_alternatives;
    private Evaluations m_alternativesEvaluations;
    private Coalitions m_coalitions;
    private Set<Criterion> m_criteria;
    private Map<Criterion, Interval> m_scales;
    private Thresholds m_thresholds;
    private String m_sourceVersion;

    /**
     * Creates a new reader which will use the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDAProblemReader() {
	this(new XMCDAErrorsManager());
    }

    /**
     * Creates a new reader with a main source. The reader will use the default error management strategy
     * {@link ErrorManagement#THROW}.
     * 
     * @param mainSource
     *            not <code>null</code>.
     */
    public XMCDAProblemReader(ByteSource mainSource) {
	this(mainSource, new XMCDAErrorsManager());
    }

    /**
     * Creates a new reader delegating error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDAProblemReader(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
	init(null);
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
    public XMCDAProblemReader(ByteSource mainSource, XMCDAErrorsManager errorsManager) {
	super(errorsManager);
	checkNotNull(mainSource);
	init(mainSource);
    }

    private void init(ByteSource mainSource) {
	m_sourceAlternatives = null;
	m_sourceAlternativesEvaluations = null;
	m_sourceCoalitions = null;
	m_sourceCriteria = null;

	m_sourceMain = mainSource;

	m_alternativesParsingMethod = null;

	clearCache();
    }

    /**
     * <p>
     * Reads the alternatives evaluations from the dedicated source, or from the the main source if the dedicated source
     * is not set, or retrieves the results of the previous read if it ended successfully.
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
    public EvaluationsRead readAlternativeEvaluations() throws IOException, XmlException, InvalidInputException {
	if (m_alternativesEvaluations != null) {
	    return EvaluationsUtils.getFilteredView(m_alternativesEvaluations, Predicates.<Alternative> alwaysTrue(),
		    null);
	}
	final XMCDA xmcda = getXMCDA(m_sourceAlternativesEvaluations);
	if (xmcda == null) {
	    m_alternativesEvaluations = EvaluationsUtils.newEvaluationMatrix();
	} else {
	    final List<XPerformanceTable> xPerformanceTableList = xmcda.getPerformanceTableList();
	    final XMCDAEvaluations xmcdaEvaluations = new XMCDAEvaluations();
	    if (getAlternativesParsingMethod() == AlternativesParsingMethod.SEEK_CONCEPT
		    || getAlternativesParsingMethod() == AlternativesParsingMethod.USE_MARKING) {
		xmcdaEvaluations.setConceptToRead(XAlternativeType.REAL);
	    }
	    m_alternativesEvaluations = xmcdaEvaluations.read(xPerformanceTableList);
	}
	return EvaluationsUtils.getFilteredView(m_alternativesEvaluations, Predicates.<Alternative> alwaysTrue(), null);
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
	if (m_coalitions != null) {
	    return CoalitionsUtils.asReadView(m_coalitions);
	}
	final XMCDA xmcda = getXMCDA(m_sourceCoalitions);
	if (xmcda == null) {
	    m_coalitions = CoalitionsUtils.newCoalitions();
	} else {
	    final List<XCriteriaSet> xCriteriaSetList = xmcda.getCriteriaSetList();
	    final XCriteriaSet xCriteriaSet = xCriteriaSetList.size() != 1 ? null : Iterables
		    .getOnlyElement(xCriteriaSetList);
	    if (xCriteriaSet == null) {
		m_coalitions = CoalitionsUtils.newCoalitions();
	    } else {
		final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
		m_coalitions = xmcdaCriteria.readCoalitions(xCriteriaSet);
	    }
	}
	return CoalitionsUtils.asReadView(m_coalitions);
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
	if (m_criteria != null) {
	    return Collections.unmodifiableSet(m_criteria);
	}
	final XMCDA xmcda = getXMCDA(m_sourceCriteria);
	if (xmcda == null) {
	    m_criteria = Collections.emptySet();
	    m_scales = Collections.emptyMap();
	    m_thresholds = ThresholdsUtils.newThresholds();
	} else {
	    final List<XCriteria> xCriteriaList = xmcda.getCriteriaList();
	    final XCriteria xCriteria = getUniqueOrZero(xCriteriaList);
	    if (xCriteria == null) {
		m_criteria = Collections.emptySet();
		m_scales = Collections.emptyMap();
		m_thresholds = ThresholdsUtils.newThresholds();
	    } else {
		final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
		m_criteria = xmcdaCriteria.read(xCriteria);
		m_scales = xmcdaCriteria.getScales();
		m_thresholds = xmcdaCriteria.getThresholds();
	    }
	}
	return Collections.unmodifiableSet(m_criteria);
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
	readCriteria();
	return Collections.unmodifiableMap(m_scales);
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
	readCriteria();
	return ThresholdsUtils.getReadView(m_thresholds);
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
	final ByteSource effectiveSource1 = source1 == null ? getSourceMain() : source1;
	final ByteSource effectiveSource2 = source2 == null ? getSourceMain() : source2;
	return Objects.equal(effectiveSource1, effectiveSource2);
    }

    /**
     * <p>
     * Reads the alternatives evaluations from the dedicated source, or from the the main source if the dedicated source
     * is not set, or retrieves the results of the previous read if it ended successfully. This method returns every
     * evaluations found, with no distinction between REAL and FICTIVE alternatives.
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
    public EvaluationsRead readEvaluationsIgnoreConcept() throws IOException, XmlException, InvalidInputException {
	checkState(getAlternativesParsingMethod() == null
		|| getAlternativesParsingMethod() == AlternativesParsingMethod.TAKE_ALL);
        if (m_alternativesEvaluations != null) {
            return EvaluationsUtils.getFilteredView(m_alternativesEvaluations, Predicates.<Alternative> alwaysTrue(),
        	    null);
        }
        final XMCDA xmcda = getXMCDA(m_sourceAlternativesEvaluations);
        if (xmcda == null) {
	    m_alternativesEvaluations = EvaluationsUtils.newEvaluationMatrix();
        } else {
            final List<XPerformanceTable> xPerformanceTableList = xmcda.getPerformanceTableList();
            final XMCDAEvaluations xmcdaEvaluations = new XMCDAEvaluations();
            m_alternativesEvaluations = xmcdaEvaluations.read(xPerformanceTableList);
        }
        return EvaluationsUtils.getFilteredView(m_alternativesEvaluations, Predicates.<Alternative> alwaysTrue(), null);
    }

}
