package org.decisiondeck.jmcda.persist.xmcda2.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;

import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * <p>
 * When reading from XMCDA fragments, data read might be different than what is
 * expected. This class permits to deal with such unexpected data and may be
 * configured to adopt various strategies to manage such situations. Classes
 * reading from XMCDA fragments typically delegate unexpected data situations
 * management to objects of this class.
 * </p>
 * <p>
 * When the context makes it unambiguous, this documentation uses the term
 * <em>error</em> to refer to a situation where an unexpected data has been
 * read. An example of an error is that a number is expected but a string was
 * read, or that a single xml entity was expected but a collection of several
 * such entities was found instead.
 * </p>
 *
 * @author Olivier Cailloux
 *
 */
public class XMCDAErrorsManager {
	/**
	 * Indicates what to do when unexpected data is read. When the
	 * {@link #COLLECT} strategy is used, the user should make sure the errors
	 * will go somewhere. If the user does not care about errors and simply want
	 * to discard them, the {@link #LOG} strategy should be chosen as it does
	 * not waste memory and it does not completely hide errors.
	 * 
	 * @author Olivier Cailloux
	 * 
	 */
	static public enum ErrorManagement {
		/**
		 * Do not stop reading at unexpected data: collect errors for further
		 * retrieval.
		 */
		COLLECT,
		/**
		 * Do not stop reading at unexpected data, log the error and go on.
		 */
		LOG,
		/**
		 * Stop reading at first error and throw an
		 * {@link InvalidInputException} exception.
		 */
		THROW
	}

	private static final Logger s_logger = LoggerFactory.getLogger(XMCDAErrorsManager.class);

	private final List<String> m_errors = Lists.newArrayList();

	private ErrorManagement m_strategy;

	/**
	 * Defaults to {@link ErrorManagement#THROW} strategy.
	 */
	public XMCDAErrorsManager() {
		m_strategy = ErrorManagement.THROW;
	}

	/**
	 * Creates a new object configured to follow the given error management
	 * strategy.
	 * 
	 * @param strategy
	 *            not <code>null</code>.
	 */
	public XMCDAErrorsManager(ErrorManagement strategy) {
		checkNotNull(strategy);
		m_strategy = strategy;
	}

	/**
	 * Throws an exception with the given error message if this object follows a
	 * {@link ErrorManagement#THROW} strategy (the default); otherwise, logs the
	 * error or collects the error.
	 * 
	 * @param error
	 *            <code>null</code> or empty for no error message.
	 * @throws InvalidInputException
	 *             if the strategy is {@link ErrorManagement#THROW}.
	 * @see #setStrategy(ErrorManagement)
	 */
	public void error(String error) throws InvalidInputException {
		switch (m_strategy) {
		case THROW:
			throw new InvalidInputException(error);
		case COLLECT:
			m_errors.add(error);
			break;
		case LOG:
			s_logger.error(error);
			break;
		default:
			throw new IllegalStateException();
		}
	}

	/**
	 * Retrieves a read-only view to the list of errors collected in this
	 * object. The list is populated only if this object uses the
	 * {@link ErrorManagement#COLLECT} strategy, and it is emptied if the
	 * strategy is changed.
	 * 
	 * @return not <code>null</code>.
	 */
	public List<String> getErrors() {
		return Collections.unmodifiableList(m_errors);
	}

	/**
	 * Retrieves the strategy this object currently follows.
	 * 
	 * @return not <code>null</code>.
	 */
	public ErrorManagement getStrategy() {
		return m_strategy;
	}

	/**
	 * Sets the strategy this object will follow.
	 * 
	 * @param strategy
	 *            not <code>null</code>.
	 */
	public void setStrategy(ErrorManagement strategy) {
		checkNotNull(strategy);
		m_strategy = strategy;
		if (m_strategy != ErrorManagement.COLLECT) {
			m_errors.clear();
		}
	}

}
