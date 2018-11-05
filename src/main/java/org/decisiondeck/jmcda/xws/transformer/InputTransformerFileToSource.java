package org.decisiondeck.jmcda.xws.transformer;

import java.io.File;

import com.google.common.base.Function;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

class InputTransformerFileToSource implements Function<File, ByteSource> {
	public InputTransformerFileToSource() {
		/** Default public constructor. */
	}

	@Override
	public ByteSource apply(File file) {
		return file == null ? null : Files.asByteSource(file);
	}
}