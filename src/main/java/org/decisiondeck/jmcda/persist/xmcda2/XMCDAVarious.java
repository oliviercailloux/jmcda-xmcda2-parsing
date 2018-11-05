package org.decisiondeck.jmcda.persist.xmcda2;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMessage;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodMessages;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodParameters;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XNumericValue;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XParameter;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XProjectReference;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValue;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelper;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Reads from and writes to various XMCDA fragments. Contains methods not found in other classes to read and write
 * numbers, strings, and other elementary structures.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDAVarious extends XMCDAHelper {

    /**
     * Creates a new object which will use the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDAVarious() {
	super();
    }

    /**
     * Creates a new object delegating error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDAVarious(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
    }

    /**
     * Retrieves the given value as an XMCDA project reference fragment.
     * 
     * @param comment
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XProjectReference writeComment(String comment) {
	checkNotNull(comment);
	final XProjectReference xmlPrRef = XMCDA.Factory.newInstance().addNewProjectReference();
	xmlPrRef.addComment(comment);
	return xmlPrRef;
    }

    /**
     * Retrieves the given message as an {@link XMessage} fragment.
     * 
     * @param message
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XMessage writeMessage(String message) {
	checkNotNull(message);
	final XMessage xMessage = XMethodMessages.Factory.newInstance().addNewMessage();
	xMessage.setText(message);
	return xMessage;
    }

    /**
     * Retrieves the given error message as an {@link XMessage} fragment.
     * 
     * @param errorMessage
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XMessage writeErrorMessage(String errorMessage) {
	checkNotNull(errorMessage);
	final XMessage xMessage = XMethodMessages.Factory.newInstance().addNewErrorMessage();
	xMessage.setText(errorMessage);
	return xMessage;
    }

    /**
     * Retrieves the given log message as an {@link XMessage} fragment.
     * 
     * @param logMessage
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XMessage writeLogMessage(String logMessage) {
	checkNotNull(logMessage);
	final XMessage xMessage = XMethodMessages.Factory.newInstance().addNewLogMessage();
	xMessage.setText(logMessage);
	return xMessage;
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
    public Double readDouble(Collection<XValue> xValues, String contextMessageIfEmpty) throws InvalidInputException {
	checkNotNull(xValues);
	final XValue xValue = getUnique(xValues, contextMessageIfEmpty);
	if (xValue == null) {
	    return null;
	}
	return readDouble(xValue);
    }

    /**
     * <p>
     * Retrieves the boolean value embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the fragments contains a unique boolean value. In case of unexpected contents, this method
     * returns <code>null</code> or throws an exception depending on the strategy this object follows.
     * </p>
     * 
     * 
     * @param xMethodParameters
     *            not <code>null</code>.
     * @param contextMessageIfEmpty
     *            if not <code>null</code>, is used to give a more useful error message about the context when the given
     *            fragment contains an empty collection.
     * @return a boolean value, or <code>null</code> iff unexpected content has been read and this object follows a
     *         permissive strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Boolean readBoolean(XMethodParameters xMethodParameters, String contextMessageIfEmpty)
	    throws InvalidInputException {
	final List<XParameter> xParameterList = xMethodParameters.getParameterList();
	final XParameter xParameter = getUnique(xParameterList, contextMessageIfEmpty);
	return readBoolean(xParameter);
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
    public Double readDouble(XNumericValue xNumber) throws InvalidInputException {
	if (!xNumber.isSetReal() && !xNumber.isSetInteger()) {
	    error("Expected numeric value instead of " + xNumber + ".");
	    return null;
	}
	final double value;
	if (xNumber.isSetReal()) {
	    value = xNumber.getReal();
	} else {
	    value = xNumber.getInteger();
	}
	return Double.valueOf(value);
    }

    /**
     * <p>
     * Retrieves the boolean value embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the fragments contains a boolean value. In case of unexpected contents, this method returns
     * <code>null</code> or throws an exception depending on the strategy this object follows.
     * </p>
     * 
     * 
     * @param xParameter
     *            not <code>null</code>.
     * @return a boolean value, or <code>null</code> iff unexpected content has been read and this object follows a
     *         permissive strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Boolean readBoolean(XParameter xParameter) throws InvalidInputException {
	final XValue xValue = xParameter.getValue();
	if (xValue == null) {
	    error("Expected value at " + xParameter + ".");
	}
	return readBoolean(xValue);
    }

    /**
     * <p>
     * Retrieves the boolean value embedded in the given fragment.
     * </p>
     * <p>
     * This method expects the fragments contains a boolean value. In case of unexpected contents, this method returns
     * <code>null</code> or throws an exception depending on the strategy this object follows.
     * </p>
     * 
     * 
     * @param xValue
     *            not <code>null</code>.
     * @return a boolean value, or <code>null</code> iff unexpected content has been read and this object follows a
     *         permissive strategy.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public Boolean readBoolean(XValue xValue) throws InvalidInputException {
	if (!xValue.isSetBoolean()) {
	    error("Expected boolean value instead of " + xValue + ".");
	    return null;
	}
	final boolean value;
	value = xValue.getBoolean();
	return Boolean.valueOf(value);
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
    public String readLabel(XMethodParameters xMethodParameters, String contextMessageIfEmpty)
	    throws InvalidInputException {
	final List<XParameter> xParameterList = xMethodParameters.getParameterList();
	final XParameter xParameter = getUnique(xParameterList, contextMessageIfEmpty);
	return readLabel(xParameter);
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
    public String readLabel(XParameter xParameter) throws InvalidInputException {
	final XValue xValue = xParameter.getValue();
	if (xValue == null) {
	    error("Expected value at " + xParameter + ".");
	}
	return readLabel(xValue);
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
    public String readLabel(XValue xValue) throws InvalidInputException {
	final String label = xValue.getLabel();
	if (label == null || label.isEmpty()) {
	    error("Expected label at " + xValue + ".");
	    return null;
	}
	return label;
    }

    /**
     * Retrieves the given value as an XMCDA fragment.
     * 
     * @param value
     *            the value to write.
     * @return not <code>null</code>.
     */
    public XMethodParameters writeAsMethodParameters(double value) {
	final XMethodParameters xMethodParameters = XMCDA.Factory.newInstance().addNewMethodParameters();
	xMethodParameters.getParameterList().add(writeAsParameter(value));
	return xMethodParameters;
    }

    /**
     * Retrieves the given value as an XMCDA fragment.
     * 
     * @param value
     *            the value to write.
     * @return not <code>null</code>.
     */
    public XMethodParameters writeAsMethodParameters(String value) {
	checkNotNull(value);
	final XMethodParameters xMethodParameters = XMCDA.Factory.newInstance().addNewMethodParameters();
	xMethodParameters.getParameterList().add(writeAsParameter(value));
	return xMethodParameters;
    }

    /**
     * Retrieves the given value as an XMCDA fragment.
     * 
     * @param value
     *            the value to write.
     * @return not <code>null</code>.
     */
    public XParameter writeAsParameter(double value) {
	final XParameter xParameter = XMethodParameters.Factory.newInstance().addNewParameter();
	xParameter.addNewValue().setReal((float) value);
	return xParameter;
    }

    /**
     * Retrieves the given value as an XMCDA fragment.
     * 
     * @param value
     *            the value to write.
     * @return not <code>null</code>.
     */
    public XParameter writeAsParameter(String value) {
	checkNotNull(value);
	final XParameter xParameter = XMethodParameters.Factory.newInstance().addNewParameter();
	xParameter.addNewValue().setLabel(value);
	return xParameter;
    }

    /**
     * Retrieves the given messages as an {@link XMethodMessages} fragment.
     * 
     * @param messages
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XMethodMessages writeMessages(Collection<String> messages) {
	checkNotNull(messages);
	final XMethodMessages xMethodMessages = XMCDA.Factory.newInstance().addNewMethodMessages();
	for (String message : messages) {
	    final XMessage xMessage = writeMessage(message);
	    xMethodMessages.addNewMessage().set(xMessage);
	}
	return xMethodMessages;
    }

    /**
     * Retrieves the given error messages as an {@link XMethodMessages} fragment.
     * 
     * @param errorMessages
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XMethodMessages writeErrorMessages(Collection<String> errorMessages) {
	checkNotNull(errorMessages);
	final XMethodMessages xMethodMessages = XMCDA.Factory.newInstance().addNewMethodMessages();
	for (String message : errorMessages) {
	    final XMessage xMessage = writeMessage(message);
	    xMethodMessages.addNewErrorMessage().set(xMessage);
	}
	return xMethodMessages;
    }

    public List<String> readMessages(XMethodMessages xMethodMessages) {
	checkNotNull(xMethodMessages);
	final List<XMessage> logMessages = xMethodMessages.getLogMessageList();
	final List<XMessage> messages = xMethodMessages.getMessageList();
	final List<XMessage> errorMessages = xMethodMessages.getErrorMessageList();
	final LinkedList<XMessage> allMessages = Lists.newLinkedList();
	allMessages.addAll(logMessages);
	allMessages.addAll(messages);
	allMessages.addAll(errorMessages);
	return Lists.newLinkedList(Collections2.transform(allMessages, new Function<XMessage, String>() {
	    @Override
	    public String apply(XMessage input) {
		return input.getText();
	    }
	}));
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
    public Double readDouble(XMethodParameters xMethodParameters, String contextMessageIfEmpty)
	    throws InvalidInputException {
	final List<XParameter> xParameterList = xMethodParameters.getParameterList();
	final XParameter xParameter = getUnique(xParameterList, contextMessageIfEmpty);
	return readDouble(xParameter);
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
    public Double readDouble(XParameter xParameter) throws InvalidInputException {
	final XValue xValue = xParameter.getValue();
	if (xValue == null) {
	    error("Expected value at " + xParameter + ".");
	}
	return readDouble(xValue);
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
    public Double readDouble(XValue xValue) throws InvalidInputException {
	if (!xValue.isSetReal() && !xValue.isSetInteger()) {
	    error("Expected numeric value instead of " + xValue + ".");
	    return null;
	}
	final double value;
	if (xValue.isSetReal()) {
	    value = xValue.getReal();
	} else {
	    value = xValue.getInteger();
	}
	return Double.valueOf(value);
    }

}
