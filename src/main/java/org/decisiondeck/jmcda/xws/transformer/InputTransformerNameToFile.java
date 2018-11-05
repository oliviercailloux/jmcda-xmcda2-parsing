package org.decisiondeck.jmcda.xws.transformer;

import java.io.File;

import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;

import com.google.common.base.Preconditions;

class InputTransformerNameToFile implements FunctionWithInputCheck<String, File> {
    private final File m_inputDirectory;
    private boolean m_optional;

    public InputTransformerNameToFile(File inputDirectory, boolean optional) {
	Preconditions.checkNotNull(inputDirectory);
	m_inputDirectory = inputDirectory;
	m_optional = optional;
    }

    @Override
    public File apply(String input) throws InvalidInputException {
	final File file = new File(m_inputDirectory, input);
	if (!file.exists()) {
	    if (m_optional) {
		return null;
	    }
	    throw new InvalidInputException("Required input " + input + " not found.");
	}
	return file;
    }
}