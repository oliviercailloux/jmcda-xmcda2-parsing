package org.decisiondeck.jmcda.persist.xmcda2.utils;

import java.util.Collection;

import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAVarious;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodParameters;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XNumericValue;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XParameter;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValue;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;

/**
 * An errors manager forwarder combined with methods from {@link XMCDAVarious} and {@link XMCDAReadUtils} to help parsing
 * XMCDA fragments. Reader classes may inherit from this class to get initial implementation support.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDAHelperWithVarious extends XMCDAHelper {
    private final XMCDAVarious m_various;

    /**
     * Creates a new object that uses the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDAHelperWithVarious() {
	this(new XMCDAErrorsManager());
    }

    /**
     * <p>
     * Retrieves the number embedded in the given fragment.
     * </p>
     * <p>
     * This method expects there is exactly one element in the given collection and it contains a number. In case of
     * unexpected contents, this method returns <code>null</code> or throws an exception depending on the strategy this
     * object follows.
     * </p>
     * 
     * 
     * @param xValues
     *            not <code>null</code>.
     * @param contextMessageIfEmpty
     *            if not <code>null</code>, is used to give a more useful error message about the context when the given
     *            collection is empty.
     * @return a real number, or <code>null</code> iff unexpected content has been read and this object follows a
     *         permissive strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    protected Double readDouble(Collection<XValue> xValues, String contextMessageIfEmpty) throws InvalidInputException {
	return m_various.readDouble(xValues, contextMessageIfEmpty);
    }

    /**
     * <p>
     * Retrieves the number embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the fragments contains a unique number. In case of unexpected contents, this method returns
     * <code>null</code> or throws an exception depending on the strategy this object follows.
     * </p>
     * 
     * 
     * @param xMethodParameters
     *            not <code>null</code>.
     * @param contextMessageIfEmpty
     *            if not <code>null</code>, is used to give a more useful error message about the context when the given
     *            fragment contains an empty collection.
     * @return a real number, or <code>null</code> iff unexpected content has been read and this object follows a
     *         permissive strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    protected Double readDouble(XMethodParameters xMethodParameters, String contextMessageIfEmpty)
	    throws InvalidInputException {
	return m_various.readDouble(xMethodParameters, contextMessageIfEmpty);
    }

    /**
     * <p>
     * Retrieves the number embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the fragments contains a number. In case of unexpected contents, this method returns
     * <code>null</code> or throws an exception depending on the strategy this object follows.
     * </p>
     * 
     * 
     * @param xNumber
     *            not <code>null</code>.
     * @return a real number, or <code>null</code> iff unexpected content has been read and this object follows a
     *         permissive strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    protected Double readDouble(XNumericValue xNumber) throws InvalidInputException {
	return m_various.readDouble(xNumber);
    }

    /**
     * <p>
     * Retrieves the number embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the fragments contains a number. In case of unexpected contents, this method returns
     * <code>null</code> or throws an exception depending on the strategy this object follows.
     * </p>
     * 
     * 
     * @param xParameter
     *            not <code>null</code>.
     * @return a real number, or <code>null</code> iff unexpected content has been read and this object follows a
     *         permissive strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    protected Double readDouble(XParameter xParameter) throws InvalidInputException {
	return m_various.readDouble(xParameter);
    }

    /**
     * <p>
     * Retrieves the number embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the fragments contains a number. In case of unexpected contents, this method returns
     * <code>null</code> or throws an exception depending on the strategy this object follows.
     * </p>
     * 
     * 
     * @param xValue
     *            not <code>null</code>.
     * @return a real number, or <code>null</code> iff unexpected content has been read and this object follows a
     *         permissive strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    protected Double readDouble(XValue xValue) throws InvalidInputException {
	return m_various.readDouble(xValue);
    }

    /**
     * <p>
     * Retrieves the label embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the given fragment contains a unique non <code>null</code> label. In case of unexpected
     * contents, this method returns <code>null</code> or throws an exception depending on the strategy this object
     * follows.
     * </p>
     * 
     * 
     * @param xMethodParameters
     *            not <code>null</code>.
     * @param contextMessageIfEmpty
     *            if not <code>null</code>, is used to give a more useful error message about the context when the given
     *            fragment contains an empty collection.
     * @return not empty; <code>null</code> iff unexpected content has been read and this object follows a permissive
     *         strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    protected String readLabel(XMethodParameters xMethodParameters, String contextMessageIfEmpty)
	    throws InvalidInputException {
	return m_various.readLabel(xMethodParameters, contextMessageIfEmpty);
    }

    /**
     * <p>
     * Retrieves the label embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the given fragment contains a non <code>null</code> label. In case of unexpected contents,
     * this method returns <code>null</code> or throws an exception depending on the strategy this object follows.
     * </p>
     * 
     * 
     * @param xParameter
     *            not <code>null</code>.
     * @return not empty; <code>null</code> iff unexpected content has been read and this object follows a permissive
     *         strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    protected String readLabel(XParameter xParameter) throws InvalidInputException {
	return m_various.readLabel(xParameter);
    }

    /**
     * <p>
     * Retrieves the label embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the given fragment contains a non <code>null</code> label. In case of unexpected contents,
     * this method returns <code>null</code> or throws an exception depending on the strategy this object follows.
     * </p>
     * 
     * 
     * @param xValue
     *            not <code>null</code>.
     * @return not empty; <code>null</code> iff unexpected content has been read and this object follows a permissive
     *         strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    protected String readLabel(XValue xValue) throws InvalidInputException {
	return m_various.readLabel(xValue);
    }

    /**
     * Creates a new object which delegates error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDAHelperWithVarious(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
	m_various = new XMCDAVarious(errorsManager);
    }
}
