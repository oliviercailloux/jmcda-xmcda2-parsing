<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:f="FROM_NAMESPACE" version="1.0">
	<xsl:output method="xml" indent="yes" />
	<xsl:template match="*">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<xsl:apply-templates />
		</xsl:copy>
	</xsl:template>
	<xsl:template match="f:*">
		<xsl:variable name="var.foo" select="local-name()" />
		<xsl:element namespace="TO_NAMESPACE" name="{$var.foo}">
			<xsl:copy-of select="@*" />
			<xsl:apply-templates />
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>
