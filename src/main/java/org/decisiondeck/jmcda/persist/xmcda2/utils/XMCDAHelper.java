package org.decisiondeck.jmcda.persist.xmcda2.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlTokenSource;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;

/**
 * An errors manager forwarder combined with methods from {@link XMCDAReadUtils} to help parsing XMCDA fragments.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDAHelper {

    /**
     * Not <code>null</code>.
     */
    private final XMCDAReadUtils m_utils;

    public void error(String error) throws InvalidInputException {
	m_utils.error(error);
    }

    public ErrorManagement getStrategy() {
	return m_utils.getStrategy();
    }

    public void setStrategy(ErrorManagement strategy) {
	m_utils.setStrategy(strategy);
    }

    /**
     * Forwards to a new {@link XMCDAReadUtils} which uses the default error management strategy
     * {@link ErrorManagement#THROW}.
     */
    public XMCDAHelper() {
	m_utils = new XMCDAReadUtils();
    }

    /**
     * Creates a new object which delegates error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDAHelper(XMCDAErrorsManager errorsManager) {
	checkNotNull(errorsManager);
	m_utils = new XMCDAReadUtils(errorsManager);
    }

    /**
     * Forwards to the given utils object.
     * 
     * @param utils
     *            not <code>null</code>.
     */
    public XMCDAHelper(XMCDAReadUtils utils) {
	checkNotNull(utils);
	m_utils = utils;
    }

    /**
     * <p>
     * Retrieves the only element from the given collection of elements. This method expects the collection of elements
     * to contain exactly one element.
     * </p>
     * <p>
     * In case of unexpected contents, this method returns <code>null</code> or throws an exception depending on the
     * strategy this object follows.
     * </p>
     * 
     * @param <T>
     *            the type of content of the collection.
     * @param collection
     *            not <code>null</code>.
     * @param contextMessageIfEmpty
     *            if not <code>null</code>, is used to give a more useful error message about the context when the given
     *            collection is empty. If parsing an {@link XmlObject}, one possibility is to use the qualified name of
     *            the parsed type, see e.g. {@link XPerformanceTable#type} and {@link SchemaType#getName()}.
     * @return <code>null</code> iff the collection contains more or less than one element and this object follows a
     *         permissive strategy.
     * @throws InvalidInputException
     *             iff the collection contains more or less than one element and this object follows the
     *             {@link ErrorManagement#THROW} strategy.
     */
    protected <T extends XmlTokenSource> T getUnique(Collection<T> collection, String contextMessageIfEmpty)
	    throws InvalidInputException {
	return m_utils.getUnique(collection, contextMessageIfEmpty);
    }

    /**
     * <p>
     * Retrieves the only element from the given collection of elements, or <code>null</code> if the collection is
     * empty. This method expects the collection of elements to contain zero or one elements, thus maximum one element.
     * </p>
     * <p>
     * If the given collection contains more than one element, this method returns <code>null</code> or throws an
     * exception depending on the strategy this object follows.
     * </p>
     * 
     * @param <T>
     *            the type of content of the collection.
     * @param collection
     *            not <code>null</code>.
     * @return <code>null</code> if the collection contains zero elements, or if the given collection contains more than
     *         one element and this object follows a permissive strategy.
     * @throws InvalidInputException
     *             iff the given collection contains more than one element and this object follows the
     *             {@link ErrorManagement#THROW} strategy.
     */
    protected <T> T getUniqueOrZero(Collection<T> collection) throws InvalidInputException {
	return m_utils.getUniqueOrZero(collection);
    }
}
