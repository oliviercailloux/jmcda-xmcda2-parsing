package org.decisiondeck.jmcda.persist.xmcda2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.utils.collection.CollectionUtils;
import org.decision_deck.utils.collection.extensional_order.ExtentionalTotalOrder;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.utils.ExportSettings;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeAffectation;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesAffectations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesInterval;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesSet;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesSet.Element;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValue;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilitiesRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Methods for reading and writing alternative assignments from and to XMCDA fragments.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDAAssignments extends XMCDAHelperWithVarious {

    /**
     * Retrieves the XMCDA equivalent of the given assignments. There must be at least one assignment.
     * 
     * @param assignments
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XAlternativesAffectations write(IOrderedAssignmentsWithCredibilitiesRead assignments) {
	checkNotNull(assignments);
	Preconditions.checkArgument(assignments.getAlternatives().size() >= 1);
	final XAlternativesAffectations xAlternativesAffectations = XMCDA.Factory.newInstance()
		.addNewAlternativesAffectations();
	for (Alternative alternative : m_exportSettings.interOrderAlternatives(assignments.getAlternatives())) {
	    final NavigableMap<Category, Double> assigned = assignments.getCredibilities(alternative);
	    final Set<Category> categories = assigned.keySet();
	    final XAlternativeAffectation xAlternativeAffectation = xAlternativesAffectations
		    .addNewAlternativeAffectation();
	    xAlternativeAffectation.setAlternativeID(alternative.getId());
	    if (categories.size() == 1) {
		final Category category = Iterables.getOnlyElement(categories);
		final double value = assigned.get(category).doubleValue();
		xAlternativeAffectation.setCategoryID(category.getId());
		xAlternativeAffectation.addNewValue().setReal((float) value);
	    } else {
		final XCategoriesSet xSet = xAlternativeAffectation.addNewCategoriesSet();
		for (Category category : categories) {
		    final double value = assigned.get(category).doubleValue();
		    final Element xElement = xSet.addNewElement();
		    xElement.setCategoryID(category.getId());
		    xElement.addNewValue().setReal((float) value);
		}
	    }
	}
	return xAlternativesAffectations;
    }

    private boolean m_forceIntervals;

    /**
     * <p>
     * Retrieves assignments to multiple categories objects per decision makers, containing the information in the given
     * fragment. The iteration order of the key set of the returned map reflects the order read in the fragment. If
     * categories are set in this object, this method checks that the all categories read from the fragment are included
     * in the categories set, otherwise, it is considered as unexpected data.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xAllAlternativesAffectations
     *            not <code>null</code>, no <code>null</code> values.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #setCategories
     */
    public Map<DecisionMaker, IAssignmentsToMultiple> readAll(
	    Collection<XAlternativesAffectations> xAllAlternativesAffectations) throws InvalidInputException {
	checkNotNull(xAllAlternativesAffectations);
	final Map<DecisionMaker, IAssignmentsToMultiple> allAssignments = Maps.newLinkedHashMap();
	for (XAlternativesAffectations xAlternativesAffectations : xAllAlternativesAffectations) {
	    final String dmId = xAlternativesAffectations.getName();
	    if (dmId == null) {
		error("Affectations has no bound name, name of the corresponding decision maker is required.");
		continue;
	    }
	    final DecisionMaker dm = new DecisionMaker(dmId);
	    final IAssignmentsToMultiple assignments = read(xAlternativesAffectations);
	    allAssignments.put(dm, assignments);
	}
	return allAssignments;
    }

    /**
     * <p>
     * Retrieves an object representing the assignments, possibly to multiple categories, contained in the given
     * fragment. If categories are set in this object, this method checks that the all categories read from the fragment
     * are included in the categories set, otherwise, it is considered as unexpected data.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xAlternativesAffectations
     *            not <code>null</code>, no <code>null</code> values.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #setCategories
     */
    public IAssignmentsToMultiple read(XAlternativesAffectations xAlternativesAffectations)
	    throws InvalidInputException {
	checkNotNull(xAlternativesAffectations);
	final IAssignmentsToMultiple assignments = AssignmentsFactory.newAssignmentsToMultiple();
	final List<XAlternativeAffectation> xAlternativeAffectationList = xAlternativesAffectations
		.getAlternativeAffectationList();
	for (XAlternativeAffectation xAlternativeAffectation : xAlternativeAffectationList) {
	    final String alternativeId = xAlternativeAffectation.getAlternativeID();
	    if (alternativeId == null || alternativeId.isEmpty()) {
		error("Expected alternative id at " + xAlternativeAffectation + ".");
		continue;
	    }
	    final Alternative alternative = new Alternative(alternativeId);
	    final String uniqueCategoryId = xAlternativeAffectation.getCategoryID();
	    final XCategoriesSet xCategoriesSet = xAlternativeAffectation.getCategoriesSet();
	    final boolean hasCategories = xCategoriesSet != null;
	    final boolean hasCategory = uniqueCategoryId != null && !uniqueCategoryId.isEmpty();
	    if (hasCategory && hasCategories) {
		error("Expected exactly one of category or category set, not both, at " + xAlternativeAffectation + ".");
		continue;
	    }

	    final Set<Category> categories;
	    boolean unknownCategory = false;
	    if (hasCategory) {
		final Category category = new Category(uniqueCategoryId);
		if (unknown(category)) {
		    unknownCategory = true;
		    error("Found " + category + " which is not in the set of known categories " + m_categories + ".");
		}
		categories = Collections.singleton(category);
	    } else if (hasCategories) {
		categories = Sets.newLinkedHashSet();
		/** <code>true</code> by definition, but we need this to satisfy compiler check. */
		assert xCategoriesSet != null;
		final List<XCategoriesSet.Element> xElementList = xCategoriesSet.getElementList();
		for (XCategoriesSet.Element xElement : xElementList) {
		    final String elementCategoryId = xElement.getCategoryID();
		    if (elementCategoryId == null || elementCategoryId.isEmpty()) {
			error("Expected category id at " + xElement + ".");
			continue;
		    }
		    final Category category = new Category(elementCategoryId);
		    if (unknown(category)) {
			unknownCategory = true;
			error("Found " + category + " which is not in the set of known categories " + m_categories
				+ ".");
			break;
		    }
		    categories.add(category);
		}
	    } else {
		error("Expected category id or category set at " + xAlternativeAffectation + ".");
		continue;
	    }
	    if (unknownCategory) {
		continue;
	    }
	    final Set<Category> newAssignments = Sets.newLinkedHashSet();
	    final Set<Category> existingAssignments = assignments.getCategories(alternative);
	    if (existingAssignments != null) {
		newAssignments.addAll(existingAssignments);
	    }
	    for (Category category : categories) {
		newAssignments.add(category);
	    }
	    assignments.setCategories(alternative, newAssignments);
	}
	return assignments;
    }

    private boolean unknown(Category category) {
	return m_categories != null && !m_categories.contains(category);
    }

    private SortedSet<Category> m_categories;
    private final ExportSettings m_exportSettings = new ExportSettings();

    /**
     * Retrieves the XMCDA equivalent of the given assignments. The assignments objects given as values must each have
     * at least one assignment.
     * 
     * @param allAssignments
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public Collection<XAlternativesAffectations> writeAllWithCredibilities(
	    Map<DecisionMaker, ? extends IOrderedAssignmentsWithCredibilitiesRead> allAssignments) {
	checkNotNull(allAssignments);
	final Set<XAlternativesAffectations> xAlternativesAffectationsSet = Sets.newLinkedHashSet();
	for (DecisionMaker dm : m_exportSettings.interOrderDms(allAssignments.keySet())) {
	    final IOrderedAssignmentsWithCredibilitiesRead assignments = allAssignments.get(dm);
	    final XAlternativesAffectations xAlternativesAffectations = write(assignments);
	    xAlternativesAffectations.setName(dm.getId());
	    xAlternativesAffectationsSet.add(xAlternativesAffectations);
	}
	return xAlternativesAffectationsSet;
    }

    /**
     * <p>
     * Retrieves an object representing the assignments with credibilities contained in the given fragment. If
     * categories are set in this object, this method checks that the all categories read from the fragment are included
     * in the categories set, otherwise, it is considered as unexpected data.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xAlternativesAffectations
     *            not <code>null</code>, no <code>null</code> values.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #setCategories
     */
    public IAssignmentsWithCredibilities readWithCredibilities(XAlternativesAffectations xAlternativesAffectations)
	    throws InvalidInputException {
	checkNotNull(xAlternativesAffectations);
	final IAssignmentsWithCredibilities assignments = AssignmentsFactory.newAssignmentsWithCredibilities();
	final List<XAlternativeAffectation> xAlternativeAffectationList = xAlternativesAffectations
		.getAlternativeAffectationList();
	for (XAlternativeAffectation xAlternativeAffectation : xAlternativeAffectationList) {
	    final String alternativeId = xAlternativeAffectation.getAlternativeID();
	    if (alternativeId == null || alternativeId.isEmpty()) {
		error("Expected alternative id at " + xAlternativeAffectation + ".");
		continue;
	    }
	    final Alternative alternative = new Alternative(alternativeId);
	    final String categoryId = xAlternativeAffectation.getCategoryID();
	    final XCategoriesSet xCategoriesSet = xAlternativeAffectation.getCategoriesSet();
	    final boolean hasCategoryId = categoryId != null && !categoryId.isEmpty();
	    final boolean hasCategoriesSet = xCategoriesSet != null;
	    if (!hasCategoryId && !hasCategoriesSet) {
		error("Expected category id or set at " + xAlternativeAffectation + ".");
		continue;
	    }
	    if (hasCategoryId && hasCategoriesSet) {
		error("Expected category id or set, but not both, at " + xAlternativeAffectation + ".");
		continue;
	    }
	    if (hasCategoryId) {
		final Category category = new Category(categoryId);
		final List<XValue> xValueList = xAlternativeAffectation.getValueList();
		final Double value = readDouble(xValueList, xAlternativeAffectation.toString());
		if (value == null) {
		    continue;
		}
		augmentCredibilities(assignments, alternative, category, value.doubleValue());
	    }
	    if (hasCategoriesSet) {
		assert xCategoriesSet != null;
		final List<Element> xElements = xCategoriesSet.getElementList();
		for (Element xElement : xElements) {
		    final String internalCategoryId = xElement.getCategoryID();
		    if (internalCategoryId == null || internalCategoryId.isEmpty()) {
			error("Expected category id at " + xElement + ".");
			continue;
		    }
		    final Category category = new Category(internalCategoryId);
		    final List<XValue> xValues = xElement.getValueList();
		    final Double value = readDouble(xValues, xElement.toString());
		    if (value == null) {
			continue;
		    }
		    augmentCredibilities(assignments, alternative, category, value.doubleValue());
		}
	    }
	}
	return assignments;
    }

    private void augmentCredibilities(IAssignmentsWithCredibilities assignments, Alternative alternative,
	    Category category, double value) throws InvalidInputException {
	if (unknown(category)) {
	    error("Found " + category + " which is not in the set of known categories " + m_categories + ".");
	    return;
	}
	final Map<Category, Double> newCredibilities = Maps.newHashMap();
	final Map<Category, Double> existingCredibilities = assignments.getCredibilities(alternative);
	if (existingCredibilities != null) {
	    newCredibilities.putAll(existingCredibilities);
	}
	if (newCredibilities.containsKey(category)) {
	    error("Duplicate entry for " + alternative + ", " + category + ". Already seen: "
		    + newCredibilities.get(category) + ", second value: " + value + ".");
	    return;
	}
	newCredibilities.put(category, Double.valueOf(value));
	assignments.setCredibilities(alternative, newCredibilities);
    }

    /**
     * Retrieves the XMCDA equivalent of the given assignments. There must be at least one assignment.
     * 
     * @param assignments
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XAlternativesAffectations write(IOrderedAssignmentsToMultipleRead assignments) {
	checkNotNull(assignments);
	checkArgument(assignments.getAlternatives().size() >= 1, "assignments may not be empty");
	final XAlternativesAffectations xAlternativesAffectations = XMCDA.Factory.newInstance()
		.addNewAlternativesAffectations();
	for (Alternative alternative : m_exportSettings.interOrderAlternatives(assignments.getAlternatives())) {
	    final NavigableSet<Category> categories = assignments.getCategories(alternative);
	    final XAlternativeAffectation xAlternativeAffectation = xAlternativesAffectations
		    .addNewAlternativeAffectation();
	    xAlternativeAffectation.setAlternativeID(alternative.getId());
	    if (m_forceIntervals) {
		if (!CollectionUtils.isContiguous(categories, assignments.getCategories())) {
		    throw new IllegalStateException("Must use intervals but non contiguous set of categories for "
			    + alternative + ".");
		}
		final XCategoriesInterval xInterval = xAlternativeAffectation.addNewCategoriesInterval();
		xInterval.addNewLowerBound().setCategoryID(categories.first().getId());
		xInterval.addNewUpperBound().setCategoryID(categories.last().getId());
	    } else if (categories.size() == 1) {
		final Category category = Iterables.getOnlyElement(categories);
		xAlternativeAffectation.setCategoryID(category.getId());
	    } else {
		final XCategoriesSet xSet = xAlternativeAffectation.addNewCategoriesSet();
		for (Category category : categories) {
		    xSet.addNewElement().setCategoryID(category.getId());
		}
	    }
	}
	return xAlternativesAffectations;
    }

    /**
     * <p>
     * Retrieves assignments with credibilities objects per decision makers, containing the information in the given
     * fragment. The iteration order of the key set of the returned map reflects the order read in the fragment. If
     * categories are set in this object, this method checks that the all categories read from the fragment are included
     * in the categories set, otherwise, it is considered as unexpected data.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xAlternativesAffectationsCollection
     *            not <code>null</code>, no <code>null</code> key or values.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     * @see #setCategories
     */
    public Map<DecisionMaker, IAssignmentsWithCredibilities> readAllWithCredibilities(
	    Collection<XAlternativesAffectations> xAlternativesAffectationsCollection) throws InvalidInputException {
	checkNotNull(xAlternativesAffectationsCollection);
	final Map<DecisionMaker, IAssignmentsWithCredibilities> allAssignments = Maps.newLinkedHashMap();
	for (XAlternativesAffectations xAlternativesAffectations : xAlternativesAffectationsCollection) {
	    final String dmId = xAlternativesAffectations.getName();
	    if (dmId == null || dmId.isEmpty()) {
		error("Expected name at " + xAlternativesAffectations + ".");
		continue;
	    }
	    final DecisionMaker dm = new DecisionMaker(dmId);
	    final IAssignmentsWithCredibilities assignments = readWithCredibilities(xAlternativesAffectations);
	    allAssignments.put(dm, assignments);
	}
	return allAssignments;
    }

    /**
     * Retrieves a copy of the categories stored in this object, or <code>null</code> if the categories have not been
     * set yet.
     * 
     * @return <code>null</code> iff not set yet.
     */
    public SortedSet<Category> getCategories() {
	return m_categories == null ? null : ExtentionalTotalOrder.create(m_categories);
    }

    /**
     * Sets the categories stored in this object. No reference is held to the given set.
     * 
     * @param categories
     *            possibly <code>null</code>.
     */
    public void setCategories(Set<Category> categories) {
	m_categories = categories == null ? null : ExtentionalTotalOrder.create(categories);
    }

    public void setAlternativesOrder(Collection<Alternative> alternativesOrder) {
	m_exportSettings.setAlternativesOrder(alternativesOrder);
    }

    public void setDmsOrder(Collection<DecisionMaker> dmsOrder) {
	m_exportSettings.setDmsOrder(dmsOrder);
    }

    /**
     * Retrieves the XMCDA equivalent of the given assignments. The assignments objects given as values must each have
     * at least one assignment.
     * 
     * @param allAssignments
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public Collection<XAlternativesAffectations> writeAll(
	    Map<DecisionMaker, ? extends IOrderedAssignmentsToMultipleRead> allAssignments) {
	checkNotNull(allAssignments);
	final Set<XAlternativesAffectations> xAlternativesAffectationsSet = Sets.newLinkedHashSet();
	for (DecisionMaker dm : m_exportSettings.interOrderDms(allAssignments.keySet())) {
	    final IOrderedAssignmentsToMultipleRead assignments = allAssignments.get(dm);
	    checkArgument(assignments.getAlternatives().size() >= 1, "assignments may not be empty (key " + dm + ").");
	    final XAlternativesAffectations xAlternativesAffectations = write(assignments);
	    xAlternativesAffectations.setName(dm.getId());
	    xAlternativesAffectationsSet.add(xAlternativesAffectations);
	}
	return xAlternativesAffectationsSet;
    }

    /**
     * Creates a new object which will use the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDAAssignments() {
	this(new XMCDAErrorsManager());
    }

    /**
     * Creates a new object delegating error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDAAssignments(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
	m_forceIntervals = false;
	m_categories = null;
    }

    public boolean forcesIntervals() {
	return m_forceIntervals;
    }

    public void setForceIntervals(boolean forceIntervals) {
	m_forceIntervals = forceIntervals;
    }
}
