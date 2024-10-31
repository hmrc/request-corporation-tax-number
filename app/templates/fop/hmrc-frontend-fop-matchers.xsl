<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:fox="http://xmlgraphics.apache.org/fop/extensions" version="2.0">
  <xsl:include href="*/CTUTRScheme-fop-attribute-sets.xsl"/>

  <xsl:variable name="hmrc-frontend-attributes" select="'*/CTUTRScheme-fop-attribute-sets.xsl'"/>

  <xsl:param name="attributeSets"
             select="document($hmrc-frontend-attributes)//xsl:attribute-set"/>
  <xsl:param name="log" select="false()"/>

  <xsl:key name="attr-by-name" match="attr" use="@name" />

  <xsl:template match="div[@class='print-document']" mode="pdf" priority="1">
    <fo:block xsl:use-attribute-sets="print-document" break-before="page">
      <xsl:apply-templates select="node()" mode="pdf"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="div[@class='govuk-list__row']" mode="pdf" priority="0.5">
    <fo:table-row xsl:use-attribute-sets="govuk-list__row">
      <xsl:comment>Applying govuk-list__row template</xsl:comment>
      <xsl:apply-templates select="node()" mode="pdf"/>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="*[self::td or self::h1 or self::h2 or self::h3 or self::p or self::strong or self::div or self::dt or self::dd]"
      mode="pdf"
      priority="0">
    <xsl:variable name="class" select="@class"/>
    <xsl:call-template name="log">
      <xsl:with-param name="message" select="concat('Processing node: ', name(), '[$class=&quot;', $class, '&quot;]', '&#xA;', '    Content: ', string(.))"/>
    </xsl:call-template>
    <xsl:call-template name="remapClass">
      <xsl:with-param name="class" select="$class"/>
      <xsl:with-param name="tag" select="name()"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="dl" mode="pdf" priority="0.5">
    <xsl:variable name="collectAttributes">
      <xsl:call-template name="combineAttributes">
        <xsl:with-param name="class" select="@class"/>
      </xsl:call-template>
    </xsl:variable>
    <fo:table table-layout="fixed" width="100%">
      <xsl:call-template name="applySortedAttributes">
        <xsl:with-param name="collectAttributes" select="$collectAttributes"/>
      </xsl:call-template>
      <fo:table-column column-width="proportional-column-width(50)"/>
      <fo:table-column column-width="proportional-column-width(50)"/>
      <fo:table-body>
        <xsl:apply-templates select="node()" mode="pdf"/>
      </fo:table-body>
    </fo:table>
  </xsl:template>

  <xsl:template match="div[contains(@class, 'govuk-grid-row')]" mode="pdf" priority="0.5">
    <xsl:variable name="collectAttributes">
      <xsl:call-template name="combineAttributes">
        <xsl:with-param name="class" select="@class"/>
      </xsl:call-template>
    </xsl:variable>
    <fo:block>
      <xsl:call-template name="applySortedAttributes">
        <xsl:with-param name="collectAttributes" select="$collectAttributes"/>
      </xsl:call-template>
      <fo:table table-layout="fixed" width="100%">
        <fo:table-column column-width="proportional-column-width(100)"/>
        <fo:table-body>
          <fo:table-row>
            <xsl:comment>Applying govuk-grid-row template</xsl:comment>
            <xsl:apply-templates select="node()" mode="pdf"/>
          </fo:table-row>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="div[@class='govuk-grid-column-full']" mode="pdf" priority="0.5">
    <fo:table-cell>
      <fo:block xsl:use-attribute-sets="govuk-grid-column-full">
        <xsl:comment>Applying govuk-grid-column-one-half template</xsl:comment>
        <xsl:apply-templates select="node()" mode="pdf"/>
      </fo:block>
    </fo:table-cell>
  </xsl:template>

  <xsl:template name="remapClass">
    <xsl:param name="class"/>
    <xsl:param name="tag"/>
    <xsl:variable name="collectAttributes">
      <xsl:call-template name="combineAttributes">
        <xsl:with-param name="class" select="$class"/>
        <xsl:with-param name="logAttributes" select="true()"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$tag = 'dt' or $tag = 'dd'">
        <fo:table-cell>
          <fo:block>
            <xsl:call-template name="applySortedAttributes">
              <xsl:with-param name="collectAttributes" select="$collectAttributes"/>
            </xsl:call-template>
            <xsl:apply-templates select="node()" mode="pdf"/>
          </fo:block>
        </fo:table-cell>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message><xsl:value-of select="$collectAttributes"/></xsl:message>
        <fo:block>
          <xsl:call-template name="applySortedAttributes">
            <xsl:with-param name="collectAttributes" select="$collectAttributes"/>
          </xsl:call-template>
          <xsl:apply-templates select="node()" mode="pdf"/>
        </fo:block>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="combineAttributes">
    <xsl:param name="class"/>
    <xsl:param name="logAttributes" select="false()"/>
    <xsl:for-each select="tokenize($class, ' ')">
      <xsl:variable name="currentClass" select="."/>
      <xsl:variable name="formatClassName" select="replace($currentClass, '!', 'i')"/>
      <xsl:if test="$logAttributes = true()">
        <xsl:call-template name="log">
          <xsl:with-param name="message" select="concat('    Attributes from Class: ', $formatClassName)"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:for-each select="$attributeSets[@name=$formatClassName]/xsl:attribute">
        <xsl:element name="attr">
          <xsl:attribute name="name">
            <xsl:value-of select="@name"/>
          </xsl:attribute>
          <xsl:attribute name="value">
            <xsl:value-of select="."/>
          </xsl:attribute>
        </xsl:element>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="applySortedAttributes">
    <xsl:param name="collectAttributes"/>
    <xsl:for-each select="$collectAttributes/attr[generate-id() = generate-id(key('attr-by-name', @name)[last()])]">
      <xsl:sort select="@name"/>
      <xsl:attribute name="{@name}">
        <xsl:value-of select="@value"/>
      </xsl:attribute>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="log">
    <xsl:param name="message"/>
    <xsl:if test="$log = true()">
      <xsl:message>
        <xsl:value-of select="$message"/>
      </xsl:message>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
