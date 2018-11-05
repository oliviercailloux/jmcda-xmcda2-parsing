package org.decisiondeck.jmcda.persist.xmcda2;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.thresholds.ThresholdsUtils;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.CoalitionsUtils;
import org.decision_deck.jmcda.structure.weights.Weights;
import org.decision_deck.jmcda.structure.weights.WeightsUtils;
import org.decision_deck.utils.collection.CollectionUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.utils.ExportSettings;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaSet;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaValues;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriterion;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriterionValue;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XFunction;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XScale;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XThresholds;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValue;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Methods for reading and writing criteria and related informations from and to XMCDA fragments.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDACriteria extends XMCDAHelperWithVarious {

    /**
     * A predicate that returns <code>true</code> if the given interval is not <code>null</code> and is continuous,
     * thus, has no step size.
     * 
     * @author Olivier Cailloux
     * 
     */
    public static class IsContinuousInterval implements Predicate<Interval> {
	@Override
	public boolean apply(Interval input) {
	    return input != null && input.getStepSize() == null;
	}
    }

    /**
     * <p>
     * Retrieves an XMCDA fragment containing the given criteria, together with scale and preference, indifference, veto
     * thresholds, if defined. If some scales or thresholds do not correspond to a criterion of the given source
     * criteria, it is not taken into account. The scales and thresholds informations are not expected to be complete,
     * some may be missing.
     * </p>
     * <p>
     * The scales used as values in the given scales map and corresponding to a criterion in the given set must be
     * continuous scales, thus must have no step size. Discrete scales are not currently supported.
     * </p>
     * 
     * @param source
     *            not <code>null</code>.
     * @param scales
     *            possibly <code>null</code>; may miss elements.
     * @param thresholds
     *            possibly <code>null</code>; may miss elements.
     * @return not <code>null</code>.
     * @see #write(Set)
     */
    static public XCriteria write(Set<Criterion> source, Map<Criterion, Interval> scales,
	    Thresholds thresholds) {
	Preconditions.checkNotNull(source);
	final XMCDACriteria writer = new XMCDACriteria();
	if (scales != null) {
	    writer.setScales(scales);
	}
	if (thresholds != null) {
	    writer.setPreferenceThresholds(thresholds.getPreferenceThresholds());
	    writer.setIndifferenceThresholds(thresholds.getIndifferenceThresholds());
	    writer.setVetoThresholds(thresholds.getVetoThresholds());
	}
	return writer.write(source);
    }

    /**
     * <p>
     * Retrieves an XMCDA fragment containing the given criteria, together with name, scale and preference,
     * indifference, veto thresholds, if set in this object. If some name, scale or threshold set in this object do not
     * correspond to a criterion of the given source criteria, it is not taken into account. The names, scales and
     * thresholds informations are not expected to be complete, some may be missing.
     * </p>
     * 
     * @param source
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @see #setNames
     * @see #setScales
     * @see #setPreferenceThresholds
     * @see #setIndifferenceThresholds
     * @see #setVetoThresholds
     */
    public XCriteria write(Set<Criterion> source) {
	Preconditions.checkNotNull(source);
	final XCriteria xCriteria = XMCDADoc.XMCDA.Factory.newInstance().addNewCriteria();
	for (final Criterion criterion : source) {
	    final XCriterion xCriterion = xCriteria.addNewCriterion();
	    xCriterion.setId(criterion.getId());
	    final String name = m_names.get(criterion);
	    if (name != null) {
		xCriterion.setName(name);
	    }

	    final boolean isInactive = m_inactiveCriteria.contains(criterion);
	    if (isInactive) {
		xCriterion.addActive(false);
	    } else if (m_markActive) {
		xCriterion.addActive(true);
	    }

	    final Interval scale = m_continuousScales.get(criterion);
	    if (scale != null) {
		final XScale xScale = new XMCDAOrderedIntervals().write(scale);
		if (xScale != null) {
		    xCriterion.addNewScale().set(xScale);
		}
	    }

	    final Double pThresh = m_preferenceThresholds.get(criterion);
	    final Double iThresh = m_indifferenceThresholds.get(criterion);
	    final Double vThresh = m_vetoThresholds.get(criterion);
	    if (pThresh != null || iThresh != null || vThresh != null) {
		final XThresholds xmlThresh = xCriterion.addNewThresholds();
		if (pThresh != null) {
		    final XFunction xmlThreshFct = xmlThresh.addNewThreshold();
		    xmlThreshFct.setMcdaConcept(PREFERENCE_CONCEPT_STRING);
		    xmlThreshFct.addNewConstant().setReal((float) pThresh.doubleValue());
		}
		if (iThresh != null) {
		    final XFunction xmlThreshFct = xmlThresh.addNewThreshold();
		    xmlThreshFct.setMcdaConcept(INDIFFERENCE_CONCEPT_STRING);
		    xmlThreshFct.addNewConstant().setReal((float) iThresh.doubleValue());
		}
		if (vThresh != null) {
		    final XFunction xmlThreshFct = xmlThresh.addNewThreshold();
		    xmlThreshFct.setMcdaConcept(VETO_CONCEPT_STRING);
		    xmlThreshFct.addNewConstant().setReal((float) vThresh.doubleValue());
		}
	    }
	}
	return xCriteria;
    }

    private final Map<Criterion, String> m_names = CollectionUtils.newMapNoNull();
    private final Map<Criterion, Interval> m_continuousScales = Maps.filterValues(
	    CollectionUtils.<Criterion, Interval> newMapNoNull(), new IsContinuousInterval());

    private final Map<Criterion, Double> m_vetoThresholds = CollectionUtils.newMapNoNull();

    private final Set<Criterion> m_criteria = Sets.newLinkedHashSet();
    private final Set<Criterion> m_inactiveCriteria = CollectionUtils.newHashSetNoNull();
    private final Map<Criterion, Double> m_indifferenceThresholds = CollectionUtils.newMapNoNull();
    private final Map<Criterion, Double> m_preferenceThresholds = CollectionUtils.newMapNoNull();
    private final ExportSettings m_exportSettings = new ExportSettings();
    public static final String INDIFFERENCE_CONCEPT_STRING = "ind";
    public static final String PREFERENCE_CONCEPT_STRING = "pref";
    public static final String VETO_CONCEPT_STRING = "veto";
    public static final String WEIGHTS_CONCEPT_STRING = "Importance";
    private boolean m_markActive;
    private static final String MAJORITY_THRESHOLD_CONCEPT_STRING = "majority threshold";

    /**
     * Retrieves a writeable view to the criteria names. The returned map has no <code>null</code> key, no
     * <code>null</code> values, and such entries may not be added to the map.
     * 
     * @return not <code>null</code>.
     */
    public Map<Criterion, String> getNames() {
	return m_names;
    }

    /**
     * TODO this should be a writeable view, other methods should be removed.
     * 
     * Retrieves a copy of the criteria thresholds.
     * 
     * @return not <code>null</code>.
     */
    public Thresholds getThresholds() {
	return ThresholdsUtils.newThresholds(m_preferenceThresholds, m_indifferenceThresholds, m_vetoThresholds);
    }

    /**
     * Sets the criteria names stored in this object as the given names. No reference is held to the given map.
     * 
     * @param names
     *            not <code>null</code>, may be empty, no <code>null</code> key or value.
     */
    public void setNames(Map<Criterion, String> names) {
	checkNotNull(names);
	m_names.clear();
	m_names.putAll(names);
    }

    /**
     * Retrieves a writeable view to the criteria preference thresholds. The returned map has no <code>null</code> key,
     * no <code>null</code> values, and such entries may not be added to the map.
     * 
     * @return not <code>null</code>.
     */
    public Map<Criterion, Double> getPreferenceThresholds() {
	return m_preferenceThresholds;
    }

    /**
     * Sets the criteria preference thresholds stored in this object as the given ones. No reference is held to the
     * given map.
     * 
     * @param preferenceThresholds
     *            not <code>null</code>, may be empty, must contain no <code>null</code> key or values.
     */
    public void setPreferenceThresholds(Map<Criterion, Double> preferenceThresholds) {
	checkNotNull(preferenceThresholds);
	m_preferenceThresholds.clear();
	m_preferenceThresholds.putAll(preferenceThresholds);
    }

    /**
     * Retrieves a writeable view to the criteria indifference thresholds. The returned map has no <code>null</code>
     * key, no <code>null</code> values, and such entries may not be added to the map.
     * 
     * @return not <code>null</code>.
     */
    public Map<Criterion, Double> getIndifferenceThresholds() {
	return m_indifferenceThresholds;
    }

    /**
     * Sets the criteria indifference thresholds stored in this object as the given ones. No reference is held to the
     * given map.
     * 
     * @param indifferenceThresholds
     *            not <code>null</code>, may be empty, must contain no <code>null</code> key or values.
     */
    public void setIndifferenceThresholds(Map<Criterion, Double> indifferenceThresholds) {
	checkNotNull(indifferenceThresholds);
	m_indifferenceThresholds.clear();
	m_indifferenceThresholds.putAll(indifferenceThresholds);
    }

    /**
     * Retrieves a writeable view to the criteria veto thresholds. The returned map has no <code>null</code> key, no
     * <code>null</code> values, and such entries may not be added to the map.
     * 
     * @return not <code>null</code>.
     */
    public Map<Criterion, Double> getVetoThresholds() {
	return m_vetoThresholds;
    }

    /**
     * Sets the criteria veto thresholds stored in this object as the given ones. No reference is held to the given map.
     * 
     * @param vetoThresholds
     *            not <code>null</code>, may be empty, must contain no <code>null</code> key or values.
     */
    public void setVetoThresholds(Map<Criterion, Double> vetoThresholds) {
	checkNotNull(vetoThresholds);
	m_vetoThresholds.clear();
	m_vetoThresholds.putAll(vetoThresholds);
    }

    /**
     * <p>
     * Retrieves the set of criteria contained into the given XMCDA fragment, except those marked as inactive. The
     * iteration order of the returned set matches the reading order.
     * </p>
     * <p>
     * This object keeps a copy of the informations just read: the names, the scales, the thresholds of the criteria, as
     * well as a copy of the set of criteria itself, and a copy of the inactive criteria. Previously existing
     * informations, that have been set or that have been read previously, are deleted before reading.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * 
     * @param xCriteria
     *            not <code>null</code>.
     * @return a copy of the set of criteria just read and kept in this object; not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #getCriteria()
     * @see #getNames()
     * @see #getScales()
     * @see #getPreferenceThresholds()
     * @see #getIndifferenceThresholds()
     * @see #getVetoThresholds()
     */
    public Set<Criterion> read(XCriteria xCriteria) throws InvalidInputException {
	m_continuousScales.clear();
	m_preferenceThresholds.clear();
	m_indifferenceThresholds.clear();
	m_vetoThresholds.clear();
	m_names.clear();
	m_criteria.clear();
	m_inactiveCriteria.clear();

	for (final XCriterion xCriterion : xCriteria.getCriterionList()) {
	    readInternal(xCriterion);
	}
	return Sets.newLinkedHashSet(m_criteria);
    }

    /**
     * <p>
     * Retrieves the set of criteria contained into the given XMCDA fragments, except for the criteria marked as
     * inactive. The iteration order of the returned set matches the reading order.
     * </p>
     * <p>
     * This object keeps a copy of the informations just read: the names, the scales, the thresholds of the criteria, as
     * well as a copy of the set of criteria itself and a separate copy of the inactive criteria. Previously existing
     * informations, that have been set or that have been read previously, are deleted before reading.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * 
     * @param xAllCriteria
     *            not <code>null</code>.
     * @return a copy of the set of criteria just read and kept in this object; not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #getCriteria()
     * @see #getNames()
     * @see #getScales()
     * @see #getPreferenceThresholds()
     * @see #getIndifferenceThresholds()
     * @see #getVetoThresholds()
     */
    public Set<Criterion> readAll(Collection<XCriteria> xAllCriteria) throws InvalidInputException {
	m_continuousScales.clear();
	m_preferenceThresholds.clear();
	m_indifferenceThresholds.clear();
	m_vetoThresholds.clear();
	m_names.clear();
	m_criteria.clear();
	m_inactiveCriteria.clear();

	for (XCriteria xCriteria : xAllCriteria) {
	    for (final XCriterion xCriterion : xCriteria.getCriterionList()) {
		readInternal(xCriterion);
	    }
	}
	return Sets.newLinkedHashSet(m_criteria);
    }

    /**
     * <p>
     * Retrieves a criterion corresponding to the given XMCDA fragment, or <code>null</code> if it is marked as
     * inactive.
     * </p>
     * <p>
     * This object keeps a copy of the associated informations, if found: name, scale, thresholds of the criterion. The
     * scale is necessarily set, possibly to a simple real scale if no information is found. The returned criterion is
     * also kept in this object, or is kept as an inactive criterion if it is marked as inactive. No defensive copy is
     * done.
     * </p>
     * <p>
     * This method expects, among others, that the given fragment has an id that does not already exist in the set of
     * criteria in this object, thus it should be cleared before a reading starts if the reading is to be independent of
     * previous readings. In case of unexpected data, an exception is thrown if this object follows the
     * {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xCriterion
     *            not <code>null</code>.
     * @return <code>null</code> iff the given criterion has no id or a duplicate id and this object does not follows
     *         the {@link ErrorManagement#THROW} strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #getCriteria()
     * @see #getNames()
     * @see #getScales()
     * @see #getPreferenceThresholds()
     * @see #getIndifferenceThresholds()
     * @see #getVetoThresholds()
     */
    private Criterion readInternal(XCriterion xCriterion) throws InvalidInputException {
	final String id = xCriterion.getId();
	if (id == null || id.isEmpty()) {
	    error("Found a criterion with no id.");
	    return null;
	}
	final Criterion criterion = new Criterion(id);
	if (m_criteria.contains(criterion)) {
	    error("Duplicate id: " + criterion + ".");
	    return null;
	}
	final String name;
	final String nameWritten = xCriterion.getName();
	if (nameWritten == null || nameWritten.isEmpty()) {
	    name = null;
	} else {
	    name = nameWritten;
	    m_names.put(criterion, name);
	}

	final List<XScale> xScaleList = xCriterion.getScaleList();
	final Interval scale;
	final XScale xScale = getUniqueOrZero(xScaleList);
	if (xScale == null) {
	    scale = null;
	} else {
	    final Interval read = new XMCDAOrderedIntervals().read(xScale);
	    scale = read == null ? null : read;
	}
	if (scale != null) {
	    m_continuousScales.put(criterion, scale);
	}

	final List<String> searchedConcepts = Arrays.asList(new String[] { PREFERENCE_CONCEPT_STRING,
		INDIFFERENCE_CONCEPT_STRING, VETO_CONCEPT_STRING });
	final Map<String, Double> thresholdsByConcept = findThresholds(xCriterion, criterion, searchedConcepts);

	final Double prefThresh = thresholdsByConcept.get(PREFERENCE_CONCEPT_STRING);
	if (prefThresh != null) {
	    m_preferenceThresholds.put(criterion, prefThresh);
	}
	final Double indThresh = thresholdsByConcept.get(INDIFFERENCE_CONCEPT_STRING);
	if (indThresh != null) {
	    m_indifferenceThresholds.put(criterion, indThresh);
	}
	final Double vetoThresh = thresholdsByConcept.get(VETO_CONCEPT_STRING);
	if (vetoThresh != null) {
	    m_vetoThresholds.put(criterion, vetoThresh);
	}

	final List<Boolean> activeList = xCriterion.getActiveList();
	final Boolean active = getUniqueOrZero(activeList);
	if (Boolean.FALSE.equals(active)) {
	    m_inactiveCriteria.add(criterion);
	    return null;
	}
	m_criteria.add(criterion);
	return criterion;
    }

    private Map<String, Double> findThresholds(XCriterion xCriterion, Criterion criterion, List<String> searchedConcepts)
	    throws InvalidInputException {
	final Map<String, Double> outThresholdsByConcept = CollectionUtils.newMapNoNull();
	for (final XThresholds thresholds : xCriterion.getThresholdsList()) {
	    for (final XFunction threshold : thresholds.getThresholdList()) {
		if (!threshold.isSetConstant()) {
		    error("Expected constrant threshold at " + threshold + " related to " + criterion + ".");
		    continue;
		}
		final String concept = threshold.getMcdaConcept();
		if (concept == null || concept.isEmpty()) {
		    error("Expected MCDA concept at " + threshold + " related to " + criterion + ".");
		    continue;
		}
		if (!searchedConcepts.contains(concept)) {
		    error("Unknown MCDA concept at " + threshold + " related to " + criterion + ".");
		    continue;
		}
		if (outThresholdsByConcept.containsKey(concept)) {
		    error("Found more than one threshold for " + criterion + " with concept " + concept + ".");
		    continue;
		}
		final Double value = readDouble(threshold.getConstant());
		if (value == null) {
		    continue;
		}
		outThresholdsByConcept.put(concept, value);
	    }
	}
	return outThresholdsByConcept;
    }

    /**
     * Retrieves a copy of the set of criteria read after the last call to {@link #read(XCriteria)}, with the iteration
     * order reflecting the order of reading. If no read has occurred yet, an empty set is returned.
     * 
     * @return not <code>null</code>.
     */
    public Set<Criterion> getCriteria() {
	return Sets.newLinkedHashSet(m_criteria);
    }

    /**
     * <p>
     * Retrieves the weights found in the given XMCDA fragment.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xCriteriaValues
     *            not <code>null</code>.
     * @return not <code>null</code>, may be empty.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Weights readWeights(XCriteriaValues xCriteriaValues) throws InvalidInputException {
	checkNotNull(xCriteriaValues);
	final Weights weights = WeightsUtils.newWeights();
	final List<XCriterionValue> xCriterionValueList = xCriteriaValues.getCriterionValueList();
	for (final XCriterionValue xCriterionValue : xCriterionValueList) {
	    if (!xCriterionValue.isSetCriterionID()) {
		error("Found a criterion value without id.");
		continue;
	    }
	    final String id = xCriterionValue.getCriterionID();
	    final Criterion criterion = new Criterion(id);
	    final List<XValue> xValueList = xCriterionValue.getValueList();
	    for (final XValue xValue : xValueList) {
		final Double real = readDouble(xValue);
		if (weights.get(criterion) != null) {
		    error("Value found for a criterion with id " + id + " having already a weight.");
		    continue;
		}
		if (real != null) {
		    weights.putWeight(criterion, real.doubleValue());
		}
	    }
	}
	return weights;
    }

    /**
     * <p>
     * Retrieves the weights from the XMCDA fragment among the given collection that has the MCDA concept corresponding
     * to the {@link #WEIGHTS_CONCEPT_STRING}.
     * </p>
     * <p>
     * This method expects, among others, that there is exactly one such XMCDA fragment among the given collection. In
     * case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xCriteriaValuesList
     *            not <code>null</code>.
     * @return not <code>null</code>, may be empty.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Weights readWeights(Collection<XCriteriaValues> xCriteriaValuesList) throws InvalidInputException {
	checkNotNull(xCriteriaValuesList);
	XCriteriaValues found = null;

	for (final XCriteriaValues xCriteriaValues : xCriteriaValuesList) {
	    final String concept = xCriteriaValues.getMcdaConcept();
	    if (WEIGHTS_CONCEPT_STRING.equals(concept)) {
		if (found != null) {
		    error("Found more than one list of criteria values corresponding to weights.");
		    return WeightsUtils.newWeights();
		}
		found = xCriteriaValues;
	    }
	}
	if (found == null) {
	    return WeightsUtils.newWeights();
	}
	return readWeights(found);
    }

    /**
     * <p>
     * Retrieves the weights found in the given XMCDA fragment.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xCriteriaSet
     *            not <code>null</code>.
     * @return not <code>null</code>, may be empty.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Weights readWeights(XCriteriaSet xCriteriaSet) throws InvalidInputException {
	checkNotNull(xCriteriaSet);
	final Weights weights = WeightsUtils.newWeights();
	final List<XCriteriaSet.Element> xmlElements = xCriteriaSet.getElementList();
	for (XCriteriaSet.Element element : xmlElements) {
	    final String id = element.getCriterionID();
	    final Criterion criterion = new Criterion(id);
	    final List<XValue> xValueList = element.getValueList();
	    for (XValue xValue : xValueList) {
		final Double value = readDouble(xValue);
		if (weights.get(criterion) != null) {
		    error("More than one element found for criterion " + id + ".");
		    continue;
		}
		if (value != null) {
		    weights.putWeight(criterion, value.doubleValue());
		}
	    }
	}
	return weights;
    }

    /**
     * <p>
     * Retrieves the coalitions, thus the weights and the majority threshold, per decision makers, found in the given
     * XMCDA fragments. The iteration order of the returned map matches that of the given collection.
     * </p>
     * <p>
     * This object expects, among others, the majority thresholds to be set. In case of unexpected data, an exception is
     * thrown if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations
     * will be skipped.
     * </p>
     * 
     * @param xAllCriteriaSet
     *            not <code>null</code>.
     * @return not <code>null</code>, with no <code>null</code> values, but possibly with empty values. If no unexpected
     *         content have been read, the values contain at least a majority threshold.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Map<DecisionMaker, Coalitions> readAllCoalitions(Collection<XCriteriaSet> xAllCriteriaSet)
	    throws InvalidInputException {
	checkNotNull(xAllCriteriaSet);
	final Map<DecisionMaker, Coalitions> allCoalitions = Maps.newLinkedHashMap();
	for (XCriteriaSet xCoalitions : xAllCriteriaSet) {
	    final String dmId = xCoalitions.getName();
	    if (dmId == null || dmId.isEmpty()) {
		error("No id found for coalitions " + xCoalitions + ".");
		continue;
	    }
	    final DecisionMaker dm = new DecisionMaker(dmId);
	    final Coalitions coalitions = readCoalitions(xCoalitions);
	    allCoalitions.put(dm, coalitions);
	}
	return allCoalitions;
    }

    /**
     * <p>
     * Tests whether the given XMCDA fragment possibly contains coalitions informations per decision maker.
     * </p>
     * 
     * @param xAllCriteriaSet
     *            not <code>null</code>.
     * @return <code>true</code> iff the given collection contains more than one element or contains exactly one element
     *         which has its name set.
     */
    public boolean mightBeCoalitionsPerDecisionMaker(Collection<XCriteriaSet> xAllCriteriaSet) {
	checkNotNull(xAllCriteriaSet);
	if (xAllCriteriaSet.size() >= 2) {
	    return true;
	}
	if (xAllCriteriaSet.isEmpty()) {
	    return false;
	}
	final XCriteriaSet xCoalitions = Iterables.getOnlyElement(xAllCriteriaSet);
	final String dmId = xCoalitions.getName();
	return (dmId != null && dmId.length() >= 1);
    }

    /**
     * <p>
     * Retrieves the coalitions, thus the weights and the majority threshold, found in the given XMCDA fragment.
     * </p>
     * <p>
     * This object expects, among others, the majority threshold to be set. In case of unexpected data, an exception is
     * thrown if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations
     * will be skipped.
     * </p>
     * 
     * @param xCriteriaSet
     *            not <code>null</code>.
     * @return not <code>null</code>; if no unexpected content have been read, contains at least a majority threshold.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Coalitions readCoalitions(XCriteriaSet xCriteriaSet) throws InvalidInputException {
	checkNotNull(xCriteriaSet);
	final Coalitions coalitions = CoalitionsUtils.newCoalitions();

	final Weights weights = readWeights(xCriteriaSet);
	for (Criterion criterion : weights.keySet()) {
	    final double weight = weights.getWeightBetter(criterion);
	    coalitions.putWeight(criterion, weight);
	}

	final List<XValue> xmlValueList = xCriteriaSet.getValueList();
	final Double value = readDouble(xmlValueList, xCriteriaSet.toString());
	if (value != null) {
	    coalitions.setMajorityThreshold(value.doubleValue());
	}
	return coalitions;
    }

    /**
     * Retrieves the XMCDA equivalent of the given coalitions.
     * 
     * @param source
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * 
     */
    public XCriteriaSet writeCoalitions(Coalitions source) {
	checkNotNull(source);
	final XCriteriaSet xCriteriaSet = write(source.getWeights());
	if (source.containsMajorityThreshold()) {
	    final XValue xValue = xCriteriaSet.addNewValue();
	    xValue.setMcdaConcept(MAJORITY_THRESHOLD_CONCEPT_STRING);
	    xValue.setReal((float) source.getMajorityThreshold());
	}
	return xCriteriaSet;
    }

    /**
     * Retrieves the XMCDA equivalent of the given collection of coalitions, as a collection of {@link XCriteriaSet},
     * each one being associated to the corresponding decision maker in the given input.
     * 
     * @param allCoalitions
     *            not <code>null</code>, with values not <code>null</code> and having each a majority threshold.
     * @return not <code>null</code>.
     * 
     */
    public List<XCriteriaSet> write(Map<DecisionMaker, Coalitions> allCoalitions) {
	checkNotNull(allCoalitions);
	final List<XCriteriaSet> xCoalitionsList = Lists.newLinkedList();
	for (DecisionMaker dm : m_exportSettings.interOrderDms(allCoalitions.keySet())) {
	    final Coalitions coalitions = allCoalitions.get(dm);
	    final XCriteriaSet xCoalitions = writeCoalitions(coalitions);
	    xCoalitions.setName(dm.getId());
	    xCoalitionsList.add(xCoalitions);
	}
	return xCoalitionsList;
    }

    /**
     * Retrieves the XMCDA equivalent of the given weights.
     * 
     * @param source
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XCriteriaSet write(Weights source) {
	checkNotNull(source);
	final XCriteriaSet xCriteriaSet = XMCDA.Factory.newInstance().addNewCriteriaSet();
	xCriteriaSet.setMcdaConcept(WEIGHTS_CONCEPT_STRING);

	for (final Criterion criterion : m_exportSettings.interOrderCriteria(source.keySet())) {
	    final XCriteriaSet.Element xElement = xCriteriaSet.addNewElement();
	    xElement.setCriterionID(criterion.getId());
	    final XValue xValue = xElement.addNewValue();
	    xValue.setReal((float) source.getWeightBetter(criterion));
	}

	return xCriteriaSet;
    }

    /**
     * Sets the criteria scales stored in this object as the given ones. No reference is held to the given map.
     * 
     * @param scales
     *            not <code>null</code>, may be empty, must contain only continuous scales, thus with no step size.
     */
    public void setScales(Map<Criterion, Interval> scales) {
	checkNotNull(scales);
	m_continuousScales.clear();
	m_continuousScales.putAll(scales);
    }

    /**
     * Retrieves a writeable view to the criteria scales. The returned map has no <code>null</code> key, no
     * <code>null</code> values, no continuous scales, and such entries may not be added to the map.
     * 
     * @return not <code>null</code>.
     */
    public Map<Criterion, Interval> getScales() {
	return m_continuousScales;
    }

    public void setCriteriaOrder(Collection<Criterion> criteriaOrder) {
	m_exportSettings.setCriteriaOrder(criteriaOrder);
    }

    public void setDmsOrder(Collection<DecisionMaker> dmsOrder) {
	m_exportSettings.setDmsOrder(dmsOrder);
    }

    /**
     * Retrieves the criteria stored in this object as inactive. The returned set is necessarily empty if no read or
     * {@link #setInactiveCriteria(Set)} occurred yet.
     * 
     * @return not <code>null</code>.
     */
    public Set<Criterion> getInactiveCriteria() {
	return Sets.newHashSet(m_inactiveCriteria);
    }

    /**
     * Stores the given criteria in this object. No reference is held to the given set.
     * 
     * @param inactiveCriteria
     *            not <code>null</code>, may be empty, no <code>null</code> entry.
     */
    public void setInactiveCriteria(Set<Criterion> inactiveCriteria) {
	Preconditions.checkNotNull(inactiveCriteria);
	m_inactiveCriteria.clear();
	m_inactiveCriteria.addAll(inactiveCriteria);
    }

    /**
     * @param markActive
     *            <code>true</code> to mark the criteria as active when they are not found in the inactive criteria).
     *            The default is to only mark inactive criteria.
     * @see #setInactiveCriteria(Set)
     */
    public void setMarkActive(boolean markActive) {
	m_markActive = markActive;
    }

    /**
     * Creates a new object which will use the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDACriteria() {
	this(new XMCDAErrorsManager());
    }

    /**
     * Creates a new object delegating error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDACriteria(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
	m_markActive = false;
    }

}
