package org.decisiondeck.jmcda.persist.xmcda2.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;

public class XMCDAErrorsManagerForwarder {

    /**
     * Not <code>null</code>.
     */
    private final XMCDAErrorsManager m_errors;

    /**
     * Throws an exception with the given error message if this object follows a {@link ErrorManagement#THROW} strategy
     * (the default); otherwise, logs the error or collects the error.
     * 
     * @param error
     *            <code>null</code> or empty for no error message.
     * @throws InvalidInputException
     *             if the strategy is {@link ErrorManagement#THROW}.
     * @see #setStrategy(ErrorManagement)
     */
    public void error(String error) throws InvalidInputException {
	m_errors.error(error);
    }

    /**
     * Retrieves the strategy this object currently follows.
     * 
     * @return not <code>null</code>.
     */
    public ErrorManagement getStrategy() {
	return m_errors.getStrategy();
    }

    /**
     * Sets the strategy this object will follow.
     * 
     * @param strategy
     *            not <code>null</code>.
     */
    public void setStrategy(ErrorManagement strategy) {
	m_errors.setStrategy(strategy);
    }

    /**
     * Forwards to a new {@link XMCDAErrorsManager} which uses the default strategy {@link ErrorManagement#THROW}.
     */
    public XMCDAErrorsManagerForwarder() {
	m_errors = new XMCDAErrorsManager();
    }

    /**
     * Forwards to the given object.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDAErrorsManagerForwarder(XMCDAErrorsManager errorsManager) {
	checkNotNull(errorsManager);
	m_errors = errorsManager;
    }

    /**
     * Retrieves a read-only view to the list of errors collected in this object. The list is populated only if this
     * object uses the {@link ErrorManagement#COLLECT} strategy, and it is emptied if the strategy is changed.
     * 
     * @return not <code>null</code>.
     */
    public List<String> getErrors() {
	return m_errors.getErrors();
    }

}
