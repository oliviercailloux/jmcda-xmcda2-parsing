package org.decisiondeck.jmcda.persist.xmcda2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.utils.StringUtils;
import org.decision_deck.utils.collection.CollectionUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternative;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeType;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeType.Enum;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Methods for reading alternatives from XMCDA fragments, and writing (sets of) alternatives to XMCDA.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDAAlternatives extends XMCDAHelperWithVarious {
    static public enum AlternativesParsingMethod {
	/**
	 * Reads all alternatives in all the tags found.
	 */
	TAKE_ALL,
	/**
	 * Reads only the alternatives in the tags named after the appropriate concept.
	 */
	SEEK_CONCEPT,
	/**
	 * Reads all the tags found, consider only the alternatives inside these tags that are marked appropriately.
	 */
	USE_MARKING
    }

    /**
     * Creates a new object which will use the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDAAlternatives() {
	this(new XMCDAErrorsManager());
    }

    /**
     * Creates a new object delegating error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDAAlternatives(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
	m_activeAlternatives = false;
	m_currentConcept = null;
    }

    /**
     * <p>
     * Returns some of the alternatives found in the given XMCDA fragments, reading only the alternatives found in tags
     * named after the appropriate concept, depending on the requested type. The alternative names, if found, are stored
     * in this object, as well as the information of whether an alternative is marked fictive. Alternatives marked as
     * inactive are stored in this object but are not included in the returned set. Any previously stored information is
     * deleted.
     * </p>
     * <p>
     * The returned set iteration order respects the order of the source.
     * </p>
     * <p>
     * This method expects each alternative to have an id set, and maximum one type. In case of unexpected data, an
     * exception is thrown if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
     * informations will be skipped.
     * </p>
     * 
     * @param xAlternativesList
     *            not <code>null</code>.
     * @param type
     *            not <code>null</code>.
     * 
     * @return may be empty, but not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Set<Alternative> readUsingConcept(Collection<XAlternatives> xAlternativesList, X2Concept type)
	    throws InvalidInputException {
	checkNotNull(xAlternativesList);
	checkNotNull(type);
	m_fictiveStatus.clear();
	m_names.clear();
	m_inactiveAlternatives.clear();

	final Set<Alternative> allAlternatives = Sets.newLinkedHashSet();
	for (final XAlternatives xAlternatives : xAlternativesList) {
	    final String concept = xAlternatives.getMcdaConcept();
	    if (type.matches(concept)) {
		final Set<Alternative> alternatives = readAllInternal(xAlternatives);
		allAlternatives.addAll(alternatives);
	    }
	}
	return allAlternatives;
    }

    /**
     * <p>
     * Returns a new set containing all the alternatives in the given set that are marked in this object as being
     * fictive, or the complementary, those that are not marked as being fictive, depending on the value of the boolean
     * parameter. See also {@link #getFictiveStatus()}. Note that this is not symmetric: this method considers an
     * alternative as being by default non fictive.
     * </p>
     * <p>
     * The iteration order of the returned set matches that of the given set of alternatives.
     * </p>
     * 
     * @param alternatives
     *            not <code>null</code>.
     * @param fictitious
     *            <code>true</code> to receive only the alternatives specifically marked as fictive, <code>false</code>
     *            to receive only alternatives not specifically marked as fictive.
     * 
     * @return a new set, not <code>null</code>, may be empty.
     */
    public Set<Alternative> keepOnly(Set<Alternative> alternatives, boolean fictitious) {
	Preconditions.checkNotNull(alternatives);
	final Set<Alternative> fictives = getMarkedAlternatives(X2Concept.FICTIVE);
	final Set<Alternative> keep;
	if (fictitious) {
	    keep = fictives;
	} else {
	    keep = Sets.difference(alternatives, fictives);
	}
	return Sets.intersection(alternatives, keep).copyInto(Sets.<Alternative> newLinkedHashSet());
    }

    private Map<Alternative, String> m_names = CollectionUtils.newMapNoNull();

    private final Set<Alternative> m_inactiveAlternatives = CollectionUtils.newHashSetNoNull();

    /**
     * (previously:) Entries with a value <code>true</code> are the alternatives marked fictive, entries with a value
     * <code>false</code> are marked real. Missing entries have no mark.
     */
    private final Map<Alternative, X2Concept> m_fictiveStatus = CollectionUtils.newMapNoNull();

    private boolean m_activeAlternatives;

    private X2Concept m_currentConcept;

    /**
     * <p>
     * Retrieves a writeable view to the status of the alternatives, observed during a {@link #read} or stored after a
     * {@link #setFictiveStatus(Set, Set)}. The returned map has no <code>null</code> key, no <code>null</code> values,
     * and such entries may not be added to the map.
     * </p>
     * 
     * @return not <code>null</code>.
     */
    public Map<Alternative, X2Concept> getFictiveStatus() {
	return m_fictiveStatus;
    }

    /**
     * Marks the given alternatives as real or fictive, for storing in this object. No reference is held to the given
     * sets. The sets must be disjoints.
     * 
     * @param realAlternatives
     *            not <code>null</code>, may be empty, no <code>null</code> entry.
     * @param fictiveAlternatives
     *            not <code>null</code>, may be empty, no <code>null</code> entry.
     * @see #writeAlternatives
     */
    public void setFictiveStatus(Set<Alternative> realAlternatives, Set<Alternative> fictiveAlternatives) {
	Preconditions.checkNotNull(realAlternatives);
	Preconditions.checkNotNull(fictiveAlternatives);
	checkArgument(Sets.intersection(realAlternatives, fictiveAlternatives).isEmpty());
	m_fictiveStatus.clear();
	for (Alternative real : realAlternatives) {
	    m_fictiveStatus.put(real, X2Concept.REAL);
	}
	for (Alternative fictive : fictiveAlternatives) {
	    m_fictiveStatus.put(fictive, X2Concept.FICTIVE);
	}
    }

    /**
     * <p>
     * Returns an alternative corresponding to the given XMCDA alternative. The name, if found, is stored in this
     * object. If the given alternative is marked as being fictive, or as being non fictive, this is also stored in this
     * object. If the alternative is marked as inactive, <code>null</code> is returned, but the alternative is added to
     * the inactive alternatives stored in this object.
     * </p>
     * <p>
     * This method expects the alternative to have an id set, and maximum one type. In case of unexpected data, an
     * exception is thrown if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, this method
     * returns <code>null</code> if the id could not be read.
     * </p>
     * 
     * @param xAlternative
     *            not <code>null</code>.
     * @return possibly <code>null</code> if unexpected content has been read and this object follows a permissive
     *         strategy, or if the given xml alternative is marked as inactive.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #getInactiveAlternatives
     */
    public Alternative read(XAlternative xAlternative) throws InvalidInputException {
	final String id = xAlternative.getId();
	if (id == null) {
	    error("Expected an id: " + xAlternative + ".");
	    return null;
	}

	final Alternative alternative = new Alternative(id);

	final String name;
	if (xAlternative.isSetName()) {
	    final String nameWritten = xAlternative.getName();
	    if (nameWritten == null || nameWritten.isEmpty()) {
		name = null;
	    } else {
		name = nameWritten;
	    }
	} else {
	    name = null;
	}
	if (name != null) {
	    m_names.put(alternative, name);
	}

	final List<XAlternativeType.Enum> xTypeList = xAlternative.getTypeList();
	final Enum xType = getUniqueOrZero(xTypeList);
	final X2Concept type;
	if (xType == null) {
	    type = m_currentConcept == null ? X2Concept.UNMARKED : m_currentConcept;
	} else if (xType == XAlternativeType.FICTIVE) {
	    type = X2Concept.FICTIVE;
	} else if (xType == XAlternativeType.REAL) {
	    type = X2Concept.REAL;
	} else {
	    error("Unknown type.");
	    return null;
	}
	if (m_currentConcept != null && !m_currentConcept.equals(type)) {
	    error("Type of alternative " + type + " does not match outer concept " + m_currentConcept + ".");
	    return null;
	}
	m_fictiveStatus.put(alternative, type);

	final List<Boolean> activeList = xAlternative.getActiveList();
	final Boolean active = getUniqueOrZero(activeList);
	if (Boolean.FALSE.equals(active)) {
	    m_inactiveAlternatives.add(alternative);
	    return null;
	}
	return alternative;
    }

    /**
     * <p>
     * Returns all the alternatives found in the given XMCDA fragment, except those marked as inactive. The alternative
     * names, if found, are stored in this object, as well as the information of whether an alternative is fictive.
     * Alternatives marked as inactive are stored in this object. Any previously stored information is deleted.
     * </p>
     * <p>
     * The returned set iteration order respects the order of the source.
     * </p>
     * <p>
     * This method expects each alternative to have an id set, and maximum one type. In case of unexpected data, an
     * exception is thrown if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
     * informations will be skipped.
     * </p>
     * 
     * @param xAlternatives
     *            not <code>null</code>.
     * 
     * @return may be empty, but not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Set<Alternative> readAll(XAlternatives xAlternatives) throws InvalidInputException {
	Preconditions.checkNotNull(xAlternatives);
	m_fictiveStatus.clear();
	m_names.clear();
	m_inactiveAlternatives.clear();

	final Set<Alternative> alternatives = readAllInternal(xAlternatives);
	return alternatives;
    }

    /**
     * <p>
     * Returns all the alternatives found in the given XMCDA object, except those marked as inactive. The alternative
     * names, if found, are stored in this object, as well as the information of whether an alternative is fictive. This
     * <em>is added</em> to possibly previously stored information. Also, alternatives marked as inactive are added to
     * inactive alternatives stored in this object.
     * </p>
     * <p>
     * The returned set iteration order respects the order of the source.
     * </p>
     * <p>
     * This method expects each alternative to have an id set, and maximum one type. In case of unexpected data, an
     * exception is thrown if this objects follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
     * informations will be skipped.
     * </p>
     * 
     * @param xAlternatives
     *            not <code>null</code>.
     * 
     * @return may be empty, but not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    private Set<Alternative> readAllInternal(XAlternatives xAlternatives) throws InvalidInputException {
	final Set<Alternative> alternatives = Sets.newLinkedHashSet();
	final List<XAlternative> xAlternativeList = xAlternatives.getAlternativeList();
	for (final XAlternative xAlternative : xAlternativeList) {
	    final String conceptString = xAlternatives.getMcdaConcept();
	    final X2Concept concept = X2Concept.asConcept(conceptString);
	    m_currentConcept = concept == X2Concept.UNMARKED ? null : concept;
	    final Alternative alternative = read(xAlternative);
	    if (alternative != null) {
		alternatives.add(alternative);
	    }
	}
	return alternatives;
    }

    /**
     * Retrieves the XMCDA equivalent of the given alternatives. For each alternative, also writes the name and the
     * fictive and active status of the alternative if they are known to this object. Optionally, this method may also
     * associate a concept to the returned tag according to the given type.
     * 
     * @param alternatives
     *            not <code>null</code>, may be empty.
     * @param type
     *            may be <code>null</code>.
     * @return not <code>null</code>.
     * @see #setFictiveStatus
     * @see #setNames
     * @see #setInactiveAlternatives
     */
    public XAlternatives writeAlternatives(Set<Alternative> alternatives, XAlternativeType.Enum type) {
	Preconditions.checkNotNull(alternatives);
	final XAlternatives xAlternatives = XMCDA.Factory.newInstance().addNewAlternatives();
	if (type != null) {
	    xAlternatives.setMcdaConcept(StringUtils.getWithFirstCap(type.toString()));
	}
	for (final Alternative alternative : alternatives) {
	    final XAlternative xAlternative = xAlternatives.addNewAlternative();
	    xAlternative.setId(alternative.getId());
	    final String name = m_names.get(alternative);
	    if (name != null) {
		xAlternative.setName(name);
	    }
	    if (m_fictiveStatus.containsKey(alternative)) {
		final boolean isFictive = m_fictiveStatus.get(alternative) == X2Concept.FICTIVE;
		xAlternative.addType(isFictive ? XAlternativeType.FICTIVE : XAlternativeType.REAL);
	    }
	    if (m_inactiveAlternatives.contains(alternative)) {
		xAlternative.addActive(false);
	    }
	    if (m_activeAlternatives && !m_inactiveAlternatives.contains(alternative)) {
		xAlternative.addActive(true);
	    }
	}
	return xAlternatives;
    }

    /**
     * @param activeAlternatives
     *            <code>true</code> to mark the alternatives as active when they are not found in the inactive
     *            alternatives. The default is to only mark inactive alternatives.
     * @see #setInactiveAlternatives(Set)
     */
    public void setMarkActiveAlternatives(boolean activeAlternatives) {
	m_activeAlternatives = activeAlternatives;
    }

    /**
     * Retrieves a writeable view of the set of the alternative names stored in this object, observed during a
     * {@link #read} or stored after a {@link #setNames}. The returned map has no <code>null</code> key, no
     * <code>null</code> values, and such entries may not be added to the map..
     * 
     * @return not <code>null</code>.
     */
    public Map<Alternative, String> getNames() {
	return m_names;
    }

    /**
     * <p>
     * Returns all the alternatives found in the given XMCDA fragments, except those marked as inactive. The alternative
     * names, if found, are stored in this object, as well as the information of whether an alternative is fictive.
     * Alternatives marked as inactive are stored in this object. Any previously stored information is deleted.
     * </p>
     * <p>
     * The returned set iteration order respects the order of the source.
     * </p>
     * <p>
     * This method expects each alternative to have an id set, and maximum one type. In case of unexpected data, an
     * exception is thrown if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
     * informations will be skipped.
     * </p>
     * 
     * @param xAlternativesList
     *            not <code>null</code>.
     * 
     * @return may be empty, but not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Set<Alternative> readAll(Collection<XAlternatives> xAlternativesList) throws InvalidInputException {
	Preconditions.checkNotNull(xAlternativesList);
	m_fictiveStatus.clear();
	m_names.clear();
	m_inactiveAlternatives.clear();

	final Set<Alternative> allAlternatives = Sets.newLinkedHashSet();
	for (final XAlternatives xAlternatives : xAlternativesList) {
	    final Set<Alternative> alternatives = readAllInternal(xAlternatives);
	    allAlternatives.addAll(alternatives);
	}
	return allAlternatives;
    }

    /**
     * Sets the alternative names stored in this object as the given names. No reference is held to the given map.
     * 
     * @param names
     *            not <code>null</code>, may be empty, no <code>null</code> key or value.
     * @see #writeAlternatives
     */
    public void setNames(Map<Alternative, String> names) {
	Preconditions.checkNotNull(names);
	m_names.clear();
	m_names.putAll(names);
    }

    /**
     * <p>
     * Returns some of the alternatives found in the given XMCDA fragments. The alternative names, if found, are stored
     * in this object, as well as the information of whether an alternative is fictive. Alternatives marked as inactive
     * are stored in this object but are not included in the returned set. Any previously stored information is deleted.
     * </p>
     * <p>
     * The returned set iteration order respects the order of the source.
     * </p>
     * <p>
     * This method expects each alternative to have an id set, and maximum one type. In case of unexpected data, an
     * exception is thrown if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
     * informations will be skipped.
     * </p>
     * 
     * @param xAlternativesList
     *            not <code>null</code>.
     * @param type
     *            not <code>null</code>, {@link XAlternativeType#FICTIVE} to receive only the alternatives specifically
     *            marked as fictive, {@link XAlternativeType#REAL} to receive only alternatives not specifically marked
     *            as fictive. Note that this is not symmetric: this method considers an alternative as being by default
     *            non fictive.
     * 
     * @return may be empty, but not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Set<Alternative> readUsingMarking(Collection<XAlternatives> xAlternativesList, XAlternativeType.Enum type)
	    throws InvalidInputException {
	checkNotNull(xAlternativesList);
	checkNotNull(type);
	final Set<Alternative> all = readAll(xAlternativesList);
	return keepOnly(all, type == XAlternativeType.FICTIVE);
    }

    /**
     * <p>
     * Returns all the alternatives found in the given XMCDA fragments, or a part of these, depending on the requested
     * parsing method. Alternatives marked as inactive are not returned.
     * <ul>
     * <li>If the parsing method is {@link AlternativesParsingMethod#TAKE_ALL}, all the alternatives are returned.</li>
     * <li>If it is {@link AlternativesParsingMethod#SEEK_CONCEPT}, only the alternatives found in tags named after the
     * appropriate concept (fictive or real), depending on the requested type, are returned.</li>
     * <li>If it is {@link AlternativesParsingMethod#USE_MARKING}, and the type is {@link XAlternativeType#FICTIVE}, the
     * alternatives specifically marked as fictive, accross all the XMCDA fragments, are returned. If it is
     * {@link AlternativesParsingMethod#USE_MARKING} and the type is {@link XAlternativeType#REAL}, the alternatives not
     * specifically marked as fictive, accross all the XMCDA fragments, are returned. Note that this is not symmetric:
     * this method considers an alternative as being by default non fictive.</li>
     * </ul>
     * </p>
     * <p>
     * For each alternative contained in the returned set, the alternative name, if found, as well as the information of
     * whether an alternative is fictive, are stored in this object. Any previously stored information is deleted.
     * </p>
     * <p>
     * The returned set iteration order matches the order of the source.
     * </p>
     * <p>
     * This method expects each alternative to have an id set, and maximum one type. In case of unexpected data, an
     * exception is thrown if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
     * informations will be skipped.
     * </p>
     * 
     * @param xAlternativesCollection
     *            not <code>null</code>.
     * @param type
     *            unused and may be <code>null</code> if parsing method is {@link AlternativesParsingMethod#TAKE_ALL},
     *            otherwise, must be one of {@link XAlternativeType#REAL} or {@link XAlternativeType#FICTIVE}.
     * @param parsingMethod
     *            not <code>null</code>.
     * 
     * @return may be empty, but not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #readAll(Collection)
     * @see #readUsingConcept(Collection, Enum)
     * @see #readUsingMarking(Collection, Enum)
     */
    public static Set<Alternative> read(Collection<XAlternatives> xAlternativesCollection, Enum type,
	    AlternativesParsingMethod parsingMethod) throws InvalidInputException {
	checkArgument(parsingMethod == AlternativesParsingMethod.TAKE_ALL || type != null);
	checkNotNull(parsingMethod);
	final XMCDAErrorsManager errorsManager = new XMCDAErrorsManager();
	final XMCDAAlternatives xmcdaAlternatives = new XMCDAAlternatives(errorsManager);
	final Set<Alternative> alternatives;
	switch (parsingMethod) {
	case TAKE_ALL:
	    alternatives = xmcdaAlternatives.readAll(xAlternativesCollection);
	    break;
	case SEEK_CONCEPT:
	    alternatives = xmcdaAlternatives.readUsingConcept(xAlternativesCollection, type);
	    break;
	case USE_MARKING:
	    alternatives = xmcdaAlternatives.readUsingMarking(xAlternativesCollection, type);
	    break;
	default:
	    throw new IllegalStateException();
	}
	if (parsingMethod != AlternativesParsingMethod.TAKE_ALL) {
	    final X2Concept otherType = type == XAlternativeType.REAL ? X2Concept.FICTIVE : X2Concept.REAL;
	    assert (Sets.intersection(alternatives, xmcdaAlternatives.getMarkedAlternatives(otherType)).isEmpty());
	}
	// errorsManager.error("Unexpected " + otherType + " alternative at " + xAlternativesCollection + ".");
	return alternatives;
    }

    /**
     * Retrieves the alternatives stored in this object as inactive. The returned set is necessarily empty if no read or
     * {@link #setInactiveAlternatives(Set)} occurred yet.
     * 
     * @return not <code>null</code>.
     */
    public Set<Alternative> getInactiveAlternatives() {
	return Sets.newHashSet(m_inactiveAlternatives);
    }

    /**
     * Stores the given alternatives in this object. No reference is held to the given set.
     * 
     * @param inactiveAlternatives
     *            not <code>null</code>, may be empty, no <code>null</code> entry.
     * @see #writeAlternatives
     */
    public void setInactiveAlternatives(Set<Alternative> inactiveAlternatives) {
	Preconditions.checkNotNull(inactiveAlternatives);
	m_inactiveAlternatives.clear();
	m_inactiveAlternatives.addAll(inactiveAlternatives);
    }

    /**
     * <p>
     * Returns some of the alternatives found in the given XMCDA fragments, reading only the alternatives found in tags
     * named after the appropriate concept (fictive or real), depending on the requested type. The alternative names, if
     * found, are stored in this object, as well as the information of whether an alternative is marked fictive.
     * Alternatives marked as inactive are stored in this object but are not included in the returned set. Any
     * previously stored information is deleted.
     * </p>
     * <p>
     * The returned set iteration order respects the order of the source.
     * </p>
     * <p>
     * This method expects each alternative to have an id set, and maximum one type. In case of unexpected data, an
     * exception is thrown if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming
     * informations will be skipped.
     * </p>
     * 
     * @param xAlternativesList
     *            not <code>null</code>.
     * @param type
     *            not <code>null</code>, {@link XAlternativeType#REAL} or {@link XAlternativeType#FICTIVE}.
     * 
     * @return may be empty, but not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Set<Alternative> readUsingConcept(Collection<XAlternatives> xAlternativesList, XAlternativeType.Enum type)
	    throws InvalidInputException {
	checkNotNull(xAlternativesList);
	checkNotNull(type);
	m_fictiveStatus.clear();
	m_names.clear();
	m_inactiveAlternatives.clear();

	final Set<Alternative> allAlternatives = Sets.newLinkedHashSet();
	for (final XAlternatives xAlternatives : xAlternativesList) {
	    final String concept = xAlternatives.getMcdaConcept();
	    if (concept == null || concept.isEmpty()) {
		error("Expected name at " + xAlternatives + ".");
		continue;
	    }
	    if (concept.equalsIgnoreCase(type.toString())) {
		final Set<Alternative> alternatives = readAllInternal(xAlternatives);
		allAlternatives.addAll(alternatives);
	    }
	}
	return allAlternatives;
    }

    /**
     * Retrieves the subset of alternatives stored in this object that are marked as requested. The returned set is
     * necessarily empty if no {@link #read} or {@link #setFictiveStatus} occurred yet.
     * 
     * @param type
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public Set<Alternative> getMarkedAlternatives(X2Concept type) {
	checkNotNull(type);
	return Maps.filterValues(m_fictiveStatus, Predicates.equalTo(type)).keySet();
    }

}
