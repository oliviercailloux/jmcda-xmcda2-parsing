package org.decisiondeck.jmcda.persist.xmcda2;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Set;

import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodParameters;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XParameter;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValue;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;

import com.google.common.collect.Sets;

/**
 * Methods for reading and writing decision makers informations from and to XMCDA fragments.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDADecisionMakers extends XMCDAHelperWithVarious {

    /**
     * Retrieves an XMCDA fragment representing the given set of decision makers. The written information order matches
     * the iteration order of the given set.
     * 
     * @param dms
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XMethodParameters write(Set<DecisionMaker> dms) {
	checkNotNull(dms);
	final XMethodParameters xMethodParameters = XMCDA.Factory.newInstance().addNewMethodParameters();
	for (DecisionMaker dm : dms) {
	    final XParameter xParameter = xMethodParameters.addNewParameter();
	    xParameter.addNewValue().setLabel(dm.getId());
	}
	return xMethodParameters;
    }

    /**
     * <p>
     * Retrieves the set of decision makers found in the given fragment.
     * </p>
     * <p>
     * This method expects the decision makers to have a label set. In case of unexpected data, an exception is thrown
     * if this object follows the {@link ErrorManagement#THROW} strategy, otherwise, non conforming informations will be
     * skipped.
     * </p>
     * 
     * @param xMethodParameters
     *            not <code>null</code>.
     * @return <code>null</code> iff the given fragment contains an {@link XValue} that is not a label and this object
     *         does not follows the {@link ErrorManagement#THROW} strategy.
     * @throws InvalidInputException
     *             iff the given fragment contains a {@link XValue} that is not a label and this object follows the
     *             {@link ErrorManagement#THROW} strategy.
     */
    public Set<DecisionMaker> read(XMethodParameters xMethodParameters) throws InvalidInputException {
	checkNotNull(xMethodParameters);
	final Set<DecisionMaker> dms = Sets.newLinkedHashSet();
	final List<XParameter> xParameterList = xMethodParameters.getParameterList();
	for (XParameter xParameter : xParameterList) {
	    XValue xValue = xParameter.getValue();
	    final String dmId = readLabel(xValue);
	    if (dmId == null) {
		continue;
	    }
	    dms.add(new DecisionMaker(dmId));
	}
	return dms;
    }

    /**
     * Creates a new object which will use the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDADecisionMakers() {
	super();
    }

    /**
     * Creates a new object delegating error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDADecisionMakers(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
    }
}
